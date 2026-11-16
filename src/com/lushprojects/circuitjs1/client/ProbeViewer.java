package com.lushprojects.circuitjs1.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.ClickHandler;

/** Display-only scope with a common time axis for probes and saved time traces. */
final class ProbeViewer {
    private static final int MAX_TRACES = 12;
    private static final int DOUBLE_TAP_MS = 350;
    private static final int AXIS_MARGIN = 51;
    // Keep these dimensions in sync with the measurement table styles.
    private static final int MEASUREMENT_ROW_HEIGHT = 17;
    private static final int MEASUREMENT_HEADER_HEIGHT = 24;
    private static final int PLOT_DIGITS = 4, MEASUREMENT_DIGITS = 5, HIGH_PRECISION_DIGITS = 6;
    private static final String HIGH_PRECISION_KEY = "probeScopeSixSignificantDigits";
    private static final String[] READING_NAMES = {
        "Latest", "Minimum", "Maximum", "Average", "RMS", "Frequency", "Duty cycle", "Vertical scale"
    };
    private static final int AVERAGE = 3, RMS = 4, FREQUENCY = 5, DUTY_CYCLE = 6, VERTICAL_SCALE = 7;
    private static final String[] COLORS = {
        "#39c978", "#f15b63", "#49b8f2", "#e4ab42", "#ac83ef", "#31c6c0"
    };
    private static final String[] CURRENT_COLORS = {
        "#9be9b8", "#ffadb2", "#a8dcfb", "#ffdc94", "#d5bdfb", "#9ce9e4"
    };

    private static final class Trace {
        final WaveformHistory history = new WaveformHistory();
        WaveformHistory acHistory;
        double previousInput = Double.NaN, previousTime, acOutput;
        String text;
        ProbeExpression expression;
        ScopePlot legacy;
        Scope scope;
        String editableExpression;
        int units;
        int expressionColor = -1;
        boolean hidden;
        String color;
        boolean separate, stacked;
        Boolean holdScale;
        double heldPeak, limit;
        PlotPane pane;

        void clearSamples() {
            history.clear();
            if (acHistory != null)
                acHistory.clear();
            previousInput = Double.NaN;
            heldPeak = 0;
        }

        void sample(double t, double input, double timeConstant) {
            history.add(t, input);
            if (units != Scope.UNITS_V)
                return;
            if (acHistory == null)
                acHistory = new WaveformHistory();
            if (!Double.isFinite(input) || !Double.isFinite(previousInput) || t < previousTime)
                acOutput = 0;
            else
                acOutput = (acOutput + input - previousInput) / (1 + (t - previousTime) / timeConstant);
            acHistory.add(t, Double.isFinite(input) ? acOutput : Double.NaN);
            previousInput = input;
            previousTime = t;
        }
    }

    private static final class PlotPane {
        final ArrayList<Trace> traces = new ArrayList<Trace>();
        int top, bottom;
    }

    boolean canArrangeTraces() {
        int count = 0;
        for (Trace trace : visible)
            if (isTraceShown(trace) && ++count > 1)
                return true;
        return false;
    }

    void arrangeTraces(String action) {
        for (Trace trace : visible) {
            trace.stacked = "stack".equals(action);
            trace.separate = "unstack".equals(action);
        }
        scopeInfoChanged();
    }

    private void setTraceSeparate(Trace trace, boolean separate) {
        if (canArrangeTraces()) {
            trace.stacked = false;
            trace.separate = separate;
            scopeInfoChanged();
        }
    }

    boolean arrangeAt(int x, int y, boolean separate) {
        if (!useWaveform() || !inPlot(x, y) || !canArrangeTraces() || isOpen() || sim.dialogIsShowing())
            return false;
        arrangeTraces(separate ? "unstack" : "combine");
        return true;
    }

    private boolean holdsScale(Trace trace) {
        return trace.holdScale == null ? verticalMode == 1 : trace.holdScale;
    }

    private void setHoldScale(Trace trace, boolean hold) {
        trace.holdScale = hold;
        trace.heldPeak = 0;
        scopeInfoChanged();
    }

    private final CirSim sim;
    private final ArrayList<Trace> probes = new ArrayList<Trace>();
    private final Map<ScopePlot, Trace> saved = new LinkedHashMap<ScopePlot, Trace>();
    private final ArrayList<Trace> visible = new ArrayList<Trace>();
    private static final class Signal {
        final CircuitElm elm;
        final int value;

        Signal(CircuitElm elm, int value) {
            this.elm = elm;
            this.value = value;
        }
    }

    private final Map<String, Signal> sources = new LinkedHashMap<String, Signal>();
    private final Map<LabeledNodeElm, String> nodeTokens = new LinkedHashMap<LabeledNodeElm, String>();
    private final Map<String, ProbeExpression> signals = new LinkedHashMap<String, ProbeExpression>();
    private final Map<ProbeExpression, Signal> signalSources = new LinkedHashMap<ProbeExpression, Signal>();
    private final Map<CircuitElm, Integer> componentColors = new LinkedHashMap<CircuitElm, Integer>();

    final Button viewButton = new Button("Time");
    final Button cursorButton = new Button("⊕");
    final Button triggerHandle = new Button("T");
    private final Dialog calculator = new Dialog();
    final FlowPanel legend = new FlowPanel();
    final FlowPanel measurements = new FlowPanel();
    final FlowPanel traceMenu = new FlowPanel();
    private final Map<Trace, MeasurementRow> measurementRows = new LinkedHashMap<Trace, MeasurementRow>();
    private final Label noMeasurements = new Label("No visible traces");
    private final FlowPanel measurementTables = new FlowPanel();
    private final ArrayList<Trace> measurementTraces = new ArrayList<Trace>();
    private int measurementColumns, measurementReadingMask = -1;
    private final Label measurementTime = new Label();
    private final boolean[] showUnit = new boolean[Scope.UNITS_COUNT + 1];
    private final boolean[] showReading = new boolean[READING_NAMES.length];
    private final FlowPanel signalList = new FlowPanel();
    private final TextBox expression = new TextBox(), search = new TextBox();
    private final Label error = new Label();
    private final Button add = new Button("Add plot");
    private final Button clear = new Button("Clear");
    private final Button trigger;
    private final FlowPanel backdrop;
    private double vertical = 1;
    private double timeZoom = 1;
    private Trace editing;
    private Trace highlightedTrace;
    private Trace lastTappedTrace;
    private double lastTapTime;
    private Scope special;
    private boolean timeTracesSelected;
    private int nextColor;
    private boolean measure;
    private boolean cursorEnabled;
    private double touchCursor = .5;
    double cursorTime = Double.NaN;
    private int plotLeft, plotRight, plotTop, plotBottom;
    private boolean holdMeasurements;
    private boolean highPrecision;
    private int verticalMode;
    private int couplingMode, triggerMode;
    private Trace triggerSource;
    private double triggerLevel;
    private boolean fallingTrigger;
    private double triggerDragLimit;
    private int triggerDragTop, triggerDragBottom;
    private final double[] axisLimits = new double[Scope.UNITS_COUNT + 1];
    private boolean legendDirty = true;
    private double lastMeasure = -1;
    private double lastMeasureTime;

