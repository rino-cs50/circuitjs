package com.lushprojects.circuitjs1.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lushprojects.circuitjs1.client.util.Locale;

final class MobileProjects {
    private static final String BLANK_CIRCUIT = "$ 1 5.0E-6 10 50 5.0\n";
    private static final int RECOVERY_DELAY_MS = 450;

    private final CirSim sim;
    private String name;
    private String lastSession;
    private boolean dirty;
    private boolean sessionPending;
    private boolean sessionError;
    private final Timer recoveryTimer = new Timer() {
        @Override
        public void run() {
            flushSession();
        }
    };

    MobileProjects(CirSim sim) {
        this.sim = sim;
    }

    void restore() {
        JavaScriptObject session = readSession();
        if (session == null) {
            sim.readCircuit(BLANK_CIRCUIT);
            sim.setCircuitTitle(Locale.LS("Blank Circuit"));
        } else {
            sim.readCircuit(sessionCircuit(session));
            sim.setCircuitTitle(sessionTitle(session));
            name = sessionName(session);
            dirty = sessionDirty(session);
        }
        if (session != null)
            lastSession = encodeSession(sessionCircuit(session), sim.getCircuitTitle(), name, dirty);
        sessionPending = false;
        recoveryTimer.cancel();
        installSessionFlush(this::flushSession);
    }

    void circuitChanged() {
        if (sim.isEditingSubcircuit())
            return;
        dirty = true;
        sessionPending = true;
        recoveryTimer.schedule(RECOVERY_DELAY_MS);
    }

    void flushSession() {
        if (sim.isEditingSubcircuit() || !sessionPending)
            return;
        String circuit = sim.dumpCircuit();
        storeSession(circuit);
        if (!sessionPending && dirty) {
            try {
                sim.writeRecoveryToStorage(circuit);
            } catch (RuntimeException ignored) {
                // The Android recovery copy has already been saved.
            }
        }
    }

    private void storeSession(String circuit) {
        recoveryTimer.cancel();
        String snapshot = encodeSession(circuit, sim.getCircuitTitle(), name, dirty);
        if (snapshot.equals(lastSession)) {
            sessionPending = false;
            return;
        }
        if (writeSession(snapshot)) {
            lastSession = snapshot;
            sessionPending = false;
            sessionError = false;
        } else {
            sessionPending = true;
            if (!sessionError) {
                sessionError = true;
                Window.alert(Locale.LS("The working circuit could not be saved for the next launch."));
            }
        }
    }

    void detachProject() {
        name = null;
        dirty = true;
        storeSession(sim.dumpCircuit());
    }

    void newBlank() {
        beforeReplacing(() -> replace(BLANK_CIRCUIT, Locale.LS("Blank Circuit"), null, false));
    }

    void open() {
        showOpenDialog();
    }

    void save(boolean saveAs, Runnable afterSave) {
        if (!saveAs && name != null)
            saveNamed(name, true, afterSave);
        else
            showSaveDialog(afterSave);
    }

    void beforeReplacing(Runnable action) {
        if (sim.isEditingSubcircuit()) {
            if (!Window.confirm(Locale.LS("Close the subcircuit editor before switching projects?")))
                return;
            while (sim.isEditingSubcircuit())
                sim.returnFromSubcircuit(false);
        }
        if (!dirty) {
            action.run();
            return;
        }
        Dialog dialog = new Dialog();
        dialog.closeOnEnter = false;
        dialog.setText(Locale.LS("Unsaved circuit"));
        VerticalPanel panel = new VerticalPanel();
        panel.setSpacing(8);
        panel.add(new Label(Locale.LS("Save changes to the current circuit?")));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(4);
        buttons.add(new Button(Locale.LS("Save"), (ClickHandler) event -> {
            dialog.closeDialog();
            save(false, action);
        }));
        buttons.add(new Button(Locale.LS("Discard"), (ClickHandler) event -> {
            dialog.closeDialog();
            action.run();
        }));
        buttons.add(new Button(Locale.LS("Cancel"), (ClickHandler) event -> dialog.closeDialog()));
        panel.add(buttons);
        show(dialog, panel);
    }

    private void showOpenDialog() {
        JsArrayString files = listProjects();
        Dialog dialog = new Dialog();
        dialog.closeOnEnter = false;
        dialog.setText(Locale.LS("Open File"));
        VerticalPanel panel = new VerticalPanel();
        panel.setSpacing(8);
        if (files == null || files.length() == 0) {
            panel.add(new Label(Locale.LS(files == null
                ? "Could not list saved circuits." : "No saved circuits yet.")));
        } else {
            VerticalPanel projects = new VerticalPanel();
            for (int i = 0; i < files.length(); i++) {
                String file = files.get(i);
                Button button = new Button();
                button.setText(file);
                button.addClickHandler(event -> {
                    String circuit = openProject(file);
                    if (circuit == null) {
                        Window.alert(Locale.LS("Could not open the saved circuit."));
                        return;
                    }
                    dialog.closeDialog();
                    beforeReplacing(() -> replace(circuit, file, file, false));
                });
                projects.add(button);
            }
            ScrollPanel scroll = new ScrollPanel(projects);
            scroll.getElement().getStyle().setProperty("maxHeight", "45vh");
            scroll.getElement().getStyle().setProperty("maxWidth", "75vw");
            panel.add(scroll);
        }
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(4);
        if (sim.loadFileInput != null)
            buttons.add(new Button(Locale.LS("Import from Device"), (ClickHandler) event -> {
                dialog.closeDialog();
                sim.loadFileInput.click();
            }));
        buttons.add(new Button(Locale.LS("Cancel"), (ClickHandler) event -> dialog.closeDialog()));
        panel.add(buttons);
        show(dialog, panel);
    }

