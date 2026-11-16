package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.function.DoubleSupplier;

/** A simulation-time exponential average of the losses represented by each model. */
final class AveragePowerLoss {
    private static final double TIME_CONSTANT = 0.1;
    private final ArrayList<Sample> samples = new ArrayList<>();
    private final IdentityHashMap<CircuitElm, Sample> byElement = new IdentityHashMap<>();
    private CircuitElm[] elements;
    private double lastStep;
    private double blend;

    void clear() {
        samples.clear();
        byElement.clear();
        elements = null;
    }

    void add(CircuitElm[] elements, double timeStep) {
        if (this.elements != elements) {
            clear();
            this.elements = elements;
            for (CircuitElm element : elements) {
                DoubleSupplier loss = lossModel(element);
                if (loss != null) {
                    Sample sample = new Sample(loss);
                    samples.add(sample);
                    byElement.put(element, sample);
                }
            }
        }
        if (timeStep != lastStep) {
            blend = 1 - Math.exp(-timeStep / TIME_CONSTANT);
            lastStep = timeStep;
        }
        for (int i = 0; i < samples.size(); i++)
            samples.get(i).add(blend);
    }

    double get(CircuitElm element) {
        Sample sample = byElement.get(element);
        return sample == null ? Double.NaN : sample.get();
    }

    static boolean supports(CircuitElm element) {
        return lossModel(element) != null;
    }

    private static DoubleSupplier lossModel(CircuitElm element) {
        if (element instanceof CapacitorElm) {
            CapacitorElm capacitor = (CapacitorElm) element;
            return () -> capacitor.getCurrent() * capacitor.getCurrent()
                    * Math.max(0, capacitor.getSeriesResistance());
        }
        if (element instanceof AnalogSwitchElm) {
            AnalogSwitchElm analogSwitch = (AnalogSwitchElm) element;
            return analogSwitch::getPowerLoss;
        }
        if (element instanceof RelayCoilElm) {
            RelayCoilElm coil = (RelayCoilElm) element;
            return () -> coil.coilCurrent * coil.coilCurrent * coil.coilR;
        }
        if (element instanceof InductorElm || element instanceof SwitchElm)
            return () -> 0;
        if (element instanceof LEDElm || element instanceof VaractorElm)
            return null;
        if (element instanceof ResistorElm || element instanceof LampElm || element instanceof LDRElm
                || element instanceof ThermistorNTCElm || element instanceof MemristorElm
                || element instanceof DiodeElm || element instanceof TransistorElm || element instanceof MosfetElm)
            return element::getPower;
        return null;
    }

    private static final class Sample {
        private final DoubleSupplier loss;
        private double value;
        private double weight;

        Sample(DoubleSupplier loss) {
            this.loss = loss;
        }

        void add(double blend) {
            double watts = loss.getAsDouble();
            if (Double.isFinite(watts)) {
                value += blend * (Math.max(0, watts) - value);
                weight += blend * (1 - weight);
            }
        }

        double get() {
            return weight > 0 ? value / weight : Double.NaN;
        }
    }
}