    ProbeViewer(CirSim sim, Button trigger, FlowPanel backdrop) {
        this.sim = sim;
        this.trigger = trigger;
        this.backdrop = backdrop;
        Storage preferences = Storage.getLocalStorageIfSupported();
        highPrecision = preferences != null && "true".equals(preferences.getItem(HIGH_PRECISION_KEY));
        triggerHandle.setStyleName("probe-trigger-handle");
        triggerHandle.setVisible(false);
        bindTriggerHandle(triggerHandle.getElement());
        cursorButton.setTitle("Waveform cursor");
        cursorButton.getElement().setAttribute("aria-label", "Waveform cursor");
        cursorButton.getElement().setAttribute("aria-pressed", "false");
        cursorButton.addClickHandler(event -> {
            cursorEnabled = !cursorEnabled;
            cursorButton.getElement().setAttribute("aria-pressed", Boolean.toString(cursorEnabled));
            touchCursor = .5;
            sim.repaint();
        });
        viewButton.setTitle("Switch scope view");
        viewButton.getElement().setAttribute("aria-label", "Switch scope view");
        viewButton.addClickHandler(event -> {
            close();
            traceMenu.clear();
            traceMenu.add(new Label("Scope view"));
            addMenuAction("Time traces", () -> selectView(null));
            for (int i = 0; i < sim.scopeCount; i++) {
                Scope scope = sim.scopes[i];
                if (scope.plot2d || scope.showFFT)
                    addMenuAction(scope.getDockedLabel() + " · " + modeLabel(scope), () -> selectView(scope));
            }
            showMenu();
        });
        calculator.setText("Probes & Math");
        calculator.closeOnEnter = false;
        calculator.getElement().setAttribute("aria-label", "Probes & Math");
        legend.setStyleName("probe-legend");
        traceMenu.setStyleName("probe-trace-menu");
        traceMenu.getElement().setAttribute("role", "dialog");
        traceMenu.getElement().setAttribute("aria-label", "Trace options");
        traceMenu.setVisible(false);
        measurements.setStyleName("probe-measurements");
        FlowPanel headings = new FlowPanel();
        headings.setStyleName("probe-measurement-heading");
        headings.add(new Label("Measurements"));
        headings.add(measurementTime);
        measurements.add(headings);
        measurementTables.setStyleName("probe-measurement-tables");
        measurements.add(measurementTables);
        measurements.setVisible(false);
        resetDisplayOptions();
        FlowPanel sheet = new FlowPanel();
        sheet.setStyleName("probe-sheet");
        calculator.setWidget(sheet);
        expression.getElement().setAttribute("aria-label", "Expression");
        expression.setMaxLength(256);
        sheet.add(expression);
        search.getElement().setAttribute("aria-label", "Search signals");
        search.getElement().setAttribute("placeholder", "Search signals");
        bindSearch(search.getElement());
        sheet.add(search);
        signalList.setStyleName("probe-signals");
        sheet.add(signalList);
        FlowPanel keypad = new FlowPanel();
        keypad.setStyleName("probe-keypad");
        String[] labels = {"AC", "DEL", "(", ")", "÷", "7", "8", "9", "×", "**",
            "4", "5", "6", "−", "abs", "1", "2", "3", "+", "sqrt", "0", ".", ",", "d", "d²"};
        String[] tokens = {"Clear", "⌫", "(", ")", "/", "7", "8", "9", "*", "**",
            "4", "5", "6", "-", "abs(", "1", "2", "3", "+", "sqrt(", "0", ".", ",", "d(", "d2("};
        for (int i = 0; i < labels.length; i++)
            addKey(keypad, labels[i], tokens[i]);
        sheet.add(keypad);
        FlowPanel scientific = new FlowPanel();
        scientific.setStyleName("probe-keypad probe-scientific");
        scientific.setVisible(false);
        Button more = new Button("Scientific functions", (ClickHandler) event -> {
            scientific.setVisible(!scientific.isVisible());
            ((Button) event.getSource()).getElement().setAttribute("aria-expanded", Boolean.toString(scientific.isVisible()));
        });
        more.getElement().setAttribute("aria-expanded", "false");
        sheet.add(more);
        for (String function : ProbeExpression.UNARY)
            if (!function.equals("abs") && !function.equals("sqrt") && !function.equals("d") && !function.equals("d2"))
                addKey(scientific, function, function + "(");
        for (String[] group : new String[][] {ProbeExpression.BINARY, ProbeExpression.TERNARY})
            for (String function : group)
                addKey(scientific, function, function + "(");
        for (String constant : new String[] {"π", "e", "t"})
            addKey(scientific, constant, constant);
        sheet.add(scientific);
        error.getElement().setAttribute("role", "status");
        sheet.add(error);
        add.addClickHandler(event -> submit());
        clear.setTitle("Remove all waveform plots");
        clear.addClickHandler(event -> clearPlots());
        FlowPanel actions = new FlowPanel();
        actions.setStyleName("probe-sheet-actions");
        actions.add(add);
        actions.add(new Button("Close", (ClickHandler) event -> {
            close();
            trigger.setFocus(true);
        }));
        actions.add(clear);
        sheet.add(actions);
        expression.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
                submit();
        });
        close();
    }

    private void addKey(FlowPanel panel, String label, String token) {
        panel.add(new Button(label, (ClickHandler) event -> insert(token)));
    }

    boolean hasProbes() {
        return !probes.isEmpty();
    }

    boolean hasPanel() {
        return hasProbes() || sim.scopeCount > 0;
    }

    boolean useWaveform() {
        return special == null && (sim.hasTouchScope() || hasProbes());
    }

    Scope specialScope() {
        return special;
    }

    private boolean inPlot(int x, int y) {
        return plotRight > plotLeft && plotBottom > plotTop
                && x >= plotLeft && x <= plotRight && y >= plotTop && y <= plotBottom;
    }

    boolean startCursorTouch(int x, int y) {
        if (!cursorEnabled || !useWaveform() || !inPlot(x, y) || isOpen() || sim.dialogIsShowing())
            return false;
        sim.mouseCursorX = -1;
        moveCursorTouch(x);
        return true;
    }

    void moveCursorTouch(int x) {
        if (plotRight > plotLeft) {
            touchCursor = Math.max(0, Math.min(1, (double) (x - plotLeft) / (plotRight - plotLeft)));
            sim.repaint();
        }
    }

    int measurementsHeight() {
        if (!measure || !useWaveform())
            return 0;
        int traces = 0, readings = 0;
        for (Trace trace : visible)
            if (isTraceShown(trace))
                traces++;
        for (boolean shown : showReading)
            if (shown)
                readings++;
        int columns = measurementColumns();
        int tables = (traces + columns - 1) / columns;
        int rows = traces == 0 || readings == 0 ? MEASUREMENT_ROW_HEIGHT
                : tables * ((1 + readings) * MEASUREMENT_ROW_HEIGHT + 1) + (tables - 1) * 4;
        return Math.min(MEASUREMENT_HEADER_HEIGHT + rows, Math.max(0, sim.canvasHeight - 238));
    }

    private int measurementColumns() {
        int valueWidth = showReading[VERTICAL_SCALE] ? 86 : 74;
        return Math.max(1, (sim.canvasWidth - 88) / valueWidth);
    }

    private boolean isTraceShown(Trace trace) {
        return !trace.hidden && showUnit[trace.units];
    }

    boolean highlights(CircuitElm elm) {
        Trace trace = highlightedTrace;
        if (trace == null)
            return false;
        if (trace.legacy != null)
            return trace.legacy.elm == elm;
        Signal signal = signalSources.get(trace.expression);
        if (signal == null)
            return false;
        return signal.elm == elm || elm instanceof LabeledNodeElm
                && signals.get(nodeTokens.get(elm)) == trace.expression;
    }

    void toggleMeasurements() {
        measure = !measure;
        lastMeasure = -1;
        sim.setCircuitArea();
        sim.repaint();
    }

    void scopeInfoChanged() {
        lastMeasure = -1;
        sim.repaint();
    }

    void resetView() {
        timeZoom = 1;
        vertical = 1;
        lastMeasure = -1;
        sim.repaint();
    }

    void scaleVertical(double factor) {
        vertical = Math.max(1.0 / 64, Math.min(64, vertical * factor));
        lastMeasure = -1;
        sim.repaint();
    }

    void zoom(double factor) {
        timeZoom = Math.max(1.0 / 64, Math.min(4, timeZoom / factor));
        if (verticalMode == 1)
        for (Trace trace : visible)
            trace.heldPeak = 0;
        lastMeasure = -1;
        sim.repaint();
    }

    boolean isOpen() {
        return calculator.isShowing() || traceMenu.isVisible();
    }

    void close() {
        calculator.closeDialog();
        traceMenu.setVisible(false);
        backdrop.setVisible(false);
        trigger.getElement().setAttribute("aria-expanded", "false");
    }

    void open(Trace trace) {
        traceMenu.setVisible(false);
        editing = trace;
        refreshSignals();
        expression.getElement().setAttribute("placeholder",
                trace == null ? "V(out), I(R1)" : "V(out) − V(in)");
        expression.setText(trace == null ? "" : trace.legacy == null ? trace.text : trace.editableExpression);
        search.setText("");
        error.setText("");
        add.setText(trace == null ? "Add plot" : "Update plot");
        updateClearButton();
        showSignals();
        backdrop.setVisible(false);
        CirSim.dialogShowing = calculator;
        calculator.center();
        trigger.getElement().setAttribute("aria-expanded", "true");
    }

    private void updateClearButton() {
        clear.setEnabled(sim.scopeCount > 0 || !probes.isEmpty());
    }

    private void clearPlots() {
        sim.clearDockedScopes();
        probes.clear();
        editing = null;
        highlightedTrace = null;
        lastTappedTrace = null;
        triggerSource = null;
        timeTracesSelected = false;
        cursorTime = Double.NaN;
        add.setText("Add plot");
        expression.getElement().setAttribute("placeholder", "V(out), I(R1)");
        error.setText("");
        updateClearButton();
        sim.repaint();
    }

    private void insert(String key) {
        String text = expression.getText();
        int start = expression.getCursorPos();
        int end = start + expression.getSelectionLength();
        if ("Clear".equals(key)) {
            expression.setText("");
            expression.setCursorPos(0);
            return;
        }
        if ("⌫".equals(key)) {
            if (start == end)
                start = Math.max(0, start - 1);
            key = "";
        }
        if (start + key.length() + text.length() - end > 256)
            return;
        expression.setText(text.substring(0, start) + key + text.substring(end));
        expression.setCursorPos(start + key.length());
    }

    private void insertSignal(String name) {
        String text = expression.getText();
        if (editing == null && expression.getCursorPos() == text.length() && !text.trim().isEmpty()) {
            try {
                ProbeExpression.compilePlots(text, signals, () -> sim.t, true);
                name = ", " + name;
            } catch (IllegalArgumentException ex) {
                // An unfinished expression still needs its next operand.
            }
        }
        insert(name);
    }

    private native void bindSearch(com.google.gwt.dom.client.Element input) /*-{
        var viewer = this;
        input.addEventListener("input", $entry(function() {
            viewer.@com.lushprojects.circuitjs1.client.ProbeViewer::showSignals()();
        }));
    }-*/;

    private void showSignals() {
        signalList.clear();
        String filter = search.getText().trim().toLowerCase();
        for (String name : signals.keySet())
            if (name.toLowerCase().contains(filter))
                signalList.add(new Button(name, (ClickHandler) event -> insertSignal(name)));
        if (signalList.getWidgetCount() == 0)
            signalList.add(new Label("No matching signals"));
    }

    private String modeLabel(Scope scope) {
        return scope.plot2d ? (scope.plotXY ? "X/Y" : "V vs I") : "FFT";
    }

    void selectView(Scope scope) {
        special = scope;
        timeTracesSelected = scope == null;
        if (scope != null)
            sim.selectDockedScope(scope);
        sim.setCircuitArea();
        sim.repaint();
    }

    void scopeModeChanged(Scope scope) {
        for (int i = 0; i < sim.scopeCount; i++) {
            if (sim.scopes[i] == scope && (sim.hasTouchScope() || hasProbes())) {
                if (scope.plot2d || scope.showFFT)
                    selectView(scope);
                else if (special == scope)
                    selectView(null);
                return;
            }
        }
    }

    private void submit() {
        try {
            ArrayList<ProbeExpression.Plot> plots =
                    ProbeExpression.compilePlots(expression.getText().trim(), signals, () -> sim.t, editing == null);
            HashSet<String> labels = new HashSet<String>();
            HashSet<ProbeExpression> plottedSignals = new HashSet<ProbeExpression>();
            for (Trace trace : visible) {
                if (trace == editing)
                    continue;
                labels.add(trace.text);
                ProbeExpression signal = trace.legacy == null ? trace.expression : signals.get(trace.editableExpression);
                if (signalSources.containsKey(signal))
                    plottedSignals.add(signal);
            }
            plots.removeIf(plot -> !labels.add(plot.text)
                    || signalSources.containsKey(plot.expression) && !plottedSignals.add(plot.expression));
            if (plots.isEmpty()) {
                error.setText("Already plotted");
                return;
            }
            int added = editing != null && editing.legacy == null ? 0 : plots.size();
            if (probes.size() + added > MAX_TRACES) {
                error.setText("Remove plots before adding " + (added == 1 ? "another" : "these plots"));
                return;
            }
            for (ProbeExpression.Plot plot : plots)
                savePlot(plot);
            special = null;
            timeTracesSelected = true;
            sim.setCircuitArea();
            legendDirty = true;
            lastMeasure = -1;
            close();
            trigger.setFocus(true);
            sim.repaint();
        } catch (IllegalArgumentException ex) {
            error.setText(ex.getMessage());
        }
    }

    private void savePlot(ProbeExpression.Plot plot) {
        Trace trace = editing;
        if (trace == null) {
            trace = new Trace();
            probes.add(trace);
        } else if (trace.legacy != null) {
            removeSaved(trace);
            trace.legacy = null;
            trace.scope = null;
            probes.add(trace);
            if (!plot.text.equals(trace.editableExpression))
                trace.clearSamples();
        } else if (!plot.text.equals(trace.text)) {
            trace.clearSamples();
        }
        trace.text = plot.text;
        trace.expression = plot.expression;
        trace.units = plot.expression.units == ProbeExpression.SCALAR ? Scope.UNITS_COUNT : plot.expression.units;
        assignColor(trace, signalSources.get(plot.expression));
    }

    private boolean supportsCurrent(CircuitElm elm) {
        return elm instanceof ResistorElm || elm instanceof CapacitorElm || elm instanceof InductorElm
                || elm instanceof VoltageElm || elm instanceof CurrentElm && !(elm instanceof OhmMeterElm)
                || elm instanceof DiodeElm || elm instanceof AmmeterElm
                || elm instanceof SwitchElm && elm.getPostCount() == 2;
    }

    void refreshSignals() {
        signals.clear();
        signalSources.clear();
        nodeTokens.clear();
        sources.clear();
        componentColors.keySet().retainAll(sim.elmList);
        for (CircuitElm elm : sim.elmList) {
            String label = elm.referenceLabel();
            if (label == null || elm.getPostCount() == 0)
                continue;
            signals.put("V(" + label + ")", new ProbeExpression(
                    elm.getPostCount() == 1 ? () -> elm.volts[0] : elm::getVoltageDiff, ProbeExpression.VOLTAGE));
            sources.put("V(" + label + ")", new Signal(elm, Scope.VAL_VOLTAGE));
            if (supportsCurrent(elm)) {
                signals.put("I(" + label + ")", new ProbeExpression(elm::getCurrent, ProbeExpression.CURRENT));
                sources.put("I(" + label + ")", new Signal(elm, Scope.VAL_CURRENT));
            }
        }
        for (CircuitElm elm : sim.elmList) {
            if (!(elm instanceof LabeledNodeElm))
                continue;
            String name = ((LabeledNodeElm) elm).getName().trim();
            if (!name.isEmpty() && name.indexOf(')') < 0) {
                String token = "V(" + name + ")";
                ProbeExpression component = signals.get(token);
                if (component != null && !nodeTokens.containsValue(token))
                    token = "V(net:" + name + ")";
                nodeTokens.put((LabeledNodeElm) elm, token);
                sources.putIfAbsent(token, new Signal(elm, Scope.VAL_VOLTAGE));
                signals.putIfAbsent(token, new ProbeExpression(elm::getVoltageDiff, ProbeExpression.VOLTAGE));
            }
        }
        for (Map.Entry<String, ProbeExpression> entry : signals.entrySet())
            signalSources.put(entry.getValue(), sources.get(entry.getKey()));
        recompileProbes();
    }

    private void recompileProbes() {
        for (Trace trace : probes) {
            try {
                trace.expression = ProbeExpression.compile(trace.text, signals, () -> sim.t);
                assignColor(trace, signalSources.get(trace.expression));
            } catch (IllegalArgumentException ex) {
                trace.expression = null;
            }
        }
    }

    void clearCircuit() {
        probes.clear();
        saved.clear();
        visible.clear();
        highlightedTrace = null;
        lastTappedTrace = null;
        signals.clear();
        signalSources.clear();
        componentColors.clear();
        nodeTokens.clear();
        sources.clear();
        special = null;
        timeTracesSelected = false;
        nextColor = 0;
        measure = false;
        holdMeasurements = false;
        resetDisplayOptions();
        legendDirty = true;
        lastMeasure = -1;
        close();
        resetView();
    }

    private void resetDisplayOptions() {
        verticalMode = 0;
        couplingMode = triggerMode = 0;
        triggerSource = null;
        triggerLevel = 0;
        fallingTrigger = false;
        java.util.Arrays.fill(showUnit, true);
        java.util.Arrays.fill(showReading, false);
        showReading[0] = showReading[1] = showReading[2] = true;
    }

    void clearHistory() {
        recompileProbes();
        for (Trace trace : probes)
            trace.clearSamples();
        for (Trace trace : saved.values())
            trace.clearSamples();
        lastMeasure = -1;
    }

    void sampleProbes(double t) {
        for (Trace trace : probes)
            trace.sample(t, trace.expression == null ? Double.NaN : trace.expression.sample.read(),
                    couplingTimeConstant());
    }

    void sampleSaved(double t) {
        for (Trace trace : saved.values()) {
            ScopePlot plot = trace.legacy;
            double sample = trace.scope.plots.contains(plot) ? plot.lastValue : plot.elm.getScopeValue(plot.value);
            trace.sample(t, sample, couplingTimeConstant());
        }
    }

    private double couplingTimeConstant() {
        return 1.15 * 4096 * sim.maxTimeStep;
    }

    private WaveformHistory displayedHistory(Trace trace) {
        boolean ac = couplingMode == 2 || couplingMode == 0 && trace.legacy != null && trace.legacy.isAcCoupled();
        return ac && trace.acHistory != null ? trace.acHistory : trace.history;
    }

    void syncScopes(boolean expanded) {
        updateClearButton();
        boolean specialExists = false;
        Scope firstSpecial = null;
        ArrayList<ScopePlot> retained = new ArrayList<ScopePlot>();
        ArrayList<Trace> ordered = new ArrayList<Trace>();
        for (int i = 0; i < sim.scopeCount; i++) {
            Scope scope = sim.scopes[i];
            retained.addAll(scope.plots);
            if (scope.timePlots != null)
                retained.addAll(scope.timePlots);
            if (scope.plot2d || scope.showFFT) {
                specialExists |= scope == special;
                if (firstSpecial == null)
                    firstSpecial = scope;
                continue;
            }
            for (ScopePlot plot : scope.visiblePlots) {
                if (plot.elm == null)
                    continue;
                Trace trace = saved.get(plot);
                if (trace == null) {
                    trace = new Trace();
                    trace.legacy = plot;
                    trace.scope = scope;
                    trace.units = plot.units;
                    assignColor(trace, new Signal(plot.elm, plot.value));
                    saved.put(plot, trace);
                }
                String reference = plot.elm.referenceLabel();
                trace.scope = scope;
                String token = null;
                if (plot.elm instanceof LabeledNodeElm && plot.value == Scope.VAL_VOLTAGE && !plot.isAcCoupled())
                    token = nodeTokens.get(plot.elm);
                if (reference != null && !plot.isAcCoupled()) {
                    if (plot.value == Scope.VAL_VOLTAGE)
                        token = "V(" + reference + ")";
                    else if (plot.value == Scope.VAL_CURRENT && supportsCurrent(plot.elm))
                        token = "I(" + reference + ")";
                }
                trace.editableExpression = token;
                String channel = Scope.getScaleUnitsText(plot.units);
                String title = token != null ? token : scope.getDockedLabel() + " [" + channel + "]";
                if (!title.equals(trace.text)) {
                    trace.text = title;
                    legendDirty = true;
                }
                ordered.add(trace);
            }
        }
        saved.keySet().retainAll(retained);
        if (!specialExists)
            special = null;
        if (special == null && !timeTracesSelected && ordered.isEmpty() && probes.isEmpty()
                && firstSpecial != null && sim.hasTouchScope()) {
            special = firstSpecial;
            sim.selectDockedScope(special);
        }
        viewButton.setVisible(firstSpecial != null && (sim.hasTouchScope() || hasProbes()));
        viewButton.setText(special == null ? "Time" : modeLabel(special));
        ordered.addAll(probes);
        if (highlightedTrace != null && (!ordered.contains(highlightedTrace) || !isTraceShown(highlightedTrace))) {
            highlightedTrace = null;
            legendDirty = true;
        }
        if (!ordered.equals(visible)) {
            visible.clear();
            visible.addAll(ordered);
            legendDirty = true;
            lastMeasure = -1;
        }
        boolean shown = expanded && hasPanel() && useWaveform();
        if (!shown || triggerMode == 0 || activeTriggerSource() == null) {
            triggerHandle.setVisible(false);
            triggerDragLimit = 0;
        }
        legend.setVisible(shown && !visible.isEmpty());
        measurements.setVisible(shown && measure);
        if (shown && legendDirty)
            updateLegend();
        if (shown && measure)
            layoutMeasurements();
    }

    void positionMeasurements(int panelHeight) {
        if (measurements.isVisible()) {
            int height = measurementsHeight();
            measurements.getElement().getStyle().setTop(
                    Math.max(0, panelHeight - height - 8), Unit.PX);
            measurements.getElement().getStyle().setHeight(height, Unit.PX);
        }
    }

    boolean hasTracePropertiesShortcut() {
        if (!legend.isVisible())
            return false;
        for (Trace trace : visible) {
            if (showUnit[trace.units])
                return true;
        }
        return false;
    }

    private void updateLegend() {
        legendDirty = false;
        legend.clear();
        for (Trace trace : visible) {
            FlowPanel channel = new FlowPanel();
            channel.setStyleName("probe-channel");
            Button toggle = new Button(trace.text, (ClickHandler) event ->
                    tapTrace(trace, isPointerClick(event.getNativeEvent())));
            toggle.setEnabled(showUnit[trace.units]);
            toggle.getElement().getStyle().setColor(color(trace));
            channel.add(toggle);
            toggle.getElement().setAttribute("aria-haspopup", "true");
            bindTraceOptions(toggle.getElement(), trace);
            toggle.addKeyDownHandler(event -> {
                if (event.getNativeKeyCode() == 93 || event.isShiftKeyDown() && event.getNativeKeyCode() == 121) {
                    event.preventDefault();
                    showTraceOptions(trace);
                }
            });
            legend.add(channel);
        }
        updateLegendHighlight();
    }

    private void tapTrace(Trace trace, boolean pointerTap) {
        double now = Duration.currentTimeMillis();
        if (trace.hidden) {
            trace.hidden = false;
            highlightedTrace = trace;
            lastTappedTrace = null;
            lastMeasure = -1;
        } else if (pointerTap && lastTappedTrace == trace && now - lastTapTime <= DOUBLE_TAP_MS) {
            trace.hidden = true;
            if (highlightedTrace == trace)
                highlightedTrace = null;
            lastTappedTrace = null;
            lastMeasure = -1;
        } else {
            highlightedTrace = highlightedTrace == trace ? null : trace;
            lastTappedTrace = pointerTap ? trace : null;
            lastTapTime = now;
        }
        updateLegendHighlight();
        sim.repaint();
    }

    private static native boolean isPointerClick(NativeEvent event) /*-{
        return event.detail > 0;
    }-*/;

    private void updateLegendHighlight() {
        legend.setStyleName("has-highlight", highlightedTrace != null);
        for (int i = 0; i < visible.size(); i++) {
            Trace trace = visible.get(i);
            FlowPanel channel = (FlowPanel) legend.getWidget(i);
            Button button = (Button) channel.getWidget(0);
            button.getElement().setAttribute("aria-pressed", Boolean.toString(trace == highlightedTrace));
            button.setStyleName("trace-hidden", trace.hidden);
            button.setTitle(trace.hidden ? "Tap to show waveform."
                    : "Tap to highlight. Double-tap to hide. Drag down to unstack or up to combine. Long-press for trace options.");
        }
    }

    private native void bindTraceOptions(com.google.gwt.dom.client.Element button, Trace trace) /*-{
        var viewer = this, timer, x, y, opened, pointer;
        var show = $entry(function() {
            clearTimeout(timer);
            if (!opened) {
                opened = true;
                viewer.@com.lushprojects.circuitjs1.client.ProbeViewer::showTraceOptions(Lcom/lushprojects/circuitjs1/client/ProbeViewer$Trace;)(trace);
            }
        });
        button.addEventListener("pointerdown", function(e) {
            clearTimeout(timer);
            opened = false;
            x = e.clientX;
            y = e.clientY;
            if (e.pointerType !== "mouse" && e.isPrimary)
                pointer = e.pointerId;
            if (pointer === e.pointerId)
                timer = setTimeout(show, 500);
        });
        button.addEventListener("pointerup", $entry(function(e) {
            var down = e.clientY - y;
            if (pointer === e.pointerId && !opened &&
                    @com.lushprojects.circuitjs1.client.CirSim::isScopeLayoutSwipe(DD)(e.clientX - x, down)) {
                opened = true;
                viewer.@com.lushprojects.circuitjs1.client.ProbeViewer::setTraceSeparate(Lcom/lushprojects/circuitjs1/client/ProbeViewer$Trace;Z)(trace, down > 0);
            }
        }));
        button.addEventListener("pointermove", function(e) {
            if (Math.hypot(e.clientX - x, e.clientY - y) > 10)
                clearTimeout(timer);
        });
        ["pointerup", "pointercancel", "pointerleave"].forEach(function(type) {
            button.addEventListener(type, function() { clearTimeout(timer); pointer = null; });
        });
        button.addEventListener("contextmenu", function(e) {
            e.preventDefault();
            show();
        });
        button.addEventListener("click", function(e) {
            if (opened && e.detail !== 0) {
                e.preventDefault();
                e.stopImmediatePropagation();
                opened = false;
            }
        }, true);
    }-*/;

    void showProbeProperties(Scope selectedScope) {
        final ScopePropertiesDialog[] componentEditor = new ScopePropertiesDialog[1];
        Dialog dialog = new Dialog() {
            @Override
            void apply() {
                if (componentEditor[0] != null)
                    componentEditor[0].apply();
            }

            @Override
            public void closeDialog() {
                apply();
                super.closeDialog();
            }
        };
        dialog.setText("Scope Properties");
        FlowPanel content = new FlowPanel();
        content.setStyleName("probe-properties");
        FlowPanel body = new FlowPanel();
        body.setStyleName("probe-properties-body");
        content.add(body);
        FlowPanel display = settingsSection(body, "Display precision");
        CheckBox precision = new CheckBox("6 significant digits");
        precision.setValue(highPrecision);
        precision.addValueChangeHandler(event -> {
            highPrecision = event.getValue();
            Storage preferences = Storage.getLocalStorageIfSupported();
            if (preferences != null)
                preferences.setItem(HIGH_PRECISION_KEY, Boolean.toString(highPrecision));
            for (Map.Entry<Trace, MeasurementRow> entry : measurementRows.entrySet())
                updateMeasurementText(entry.getValue(), entry.getKey().units);
            sim.repaint();
        });
        display.add(precision);
        if (useWaveform() && canArrangeTraces())
            addLayoutActions(settingsSection(body, "Time trace layout"));
        FlowPanel plots = settingsSection(body, "Time trace visibility");
        String[] channelNames = {"Show Voltage", "Show Current", "Show Power", "Show Resistance", "Show unitless traces"};
        for (int i = 0; i < channelNames.length; i++) {
            final int unit = i;
            CheckBox show = new CheckBox(channelNames[i]);
            show.setValue(showUnit[unit]);
            show.addValueChangeHandler(event -> {
                showUnit[unit] = event.getValue();
                legendDirty = true;
                scopeInfoChanged();
            });
            plots.add(show);
        }

        FlowPanel scales = settingsSection(body, "Time trace scale");
        ListBox mode = new ListBox();
        mode.getElement().setAttribute("aria-label", "Vertical scale mode");
        mode.addItem("Vertical: Auto");
        mode.addItem("Vertical: Hold maximum");
        mode.setSelectedIndex(verticalMode);
        mode.addChangeHandler(event -> {
            verticalMode = mode.getSelectedIndex();
            for (Trace trace : visible) {
                trace.holdScale = null;
                trace.heldPeak = 0;
            }
            scopeInfoChanged();
        });
        scales.add(mode);
        ListBox coupling = new ListBox();
        coupling.getElement().setAttribute("aria-label", "Voltage coupling");
        coupling.addItem("Per-channel coupling");
        coupling.addItem("DC Coupled");
        coupling.addItem("AC Coupled");
        coupling.setSelectedIndex(couplingMode);
        coupling.addChangeHandler(event -> {
            couplingMode = coupling.getSelectedIndex();
            scopeInfoChanged();
        });
        scales.add(coupling);
        Label timeHeading = new Label("Horizontal scale");
        timeHeading.setStyleName("probe-settings-title");
        scales.add(timeHeading);
        FlowPanel horizontal = new FlowPanel();
        horizontal.setStyleName("probe-time-scale");
        Label timeScale = new Label();
        Runnable refresh = () -> timeScale.setText(timeScaleText(timeRange()));
        Button wider = new Button("−", (ClickHandler) event -> {
            zoom(.5);
            refresh.run();
        });
        wider.getElement().setAttribute("aria-label", "Show more time");
        horizontal.add(wider);
        horizontal.add(timeScale);
        Button narrower = new Button("+", (ClickHandler) event -> {
            zoom(2);
            refresh.run();
        });
        narrower.getElement().setAttribute("aria-label", "Show less time");
        horizontal.add(narrower);
        refresh.run();
        horizontal.getElement().setAttribute("role", "group");
        horizontal.getElement().setAttribute("aria-label", "Horizontal scale");
        scales.add(horizontal);

        FlowPanel triggerSettings = settingsSection(body, "Trigger");
        ListBox triggerSourceList = new ListBox();
        triggerSourceList.getElement().setAttribute("aria-label", "Trigger source");
        ArrayList<Trace> triggerChannels = new ArrayList<Trace>(visible);
        triggerSourceList.addItem("First visible trace");
        for (Trace trace : triggerChannels)
            triggerSourceList.addItem(trace.text);
        triggerSourceList.setSelectedIndex(triggerChannels.indexOf(triggerSource) + 1);
        triggerSourceList.addChangeHandler(event -> {
            int index = triggerSourceList.getSelectedIndex();
            triggerSource = index == 0 ? null : triggerChannels.get(index - 1);
            scopeInfoChanged();
        });
        FlowPanel modes = triggerRow("Trigger mode");
        String[] modeNames = {"Free Run", "Normal", "Auto"};
        for (int i = 0; i < modeNames.length; i++) {
            final int modeIndex = i;
            RadioButton choice = new RadioButton("scope-trigger-mode", modeNames[i]);
            choice.setValue(triggerMode == i);
            choice.addClickHandler(event -> {
                triggerMode = modeIndex;
                scopeInfoChanged();
            });
            modes.add(choice);
        }
        FlowPanel edges = triggerRow("Trigger edge");
        edges.add(new Label("Edge"));
        for (boolean falling : new boolean[] {false, true}) {
            RadioButton choice = new RadioButton("scope-trigger-edge", falling ? "Falling" : "Rising");
            choice.setValue(fallingTrigger == falling);
            choice.addClickHandler(event -> {
                fallingTrigger = falling;
                scopeInfoChanged();
            });
            edges.add(choice);
        }
        TextBox level = new TextBox();
        level.getElement().setAttribute("aria-label", "Trigger level in source units");
        level.getElement().setAttribute("inputmode", "decimal");
        level.setText(Double.toString(triggerLevel));
        Label levelError = new Label();
        levelError.getElement().setAttribute("role", "status");
        levelError.setVisible(false);
        Runnable applyLevel = () -> {
            try {
                double input = Double.parseDouble(level.getText().trim());
                if (!Double.isFinite(input))
                    throw new NumberFormatException();
                triggerLevel = input;
                levelError.setVisible(false);
                scopeInfoChanged();
            } catch (NumberFormatException invalid) {
                levelError.setText("Enter a finite trigger level.");
                levelError.setVisible(true);
            }
        };
        FlowPanel levelRow = triggerRow("Trigger level");
        levelRow.add(new Label("Level"));
        levelRow.add(level);
        levelRow.add(new Button("Apply", (ClickHandler) event -> applyLevel.run()));
        triggerSettings.add(modes);
        triggerSettings.add(edges);
        triggerSettings.add(levelRow);
        triggerSettings.add(levelError);
        triggerSettings.add(triggerSourceList);

        FlowPanel xy = settingsSection(body, "Scope plots");
        ListBox xySource = new ListBox();
        xySource.getElement().setAttribute("aria-label", "Scope to configure");
        ArrayList<Scope> xyScopes = new ArrayList<Scope>();
        for (int i = 0; i < sim.scopeCount; i++) {
            Scope scope = sim.scopes[i];
            if (!scope.plots.isEmpty() && scope.getElm() != null) {
                xyScopes.add(scope);
                xySource.addItem(scope.getDockedLabel());
            }
        }
        if (xyScopes.isEmpty()) {
            xy.add(new Label("Add a component to a docked scope to configure its plots."));
        } else {
            xySource.setSelectedIndex(Math.max(0, xyScopes.indexOf(selectedScope)));
            xy.add(xySource);
            FlowPanel component = new FlowPanel();
            component.setStyleName("probe-component-controls");
            Runnable editScope = () -> {
                dialog.apply();
                component.clear();
                Scope scope = xyScopes.get(xySource.getSelectedIndex());
                componentEditor[0] = new ScopePropertiesDialog(sim, scope, true);
                component.add(componentEditor[0].controls());
            };
            xySource.addChangeHandler(event -> editScope.run());
            xy.add(new Button("Show V vs I", (ClickHandler) event -> {
                dialog.apply();
                componentEditor[0] = null;
                openXY(xyScopes.get(xySource.getSelectedIndex()), "showvvsi");
                editScope.run();
            }));
            xy.add(new Button("Plot X/Y", (ClickHandler) event -> {
                dialog.apply();
                componentEditor[0] = null;
                openXY(xyScopes.get(xySource.getSelectedIndex()), "plotxy");
                editScope.run();
            }));
            xy.add(new Button("Return to time traces", (ClickHandler) event -> selectView(null)));
            xy.add(component);
            editScope.run();
        }

        FlowPanel readings = settingsSection(body, "Measurements");
        CheckBox show = new CheckBox("Show measurements");
        show.setValue(measure);
        show.addValueChangeHandler(event -> toggleMeasurements());
        readings.add(show);
        CheckBox hold = new CheckBox("Hold readings");
        hold.setValue(holdMeasurements);
        hold.addValueChangeHandler(event -> setHoldMeasurements(event.getValue()));
        readings.add(hold);
        for (int i = 0; i < READING_NAMES.length; i++) {
            final int reading = i;
            CheckBox option = new CheckBox(READING_NAMES[i]);
            option.setValue(showReading[i]);
            option.addValueChangeHandler(event -> {
                showReading[reading] = event.getValue();
                scopeInfoChanged();
            });
            readings.add(option);
        }
        content.add(new Button("OK", (ClickHandler) event -> dialog.closeDialog()));
        dialog.setWidget(content);
        CirSim.dialogShowing = dialog;
        dialog.center();
    }

    private FlowPanel triggerRow(String label) {
        FlowPanel row = new FlowPanel();
        row.setStyleName("probe-trigger-row");
        row.getElement().setAttribute("role", "group");
        row.getElement().setAttribute("aria-label", label);
        return row;
    }

    private void openXY(Scope scope, String mode) {
        boolean xy = "plotxy".equals(mode);
        if (!scope.plot2d || scope.plotXY != xy)
            scope.handleMenu(mode, true);
        selectView(scope);
    }

    private FlowPanel settingsSection(FlowPanel content, String title) {
        FlowPanel section = new FlowPanel();
        section.setStyleName("probe-settings-section");
        Label heading = new Label(title);
        heading.setStyleName("probe-settings-title");
        section.add(heading);
        content.add(section);
        return section;
    }

    private void setHoldMeasurements(boolean hold) {
        holdMeasurements = hold;
        if (hold)
            updateMeasurementHeading(timeRange());
        else
            scopeInfoChanged();
    }

    private Button addMenuAction(String label, Runnable action) {
        Button button = new Button(label, (ClickHandler) event -> {
            close();
            action.run();
        });
        traceMenu.add(button);
        return button;
    }

    private void showMenu() {
        backdrop.setVisible(true);
        traceMenu.setVisible(true);
    }

    private void openSavedOptions(Scope scope, int plot) {
        sim.openDockedScopeMenu(scope, plot, traceMenu.getAbsoluteLeft(),
                legend.getAbsoluteTop() + legend.getOffsetHeight());
    }

    private void showTraceOptions(Trace trace) {
        traceMenu.clear();
        traceMenu.add(new Label(trace.text));
        if (trace.legacy == null) {
            if (canArrangeTraces()) {
                addMenuAction("Stack", () -> arrangeTraces("stack"));
                addMenuAction("Unstack", () -> setTraceSeparate(trace, true));
                addMenuAction("Combine", () -> arrangeTraces("combine"));
            }
            CheckBox max = new CheckBox("Max Scale");
            max.setValue(holdsScale(trace));
            max.addValueChangeHandler(event -> {
                close();
                setHoldScale(trace, event.getValue());
            });
            traceMenu.add(max);
        }
        addMenuAction("Properties…", () -> showProbeProperties(trace.scope));
        if (trace.legacy == null || trace.editableExpression != null)
            traceMenu.add(new Button("Edit in calculator", (ClickHandler) event -> open(trace)));
        if (trace.legacy != null) {
            addMenuAction("Component scope options", () -> openSavedOptions(trace.scope,
                    trace.scope.visiblePlots.indexOf(trace.legacy)));
        } else {
            Signal signal = sources.get(trace.text);
            if (signal != null)
                addMenuAction("Undock Scope", () -> {
                    sim.undockProbe(signal.elm, signal.value);
                    probes.remove(trace);
                });
        }
        addMenuAction("Remove", () -> {
            if (trace.legacy == null)
                probes.remove(trace);
            else
                removeSaved(trace);
            sim.repaint();
        });
        if (measure)
            addMenuAction(holdMeasurements ? "Resume readings" : "Hold readings",
                    () -> setHoldMeasurements(!holdMeasurements));
        addMenuAction("Cancel", () -> {});
        showMenu();
    }

    private void addLayoutActions(FlowPanel section) {
        String[] actions = {"Stack", "Unstack", "Combine"};
        for (String action : actions)
            section.add(new Button(action, (ClickHandler) event -> arrangeTraces(action.toLowerCase())));
    }

    boolean applySavedTraceOption(Scope scope, int plot, String action) {
        if (!useWaveform() || plot < 0 || plot >= scope.visiblePlots.size())
            return false;
        Trace trace = saved.get(scope.visiblePlots.get(plot));
        if (trace == null)
            return false;
        switch (action) {
            case "stack":
            case "combine":
                arrangeTraces(action);
                return true;
            case "unstack":
                setTraceSeparate(trace, true);
                return true;
            case "maxscale":
                setHoldScale(trace, !holdsScale(trace));
                return true;
            default:
                return false;
        }
    }

    boolean savedTraceHoldsScale(Scope scope, int plot) {
        Trace trace = plot >= 0 && plot < scope.visiblePlots.size() ? saved.get(scope.visiblePlots.get(plot)) : null;
        return trace == null ? scope.maxScale : holdsScale(trace);
    }

    void resetSavedHistory(Scope scope) {
        for (Trace trace : saved.values())
            if (trace.scope == scope)
                trace.clearSamples();
        lastMeasure = -1;
    }

    private void removeSaved(Trace trace) {
        int index = trace.scope.visiblePlots.indexOf(trace.legacy);
        if (index < 0)
            return;
        sim.pushUndo();
        trace.scope.removePlot(index);
        saved.remove(trace.legacy);
        sim.writeRecoveryToStorage();
        sim.unsavedChanges = true;
    }

    private String color(Trace trace) {
        return trace.color;
    }

    private void assignColor(Trace trace, Signal signal) {
        String color;
        if (signal != null && (signal.value == Scope.VAL_VOLTAGE || signal.value == Scope.VAL_CURRENT)) {
            Integer index = componentColors.get(signal.elm);
            if (index == null) {
                index = nextColor++;
                componentColors.put(signal.elm, index);
            }
            String[] palette = signal.value == Scope.VAL_CURRENT ? CURRENT_COLORS : COLORS;
            color = palette[index % palette.length];
        } else {
            if (trace.expressionColor == -1)
                trace.expressionColor = nextColor++;
            color = COLORS[trace.expressionColor % COLORS.length];
        }
        if (!color.equals(trace.color)) {
            trace.color = color;
            legendDirty = true;
        }
    }

    private String units(int index) {
        return index == Scope.UNITS_COUNT ? "" : Scope.getScaleUnitsText(index);
    }

    private String value(double value, int unit) {
        return readout(value, units(unit), highPrecision ? HIGH_PRECISION_DIGITS : PLOT_DIGITS);
    }

    private static String readout(double value, String unit, int digits) {
        return Double.isFinite(value) ? CircuitElm.getUnitText(value, unit, digits) : "—";
    }

    private static final class MeasurementRow {
        final Label name = new Label();
        final Label[] values = new Label[READING_NAMES.length];
        final double[] readings = new double[READING_NAMES.length];

        MeasurementRow() {
            name.setStyleName("probe-measurement-trace");
            for (int i = 0; i < values.length; i++)
                values[i] = new Label();
        }
    }

    private void layoutMeasurements() {
        int mask = 0;
        for (int i = 0; i < showReading.length; i++)
            if (showReading[i])
                mask |= 1 << i;
        int columns = measurementColumns();
        ArrayList<Trace> traces = new ArrayList<Trace>();
        for (Trace trace : visible)
            if (isTraceShown(trace))
                traces.add(trace);
        boolean readingsChanged = mask != measurementReadingMask || !traces.equals(measurementTraces);
        if (columns == measurementColumns && !readingsChanged)
            return;
        measurementColumns = columns;
        measurementReadingMask = mask;
        measurementTraces.clear();
        measurementTraces.addAll(traces);
        measurementRows.keySet().retainAll(traces);
        measurementTables.clear();
        if (readingsChanged)
            lastMeasure = -1;
        if (traces.isEmpty() || mask == 0) {
            noMeasurements.setText(traces.isEmpty() ? "No visible traces" : "No readings selected");
            measurementTables.add(noMeasurements);
            return;
        }
        for (int start = 0; start < traces.size(); start += columns) {
            FlexTable table = new FlexTable();
            table.setStyleName("probe-measurement-table");
            table.getElement().setAttribute("aria-label", "Waveform measurements");
            table.setText(0, 0, "Reading");
            int row = 1;
            for (int i = 0; i < showReading.length; i++) {
                if (!showReading[i])
                    continue;
                table.setText(row, 0, READING_NAMES[i]);
                table.getCellFormatter().getElement(row++, 0).setAttribute("role", "rowheader");
            }
            table.getRowFormatter().setStyleName(0, "probe-measurement-table-header");
            table.getCellFormatter().getElement(0, 0).setAttribute("role", "columnheader");
            int end = Math.min(start + columns, traces.size());
            for (int i = start; i < end; i++) {
                Trace trace = traces.get(i);
                MeasurementRow readings = measurementRows.get(trace);
                if (readings == null) {
                    readings = new MeasurementRow();
                    measurementRows.put(trace, readings);
                }
                int column = i - start + 1;
                table.setWidget(0, column, readings.name);
                table.getCellFormatter().getElement(0, column).setAttribute("role", "columnheader");
                row = 1;
                for (int reading = 0; reading < showReading.length; reading++)
                    if (showReading[reading])
                        table.setWidget(row++, column, readings.values[reading]);
            }
            measurementTables.add(table);
        }
    }

    private void setReading(Label label, String text) {
        if (!text.equals(label.getText()))
            label.setText(text);
    }

    private void updateMeasurementHeading(double range) {
        setReading(measurementTime, CircuitElm.getUnitText(range, "s") + " window"
                + (holdMeasurements ? " · Held" : ""));
    }

    private void updateMeasurementText(MeasurementRow row, int unit) {
        int digits = highPrecision ? HIGH_PRECISION_DIGITS : MEASUREMENT_DIGITS;
        for (int i = 0; i < row.values.length; i++) {
            if (!showReading[i])
                continue;
            double reading = row.readings[i];
            String text;
            if (i == DUTY_CYCLE)
                text = Double.isFinite(reading) ? CircuitElm.formatSignificant(100 * reading, digits) + "%" : "—";
            else if (i == FREQUENCY)
                text = readout(reading, "Hz", digits);
            else
                text = readout(reading, units(unit), digits);
            if (i == VERTICAL_SCALE && Double.isFinite(reading))
                text += "/div";
            setReading(row.values[i], text);
        }
    }

    private void updateMeasurements(double start, double end, double range) {
        double now = com.google.gwt.core.client.Duration.currentTimeMillis();
        if (lastMeasure >= 0 && (holdMeasurements || now - lastMeasure < 500
                && (sim.simIsRunning() || lastMeasureTime == sim.t)))
            return;
        lastMeasure = now;
        lastMeasureTime = sim.t;
        updateMeasurementHeading(range);
        if (measurementReadingMask == 0)
            return;
        for (Map.Entry<Trace, MeasurementRow> entry : measurementRows.entrySet()) {
            Trace trace = entry.getKey();
            MeasurementRow row = entry.getValue();
            WaveformHistory h = displayedHistory(trace);
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
            int first = h.firstAtOrAfter(start);
            int last = h.endAt(end);
            for (int i = first; i < last; i++) {
                double v = h.value(i);
                if (Double.isFinite(v)) {
                    low = Math.min(low, v);
                    high = Math.max(high, v);
                }
            }
            double latest = last <= first ? Double.NaN : h.value(last - 1);
            row.name.setTitle(trace.text);
            row.name.getElement().getStyle().setColor(color(trace));
            setReading(row.name, trace.text);
            row.readings[0] = latest;
            row.readings[1] = low;
            row.readings[2] = high;
            row.readings[VERTICAL_SCALE] = trace.limit / 2;
            if (showReading[AVERAGE] || showReading[RMS] || showReading[FREQUENCY] || showReading[DUTY_CYCLE]) {
                WaveformMeasurements readings = new WaveformMeasurements(h, first, last, low, high,
                        showReading[FREQUENCY] || showReading[DUTY_CYCLE]);
                row.readings[AVERAGE] = readings.average();
                row.readings[RMS] = readings.rms();
                row.readings[FREQUENCY] = readings.frequency();
                row.readings[DUTY_CYCLE] = readings.dutyCycle();
            }
            updateMeasurementText(row, trace.units);
        }
    }

    private String timeScaleText(double range) {
        return CircuitElm.getUnitText(range / 8, "s") + "/div";
    }

    private double timeRange() {
        return sim.maxTimeStep * 4096 * timeZoom;
    }

    private Trace activeTriggerSource() {
        Trace source = triggerSource;
        if (source == null) {
            for (Trace trace : visible) {
                if (isTraceShown(trace)) {
                    source = trace;
                    break;
                }
            }
        }
        return source != null && visible.contains(source) && isTraceShown(source) ? source : null;
    }

    private double triggeredEnd(double range, Trace source) {
        if (triggerMode == 0)
            return sim.t;
        if (source != null) {
            WaveformHistory history = displayedHistory(source);
            double postTrigger = range * .75;
            double earliest = triggerMode == 2 ? sim.t - postTrigger - range : history.size == 0
                    ? sim.t : history.time(0);
            int index = Math.min(history.size - 1, history.firstAtOrAfter(sim.t - postTrigger));
            for (; index > 0 && history.time(index) >= earliest; index--) {
                double a = history.value(index - 1), b = history.value(index);
                boolean crosses = fallingTrigger ? a > triggerLevel && b <= triggerLevel
                        : a < triggerLevel && b >= triggerLevel;
                if (!Double.isFinite(a) || !Double.isFinite(b) || !crosses)
                    continue;
                double before = history.time(index - 1), after = history.time(index);
                double edge = before + (after - before) * (triggerLevel - a) / (b - a);
                double end = edge + postTrigger;
                if (end <= sim.t && (triggerMode == 1 || edge >= earliest))
                    return end;
            }
        }
        return triggerMode == 2 ? sim.t : Double.NaN;
    }

    void draw(Graphics graphics, int panelHeight, int top) {
        int left = AXIS_MARGIN, right = sim.canvasWidth - AXIS_MARGIN;
        boolean extraScale = false;
        for (Trace trace : visible)
            extraScale |= isTraceShown(trace) && trace.units >= Scope.UNITS_W;
        int scaleTop = legend.isVisible() ? legend.getElement().getOffsetTop() + legend.getOffsetHeight() : top;
        top = Math.max(top, scaleTop + (extraScale ? 18 : 4));
        int bottom = panelHeight - 28 - measurementsHeight();
        plotLeft = left;
        plotRight = right;
        plotTop = top;
        plotBottom = bottom;
        Context2d g = graphics.context;
        double range = timeRange();
        Trace source = activeTriggerSource();
        double end = triggeredEnd(range, source);
        boolean waiting = Double.isNaN(end);
        if (waiting)
            end = sim.t;
        double start = end - range;
        ArrayList<PlotPane> panes = new ArrayList<PlotPane>();
        PlotPane overlay = new PlotPane();
        for (Trace trace : visible) {
            if (!isTraceShown(trace))
                continue;
            if (trace.stacked || trace.separate) {
                trace.pane = new PlotPane();
                panes.add(trace.pane);
            } else {
                trace.pane = overlay;
            }
            trace.pane.traces.add(trace);
        }
        if (!overlay.traces.isEmpty())
            panes.add(0, overlay);
        if (panes.isEmpty())
            panes.add(overlay);

        double[] limits = axisLimits;
        java.util.Arrays.fill(limits, 0);
        for (PlotPane pane : panes) {
            for (Trace trace : pane.traces) {
                WaveformHistory h = displayedHistory(trace);
                double peak = 0;
                int last = h.endAt(end);
                for (int i = h.firstAtOrAfter(start); i < last; i++) {
                    double v = h.value(i);
                    if (Double.isFinite(v))
                        peak = Math.max(peak, Math.abs(v));
                }
                if (holdsScale(trace))
                    trace.heldPeak = Math.max(trace.heldPeak, peak);
                trace.limit = holdsScale(trace) ? trace.heldPeak : peak;
                if (!trace.separate && trace.holdScale == null)
                    limits[trace.units] = Math.max(limits[trace.units], trace.limit);
            }
        }
        for (PlotPane pane : panes) {
            for (Trace trace : pane.traces) {
                if (!trace.separate && trace.holdScale == null)
                    trace.limit = limits[trace.units];
                trace.limit = Math.max(Double.MIN_NORMAL, Math.min(Double.MAX_VALUE,
                        (trace.limit == 0 ? 1 : trace.limit) * vertical));
            }
        }
        if (source != null && triggerDragLimit > 0)
            source.limit = triggerDragLimit;
        boolean showTrigger = triggerMode != 0 && source != null && right > left && bottom > top;
        triggerHandle.setVisible(showTrigger);
        if (measure)
            updateMeasurements(start, waiting ? Double.NEGATIVE_INFINITY : end, range);
        if (right <= left || bottom <= top)
            return;
        g.save();
        g.setFillStyle("#101315");
        g.fillRect(0, top - 1, sim.canvasWidth, bottom - top + 25);
        g.setFont("11px sans-serif");
        for (int i = 0; i < panes.size(); i++) {
            PlotPane pane = panes.get(i);
            pane.top = top + (bottom - top) * i / panes.size();
            pane.bottom = top + (bottom - top) * (i + 1) / panes.size() - (i + 1 < panes.size() ? 3 : 0);
            if (pane.bottom > pane.top)
                drawPane(g, pane, left, right, start, end, range, waiting, panes.size() > 1);
        }
        g.setFillStyle("#aebdd0");
        g.fillText(CircuitElm.getUnitText(start, "s"), left, bottom + 14, 72);
        g.setTextAlign(Context2d.TextAlign.CENTER);
        g.fillText(timeScaleText(range), (left + right) / 2.0, bottom + 14, 74);
        g.setTextAlign(Context2d.TextAlign.RIGHT);
        g.fillText(CircuitElm.getUnitText(end, "s"), right, bottom + 14, 72);
        g.setTextAlign(Context2d.TextAlign.LEFT);
        if (waiting)
            g.fillText("Waiting for trigger", left + 8, top + 16);
        if (!waiting && cursorEnabled && !isOpen() && !sim.dialogIsShowing())
            drawCursor(g, start, range);
        if (showTrigger)
            drawTrigger(g, source);
        g.restore();
    }

    private void drawPane(Context2d g, PlotPane pane, int left, int right, double start, double end,
                          double range, boolean waiting, boolean showNames) {
        int top = pane.top, bottom = pane.bottom;
        g.setStrokeStyle("#304155");
        g.setLineWidth(1);
        g.beginPath();
        for (int i = 0; i <= 8; i++) {
            double x = left + (right - left) * i / 8.0;
            g.moveTo(x, top);
            g.lineTo(x, bottom);
        }
        for (int i = 0; i <= 4; i++) {
            double y = top + (bottom - top) * i / 4.0;
            g.moveTo(left, y);
            g.lineTo(right, y);
        }
        dashedStroke(g);
        g.beginPath();
        g.setStrokeStyle("#697682");
        g.moveTo(left, (top + bottom) / 2.0);
        g.lineTo(right, (top + bottom) / 2.0);
        g.stroke();

        boolean[] drawn = new boolean[axisLimits.length];
        for (Trace trace : pane.traces) {
            if (drawn[trace.units] || pane.traces.size() > 1 && trace.holdScale != null)
                continue;
            drawn[trace.units] = true;
            int x = trace.units == Scope.UNITS_A ? right + 4 : 4;
            if (trace.units <= Scope.UNITS_A || pane.traces.size() == 1)
                drawAxis(g, trace, x, top, bottom);
            else {
                g.setFillStyle(color(trace));
                g.fillText(value(trace.limit / 2, trace.units) + "/div", left + 5, top + 12, 80);
            }
        }
        g.save();
        g.beginPath();
        g.rect(left, top, right - left, bottom - top);
        g.clip();
        if (!waiting) {
            g.setGlobalAlpha(highlightedTrace == null ? 1 : .25);
            for (Trace trace : pane.traces)
                if (trace != highlightedTrace)
                    drawTrace(g, trace, trace.limit, start, end, range, left, right, top, bottom);
            g.setGlobalAlpha(1);
            if (highlightedTrace != null && highlightedTrace.pane == pane)
                drawTrace(g, highlightedTrace, highlightedTrace.limit, start, end, range, left, right, top, bottom);
        }
        if (showNames && pane.traces.size() == 1) {
            Trace trace = pane.traces.get(0);
            g.setFillStyle(color(trace));
            g.fillText(trace.text, left + 5, top + 12, right - left - 10);
        }
        g.restore();
    }

    private void drawTrigger(Context2d g, Trace source) {
        double ratio = Math.max(-1, Math.min(1, triggerLevel / source.limit));
        double y = source.pane.top + (source.pane.bottom - source.pane.top) * (1 - ratio) / 2;
        triggerHandle.getElement().getStyle().setLeft(plotRight + 3, Unit.PX);
        triggerHandle.getElement().getStyle().setTop(y - 14, Unit.PX);
        String label = "Trigger level for " + source.text + ": " + value(triggerLevel, source.units);
        triggerHandle.setTitle(label);
        triggerHandle.getElement().setAttribute("aria-label", label);
        g.save();
        g.setStrokeStyle("#ffca63");
        g.setLineWidth(1);
        g.beginPath();
        g.moveTo(plotLeft, y);
        g.lineTo(plotRight, y);
        dashedStroke(g);
        g.restore();
    }

    private boolean beginTriggerDrag() {
        Trace source = activeTriggerSource();
        if (source == null || source.pane == null || triggerMode == 0 || isOpen() || sim.dialogIsShowing())
            return false;
        triggerDragLimit = source.limit;
        triggerDragTop = source.pane.top;
        triggerDragBottom = source.pane.bottom;
        return triggerDragLimit > 0 && triggerDragBottom > triggerDragTop;
    }

    private void moveTrigger(double y) {
        if (triggerDragLimit <= 0)
            return;
        double fraction = Math.max(0, Math.min(1, (y - triggerDragTop) / (triggerDragBottom - triggerDragTop)));
        triggerLevel = triggerDragLimit * (1 - 2 * fraction);
        scopeInfoChanged();
    }

    private void endTriggerDrag() {
        triggerDragLimit = 0;
        sim.repaint();
    }

    private native void bindTriggerHandle(com.google.gwt.dom.client.Element handle) /*-{
        var viewer = this, pointer = null, origin;
        handle.addEventListener("pointerdown", function(event) {
            if (pointer !== null || event.button !== 0 ||
                    !viewer.@com.lushprojects.circuitjs1.client.ProbeViewer::beginTriggerDrag()())
                return;
            event.preventDefault();
            pointer = event.pointerId;
            origin = event.clientY - handle.offsetTop - handle.offsetHeight / 2;
            handle.setPointerCapture(pointer);
        });
        handle.addEventListener("pointermove", function(event) {
            if (event.pointerId === pointer)
                viewer.@com.lushprojects.circuitjs1.client.ProbeViewer::moveTrigger(D)(event.clientY - origin);
        });
        handle.addEventListener("lostpointercapture", function(event) {
            if (event.pointerId === pointer) {
                pointer = null;
                viewer.@com.lushprojects.circuitjs1.client.ProbeViewer::endTriggerDrag()();
            }
        });
    }-*/;

    private void drawCursor(Context2d g, double start, double range) {
        double x = sim.simIsRunning() && inPlot(sim.mouseCursorX, sim.mouseCursorY) ? sim.mouseCursorX
                : plotLeft + touchCursor * (plotRight - plotLeft);
        double time = start + range * (x - plotLeft) / (plotRight - plotLeft);
        cursorTime = time;
        g.setLineWidth(1);
        g.setStrokeStyle("#e2e8f0");
        g.beginPath();
        g.moveTo(x, plotTop);
        g.lineTo(x, plotBottom);
        g.stroke();

        int width = Math.min(140, (plotRight - plotLeft) / 2 - 8);
        if (width <= 6)
            return;
        double labelX = x < (plotLeft + plotRight) / 2.0 ? x + 5 : x - width - 5;
        int row = plotTop + 2;
        g.setFont("11px sans-serif");
        drawCursorLabel(g, "t = " + CircuitElm.getTimeText(time), "#e2e8f0", labelX, row, width);
        for (Trace trace : visible) {
            WaveformHistory history = displayedHistory(trace);
            if (!isTraceShown(trace) || history.size == 0 || time < history.time(0)
                    || time > history.time(history.size - 1))
                continue;
            int sample = history.firstAtOrAfter(time);
            if (sample > 0 && time - history.time(sample - 1) < history.time(sample) - time)
                sample--;
            double reading = history.value(sample);
            if (!Double.isFinite(reading))
                continue;
            double y = (trace.pane.top + trace.pane.bottom) / 2.0
                    - reading / trace.limit * (trace.pane.bottom - trace.pane.top) / 2.0;
            g.setFillStyle(color(trace));
            g.beginPath();
            g.arc(x, y, 3, 0, 2 * Math.PI);
            g.fill();
            row += 14;
            drawCursorLabel(g, trace.text + " = " + value(reading, trace.units),
                    color(trace), labelX, row, width);
        }
    }

    private void drawCursorLabel(Context2d g, String text, String color, double x, int y, int width) {
        g.setFillStyle("#101315");
        g.fillRect(x, y, width, 14);
        g.setFillStyle(color);
        g.fillText(text, x + 3, y + 11, width - 6);
    }

    private static native void dashedStroke(Context2d context) /*-{
        context.setLineDash([2, 5]);
        context.stroke();
        context.setLineDash([]);
    }-*/;

    private void drawAxis(Context2d g, Trace trace, int x, int top, int bottom) {
        g.setFillStyle(color(trace));
        g.fillText(value(trace.limit, trace.units), x, top + 9, 46);
        if (bottom - top >= 30)
            g.fillText("0 " + units(trace.units), x, (top + bottom) / 2.0 + 4, 46);
        if (bottom - top >= 20)
            g.fillText(value(-trace.limit, trace.units), x, bottom, 46);
    }

    private void drawTrace(Context2d g, Trace trace, double limit, double start, double end, double range,
                           int left, int right, int top, int bottom) {
        WaveformHistory history = displayedHistory(trace);
        double center = (bottom + top) / 2.0;
        double halfHeight = (bottom - top) / 2.0;
        int index = history.firstAtOrAfter(start);
        int lastIndex = history.endAt(end);
        g.setStrokeStyle(color(trace));
        g.setLineWidth(trace == highlightedTrace ? 1.8 : .9);
        g.beginPath();
        boolean connected = false;
        while (index < lastIndex) {
            double v = history.value(index);
            if (!Double.isFinite(v)) {
                connected = false;
                index++;
                continue;
            }
            int x = left + (int) ((history.time(index) - start) / range * (right - left));
            double first = v, low = v, high = v, last = v;
            index++;
            while (index < lastIndex) {
                v = history.value(index);
                int nextX = left + (int) ((history.time(index) - start) / range * (right - left));
                if (nextX != x || !Double.isFinite(v))
                    break;
                low = Math.min(low, v);
                high = Math.max(high, v);
                last = v;
                index++;
            }
            double y = center - Math.max(-2, Math.min(2, first / limit)) * halfHeight;
            if (connected)
                g.lineTo(x, y);
            else {
                g.moveTo(x, y);
                g.lineTo(x + .5, y);
            }
            g.lineTo(x, center - Math.max(-2, Math.min(2, low / limit)) * halfHeight);
            g.lineTo(x, center - Math.max(-2, Math.min(2, high / limit)) * halfHeight);
            g.lineTo(x, center - Math.max(-2, Math.min(2, last / limit)) * halfHeight);
            connected = true;
        }
        g.stroke();
    }
}
