package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Command;

public class HtmlSlider extends Composite {

    static final int HORIZONTAL = 1;

    private FlowPanel container;
    private InputElement rangeInput;
    private CircuitElm attachedElm;
    private Command command;
    private int min, max, currentValue;
    private boolean enabled = true;

    // Main constructor matching old Scrollbar signature
    public HtmlSlider(int orientation, int value, int visible, int minimum, int maximum, Command cmd, CircuitElm elm) {
        this.min = minimum;
        this.max = maximum;
        this.currentValue = value;
        this.command = cmd;
        this.attachedElm = elm;

        init();
    }

    // Constructor without CircuitElm
    public HtmlSlider(int orientation, int value, int visible, int minimum, int maximum, Command cmd) {
        this(orientation, value, visible, minimum, maximum, cmd, null);
    }

    // Simple constructor (what we used before)
    public HtmlSlider(int minimum, int maximum, int value) {
        this.min = minimum;
        this.max = maximum;
        this.currentValue = value;
        this.command = null;
        this.attachedElm = null;

        init();
    }

    private void init() {
        container = new FlowPanel();
        container.setStyleName("slider-container");

        HTML sliderHtml = new HTML("<input type='range' class='modern-slider' />");
        container.add(sliderHtml);

        rangeInput = (InputElement) sliderHtml.getElement().getFirstChildElement();
        rangeInput.setAttribute("min", String.valueOf(min));
        rangeInput.setAttribute("max", String.valueOf(max));
        rangeInput.setAttribute("value", String.valueOf(currentValue));

        Event.sinkEvents(rangeInput, Event.ONCHANGE);
        Event.setEventListener(rangeInput, new EventListener() {
            public void onBrowserEvent(Event event) {
                currentValue = Integer.parseInt(rangeInput.getValue());
                if (command != null)
                    command.execute();
            }
        });

        initWidget(container);
    }

    public int getValue() {
        if (rangeInput != null) {
            currentValue = Integer.parseInt(rangeInput.getValue());
        }
        return currentValue;
    }

    public void setValue(int val) {
        if (val < min) val = min;
        if (val > max) val = max;
        currentValue = val;
        if (rangeInput != null) {
            rangeInput.setValue(String.valueOf(val));
        }
    }

    public void enable() {
        enabled = true;
        if (rangeInput != null)
            rangeInput.setDisabled(false);
    }

    public void disable() {
        enabled = false;
        if (rangeInput != null)
            rangeInput.setDisabled(true);
    }

    // Compatibility methods for old Scrollbar API
    public void draw() {
        // HTML does the drawing automatically, no need for this
    }

    public void onMouseWheel(com.google.gwt.event.dom.client.MouseWheelEvent event) {
        // HTML range inputs handle mouse wheel automatically
        event.preventDefault();
    }
}