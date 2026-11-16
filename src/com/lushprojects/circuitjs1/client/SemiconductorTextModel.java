package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gwt.http.client.URL;
import com.google.gwt.storage.client.Storage;
import com.lushprojects.circuitjs1.client.util.Locale;

final class SemiconductorTextModel {
    private static final String PREFIX = "~text-model:";
    private static final String STORAGE_PREFIX = "circuitjs.semiconductorModels.v1.";
    final String name;
    private final String type;
    private String source;
    private final List<String> unsupported = new ArrayList<>();
    private final Map<String, Double> values = new LinkedHashMap<>();

    private SemiconductorTextModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    static String typeFor(Editable component) {
        if (component instanceof DiodeElm)
            return "D";
        if (component instanceof TransistorElm)
            return ((TransistorElm) component).pnp > 0 ? "NPN" : "PNP";
        if (component instanceof MosfetElm) {
            MosfetElm fet = (MosfetElm) component;
            if (fet instanceof JfetElm)
                return fet.pnp > 0 ? "NJF" : "PJF";
            return fet.pnp > 0 ? "NMOS" : "PMOS";
        }
        return null;
    }

    private static String scope(CircuitElm component) {
        return component.getDumpType() + "." + typeFor(component);
    }

    static String example(CircuitElm component) {
        String type = typeFor(component);
        String parameters;
        if (component instanceof DiodeElm)
            parameters = "IS=1e-14 N=1 RS=0" + (component instanceof ZenerElm ? " BV=5.1" : "");
        else if (component instanceof TransistorElm)
            parameters = "IS=1e-16 BF=100 VAF=80";
        else if (component instanceof JfetElm)
            parameters = "VTO=" + ("PJF".equals(type) ? "4" : "-4") + " BETA=1m";
        else
            parameters = "LEVEL=1 VTO=" + ("PMOS".equals(type) ? "-1.5" : "1.5") + " KP=20m";
        return ".model MyModel " + type + " (" + parameters + ")";
    }

    static String supportedParameters(String type) {
        switch (type) {
        case "D":
            return "IS N RS BV";
        case "NPN":
        case "PNP":
            return "IS BF NF VAF IKF ISE NE BR NR VAR IKR ISC NC";
        case "NJF":
        case "PJF":
            return "VTO BETA";
        default:
            return "LEVEL VTO KP";
        }
    }

    static SemiconductorTextModel parse(String source, String expectedType) {
        StringBuilder statement = new StringBuilder();
        boolean inParameters = false;
        for (String raw : source.split("\\r\\n|\\n|\\r")) {
            String line = raw.split("[;$]", 2)[0].trim();
            if (line.isEmpty() || line.startsWith("*"))
                continue;
            if (statement.length() > 0) {
                if (line.startsWith("+"))
                    line = line.substring(1).trim();
                else if (!inParameters && !"(".equals(line))
                    throw invalid("Enter one .model statement. Start continuation lines with +.");
                statement.append(' ');
            }
            statement.append(line);
            if (line.contains("("))
                inParameters = true;
            if (line.contains(")"))
                inParameters = false;
        }
        String[] fields = statement.toString().trim().split("\\s+", 3);
        if (fields.length < 3 || !".model".equalsIgnoreCase(fields[0])
                || !fields[1].matches("[A-Za-z0-9_][A-Za-z0-9_.+-]*"))
            throw invalid("Enter a SPICE .model statement with a model name and type.");
        String[] body = fields[2].split("[\\s(]+", 2);
        if (!expectedType.equalsIgnoreCase(body[0]))
            throw new IllegalArgumentException(Locale.LS("Expected model type") + " " + expectedType + ".");
        String parameters = fields[2].substring(body[0].length()).trim();
        if (parameters.startsWith("(") && parameters.endsWith(")"))
            parameters = parameters.substring(1, parameters.length() - 1).trim();
        if (parameters.contains("(") || parameters.contains(")"))
            throw invalid("Check the parentheses around the model parameters.");

        SemiconductorTextModel model = new SemiconductorTextModel(fields[1], expectedType);
        String supported = " " + supportedParameters(expectedType).toLowerCase() + " ";
        if (parameters.isEmpty())
            throw invalid("Enter at least one model parameter.");
        for (String assignment : parameters.replaceAll("\\s*=\\s*", "=").split("[\\s,]+")) {
            String[] pair = assignment.split("=", -1);
            if (pair.length != 2 || !pair[0].matches("[A-Za-z][A-Za-z0-9_]*") || pair[1].isEmpty())
                throw invalid("Enter each parameter as NAME=value.");
            String key = pair[0].toLowerCase();
            if (model.values.containsKey(key))
                throw new IllegalArgumentException(Locale.LS("Duplicate model parameter") + " " + pair[0] + ".");
            double value = NetlistImport.parseValue(pair[1], pair[0] + ": ");
            if (!supported.contains(" " + key + " ")) {
                model.unsupported.add(pair[0].toUpperCase());
                model.values.put(key, value);
                continue;
            }
            boolean positive = " is n bf nf ne br nr nc kp beta ".contains(" " + key + " ");
            boolean inverted = " vaf var ikf ikr ".contains(" " + key + " ");
            if (("level".equals(key) && value != 1) || (!"vto".equals(key) && value < 0)
                    || (positive && value == 0) || ("beta".equals(key) && Double.isInfinite(2 * value))
                    || (inverted && value > 0 && Double.isInfinite(1 / value)))
                throw new IllegalArgumentException(Locale.LS("Invalid model parameter value") + " " + pair[0] + ".");
            model.values.put(key, value);
        }
        if (model.values.size() == model.unsupported.size())
            throw invalid("Include at least one supported model parameter.");
        model.source = source;
        return model;
    }

