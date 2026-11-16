package com.lushprojects.circuitjs1.client;

import com.lushprojects.circuitjs1.client.util.Locale;

/** Solver measurements for components that may be selected after pausing. */
final class ComponentMeasurementHistory {
    private static final int VALUES_PER_COMPONENT = 5;
    private static final int MAX_SAMPLES = 16384;
    private static final int VALUE_BUDGET = 1024 * 1024;

    private CircuitElm[] elements;
    private double[] times;
    private double[] values;
    private int next, size;

    void clear() {
        size = next = 0;
    }

    void add(double time, CircuitElm[] elements) {
        if (this.elements != elements) {
            this.elements = elements;
            int stride = elements.length * VALUES_PER_COMPONENT;
            int capacity = Math.max(1, Math.min(MAX_SAMPLES, VALUE_BUDGET / stride));
            times = new double[capacity];
            values = new double[capacity * stride];
            clear();
        }
        if (size > 0 && time <= times[slot(size - 1)])
            clear();
        times[next] = time;
        int offset = next * elements.length * VALUES_PER_COMPONENT;
        for (CircuitElm elm : elements) {
            if (elm.getPostCount() > 0 && !elm.isGraphicElmt()) {
                values[offset] = elm.getCurrent();
                values[offset + 1] = elm.getVoltageDiff();
                values[offset + 2] = elm.getPower();
                values[offset + 3] = elm.canShowValueInScope(Scope.VAL_R) ? elm.getScopeValue(Scope.VAL_R)
                        : elm instanceof ResistorElm ? ((ResistorElm) elm).getResistance() : Double.NaN;
                values[offset + 4] = elm instanceof LampElm ? ((LampElm) elm).temp : Double.NaN;
            }
            offset += VALUES_PER_COMPONENT;
        }
        next = (next + 1) % times.length;
        size = Math.min(size + 1, times.length);
    }

    String[] getInfo(CircuitElm elm, double time, String name) {
        String[] info = new String[10];
        info[0] = name;
        if (elm.getPostCount() == 0 || elm.isGraphicElmt())
            return info;

        int index = 0;
        while (elements != null && index < elements.length && elements[index] != elm)
            index++;
        if (size == 0 || index == elements.length || time < times[slot(0)] || time > times[slot(size - 1)]) {
            info[1] = "No component data at cursor time";
            return info;
        }

        int lo = 0, hi = size - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (times[slot(mid)] < time)
                lo = mid + 1;
            else
                hi = mid;
        }
        if (lo > 0 && time - times[slot(lo - 1)] < times[slot(lo)] - time)
            lo--;
        int offset = (slot(lo) * elements.length + index) * VALUES_PER_COMPONENT;
        info[1] = "I = " + CircuitElm.getCurrentText(values[offset]);
        info[2] = "Vd = " + CircuitElm.getVoltageText(values[offset + 1]);
        int line = 3;
        if (Double.isFinite(values[offset + 3]))
            info[line++] = "R = " + CircuitElm.getUnitText(values[offset + 3], Locale.ohmString);
        info[line++] = "P = " + CircuitElm.getUnitText(values[offset + 2], "W");
        if (Double.isFinite(values[offset + 4]))
            info[line] = "T = " + ((int) values[offset + 4]) + " K";
        return info;
    }

    private int slot(int index) {
        return (next - size + index + times.length) % times.length;
    }
}
