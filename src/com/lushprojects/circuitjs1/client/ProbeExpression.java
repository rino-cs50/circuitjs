package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.Map;

/** Expressions are compiled once and read from the completed simulation step. */
final class ProbeExpression {
    static final int VOLTAGE = 0, CURRENT = 1, SCALAR = 2;

    static final String[] UNARY = {
        "abs", "sqrt", "cbrt", "sin", "cos", "tan", "asin", "acos", "atan", "cot",
        "sinh", "cosh", "tanh", "asinh", "acosh", "atanh", "exp", "exp10", "log",
        "log2", "log10", "floor", "ceil", "round", "rint", "sign", "isnan", "d", "d2"
    };
    static final String[] BINARY = {"atan2", "min", "max", "pow", "pwrs", "hypot"};
    static final String[] TERNARY = {"limit", "if"};

    interface Sample {
        double read();
    }

    final Sample sample;
    final int units;

    ProbeExpression(Sample sample, int units) {
        this.sample = sample;
        this.units = units;
    }

    static ProbeExpression compile(String text, Map<String, ProbeExpression> signals) {
        return compile(text, signals, () -> Double.NaN);
    }

    static ProbeExpression compile(String text, Map<String, ProbeExpression> signals, Sample clock) {
        return compilePlots(text, signals, clock, false).get(0).expression;
    }

    static final class Plot {
        final String text;
        final ProbeExpression expression;

        Plot(String text, ProbeExpression expression) {
            this.text = text;
            this.expression = expression;
        }
    }

    static ArrayList<Plot> compilePlots(String text, Map<String, ProbeExpression> signals,
                                       Sample clock, boolean allowMultiple) {
        if (text.length() > 256)
            throw new IllegalArgumentException("Expression is too long");
        Parser parser = new Parser(text, signals, clock);
        ArrayList<Plot> plots = new ArrayList<Plot>();
        do {
            int start = parser.pos;
            ProbeExpression result = parser.sum();
            plots.add(new Plot(text.substring(start, parser.pos).trim(), result));
        } while (allowMultiple && parser.take(','));
        if (parser.peek() != '\0')
            throw new IllegalArgumentException("Unexpected text at position " + (parser.pos + 1));
        return plots;
    }

    private static final class Parser {
        final String text;
        final Map<String, ProbeExpression> signals;
        final Sample clock;
        int pos;

        Parser(String text, Map<String, ProbeExpression> signals, Sample clock) {
            this.text = text;
            this.signals = signals;
            this.clock = clock;
        }

