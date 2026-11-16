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

class ScopeElm extends CircuitElm {
    
    static final int DEFAULT_WIDTH = 128;
    static final int DEFAULT_HEIGHT = 64;

    Scope elmScope;
    boolean cursorEnabled;

    public ScopeElm(int xx, int yy) {
	super(xx, yy);
	noDiagonal=false;
	x2=x+DEFAULT_WIDTH;
	y2=y+DEFAULT_HEIGHT;
	elmScope = new Scope(sim);
	setPoints();
    }
    
    public void setScopeElm(CircuitElm e) {
	elmScope.setElm(e);
	elmScope.resetGraph();
    }
    
    public ScopeElm(int xa, int ya, int xb, int yb, int f,
		   StringTokenizer st) {
	super(xa, ya, xb, yb, f);
	noDiagonal=false;
	String sStr = st.nextToken();
	StringTokenizer sst = new StringTokenizer(sStr, "_");
	elmScope = new Scope(sim);
	elmScope.undump(sst);
	setPoints();
	elmScope.resetGraph();
    }
    
    public void setScopeRect() {
	int i1 = sim.transformX(min(x,x2));
	int i2 = sim.transformX(max(x,x2));
	int j1 = sim.transformY(min(y,y2));
	int j2 = sim.transformY(max(y,y2));
	Rectangle r = new Rectangle(i1,j1,i2-i1, j2-j1);
	if (!r.equals(elmScope.rect))
	    elmScope.setRect(r);
    }
    
    public void setPoints() {
	super.setPoints();
	setScopeRect();
    }
    
    public void setElmScope( Scope s) {
	elmScope=s;
	s.position = -1;
	s.dockedHeaderHeight = 0;
	setScopeRect();
    }
    
    
    public void stepScope() {
	elmScope.timeStep();
    }
    
    public void reset() {
	super.reset();
	elmScope.resetGraph(true);
    }
    
    public void clearElmScope() {
	elmScope = null;
    }
    
    boolean canViewInScope() { return false; }
    
    int getDumpType() { return 403; }

    public String dump() {
	String dumpStr = super.dump();
	String elmDump = elmScope.dump();
	if (elmDump == null)
	    return null;
	String sStr = elmDump.replace(' ', '_');
	sStr = sStr.replaceFirst("o_", ""); // remove unused prefix for embedded Scope
	return dumpStr + " " + sStr;
    }
    
    void draw(Graphics g) {
	g.setColor(needsHighlight() ? selectColor : whiteColor);
	g.context.save();
	// setTransform() doesn't work in version of canvas2svg we are using
	g.context.scale(1/sim.transform[0], 1/sim.transform[3]);
	g.context.translate(-sim.transform[4], -sim.transform[5] - sim.circuitArea.y);
	//g.context.scale(CirSim.devicePixelRatio(), CirSim.devicePixelRatio());

	setScopeRect();
	elmScope.position = -1;
	elmScope.draw(g);
	Rectangle button = cursorButtonBounds();
	g.setColor(cursorEnabled ? "#115ba1" : "#263445");
	g.fillRect(button.x, button.y, button.width, button.height);
	g.setColor(Color.white);
	int cx = button.x + button.width / 2;
	int cy = button.y + button.height / 2;
	g.context.beginPath();
	g.context.arc(cx, cy, 6, 0, 2 * Math.PI);
	g.context.stroke();
	g.drawLine(cx - 9, cy, cx + 9, cy);
	g.drawLine(cx, cy - 9, cx, cy + 9);
	g.context.restore();
	setBbox(point1, point2, 0);
	drawPosts(g);

    }
    
    int getPostCount() { return 0; }
    int getNumHandles() { return 2; }
    
    Rectangle cursorButtonBounds() {
	Rectangle rect = elmScope.rect;
	return new Rectangle(rect.x + rect.width - 30, rect.y + 4, 26, 26);
    }

    int touchResizeHandle(int sx, int sy) {
	double first = Math.hypot(sx - sim.transformX(x), sy - sim.transformY(y));
	double second = Math.hypot(sx - sim.transformX(x2), sy - sim.transformY(y2));
	int radius = 12;
	return Math.min(first, second) <= radius ? (first <= second ? 0 : 1) : -1;
    }

    void selectScope(int mx, int my) {
	if (cursorEnabled && !sim.dragging && !cursorButtonBounds().contains(mx, my))
	    elmScope.selectScope(mx, my);
	else
	    elmScope.checkForSelection(mx, my);
    }
}
