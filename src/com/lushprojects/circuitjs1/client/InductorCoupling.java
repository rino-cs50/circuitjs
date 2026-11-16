package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/** Norton companion model for separate windings on one linear core. */
final class InductorCoupling {
    final ArrayList<InductorElm> windings = new ArrayList<InductorElm>();
    private double[][] conductance;
    private double[] history;
    private boolean trapezoidal;

    static boolean validCoefficient(double k) {
        // Perfect coupling makes the inductance matrix singular.
        return k >= 0 && k < 1;
    }

    static void prepare(Vector<CircuitElm> elements) {
        HashMap<Integer, InductorCoupling> groups = new HashMap<Integer, InductorCoupling>();
        for (CircuitElm element : elements) {
            if (!(element instanceof InductorElm))
                continue;
            InductorElm winding = (InductorElm) element;
            if (winding.coupling != null)
                winding.ind.setup(winding.inductance, winding.current, winding.flags);
            winding.coupling = null;
            if (winding.couplingId == 0)
                continue;
            InductorCoupling group = groups.get(winding.couplingId);
            if (group == null) {
                group = new InductorCoupling();
                groups.put(winding.couplingId, group);
            }
            group.windings.add(winding);
        }
        for (InductorCoupling group : groups.values()) {
            if (group.windings.size() >= 2) {
                for (InductorElm winding : group.windings)
                    winding.coupling = group;
            }
        }
    }

    static void clearSingletons(Vector<CircuitElm> elements) {
        HashMap<Integer, InductorElm> first = new HashMap<Integer, InductorElm>();
        for (CircuitElm element : elements) {
            if (element instanceof InductorElm) {
                InductorElm winding = (InductorElm) element;
                if (winding.couplingId > 0) {
                    if (first.containsKey(winding.couplingId))
                        first.put(winding.couplingId, null);
                    else
                        first.put(winding.couplingId, winding);
                }
            }
        }
        for (InductorElm winding : first.values()) {
            if (winding != null) {
                winding.couplingId = 0;
                winding.reverseDot = false;
            }
        }
    }

    void stamp(CirSim sim) {
        int count = windings.size();
        double k = windings.get(0).couplingCoefficient;
        trapezoidal = windings.get(0).ind.isTrapezoidal();
        conductance = new double[count][count];
        history = new double[count];
        for (int i = 0; i < count; i++) {
            InductorElm winding = windings.get(i);
            conductance[i][i] = winding.inductance;
            for (int j = 0; j < i; j++) {
                InductorElm other = windings.get(j);
                double mutual = k * Math.sqrt(winding.inductance) * Math.sqrt(other.inductance);
                conductance[i][j] = conductance[j][i] =
                    winding.reverseDot == other.reverseDot ? mutual : -mutual;
            }
        }
        CirSim.invertMatrix(conductance, count);
        double dt = trapezoidal ? sim.timeStep / 2 : sim.timeStep;
        for (int i = 0; i < count; i++) {
            int[] nodes = windings.get(i).nodes;
            for (int j = 0; j < count; j++) {
                int[] controls = windings.get(j).nodes;
                conductance[i][j] *= dt;
                if (i == j)
                    sim.stampConductance(nodes[0], nodes[1], conductance[i][j]);
                else
                    sim.stampVCCurrentSource(nodes[0], nodes[1], controls[0], controls[1], conductance[i][j]);
            }
            sim.stampRightSide(nodes[0]);
            sim.stampRightSide(nodes[1]);
        }
    }

    private double voltageContribution(int i) {
        double contribution = 0;
        for (int j = 0; j < windings.size(); j++) {
            double[] volts = windings.get(j).volts;
            contribution += conductance[i][j] * (volts[0] - volts[1]);
        }
        return contribution;
    }

    void startIteration() {
        for (int i = 0; i < windings.size(); i++)
            history[i] = windings.get(i).current + (trapezoidal ? voltageContribution(i) : 0);
    }

    void doStep(CirSim sim) {
        for (int i = 0; i < windings.size(); i++) {
            int[] nodes = windings.get(i).nodes;
            sim.stampCurrentSource(nodes[0], nodes[1], history[i]);
        }
    }

    void finishStep() {
        for (int i = 0; i < windings.size(); i++)
            windings.get(i).current = history[i] + voltageContribution(i);
    }
}