    private void showSaveDialog(Runnable afterSave) {
        Dialog dialog = new Dialog();
        dialog.closeOnEnter = false;
        dialog.setText(Locale.LS("Save As"));
        VerticalPanel panel = new VerticalPanel();
        panel.setSpacing(8);
        panel.add(new Label(Locale.LS("Project name")));
        TextBox input = new TextBox();
        input.getElement().setAttribute("aria-label", Locale.LS("Project name"));
        input.setText(name == null ? "" : name.substring(0, name.length() - 4));
        input.setMaxLength(76);
        panel.add(input);
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(4);
        buttons.add(new Button(Locale.LS("Save"), (ClickHandler) event -> {
            String file = input.getText().trim();
            if (file.endsWith(".txt"))
                file = file.substring(0, file.length() - 4).trim();
            if (file.isEmpty() || file.startsWith(".") || file.matches(".*[\\\\/:*?\"<>|\\x00-\\x1f\\x7f].*")) {
                Window.alert(Locale.LS("Enter a project name without special file characters."));
                return;
            }
            file += ".txt";
            JsArrayString files = listProjects();
            if (files == null) {
                Window.alert(Locale.LS("Could not list saved circuits."));
                return;
            }
            boolean replace = false;
            for (int i = 0; i < files.length(); i++)
                if (file.equals(files.get(i)))
                    replace = true;
            if (replace && !Window.confirm(Locale.LS("Replace the existing circuit?") + "\n" + file))
                return;
            saveNamed(file, replace, () -> {
                dialog.closeDialog();
                afterSave.run();
            });
        }));
        buttons.add(new Button(Locale.LS("Cancel"), (ClickHandler) event -> dialog.closeDialog()));
        panel.add(buttons);
        show(dialog, panel);
        input.setFocus(true);
        input.selectAll();
    }

    private void saveNamed(String file, boolean replace, Runnable afterSave) {
        String circuit = sim.dumpCircuit();
        if (!writeProject(file, circuit, replace)) {
            Window.alert(Locale.LS("Could not save the circuit."));
            return;
        }
        name = file;
        dirty = false;
        sim.updateSavedProjectHistory(file, circuit);
        sim.setCircuitTitle(file);
        sim.unsavedChanges = false;
        sim.savedFlag = true;
        sim.repaint();
        storeSession(circuit);
        afterSave.run();
    }

    private void replace(String circuit, String title, String file, boolean changed) {
        sim.pushUndo();
        sim.readCircuit(circuit);
        restoreProject(file, title, changed);
    }

    void restoreProject(String file, String title, boolean changed) {
        name = file;
        dirty = changed;
        sim.setCircuitTitle(title);
        sim.savedFlag = !changed;
        storeSession(sim.dumpCircuit());
    }

    String projectName() {
        return name;
    }

    boolean hasUnsavedEdits() {
        return dirty;
    }

    private void show(Dialog dialog, VerticalPanel panel) {
        dialog.setWidget(panel);
        CirSim.dialogShowing = dialog;
        dialog.center();
    }

    static native boolean isAvailable() /*-{
        return !!$wnd.CircuitJSProjects;
    }-*/;

    private static native JsArrayString listProjects() /*-{
        var files = $wnd.CircuitJSProjects.list();
        return files == null ? null : JSON.parse(files);
    }-*/;

    private static native String openProject(String name) /*-{
        return $wnd.CircuitJSProjects.open(name);
    }-*/;

    private static native boolean writeProject(String name, String circuit, boolean replace) /*-{
        return $wnd.CircuitJSProjects.save(name, circuit, replace);
    }-*/;

    private static native JavaScriptObject readSession() /*-{
        try {
            var data = JSON.parse($wnd.CircuitJSProjects.restoreSession());
            return data && typeof data.circuit === "string" ? data : null;
        } catch (error) {
            return null;
        }
    }-*/;

    private static native String sessionCircuit(JavaScriptObject session) /*-{
        return session.circuit;
    }-*/;

    private static native String sessionTitle(JavaScriptObject session) /*-{
        return session.title;
    }-*/;

    private static native String sessionName(JavaScriptObject session) /*-{
        return session.name;
    }-*/;

    private static native boolean sessionDirty(JavaScriptObject session) /*-{
        return session.dirty;
    }-*/;

    private static native String encodeSession(String circuit, String title, String name, boolean dirty) /*-{
        return JSON.stringify({circuit: circuit, title: title, name: name, dirty: dirty});
    }-*/;

    private static native boolean writeSession(String data) /*-{
        return $wnd.CircuitJSProjects.saveSession(data);
    }-*/;

    private static native void installSessionFlush(Runnable handler) /*-{
        $wnd.flushCircuitJSSession = $entry(function() {
            handler.@java.lang.Runnable::run()();
        });
        $doc.addEventListener("visibilitychange", function() {
            if ($doc.hidden)
                $wnd.flushCircuitJSSession();
        });
    }-*/;
}
