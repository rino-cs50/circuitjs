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

    class InductorElm extends CircuitElm {
	Inductor ind;
	double inductance;
	double initialCurrent;
	int couplingId;
	double couplingCoefficient;
	boolean reverseDot;
	InductorCoupling coupling;
	public InductorElm(int xx, int yy) {
	    super(xx, yy);
	    ind = new Inductor(sim);
	    inductance = 1;
	    ind.setup(inductance, current, flags);
	}
	public InductorElm(int xa, int ya, int xb, int yb, int f,
		    StringTokenizer st) {
	    super(xa, ya, xb, yb, f);
	    ind = new Inductor(sim);
	    inductance = new Double(st.nextToken()).doubleValue();
	    current = new Double(st.nextToken()).doubleValue();
	    try {
		initialCurrent = new Double(st.nextToken()).doubleValue();
	    } catch (Exception e) {}
	    if (st.hasMoreTokens() && st.nextToken().equals("coupled")) {
		int id = Integer.parseInt(st.nextToken());
		double k = Double.parseDouble(st.nextToken());
		boolean reversed = st.nextToken().equals("1");
		if (id > 0 && InductorCoupling.validCoefficient(k)) {
		    couplingId = id;
		    couplingCoefficient = k;
		    reverseDot = reversed;
		}
	    }
	    ind.setup(inductance, current, flags);
	}
	int getDumpType() { return 'l'; }
	String dump() {
	    String state = super.dump() + " " + inductance + " " + current + " " + initialCurrent;
	    if (couplingId > 0)
		state += " coupled " + couplingId + " " + couplingCoefficient + " " + (reverseDot ? 1 : 0);
	    return state;
	}
	void setPoints() {
	    super.setPoints();
	    calcLeads(32);
	}
	void draw(Graphics g) {
	    double v1 = volts[0];
	    double v2 = volts[1];
	    int i;
	    int hs = 8;
	    setBbox(point1, point2, hs);
	    draw2Leads(g);
	    setPowerColor(g, false);
	    drawCoil(g, 8, lead1, lead2, v1, v2);
	    if (referenceNumber == 0 && sim.showValuesCheckItem.getState()) {
		String s = getShortUnitText(inductance, "H");
		drawValues(g, s, hs);
	    }
	    if (couplingId > 0) {
		Point dot = interpPoint(lead1, lead2, reverseDot ? .85 : .15, -12);
		g.setColor(needsHighlight() ? selectColor : whiteColor);
		g.fillOval(dot.x - 3, dot.y - 3, 6, 6);
		Point label = interpPoint(lead1, lead2, .5, -21);
		g.setFont(unitsFont);
		drawCenteredText(g, "K" + couplingId, label.x, label.y, true);
	    }
	    doDots(g);
	    drawPosts(g);
	}
	void reset() {
	    volts[0] = volts[1] = curcount = 0;
	    current = initialCurrent;
	    ind.resetTo(initialCurrent);
	}
	void stamp() {
	    if (coupling == null)
		ind.stamp(nodes[0], nodes[1]);
	    else if (coupling.windings.get(0) == this)
		coupling.stamp(sim);
	}
	void startIteration() {
	    if (coupling == null)
		ind.startIteration(volts[0]-volts[1]);
	    else if (coupling.windings.get(0) == this)
		coupling.startIteration();
	}
	boolean nonLinear() { return ind.nonLinear(); }
	void calculateCurrent() {
	    if (coupling == null)
		current = ind.calculateCurrent(volts[0]-volts[1]);
	}
	void doStep() {
	    if (coupling == null)
		ind.doStep(volts[0]-volts[1]);
	    else if (coupling.windings.get(0) == this)
		coupling.doStep(sim);
	}
	void stepFinished() {
	    if (coupling != null && coupling.windings.get(0) == this)
		coupling.finishStep();
	}
	void flipPosts() {
	    super.flipPosts();
	    if (couplingId > 0) {
		reverseDot = !reverseDot;
		current = -current;
		initialCurrent = -initialCurrent;
	    }
	}
	void getInfo(String arr[]) {
	    arr[0] = "inductor";
	    getBasicInfo(arr);
	    arr[3] = "L = " + getUnitText(inductance, "H");
	    arr[4] = "P = " + getUnitText(getPower(), "W");
	}
	public EditInfo getEditInfo(int n) {
	    if (n == 0)
		return new EditInfo("Inductance (H)", inductance, 1e-2, 10);
	    if (n == 1 && couplingId == 0) {
		EditInfo ei = new EditInfo("", 0, -1, -1);
		ei.checkbox = new Checkbox("Trapezoidal Approximation",
					   ind.isTrapezoidal());
		return ei;
	    }
            if (n == (couplingId == 0 ? 2 : 1))
                return new EditInfo("Initial Current (on Reset) (A)", initialCurrent);
	    return null;
	}
	
	public void setEditValue(int n, EditInfo ei) {
	    if (n == 0 && ei.value > 0)
		inductance = ei.value;
	    if (n == 1 && couplingId == 0) {
		if (ei.checkbox.getState())
		    flags &= ~Inductor.FLAG_BACK_EULER;
		else
		    flags |= Inductor.FLAG_BACK_EULER;
	    }
            if (n == (couplingId == 0 ? 2 : 1))
                initialCurrent = ei.value;
	    ind.setup(inductance, current, flags);
	}
	
	int getShortcut() { return 'L'; }
	public double getInductance() { return inductance; }
	void setInductance(double l) {
	    inductance = l;
	    ind.setup(inductance, current, flags);
	}
    }
