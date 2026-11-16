/*    
    Copyright (C) Paul Falstad and Iain Sharp
    
    This file is part of CircuitJS1.

    CircuitJS1 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    CircuitJS1 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CircuitJS1.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lushprojects.circuitjs1.client;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lushprojects.circuitjs1.client.ChipElm.Pin;
import com.lushprojects.circuitjs1.client.util.Locale;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class EditCompositeModelDialog extends Dialog {
	
	VerticalPanel vp;
	boolean error;
	CustomCompositeChipElm chip;
	int postCount;
	Context2d context;
	CustomCompositeModel model;
	
	void setModel(CustomCompositeModel m) { model = m; }
	
        boolean createModel() {
            HashSet<Integer> nodeSet = new HashSet<Integer>();
            model = CirSim.theSim.getCircuitAsComposite();
            if (model == null)
        	return false;
            if (model.extList.size() == 0) {
        	Window.alert(Locale.LS("Device has no external inputs/outputs!"));
        	return false;
            }
            Collections.sort(model.extList, new Comparator<ExtListEntry>() {
        	public int compare(ExtListEntry a, ExtListEntry b) {
        	    return a.name.toLowerCase().compareTo(b.name.toLowerCase());
        	}
            });
            int i;
            int postCount = model.extList.size();
            int sideCounts[] = new int[] { 0, 0, 0, 0 };
            for (i = 0; i != postCount; i++) {
        	ExtListEntry pin = model.extList.get(i);
                sideCounts[pin.side] += 1;

        	if (nodeSet.contains(pin.node)) {
        	    Window.alert(Locale.LS("Can't have two input/output nodes connected!"));
        	    return false;
        	}
        	nodeSet.add(pin.node);
            }

            int xOffsetLeft = (sideCounts[ChipElm.SIDE_W] > 0) ? 1 : 0;
            int xOffsetRight = (sideCounts[ChipElm.SIDE_E] > 0) ? 1 : 0;
            for (i = 0; i != postCount; i++) {
                ExtListEntry pin = model.extList.get(i);
                if (pin.side == ChipElm.SIDE_N || pin.side == ChipElm.SIDE_S) {
                    pin.pos += xOffsetLeft;
                }
            }

            int minHeight = (sideCounts[ChipElm.SIDE_N] > 0 && sideCounts[ChipElm.SIDE_S] > 0) ? 2 : 1;
            int minWidth = 2;
            int pinsNS = Math.max(sideCounts[ChipElm.SIDE_N], sideCounts[ChipElm.SIDE_S]);
            int pinsWE = Math.max(sideCounts[ChipElm.SIDE_W], sideCounts[ChipElm.SIDE_E]);
            model.sizeX = Math.max(minWidth, pinsNS + xOffsetLeft + xOffsetRight);
            model.sizeY = Math.max(minHeight, pinsWE);

            model.modelCircuit = CirSim.theSim.dumpCircuit();
            return true;
        }
        
	public EditCompositeModelDialog() {
		super();
		closeOnEnter = true;
	}
	
	TextBox modelNameTextBox = null;
	Checkbox saveCheck = null;
	Checkbox labelCheck = null;

	void createDialog() {
		Button okButton;
		vp=new VerticalPanel();
		setWidget(vp);
		setText(Locale.LS("Edit Subcircuit Pin Layout"));
		vp.add(new Label(Locale.LS("Drag the pins to the desired position")));

		Canvas canvas = Canvas.createIfSupported();
		canvas.setCoordinateSpaceWidth(400);
		canvas.setCoordinateSpaceHeight(400);
		vp.add(canvas);
		addPinPointerHandlers(canvas.getCanvasElement());
		context = canvas.getContext2d();
		
		chip = new CustomCompositeChipElm(50, 50);
		chip.x2 = 200;
		chip.y2 = 50;
		selectedPin = -1;
		createPinsFromModel();
		
		if (model.name == null) {
		    vp.add(new Label(Locale.LS("Model Name")));
		    modelNameTextBox = new TextBox();
		    vp.add(modelNameTextBox);
		    modelNameTextBox.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
			    drawChip();
			}
		    });
//		    modelNameTextBox.setText(model.name);
		}
		
		HorizontalPanel hp = new HorizontalPanel();
		hp.add(new Label(Locale.LS("Width")));
		Button b;
		hp.add(b = new Button("+"));
		b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                	adjustChipSize(1, 0);
                    }
                });
		hp.add(b = new Button("-"));
		b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                	adjustChipSize(-1, 0);
                    }
                });
		hp.add(new Label(Locale.LS("Height")));
		hp.add(b = new Button("+"));
		b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                	adjustChipSize(0, 1);
                    }
                });
		hp.add(b = new Button("-"));
		b.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                	adjustChipSize(0, -1);
                    }
                });
		vp.add(hp);
		hp.addStyleName("topSpace");
		vp.add(labelCheck = new Checkbox(Locale.LS("Show Label")));
		labelCheck.setState(model.showLabel());
		labelCheck.addClickHandler(new ClickHandler() {
		    public void onClick(ClickEvent event) {
			model.setShowLabel(labelCheck.getValue());
			drawChip();
		    }
		});
		vp.add(saveCheck = new Checkbox(Locale.LS("Save Across Sessions")));
		saveCheck.setState(model.isSaved());
	
                hp = new HorizontalPanel();
                hp.setWidth("100%");
                hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
                hp.setStyleName("topSpace");
                vp.add(hp);
                hp.add(okButton = new Button(Locale.LS("OK")));
                hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
                Button cancelButton;
		if (model.name == null) {
		    hp.add(cancelButton = new Button(Locale.LS("Cancel")));
		    cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
			    closeDialog();
			}
		    });
		}
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
			    enterPressed();
			}
		});
		this.center();
	}
	
	void createPinsFromModel() {
	    postCount = model.extList.size();
	    chip.allocPins(postCount);
	    chip.sizeX = model.sizeX;
	    chip.sizeY = model.sizeY;
	    for (int i = 0; i != postCount; i++) {
		ExtListEntry pin = model.extList.get(i);
		chip.setPin(i, pin.pos, pin.side, pin.name);
		chip.volts[i] = 0;
		if (i == selectedPin)
		    chip.pins[i].selected = true;
	    }
	    chip.setPoints();
	}
	
	public void enterPressed() {
	    if (modelNameTextBox != null) {
		String name = modelNameTextBox.getText();
		if (name.length() == 0) {
		    Window.alert(Locale.LS("Please enter a model name."));
		    return;
		}
		model.setName(CustomCompositeElm.lastModelName = name);
	    }
	    model.setSaved(saveCheck.getState());
	    CirSim.theSim.updateModels();
	    CirSim.theSim.needAnalyze(); // will get singular matrix if we don't do this
	    closeDialog();
	}

	double scale;
	
	void drawChip() {
	    Graphics g = new Graphics(context);
	    double scalew = context.getCanvas().getWidth()  / (double)(chip.boundingBox.width + chip.boundingBox.x*2);
	    double scaleh = context.getCanvas().getHeight() / (double)(chip.boundingBox.height + chip.boundingBox.y*2);
	    scale = 1/Math.min(scalew, scaleh);
	    context.setFillStyle(CirSim.theSim.getBackgroundColor().getHexValue());
		context.setTransform(1, 0, 0, 1, 0, 0);
	    context.fillRect(0, 0, context.getCanvas().getWidth(), context.getCanvas().getHeight());
	    context.setTransform(1/scale, 0, 0, 1/scale, 0, 0);
	    chip.setLabel(!labelCheck.getValue() ? null : (modelNameTextBox != null) ? modelNameTextBox.getText() : model.name);
	    chip.draw(g);
	}
	
	void adjustChipSize(int dx, int dy) {
	    if (dx < 0 || dy < 0) {
		for (int i = 0; i != postCount; i++) {
		    Pin p = chip.pins[i];
		    if (dx < 0 && (p.side == ChipElm.SIDE_N || p.side == ChipElm.SIDE_S) && p.pos >= chip.sizeX+dx)
			return;
		    if (dy < 0 && (p.side == ChipElm.SIDE_E || p.side == ChipElm.SIDE_W) && p.pos >= chip.sizeY+dy)
			return;
		}
	    }
	    if (chip.sizeX + dx < 1 || chip.sizeY + dy < 1)
		return;
	    model.sizeX += dx;
	    model.sizeY += dy;
	    createPinsFromModel();
	    drawChip();
	}
	
	int selectedPin;

	private native void addPinPointerHandlers(CanvasElement canvas) /*-{
	    var dialog = this;
	    var pointer = null;
	    canvas.style.touchAction = "none";

	    function update(event, dragging) {
		var bounds = canvas.getBoundingClientRect();
		var x = (event.clientX - bounds.left) * canvas.width / bounds.width;
		var y = (event.clientY - bounds.top) * canvas.height / bounds.height;
		if (dragging)
		    dialog.@com.lushprojects.circuitjs1.client.EditCompositeModelDialog::movePin(DD)(x, y);
		else
		    return dialog.@com.lushprojects.circuitjs1.client.EditCompositeModelDialog::selectPin(DD)(x, y);
	    }

	    canvas.addEventListener("pointerdown", function(event) {
		if (pointer !== null || !event.isPrimary || event.button !== 0 || !update(event, false))
		    return;
		event.preventDefault();
		pointer = event.pointerId;
		canvas.setPointerCapture(pointer);
	    });
	    canvas.addEventListener("pointermove", function(event) {
		if (event.pointerId === pointer)
		    update(event, true);
		else if (pointer === null && event.pointerType === "mouse")
		    update(event, false);
	    });
	    canvas.addEventListener("lostpointercapture", function(event) {
		if (event.pointerId === pointer)
		    pointer = null;
	    });
	}-*/;

	boolean selectPin(double x, double y) {
	    // Give labels and leads a little padding, capped to half the pin spacing.
	    double bestDistance = Math.min(10 * scale, chip.cspc);
	    int nearest = -1;
	    for (int i = 0; i < postCount; i++) {
		Pin pin = chip.pins[i];
		double labelDistance = distanceFromBox(x * scale, y * scale,
			pin.labelLeft, pin.labelTop, pin.labelRight, pin.labelBottom);
		double leadDistance = distanceFromBox(x * scale, y * scale,
			Math.min(pin.post.x, pin.stub.x), Math.min(pin.post.y, pin.stub.y),
			Math.max(pin.post.x, pin.stub.x), Math.max(pin.post.y, pin.stub.y));
		double distance = Math.min(labelDistance, leadDistance);
		if (distance < bestDistance) {
		    bestDistance = distance;
		    nearest = i;
		}
	    }
	    if (nearest != selectedPin) {
		if (selectedPin >= 0)
		    chip.pins[selectedPin].selected = false;
		selectedPin = nearest;
		if (selectedPin >= 0)
		    chip.pins[selectedPin].selected = true;
		drawChip();
	    }
	    return selectedPin >= 0;
	}

	private double distanceFromBox(double x, double y, int left, int top, int right, int bottom) {
	    return Math.hypot(Math.max(0, Math.max(left - x, x - right)),
			      Math.max(0, Math.max(top - y, y - bottom)));
	}

	void movePin(double x, double y) {
	    int pos[] = new int[2];
	    if (!chip.getPinPos((int)(x * scale), (int)(y * scale), selectedPin, pos))
		return;
	    ExtListEntry pin = model.extList.get(selectedPin);
	    if (pin.pos == pos[0] && pin.side == pos[1])
		return;
	    int overlapping = chip.getOverlappingPin(pos[0], pos[1], selectedPin);
	    if (overlapping >= 0) {
		ExtListEntry other = model.extList.get(overlapping);
		other.pos = pin.pos;
		other.side = pin.side;
	    }
	    pin.pos = pos[0];
	    pin.side = pos[1];
	    createPinsFromModel();
	    drawChip();
	}

	public void show() {
	    super.show();
	    drawChip();
	}
}