    boolean isApproximation() {
        return !unsupported.isEmpty();
    }

    String unsupportedParameters() {
        return String.join(", ", unsupported);
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(Locale.LS(message));
    }

    private double value(String key, double fallback) {
        Double number = values.get(key);
        return number == null ? fallback : number;
    }

    private double inverse(String key) {
        double number = value(key, 0);
        return reciprocal(number);
    }

    private void validateFor(CircuitElm component) {
        if (component instanceof ZenerElm && value("bv", 0) <= 0)
            throw invalid("A Zener model needs a positive BV.");
    }

    private String statement() {
        StringBuilder result = new StringBuilder(".model ").append(name).append(' ').append(type).append(" (");
        for (Map.Entry<String, Double> entry : values.entrySet())
            result.append(' ').append(entry.getKey()).append('=').append(entry.getValue());
        return result.append(" )").toString();
    }

    private static String storageKey(CircuitElm component) {
        return STORAGE_PREFIX + scope(component);
    }

    private static String storedSource(String entry) {
        return entry.startsWith("@") ? URL.decodeQueryString(entry.substring(1)) : entry;
    }

    static List<SemiconductorTextModel> savedFor(CircuitElm component) {
        Storage storage = Storage.getLocalStorageIfSupported();
        List<SemiconductorTextModel> models = new ArrayList<>();
        String saved = storage == null ? null : storage.getItem(storageKey(component));
        if (saved != null) {
            for (String line : saved.split("\n")) {
                try {
                    SemiconductorTextModel model = parse(storedSource(line), typeFor(component));
                    model.validateFor(component);
                    if (!line.startsWith("@"))
                        model.source = model.formattedStatement();
                    models.add(model);
                } catch (IllegalArgumentException ignored) {
                    // Keep valid entries usable if a stored entry is damaged.
                }
            }
        }
        return models;
    }

