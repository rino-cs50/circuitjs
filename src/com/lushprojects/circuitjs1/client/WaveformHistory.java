package com.lushprojects.circuitjs1.client;

/** Timestamped samples retained independently of panel size and zoom. */
final class WaveformHistory {
    private static final int CAPACITY = 16384;
    private final double[] time = new double[CAPACITY];
    private final double[] value = new double[CAPACITY];
    private int next;
    int size;

    void add(double t, double v) {
        if (size > 0 && t < time(size - 1))
            clear();
        time[next] = t;
        value[next] = v;
        next = (next + 1) % CAPACITY;
        size = Math.min(size + 1, CAPACITY);
    }

    void clear() {
        size = next = 0;
    }

    double time(int index) {
        return time[(next - size + index + CAPACITY) % CAPACITY];
    }

    double value(int index) {
        return value[(next - size + index + CAPACITY) % CAPACITY];
    }

    int firstAtOrAfter(double t) {
        int lo = 0, hi = size;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (time(mid) < t)
                lo = mid + 1;
            else
                hi = mid;
        }
        return lo;
    }

    int endAt(double t) {
        int index = firstAtOrAfter(t);
        return index < size && time(index) == t ? index + 1 : index;
    }
}
