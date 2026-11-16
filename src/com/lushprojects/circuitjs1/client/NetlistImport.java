package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/** Converts a small SPICE-style netlist into CircuitJS schematic records. */
final class NetlistImport {
    private NetlistImport() {
    }

    static boolean looksLikeCircuitJS(String text) {
        String first = text.trim().split("\\r\\n|\\r|\\n", 2)[0].trim();
        return first.matches("[$!%.?&]\\s+.*")
            || first.matches("(?:[A-Za-z]|[0-9]+)(?:\\s+-?[0-9]+){5}(?:\\s+.*)?");
    }

    static String toCircuit(String netlist, boolean hasTitle) {
        ArrayList<SourceLine> top = new ArrayList<SourceLine>();
        HashMap<String, Subcircuit> definitions = new HashMap<String, Subcircuit>();
        Subcircuit current = null;
        boolean ended = false;
        String[] lines = netlist.split("\\r\\n|\\r|\\n");
        for (int index = hasTitle ? 1 : 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty() || line.startsWith("*"))
                continue;
            int comment = line.indexOf(';');
            if (comment >= 0)
                line = line.substring(0, comment).trim();
            if (line.isEmpty())
                continue;

            String location = "Line " + (index + 1) + ". ";
            if (ended)
                throw new IllegalArgumentException(location + "Unexpected text after .end.");
            if (line.equalsIgnoreCase(".end")) {
                if (current != null)
                    throw new IllegalArgumentException(current.header.location + "Missing .ends.");
                ended = true;
                continue;
            }
            String[] fields = line.split("\\s+");
            String name = fields[0];
            if (name.equalsIgnoreCase(".subckt")) {
                if (current != null || fields.length < 3 || !fields[1].matches("[A-Za-z0-9_.-]+"))
                    throw new IllegalArgumentException(location + "Use .subckt NAME PIN [PIN ...] outside another definition.");
                current = new Subcircuit(new SourceLine(fields, location));
                String key = normalize(fields[1]);
                if (definitions.put(key, current) != null)
                    throw new IllegalArgumentException(location + "Duplicate subcircuit " + fields[1] + ".");
                for (int pin = 2; pin < fields.length; pin++) {
                    String node = nodeName(fields[pin], location);
                    if (current.pins.contains(node) || node.equals("0"))
                        throw new IllegalArgumentException(location + "Invalid or repeated pin " + fields[pin] + ".");
                    current.pins.add(node);
                }
                continue;
            }
            if (name.equalsIgnoreCase(".ends")) {
                if (current == null || fields.length > 2 || (fields.length == 2
                        && !fields[1].equalsIgnoreCase(current.header.fields[1])))
                    throw new IllegalArgumentException(location + ".ends must match an open subcircuit.");
                current = null;
                continue;
            }
            if (name.equalsIgnoreCase(".title") || line.equalsIgnoreCase(".op"))
                continue;
            if (name.startsWith("."))
                throw new IllegalArgumentException(location + "Unsupported ngspice directive " + name + ".");
            (current == null ? top : current.body).add(new SourceLine(fields, location));
        }
        if (current != null)
            throw new IllegalArgumentException(current.header.location + "Missing .ends.");
        if (top.isEmpty())
            throw new IllegalArgumentException("Enter at least one component.");
        StringBuilder models = new StringBuilder();
        ArrayList<Part> parts = parseParts(top, definitions, models, new HashSet<String>(), null);
        StringBuilder schematic = new StringBuilder();
        boolean hasGround = false;
        int y = 64;
        for (Part part : parts) {
            if (part.model == null) {
                boolean voltage = part.type == 'v';
                schematic.append(part.type).append(voltage ? " 256 " : " 64 ").append(y)
                    .append(voltage ? " 64 " : " 256 ").append(y).append(' ').append(part.state);
                boolean numericReference = part.name.matches("[A-Za-z][1-9][0-9]{0,8}");
                if (numericReference)
                    schematic.append(" @ref=").append(part.name.substring(1));
                schematic.append('\n');
                appendLabel(schematic, part.nodes[voltage ? 1 : 0], 64, y, 64, y - 32);
                appendLabel(schematic, part.nodes[voltage ? 0 : 1], 256, y, 256, y - 32);
                hasGround |= part.nodes[0].equals("0") || part.nodes[1].equals("0");
                if (!numericReference)
                    appendCaption(schematic, part.name, 80, y + 32);
                y += 96;
            } else {
                Subcircuit model = part.model;
                int left = 64;
                int right = left + (model.width + 1) * 32;
                schematic.append("410 ").append(left).append(' ').append(y).append(' ')
                    .append(right).append(' ').append(y + (model.rows - 1) * 32)
                    .append(" 1 ").append(part.state).append('\n');
                for (int pin = 0; pin < part.nodes.length; pin++) {
                    boolean west = pin % 2 == 0;
                    int x = west ? left : right;
                    int pinY = y + (pin / 2) * 32;
                    appendLabel(schematic, part.nodes[pin], x, pinY, x + (west ? -32 : 32), pinY);
                    hasGround |= part.nodes[pin].equals("0");
                }
                appendCaption(schematic, part.name, left + 32, y + model.rows * 32 + 16);
                hasGround |= model.grounded;
                y += model.rows * 32 + 96;
            }
        }
        if (!hasGround)
            throw new IllegalArgumentException("Use node 0 for ground.");
        // Keep CircuitJS from implicitly grounding the first voltage source
        // when the only SPICE ground is inside a subcircuit.
        schematic.append("g 64 ").append(y).append(" 64 ").append(y + 16).append(" 0 0\n");
        if (schematic.indexOf(" 4 0\n") >= 0)
            appendLabel(schematic, "0", 64, y, 64, y - 32);
        return models.append(schematic).toString();
    }

    private static void appendLabel(StringBuilder out, String net, int x, int y, int endX, int endY) {
        out.append("207 ").append(x).append(' ').append(y).append(' ').append(endX).append(' ')
            .append(endY).append(" 4 ").append(net).append('\n');
    }

    private static void appendCaption(StringBuilder out, String name, int x, int y) {
        out.append("x ").append(x).append(' ').append(y).append(' ').append(x + 16)
            .append(' ').append(y).append(" 4 14 ").append(CustomLogicModel.escape(name)).append('\n');
    }

    private static ArrayList<Part> parseParts(ArrayList<SourceLine> body, HashMap<String, Subcircuit> definitions,
            StringBuilder models, HashSet<String> active, Subcircuit parent) {
        ArrayList<Part> parts = new ArrayList<Part>();
        HashSet<String> names = new HashSet<String>();
        int expandedCount = 0;
        ArrayList<SourceLine> couplings = new ArrayList<SourceLine>();
        HashMap<String, Part> partsByName = new HashMap<String, Part>();
        for (SourceLine line : body) {
            String[] fields = line.fields;
            String name = fields[0];
            if (!name.matches("[A-Za-z][A-Za-z0-9_.-]*"))
                throw new IllegalArgumentException(line.location + "Invalid component name " + name + ".");
            if (!names.add(normalize(name)))
                throw new IllegalArgumentException(line.location + "Duplicate component " + name + ".");
            char type = Character.toLowerCase(name.charAt(0));
            if (type == 'k') {
                couplings.add(line);
                continue;
            }
            Part part;
            if (type == 'x') {
                String target = normalize(fields[fields.length - 1]);
                Subcircuit model = definitions.get(target);
                if (model == null)
                    throw new IllegalArgumentException(line.location + "Unknown subcircuit " + target + ".");
                if (model.pins.size() != fields.length - 2)
                    throw new IllegalArgumentException(line.location + name + " needs " + model.pins.size() + " node connections.");
                compileModel(model, definitions, models, active, line.location);
                String[] nodes = new String[model.pins.size()];
                for (int pin = 0; pin < nodes.length; pin++)
                    nodes[pin] = resolveNode(fields[pin + 1], line.location, parent);
                part = new Part(name, type, "CustomCompositeElm:" + model.name, model.state, nodes, model);
                expandedCount += model.expandedCount;
            } else {
                part = primitive(line, type, parent);
                expandedCount++;
            }
            if (expandedCount > 1000)
                throw new IllegalArgumentException(line.location + "The expanded circuit exceeds 1,000 components.");
            parts.add(part);
            partsByName.put(normalize(name), part);
        }
        int couplingId = 0;
        for (SourceLine line : couplings) {
            String[] fields = line.fields;
            if (fields.length < 4)
                throw new IllegalArgumentException(line.location + "Use KNAME L1 L2 [L3 ...] COEFFICIENT.");
            double k = parseValue(fields[fields.length - 1], line.location);
            if (!InductorCoupling.validCoefficient(k))
                throw new IllegalArgumentException(line.location + "Coupling coefficient must be at least 0 and below 1.");
            couplingId++;
            for (int i = 1; i < fields.length - 1; i++) {
                Part winding = partsByName.get(normalize(fields[i]));
                if (winding == null || winding.type != 'l')
                    throw new IllegalArgumentException(line.location + "Unknown inductor " + fields[i] + ".");
                if (winding.coupled)
                    throw new IllegalArgumentException(line.location + fields[i] + " already belongs to a coupling group.");
                winding.coupled = true;
                winding.state += " coupled " + couplingId + " " + k + " 0";
            }
        }
        if (parent != null)
            parent.expandedCount = expandedCount;
        return parts;
    }

    private static Part primitive(SourceLine line, char type, Subcircuit parent) {
        String[] fields = line.fields;
        if ("rclvi".indexOf(type) < 0)
            throw new IllegalArgumentException(line.location + "Expected an R, C, L, V, I, or X component.");
        boolean source = type == 'v' || type == 'i';
        boolean explicitDc = source && fields.length == 5 && fields[3].equalsIgnoreCase("DC");
        if (fields.length != 4 && !explicitDc)
            throw new IllegalArgumentException(line.location + "Use NAME NODE1 NODE2 VALUE. Sources may include DC before the value.");
        double value = parseValue(fields[fields.length - 1], line.location);
        if (!source && value <= 0)
            throw new IllegalArgumentException(line.location + "R, C, and L values must be positive.");
        String first = resolveNode(fields[1], line.location, parent);
        String second = resolveNode(fields[2], line.location, parent);
        // CircuitJS measures voltage from the second post relative to the first.
        String[] nodes = type == 'v' ? new String[] {second, first} : new String[] {first, second};
        String className;
        String state = "0 " + value;
        switch (type) {
        case 'v': className = "VoltageElm"; state = "0 0 40 " + value + " 0 0 0.5"; break;
        case 'i': className = "CurrentElm"; break;
        case 'c': className = "CapacitorElm"; state += " 0 0"; break;
        case 'l': className = "InductorElm"; state += " 0 0"; break;
        default: className = "ResistorElm"; break;
        }
        return new Part(fields[0], type, className, state, nodes, null);
    }

    private static void compileModel(Subcircuit model, HashMap<String, Subcircuit> definitions,
            StringBuilder models, HashSet<String> active, String location) {
        if (model.state != null)
            return;
        String base = model.header.fields[1];
        String key = normalize(base);
        if (active.size() >= 64 || !active.add(key))
            throw new IllegalArgumentException(location + "Recursive or too deeply nested subcircuit " + base + ".");
        ArrayList<Part> parts = parseParts(model.body, definitions, models, active, model);
        active.remove(key);

        HashMap<String, Integer> netIds = new HashMap<String, Integer>();
        StringBuilder topology = new StringBuilder();
        StringBuilder states = new StringBuilder();
        for (Part part : parts) {
            topology.append(part.className);
            for (String node : part.nodes) {
                if (!netIds.containsKey(node))
                    netIds.put(node, netIds.size() + 1);
                topology.append(' ').append(netIds.get(node));
            }
            topology.append('\r');
            if (states.length() > 0)
                states.append(' ');
            String state = part.model == null ? part.state : "1 " + part.state;
            states.append(CustomLogicModel.escape(state));
            model.grounded |= part.model != null && part.model.grounded;
        }
        if (netIds.containsKey("0")) {
            topology.append("GroundElm ").append(netIds.get("0")).append('\r');
            states.append(' ').append(CustomLogicModel.escape("1 0"));
            model.grounded = true;
        }

        model.rows = (model.pins.size() + 1) / 2;
        int longestPin = 0;
        StringBuilder pins = new StringBuilder();
        for (int pin = 0; pin < model.pins.size(); pin++) {
            String name = model.pins.get(pin);
            Integer node = netIds.get(name);
            if (node == null)
                throw new IllegalArgumentException(model.header.location + "Unconnected subcircuit pin " + name + ".");
            longestPin = Math.max(longestPin, name.length());
            pins.append(' ').append(name).append(' ').append(node).append(' ').append(pin / 2)
                .append(' ').append(pin % 2 == 0 ? ChipElm.SIDE_W : ChipElm.SIDE_E);
        }
        model.width = Math.max(5, 2 + (longestPin * 2 + base.length()) / 3);
        String definition = CustomCompositeModel.FLAG_SHOW_LABEL + " " + model.width + " " + model.rows
            + " " + model.pins.size() + pins + " " + CustomLogicModel.escape(topology.toString())
            + " " + CustomLogicModel.escape(states.toString());
        String name = base;
        for (int suffix = 2; ; suffix++) {
            CustomCompositeModel existing = CustomCompositeModel.getModelWithName(name);
            if (existing == null || matches(existing, model, topology.toString(), states.toString(), netIds))
                break;
            name = base + "-" + suffix;
            // Never claim another definition's original name.
            while (definitions.containsKey(normalize(name)))
                name = base + "-" + ++suffix;
        }
        // Different definitions in this import may otherwise claim the same suffixed name.
        while (models.indexOf(". " + name + " ") >= 0)
            name += "-import";
        model.name = name;
        model.state = CustomLogicModel.escape(name) + " " + states;
        models.append(". ").append(CustomLogicModel.escape(name)).append(' ').append(definition).append('\n');
    }

    private static boolean matches(CustomCompositeModel existing, Subcircuit model, String topology,
            String states, HashMap<String, Integer> netIds) {
        if (existing.modelCircuit != null || existing.builtin || existing.internal
                || existing.flags != CustomCompositeModel.FLAG_SHOW_LABEL || existing.sizeX != model.width
                || existing.sizeY != model.rows || !topology.equals(existing.nodeList)
                || !states.equals(existing.elmDump) || existing.extList.size() != model.pins.size())
            return false;
        for (int pin = 0; pin < model.pins.size(); pin++) {
            ExtListEntry entry = existing.extList.get(pin);
            String name = model.pins.get(pin);
            if (!name.equals(entry.name) || entry.node != netIds.get(name) || entry.pos != pin / 2
                    || entry.side != (pin % 2 == 0 ? ChipElm.SIDE_W : ChipElm.SIDE_E))
                return false;
        }
        return true;
    }

    private static String resolveNode(String value, String location, Subcircuit parent) {
        String node = nodeName(value, location);
        return node.equals("GND") && (parent == null || !parent.pins.contains(node)) ? "0" : node;
    }

    private static class Part {
        final String name, className;
        String state;
        boolean coupled;
        final char type;
        final String[] nodes;
        final Subcircuit model;

        Part(String name, char type, String className, String state, String[] nodes, Subcircuit model) {
            this.name = name;
            this.type = type;
            this.className = className;
            this.state = state;
            this.nodes = nodes;
            this.model = model;
        }
    }

    private static String nodeName(String value, String location) {
        String node = normalize(value);
        if (!node.matches("[A-Z0-9_./-]+"))
            throw new IllegalArgumentException(location + "Invalid node name " + value + ".");
        return node;
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    private static class SourceLine {
        final String[] fields;
        final String location;

        SourceLine(String[] fields, String location) {
            this.fields = fields;
            this.location = location;
        }
    }

    private static class Subcircuit {
        String name, state;
        int width, rows, expandedCount;
        boolean grounded;
        final SourceLine header;
        final ArrayList<String> pins = new ArrayList<String>();
        final ArrayList<SourceLine> body = new ArrayList<SourceLine>();

        Subcircuit(SourceLine header) {
            this.header = header;
        }
    }

    static double parseValue(String token, String location) {
        String value = token.toLowerCase(Locale.ROOT);
        String number = value.replaceFirst("[a-z]+$", "");
        String letters = value.substring(number.length());
        String suffix = letters.startsWith("meg") || letters.startsWith("mil")
            ? letters.substring(0, 3) : letters.substring(0, Math.min(1, letters.length()));
        double multiplier = 1;
        switch (suffix) {
        case "t": multiplier = 1e12; break;
        case "g": multiplier = 1e9; break;
        case "meg": multiplier = 1e6; break;
        case "mil": multiplier = 25.4e-6; break;
        case "k": multiplier = 1e3; break;
        case "m": multiplier = 1e-3; break;
        case "u": multiplier = 1e-6; break;
        case "n": multiplier = 1e-9; break;
        case "p": multiplier = 1e-12; break;
        case "f": multiplier = 1e-15; break;
        case "a": multiplier = 1e-18; break;
        }
        if (number.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?")) {
            double result = Double.parseDouble(number) * multiplier;
            if (!Double.isInfinite(result) && !Double.isNaN(result))
                return result;
        }
        throw new IllegalArgumentException(location + "Invalid value " + token + ".");
    }
}
