package com.lushprojects.circuitjs1.client;

/** Time-weighted measurements of the sampled segment shown in the waveform viewer. */
final class WaveformMeasurements {
    private double duration, integral, squares;
    private final double peak;
    private int cycles;
    private double cycleTime, highTime;
    private double shortest = Double.POSITIVE_INFINITY, longest;

    WaveformMeasurements(WaveformHistory history, int first, int end, double low, double high, boolean measureCycles) {
        peak = Math.max(Math.abs(low), Math.abs(high));
        if (!Double.isFinite(peak))
            return;
        double midpoint = low / 2 + high / 2;
        double rising = Double.NaN, falling = Double.NaN;
        for (int i = first + 1; i < end; i++) {
            double a = history.value(i - 1), b = history.value(i);
            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                rising = falling = Double.NaN;
                continue;
            }
            double start = history.time(i - 1);
            double dt = history.time(i) - start;
            if (dt <= 0)
                continue;
            duration += dt;
            if (peak == 0)
                continue;
            double x = a / peak, y = b / peak;
            integral += dt * (x + y) / 2;
            squares += dt * (x * x + x * y + y * y) / 3;
            if (!measureCycles || low == high)
                continue;
            boolean rise = a < midpoint && b >= midpoint;
            boolean fall = a >= midpoint && b < midpoint;
            if (!rise && !fall)
                continue;
            double at = start + dt * ((midpoint / peak - x) / (y - x));
            if (rise) {
                if (Double.isFinite(falling) && at > rising) {
                    double period = at - rising;
                    cycles++;
                    cycleTime += period;
                    highTime += falling - rising;
                    shortest = Math.min(shortest, period);
                    longest = Math.max(longest, period);
                }
                rising = at;
                falling = Double.NaN;
            } else if (Double.isFinite(rising)) {
                falling = at;
            }
        }
    }

    double average() {
        return duration > 0 ? peak * (integral / duration) : Double.NaN;
    }

    double rms() {
        return duration > 0 ? peak * Math.sqrt(squares / duration) : Double.NaN;
    }

    double frequency() {
        return cycles > 0 && longest <= 1.2 * shortest ? cycles / cycleTime : Double.NaN;
    }

    double dutyCycle() {
        return Double.isFinite(frequency()) ? highTime / cycleTime : Double.NaN;
    }
}
