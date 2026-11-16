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

import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.lushprojects.circuitjs1.client.util.Locale;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class ImportFromTextDialog extends Dialog {
	
VerticalPanel vp;
HorizontalPanel hp;
CirSim sim;
TextArea textArea;
	
	public ImportFromTextDialog( CirSim asim) {
		super();
		sim=asim;
		closeOnEnter = false;
		Button okButton, cancelButton;
		final Checkbox subCheck;
		vp=new VerticalPanel();
		setWidget(vp);
		setText(Locale.LS("Import from Text"));
		final ListBox format = new ListBox();
		format.addItem(Locale.LS("Auto-detect"));
		format.addItem(Locale.LS("SPICE-style netlist"));
		format.addItem(Locale.LS("ngspice deck (first line is title)"));
		format.addItem(Locale.LS("CircuitJS text"));
		format.getElement().setAttribute("aria-label", Locale.LS("Import format"));
		vp.add(format);
		String pasteInstructions = Locale.LS("Paste CircuitJS text or a netlist. For a titled ngspice file, choose ngspice deck.");
		final Label instructions = new Label(pasteInstructions);
		instructions.setWidth("min(300px, 80vw)");
		vp.add(instructions);
		CircuitTextEditor editor = new CircuitTextEditor(Locale.LS("Circuit text"));
		textArea = editor.textArea;
		vp.add(editor);
		if (!sim.elmList.isEmpty())
			editor.setText(sim.dumpCircuit());
		vp.add(subCheck = new Checkbox(Locale.LS("Load Subcircuits Only")));
		final Label error = new Label();
		error.setWidth("min(300px, 80vw)");
		error.getElement().setAttribute("role", "alert");
		vp.add(error);
		format.addChangeHandler(event -> {
			int selected = format.getSelectedIndex();
			boolean netlist = selected == 1 || selected == 2;
			subCheck.setVisible(!netlist);
			instructions.setText(netlist
				? Locale.LS("Enter R, C, L, DC V/I, K coupling groups, or X subcircuit calls. Use 0 or GND for ground.")
				: pasteInstructions);
			textArea.getElement().setAttribute("placeholder", netlist
				? (selected == 2 ? "RC circuit\n" : "") + "V1 IN 0 DC 5\nR1 IN OUT 1k\nC1 OUT 0 100n\n.end"
				: "");
			error.setText("");
		});
		hp = new HorizontalPanel();
		hp.setWidth("100%");
		vp.add(hp);
		hp.add(okButton = new Button(Locale.LS("OK")));
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				String s = textArea.getText();
				int selected = format.getSelectedIndex();
				boolean netlist = selected == 1 || selected == 2
					|| (selected == 0 && !subCheck.getState() && !NetlistImport.looksLikeCircuitJS(s));
				if (netlist) {
					try {
						s = NetlistImport.toCircuit(s, selected == 2);
					} catch (IllegalArgumentException e) {
						error.setText(e.getMessage());
						return;
					}
				}
                boolean subcircuitsOnly = !netlist && subCheck.getState();
                closeDialog();
                if (!sim.importMobileCircuit(s, null, subcircuitsOnly)) {
                    sim.pushUndo();
                    sim.importCircuitFromText(s, subcircuitsOnly);
                }
			}
		});
		hp.add(cancelButton = new Button(Locale.LS("Cancel")));
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				closeDialog();
			}
		});
		Button clearButton = new Button(Locale.LS("Clear"));
		hp.add(clearButton);
		hp.setCellWidth(clearButton, "100%");
		hp.setCellHorizontalAlignment(clearButton, HorizontalPanel.ALIGN_RIGHT);
		clearButton.addClickHandler(event -> {
			editor.setText("");
			error.setText("");
			textArea.setFocus(true);
		});
		this.center();
		show();
	}
}
