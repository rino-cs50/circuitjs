package com.lushprojects.circuitjs1.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;

final class CircuitTextEditor extends FlowPanel {
    final TextArea textArea = new TextArea();
    private final Label lineNumbers = new Label("1");

    CircuitTextEditor(String label) {
        setStyleName("circuit-text-editor");
        lineNumbers.setStyleName("circuit-text-line-numbers");
        lineNumbers.getElement().setAttribute("aria-hidden", "true");
        add(lineNumbers);
        textArea.getElement().setAttribute("wrap", "off");
        textArea.getElement().setAttribute("spellcheck", "false");
        textArea.getElement().setAttribute("aria-label", label);
        add(textArea);
        addInputHandler(this::updateLineNumbers);
        textArea.addDomHandler(event -> syncScroll(), ScrollEvent.getType());
    }

    void addInputHandler(Runnable handler) {
        onInput(textArea.getElement(), handler);
    }

    void setText(String text) {
        textArea.setText(text);
        updateLineNumbers();
    }

    private void updateLineNumbers() {
        int count = textArea.getText().split("\n", -1).length;
        StringBuilder numbers = new StringBuilder("1");
        for (int line = 2; line <= count; line++)
            numbers.append('\n').append(line);
        lineNumbers.setText(numbers.toString());
        syncScroll();
    }

    private void syncScroll() {
        lineNumbers.getElement().setScrollTop(textArea.getElement().getScrollTop());
    }

    private static native void onInput(Element input, Runnable handler) /*-{
        input.addEventListener("input", $entry(function() {
            handler.@java.lang.Runnable::run()();
        }));
    }-*/;
}