        char peek() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos)))
                pos++;
            return pos == text.length() ? '\0' : text.charAt(pos);
        }

        boolean take(char expected) {
            if (peek() != expected)
                return false;
            pos++;
            return true;
        }

        ProbeExpression sum() {
            ProbeExpression result = product();
            while (peek() == '+' || peek() == '-') {
                char op = text.charAt(pos++);
                ProbeExpression left = result, right = product();
                result = new ProbeExpression(() -> op == '+' ? left.sample.read() + right.sample.read()
                        : left.sample.read() - right.sample.read(), left.units == right.units ? left.units : SCALAR);
            }
            return result;
        }

        ProbeExpression product() {
            ProbeExpression result = signed();
            while ((peek() == '*' && !text.startsWith("**", pos)) || peek() == '/') {
                char op = text.charAt(pos++);
                ProbeExpression left = result, right = signed();
                int units = right.units == SCALAR ? left.units
                        : op == '*' && left.units == SCALAR ? right.units : SCALAR;
                result = new ProbeExpression(() -> op == '*' ? left.sample.read() * right.sample.read()
                        : left.sample.read() / right.sample.read(), units);
            }
            return result;
        }

        ProbeExpression signed() {
            if (take('+'))
                return signed();
            if (take('-')) {
                ProbeExpression inner = signed();
                return new ProbeExpression(() -> -inner.sample.read(), inner.units);
            }
            ProbeExpression base = atom();
            if (take('^') || takePower()) {
                ProbeExpression exponent = signed();
                return new ProbeExpression(() -> Math.pow(base.sample.read(), exponent.sample.read()), SCALAR);
            }
            return base;
        }

        boolean takePower() {
            peek();
            if (!text.startsWith("**", pos))
                return false;
            pos += 2;
            return true;
        }

        ProbeExpression atom() {
            if (take('(')) {
                ProbeExpression inner = sum();
                close();
                return inner;
            }
            char first = peek();
            if (first == 'V' || first == 'I') {
                pos++;
                if (!take('('))
                    throw new IllegalArgumentException("Expected ( after " + first);
                int end = text.indexOf(')', pos);
                if (end < 0)
                    throw new IllegalArgumentException("Missing )");
                String name = first + "(" + text.substring(pos, end).trim() + ")";
                pos = end + 1;
                ProbeExpression signal = signals.get(name);
                if (signal == null)
                    throw new IllegalArgumentException("Unknown signal " + name);
                return signal;
            }
            if (Character.isLetter(first)) {
                int start = pos++;
                while (pos < text.length() && Character.isLetterOrDigit(text.charAt(pos)))
                    pos++;
                String name = text.substring(start, pos);
                if (name.equals("pi") || name.equals("π"))
                    return new ProbeExpression(() -> Math.PI, SCALAR);
                if (name.equals("e"))
                    return new ProbeExpression(() -> Math.E, SCALAR);
                if (name.equals("t"))
                    return new ProbeExpression(clock, SCALAR);
                int arity = contains(UNARY, name) ? 1 : contains(BINARY, name) ? 2 : contains(TERNARY, name) ? 3 : 0;
                if (arity == 0)
                    throw new IllegalArgumentException("Unknown function " + name);
                if (!take('('))
                    throw new IllegalArgumentException("Expected ( after " + name);
                ProbeExpression a = sum();
                ProbeExpression b = arity >= 2 ? argument() : null;
                ProbeExpression c = arity == 3 ? argument() : null;
                close();
                return function(name, a, b, c, clock);
            }
            int start = pos;
            digits();
            if (pos < text.length() && text.charAt(pos) == '.') {
                pos++;
                digits();
            }
            if (pos > start && pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
                pos++;
                if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-'))
                    pos++;
                digits();
            }
            try {
                double value = Double.parseDouble(text.substring(start, pos));
                return new ProbeExpression(() -> value, SCALAR);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected a number, signal, or function at position " + (start + 1));
            }
        }

        void digits() {
            while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9')
                pos++;
        }

        ProbeExpression argument() {
            if (!take(','))
                throw new IllegalArgumentException("Expected another argument");
            return sum();
        }

        void close() {
            if (!take(')'))
                throw new IllegalArgumentException("Missing )");
        }
    }

    private static boolean contains(String[] names, String name) {
        for (String candidate : names)
            if (candidate.equals(name))
                return true;
        return false;
    }

    private static ProbeExpression function(String name, ProbeExpression a, ProbeExpression b,
                                            ProbeExpression c, Sample clock) {
        Sample x = a.sample;
        if (name.equals("d") || name.equals("d2"))
            return new ProbeExpression(derivative(x, clock, name.equals("d2")), SCALAR);
        if (name.equals("if"))
            return new ProbeExpression(() -> {
                double condition = x.read();
                return Double.isFinite(condition) ? (condition != 0 ? b.sample.read() : c.sample.read()) : Double.NaN;
            }, b.units == c.units ? b.units : SCALAR);
        if (name.equals("limit"))
            return new ProbeExpression(() -> Math.max(b.sample.read(), Math.min(c.sample.read(), x.read())),
                    a.units);
        if (b != null) {
            int units = (name.equals("min") || name.equals("max") || name.equals("hypot"))
                    && a.units == b.units ? a.units : SCALAR;
            return new ProbeExpression(() -> binary(name, x.read(), b.sample.read()), units);
        }
        int units = contains(new String[] {"abs", "floor", "ceil", "round", "rint"}, name) ? a.units : SCALAR;
        return new ProbeExpression(() -> unary(name, x.read()), units);
    }

    private static double unary(String name, double x) {
        switch (name) {
        case "abs": return Math.abs(x);
        case "sqrt": return Math.sqrt(x);
        case "cbrt": return Math.cbrt(x);
        case "sin": return Math.sin(x);
        case "cos": return Math.cos(x);
        case "tan": return Math.tan(x);
        case "asin": return Math.asin(x);
        case "acos": return Math.acos(x);
        case "atan": return Math.atan(x);
        case "cot": return 1 / Math.tan(x);
        case "sinh": return Math.sinh(x);
        case "cosh": return Math.cosh(x);
        case "tanh": return Math.tanh(x);
        case "asinh": {
            double y = Math.abs(x);
            return Math.copySign(y > 1e150 ? Math.log(y) + Math.log(2)
                    : Math.log1p(y + y * y / (1 + Math.hypot(y, 1))), x);
        }
        case "acosh":
            return x > 1e150 ? Math.log(x) + Math.log(2)
                    : Math.log1p(x - 1 + Math.sqrt(x - 1) * Math.sqrt(x + 1));
        case "atanh": return .5 * (Math.log1p(x) - Math.log1p(-x));
        case "exp": return Math.exp(x);
        case "exp10": return Math.pow(10, x);
        case "log": return Math.log(x);
        case "log2": return Math.log(x) / Math.log(2);
        case "log10": return Math.log10(x);
        case "floor": return Math.floor(x);
        case "ceil": return Math.ceil(x);
        case "round":
        case "rint": return Math.rint(x);
        case "sign": return Math.signum(x);
        case "isnan": return Double.isNaN(x) ? 1 : 0;
        default: throw new IllegalArgumentException(name);
        }
    }

    private static double binary(String name, double x, double y) {
        switch (name) {
        case "atan2": return Math.atan2(x, y);
        case "min": return Math.min(x, y);
        case "max": return Math.max(x, y);
        case "pow": return Math.pow(x, y);
        case "pwrs": return Math.signum(x) * Math.pow(Math.abs(x), y);
        case "hypot": return Math.hypot(x, y);
        default: throw new IllegalArgumentException(name);
        }
    }

    private static Sample derivative(Sample sample, Sample clock, boolean second) {
        return new Sample() {
            double lastTime = Double.NaN, lastValue = Double.NaN, result = Double.NaN;
            double lastStep = Double.NaN, lastSlope = Double.NaN;

            @Override public double read() {
                double time = clock.read();
                if (time != lastTime) {
                    double value = sample.read();
                    double step = time - lastTime;
                    double slope = step > 0 && Double.isFinite(value) && Double.isFinite(lastValue)
                            ? (value - lastValue) / step : Double.NaN;
                    result = second ? 2 * (slope - lastSlope) / (step + lastStep) : slope;
                    lastStep = step;
                    lastSlope = slope;
                    lastTime = time;
                    lastValue = value;
                }
                return result;
            }
        };
    }
}
