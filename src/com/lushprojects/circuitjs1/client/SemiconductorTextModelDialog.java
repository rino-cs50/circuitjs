package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lushprojects.circuitjs1.client.util.Locale;

final class SemiconductorTextModelDialog extends Dialog {
    SemiconductorTextModelDialog(CircuitElm component, EditDialog parent, SemiconductorTextModel loaded) {
        closeOnEnter = false;
        setText(Locale.LS(loaded == null ? "Create Model from Text" : "Edit Model"));
        VerticalPanel panel = new VerticalPanel();
        setWidget(panel);

        String type = SemiconductorTextModel.typeFor(component);
        Label instructions = new Label(Locale.LS("Paste one SPICE .model statement for") + " " + type + ".");
        instructions.setWidth("min(300px, 80vw)");
        panel.add(instructions);
        Label parameters = new Label(Locale.LS("Supported parameters") + ": "
            + SemiconductorTextModel.supportedParameters(type));
        parameters.setWidth("min(300px, 80vw)");
        panel.add(parameters);

        CircuitTextEditor editor = new CircuitTextEditor(Locale.LS("Model text"));
        editor.textArea.getElement().setAttribute("placeholder", SemiconductorTextModel.example(component));
        if (loaded != null)
            editor.setText(loaded.editorSource());
        panel.add(editor);

        Label error = new Label();
        error.setWidth("min(300px, 80vw)");
        error.getElement().setAttribute("role", "alert");
        panel.add(error);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setWidth("100%");
        panel.add(buttons);
        String saveLabel = Locale.LS("Save");
        Button save = new Button(saveLabel);
        Runnable resetConfirmation = () -> {
            save.setText(saveLabel);
            error.setText("");
        };
        editor.addInputHandler(resetConfirmation);
        buttons.add(save);
        String approximationLabel = Locale.LS("Save Approximation");
        save.addClickHandler(event -> {
            SemiconductorTextModel model;
            try {
                model = SemiconductorTextModel.parse(editor.textArea.getText(), type);
                if (model.isApproximation() && !approximationLabel.equals(save.getText())) {
                    error.setText(Locale.LS("CircuitJS will skip these parameters") + ": "
                        + model.unsupportedParameters() + ". "
                        + Locale.LS("This can change the simulated behavior."));
                    save.setText(approximationLabel);
                    return;
                }
                model.saveFor(component, loaded == null ? null : loaded.name);
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                return;
            }
            parent.applyTextModel(component, model);
            closeDialog();
        });
        Button cancel = new Button(Locale.LS("Cancel"));
        cancel.addClickHandler(event -> closeDialog());
        buttons.add(cancel);
        Button clear = new Button(Locale.LS("Clear"));
        clear.addClickHandler(event -> {
            editor.setText("");
            resetConfirmation.run();
            editor.textArea.setFocus(true);
        });
        buttons.add(clear);
        buttons.setCellWidth(clear, "100%");
        buttons.setCellHorizontalAlignment(clear, HorizontalPanel.ALIGN_RIGHT);
    }
}