    void saveFor(CircuitElm component, String replacedName) {
        validateFor(component);
        for (SemiconductorTextModel model : savedFor(component)) {
            if (name.equalsIgnoreCase(model.name) && !model.name.equalsIgnoreCase(replacedName))
                throw invalid("A model with this name is already saved for this component.");
        }
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage == null)
            throw invalid("Model storage is unavailable.");
        String key = storageKey(component);
        try {
            String saved = storage.getItem(key);
            StringBuilder entries = new StringBuilder();
            if (saved != null) {
                for (String entry : saved.split("\n")) {
                    if (replacedName != null && storedName(entry).equalsIgnoreCase(replacedName))
                        continue;
                    entries.append(entry).append('\n');
                }
            }
            entries.append('@').append(URL.encodeQueryString(source));
            storage.setItem(key, entries.toString());
        } catch (RuntimeException e) {
            throw invalid("The app could not save the model to local storage.");
        }
    }

    private static String storedName(String entry) {
        try {
            for (String raw : storedSource(entry).split("\\r\\n|\\n|\\r")) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("*") || line.startsWith(";") || line.startsWith("$"))
                    continue;
                String[] fields = line.split("\\s+", 3);
                return fields.length > 1 && ".model".equalsIgnoreCase(fields[0]) ? fields[1] : "";
            }
        } catch (IllegalArgumentException ignored) {
            // Preserve unreadable entries when updating other models.
        }
        return "";
    }

    String editorSource() {
        return source == null ? formattedStatement() : source;
    }

    private String formattedStatement() {
        StringBuilder text = new StringBuilder(".model ").append(name).append(' ').append(type).append(" (\n");
        for (Map.Entry<String, Double> entry : values.entrySet())
            text.append("+ ").append(entry.getKey().toUpperCase()).append('=').append(entry.getValue()).append('\n');
        return text.append(')').toString();
    }

    static String loadedTextName(CircuitElm component) {
        if (component instanceof DiodeElm) {
            String name = ((DiodeElm) component).modelName;
            return isTextModel(name) ? name : null;
        }
        if (component instanceof TransistorElm) {
            String name = ((TransistorElm) component).modelName;
            return isTextModel(name) ? name : null;
        }
        return ((MosfetElm) component).textModelName;
    }

    boolean isLoadedOn(CircuitElm component) {
        String loadedName = loadedTextName(component);
        if (!(component instanceof MosfetElm))
            return circuitName(component).equals(loadedName);
        MosfetElm fet = (MosfetElm) component;
        return name.equals(loadedName) && fet.vt == fetThreshold(fet) && fet.beta == fetBeta(fet);
    }

    static SemiconductorTextModel loadedOn(CircuitElm component, List<SemiconductorTextModel> saved) {
        String name = loadedTextName(component);
        if (name == null)
            return null;
        for (SemiconductorTextModel model : saved)
            if (model.isLoadedOn(component))
                return model;
        // Imported circuits can contain a model that is missing from the local library.
        SemiconductorTextModel loaded = new SemiconductorTextModel(displayName(name), typeFor(component));
        Map<String, Double> parameters = loaded.values;
        if (component instanceof DiodeElm) {
            DiodeModel diode = ((DiodeElm) component).model;
            parameters.put("is", diode.saturationCurrent);
            parameters.put("n", diode.emissionCoefficient);
            parameters.put("rs", diode.seriesResistance);
            parameters.put("bv", diode.breakdownVoltage);
        } else if (component instanceof TransistorElm) {
            TransistorElm bjt = (TransistorElm) component;
            TransistorModel model = bjt.model;
            parameters.put("is", model.satCur);
            parameters.put("bf", bjt.beta);
            parameters.put("nf", model.emissionCoeffF);
            parameters.put("vaf", reciprocal(model.invEarlyVoltF));
            parameters.put("ikf", reciprocal(model.invRollOffF));
            parameters.put("ise", model.BEleakCur);
            parameters.put("ne", model.leakBEemissionCoeff);
            parameters.put("br", model.betaR);
            parameters.put("nr", model.emissionCoeffR);
            parameters.put("var", reciprocal(model.invEarlyVoltR));
            parameters.put("ikr", reciprocal(model.invRollOffR));
            parameters.put("isc", model.BCleakCur);
            parameters.put("nc", model.leakBCemissionCoeff);
        } else {
            MosfetElm fet = (MosfetElm) component;
            parameters.put("vto", fet.pnp * fet.vt);
            if (fet instanceof JfetElm)
                parameters.put("beta", fet.beta / 2);
            else
                parameters.put("kp", fet.beta);
        }
        return loaded;
    }

    private static double reciprocal(double value) {
        return value == 0 ? 0 : 1 / value;
    }

    static boolean isTextModel(String name) {
        return name != null && name.startsWith(PREFIX);
    }

    static String displayName(String name) {
        return isTextModel(name) ? name.substring(name.lastIndexOf(':') + 1) : name;
    }

    private String circuitName(CircuitElm component) {
        return PREFIX + scope(component) + ":" + statement().hashCode() + ":" + name;
    }

    private double fetThreshold(MosfetElm fet) {
        return fet.pnp * value("vto", fet.pnp * fet.getDefaultThreshold());
    }

    private double fetBeta(MosfetElm fet) {
        return fet instanceof JfetElm
            ? 2 * value("beta", fet.getBackwardCompatibilityBeta() / 2)
            : value("kp", fet.getBackwardCompatibilityBeta());
    }

    void applyTo(CircuitElm component) {
        // Keep definitions in circuit exports independently of the saved library.
        String circuitName = circuitName(component);
        if (component instanceof DiodeElm) {
            DiodeModel diode = new DiodeModel(value("is", 1e-14), value("rs", 0),
                value("n", 1), value("bv", 0), null);
            diode.name = circuitName;
            DiodeModel.createModelMap();
            if (!DiodeModel.modelMap.containsKey(circuitName))
                DiodeModel.modelMap.put(circuitName, diode);
            ((DiodeElm) component).newModelCreated(DiodeModel.modelMap.get(circuitName));
        } else if (component instanceof TransistorElm) {
            TransistorModel transistor = new TransistorModel(null, value("is", 1e-16));
            transistor.name = circuitName;
            transistor.emissionCoeffF = value("nf", 1);
            transistor.invEarlyVoltF = inverse("vaf");
            transistor.invRollOffF = inverse("ikf");
            transistor.BEleakCur = value("ise", 0);
            transistor.leakBEemissionCoeff = value("ne", 1.5);
            transistor.betaR = value("br", 1);
            transistor.emissionCoeffR = value("nr", 1);
            transistor.invEarlyVoltR = inverse("var");
            transistor.invRollOffR = inverse("ikr");
            transistor.BCleakCur = value("isc", 0);
            transistor.leakBCemissionCoeff = value("nc", 2);
            transistor.updateModel();
            TransistorModel.createModelMap();
            if (!TransistorModel.modelMap.containsKey(circuitName))
                TransistorModel.modelMap.put(circuitName, transistor);
            TransistorElm bjt = (TransistorElm) component;
            bjt.setBeta(value("bf", 100));
            bjt.newModelCreated(TransistorModel.modelMap.get(circuitName));
        } else {
            MosfetElm fet = (MosfetElm) component;
            fet.textModelName = name;
            fet.vt = fetThreshold(fet);
            fet.beta = fetBeta(fet);
        }
    }
}
