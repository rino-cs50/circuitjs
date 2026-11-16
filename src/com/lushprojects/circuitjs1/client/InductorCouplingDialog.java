package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.lushprojects.circuitjs1.client.util.Locale;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

final class InductorCouplingDialog extends Dialog {
    private final CirSim sim;
    private final int groupId;
    private final ArrayList<WindingRow> rows = new ArrayList<WindingRow>();
    private final TextBox coefficient = new TextBox();
    private final Checkbox trapezoidal = new Checkbox("Trapezoidal Approximation");
    private final Label error = new Label();

    InductorCouplingDialog(CirSim sim, InductorElm target) {
        this.sim = sim;
        groupId = target.couplingId;
        closeOnEnter = false;
        setText(groupId > 0 ? Locale.LS("Edit Coupling") + " K" + groupId : Locale.LS("Couple Inductors"));
        VerticalPanel panel = new VerticalPanel();
        panel.setSpacing(6);
        setWidget(panel);

        panel.add(new Label(Locale.LS("Coupling coefficient (0 ≤ k < 1)")));
        coefficient.setText(groupId > 0 ? Double.toString(target.couplingCoefficient) : "0.999");
        coefficient.getElement().setAttribute("aria-label", Locale.LS("Coupling coefficient"));
        panel.add(coefficient);
        trapezoidal.setValue(target.ind.isTrapezoidal());
        panel.add(trapezoidal);

        VerticalPanel windings = new VerticalPanel();
        for (CircuitElm element : sim.elmList) {
            if (!(element instanceof InductorElm))
                continue;
            InductorElm winding = (InductorElm) element;
            if (winding.couplingId > 0 && winding.couplingId != groupId)
                continue;
            boolean included = groupId > 0 ? winding.couplingId == groupId
                : winding == target || (target.isSelected() && winding.isSelected());
            WindingRow row = new WindingRow(winding, included);
            rows.add(row);
            windings.add(row);
        }
        ScrollPanel scroll = new ScrollPanel(windings);
        scroll.getElement().getStyle().setProperty("maxHeight", "40vh");
        panel.add(scroll);

        error.getElement().setAttribute("role", "alert");
        error.getElement().getStyle().setProperty("maxWidth", "75vw");
        panel.add(error);
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(4);
        buttons.add(new Button(Locale.LS("OK"), (ClickHandler) event -> apply()));
        if (groupId > 0)
            buttons.add(new Button(Locale.LS("Uncouple"), (ClickHandler) event -> save(true, 0)));
        buttons.add(new Button(Locale.LS("Cancel"), (ClickHandler) event -> closeDialog()));
        panel.add(buttons);
    }

    @Override
    void apply() {
        double k = Double.NaN;
        try {
            k = Double.parseDouble(coefficient.getText().trim());
        } catch (NumberFormatException ignored) {
            // The range check also catches an empty or malformed field.
        }
        if (!InductorCoupling.validCoefficient(k)) {
            error.setText(Locale.LS("Enter a coupling coefficient from 0 up to, but below, 1."));
            return;
        }
        int count = 0;
        for (WindingRow row : rows) {
            if (row.included.getValue())
                count++;
        }
        if (count < 2) {
            error.setText(Locale.LS("Select at least two inductors."));
            return;
        }
        save(false, k);
    }

    private void save(boolean remove, double k) {
        sim.pushUndo();
        int id = groupId > 0 ? groupId : sim.nextInductorCouplingId();
        for (WindingRow row : rows) {
            InductorElm winding = row.winding;
            if (!remove && row.included.getValue()) {
                winding.couplingId = id;
                winding.couplingCoefficient = k;
                winding.reverseDot = row.reversed.getValue();
                if (trapezoidal.getValue())
                    winding.flags &= ~Inductor.FLAG_BACK_EULER;
                else
                    winding.flags |= Inductor.FLAG_BACK_EULER;
                winding.ind.setup(winding.inductance, winding.current, winding.flags);
            } else if (winding.couplingId == id) {
                winding.couplingId = 0;
                winding.reverseDot = false;
            }
        }
        sim.needAnalyze();
        sim.writeRecoveryToStorage();
        closeDialog();
    }

    private static final class WindingRow extends HorizontalPanel {
        final InductorElm winding;
        final Checkbox included;
        final Checkbox reversed = new Checkbox("Reverse dot");

        WindingRow(InductorElm winding, boolean selected) {
            this.winding = winding;
            String name = winding.referenceLabel();
            included = new Checkbox((name == null ? "L" : name) + "  " + winding.componentValue());
            included.setValue(selected);
            reversed.setValue(winding.couplingId > 0 && winding.reverseDot);
            reversed.setEnabled(selected);
            included.addValueChangeHandler(event -> reversed.setEnabled(event.getValue()));
            setSpacing(4);
            add(included);
            add(reversed);
        }
    }
}
