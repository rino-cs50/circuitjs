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

// GWT conversion (c) 2015 by Iain Sharp

// For information about the theory behind this, see Electronic Circuit & System Simulation Methods by Pillage
// or https://github.com/sharpie7/circuitjs1/blob/master/INTERNALS.md

import java.util.Vector;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.lang.Math;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.LineCap;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.PopupPanel;
import static com.google.gwt.event.dom.client.KeyCodes.*;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;
import com.lushprojects.circuitjs1.client.util.Locale;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.Navigator;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;

public class CirSim implements MouseDownHandler, MouseMoveHandler, MouseUpHandler,
ClickHandler, DoubleClickHandler, ContextMenuHandler, NativePreviewHandler,
MouseOutHandler, MouseWheelHandler {

    Timer resizeTimer = null;
    Random random;
    Button mobileMenuBtn;
    Button componentToolsButton;
    private CircuitElm touchSelectedComponent;
    private final ComponentMeasurementHistory componentMeasurements = new ComponentMeasurementHistory();
    private final AveragePowerLoss averagePowerLoss = new AveragePowerLoss();
    private static final int SHOW_AVERAGE_POWER_LOSS = 128;
    private boolean draggingPausedWaveformCursor;
    private boolean touchSelectionEnabled;
    Button resetButton;
    Button runStopButton;
    Button dumpMatrixButton;
    MenuItem aboutItem;
    MenuItem importFromLocalFileItem, importFromTextItem, exportAsUrlItem, exportAsLocalFileItem, exportAsTextItem,
            printItem, recoverItem, saveFileItem;
    MenuItem importFromDropboxItem;
    MenuItem undoItem, redoItem, cutItem, copyItem, pasteItem, selectAllItem, optionsItem, flipXItem, flipYItem, flipXYItem;
    MenuBar optionsMenuBar;
    CheckboxMenuItem dotsCheckItem;
    CheckboxMenuItem voltsCheckItem;
    CheckboxMenuItem powerCheckItem;
    private CheckboxMenuItem averagePowerLossCheckItem;
    CheckboxMenuItem smallGridCheckItem;
    CheckboxMenuItem crossHairCheckItem;
    CheckboxMenuItem showValuesCheckItem;
    private CheckBox voltageColorsCheckBox;
    private CheckBox currentFlowCheckBox;
    private CheckBox powerCheckBox;
    private CheckBox averagePowerLossCheckBox;
    private CheckBox componentValuesCheckBox;
    boolean showComponentLabels = true;
    private FlowPanel circuitControls;
    CheckboxMenuItem conductanceCheckItem;
    CheckboxMenuItem euroResistorCheckItem;
    CheckboxMenuItem euroGatesCheckItem;
    CheckboxMenuItem printableCheckItem;
    CheckboxMenuItem conventionCheckItem;
    CheckboxMenuItem noEditCheckItem;
    CheckboxMenuItem mouseWheelEditCheckItem;
    CheckboxMenuItem toolbarCheckItem;
    private Label powerLabel;
    private Label titleLabel;
    private HtmlSlider speedBar;
    private HtmlSlider currentBar;
    private HtmlSlider powerBar;
    MenuBar elmMenuBar;
    MenuItem elmCouplingMenuItem;
    MenuItem elmEditMenuItem;
    MenuItem elmCutMenuItem;
    MenuItem elmCopyMenuItem;
    MenuItem elmDeleteMenuItem;
    MenuItem elmScopeMenuItem;
    MenuItem elmFloatScopeMenuItem;
    MenuItem elmAddScopeMenuItem;
    MenuItem elmSplitMenuItem;
    MenuItem elmSliderMenuItem;
    MenuItem elmFlipXMenuItem, elmFlipYMenuItem, elmFlipXYMenuItem;
    MenuItem elmSwapMenuItem;
    MenuItem stackAllItem;
    MenuItem unstackAllItem;
    MenuItem combineAllItem;
    MenuItem separateAllItem;
    MenuBar mainMenuBar;
    boolean hideMenu = false;
    MenuBar selectScopeMenuBar;
    Vector<MenuItem> selectScopeMenuItems;
    MenuBar subcircuitMenuBar[];
    MenuItem scopeRemovePlotMenuItem;
    MenuItem scopeSelectYMenuItem;
    ScopePopupMenu scopePopupMenu;
    Element sidePanelCheckboxLabel;
   
    String lastCursorStyle;
    boolean mouseWasOverSplitter = false;

    // Class addingClass;
    PopupPanel contextPanel = null;
    int mouseMode = MODE_SELECT;
    private boolean multiplePlacement;
    int tempMouseMode = MODE_SELECT;
    String mouseModeStr = "Select";
    static final double pi = 3.14159265358979323846;
    static final int MODE_ADD_ELM = 0;
    static final int MODE_DRAG_ALL = 1;
    static final int MODE_DRAG_ROW = 2;
    static final int MODE_DRAG_COLUMN = 3;
    static final int MODE_DRAG_SELECTED = 4;
    static final int MODE_DRAG_POST = 5;
    static final int MODE_SELECT = 6;
    static final int MODE_DRAG_SPLITTER = 7;
    static final int infoWidth = 160;
    int dragGridX, dragGridY, dragScreenX, dragScreenY, initDragGridX, initDragGridY;
    long mouseDownTime;
    long zoomTime;
    int mouseCursorX = -1;
    int mouseCursorY = -1;
    Rectangle selectedArea;
    int gridSize, gridMask, gridRound;
    boolean dragging;
    boolean analyzeFlag, needsStamp, savedFlag;
    boolean dumpMatrix;
    boolean dcAnalysisFlag;
    // boolean useBufferedImage;
    boolean isMac;
    String ctrlMetaKey;
    double t;
    int pause = 10;
    int scopeSelected = -1;
    int scopeMenuSelected = -1;
    int menuScope = -1;
    int menuPlot = -1;
    int hintType = -1, hintItem1, hintItem2;
    String stopMessage;

    // current timestep (time between iterations)
    double timeStep;

    // maximum timestep (== timeStep unless we reduce it because of trouble
    // converging)
    double maxTimeStep;

    double wheelSensitivity = 1;

    // accumulated time since we incremented timeStepCount
    double timeStepAccum;

    // incremented each time we advance t by maxTimeStep
    int timeStepCount;

    double minFrameRate = 20;
    static final int HINT_LC = 1;
    static final int HINT_RC = 2;
    static final int HINT_3DB_C = 3;
    static final int HINT_TWINT = 4;
    static final int HINT_3DB_L = 5;
    Vector<CircuitElm> elmList;
    Vector<Adjustable> adjustables;
    // Vector setupList;
    CircuitElm dragElm, menuElm, stopElm;
    CircuitElm elmArr[];
    ScopeElm scopeElmArr[];
    private CircuitElm mouseElm = null;
    boolean didSwitch = false;
    int mousePost = -1;
    CircuitElm plotXElm, plotYElm;
    int draggingPost;
    SwitchElm heldSwitchElm;
    double circuitMatrix[][], circuitRightSide[], nodeVoltages[], origRightSide[], origMatrix[][];
    RowInfo circuitRowInfo[];
    int circuitPermute[];
    boolean simRunning;
    boolean circuitNonLinear;
    boolean hideSidebar = false;
    int voltageSourceCount;
    int circuitMatrixSize, circuitMatrixFullSize;
    boolean circuitNeedsMap;
    // public boolean useFrame;
    int scopeCount;
    Scope scopes[];
    boolean showResistanceInVoltageSources;
    boolean hideInfoBox;
    int scopeColCount[];
    static EditDialog editDialog, customLogicEditDialog, diodeModelEditDialog;
    static ScrollValuePopup scrollValuePopup;
    static Dialog dialogShowing;
    static AboutBox aboutBox;
    // Class dumpTypes[], shortcuts[];
    String shortcuts[];
    String clipboard;
    String recovery;
    Rectangle circuitArea;
    Vector<UndoItem> undoStack, redoStack;
    private SubcircuitEdit subcircuitEdit;
    private FlowPanel subcircuitActions;
    private final HashSet<String> staleSubcircuitStates = new HashSet<String>();
    double transform[];
    boolean unsavedChanges;
    private MobileProjects mobileProjects;
    HashMap<String, String> classToLabelMap;
    Toolbar toolbar;

    DockLayoutPanel layoutPanel;
    MenuBar menuBar;
    MenuBar fileMenuBar;
    FlowPanel verticalPanel;
    FlowPanel buttonPanel;
    private boolean mouseDragging;
    double scopeHeightFraction = -1;

    Vector<CheckboxMenuItem> mainMenuItems = new Vector<CheckboxMenuItem>();
    Vector<String> mainMenuItemNames = new Vector<String>();

    LoadFile loadFileInput;
    private CanvasImages canvasImages;
    Frame iFrame;

    Canvas cv;
    private Label canvasModeLabel, simulationStopLabel;
    Context2d cvcontext;

    // canvas width/height in px (before device pixel ratio scaling)
    int canvasWidth, canvasHeight;

    static final int MENUBARHEIGHT = 44;
    static final int MOBILE_MENUBAR_HEIGHT = 24;
    static final int TOOLBARHEIGHT = 48;
    static int VERTICALPANELWIDTH = 166; // default
    static final int POSTGRABSQ = 25;
    static final int MINPOSTGRABSIZE = 256;
    private boolean framePending;

    private void requestFrame() {
        if (framePending)
            return;
        framePending = true;
        AnimationScheduler.get().requestAnimationFrame(timestamp -> {
            framePending = false;
            updateCircuit();
            if (simRunning)
                requestFrame();
        }, cv.getElement());
    }

    int getrand(int x) {
        int q = random.nextInt();
        if (q < 0)
            q = -q;
        return q % x;
    }

    static native float devicePixelRatio() /*-{
        return window.devicePixelRatio;
    }-*/;

    void checkCanvasSize() {
        if (cv.getCoordinateSpaceWidth() != (int) (canvasWidth * devicePixelRatio()))
            setCanvasSize();
    }

    native boolean isMobile(Element element) /*-{
	if (!element)
	    return false;
	var style = getComputedStyle(element);
	return style.display != 'none';
    }-*/;
    
    public void setCanvasSize() {
	    int width, height;
	    width = (int) RootLayoutPanel.get().getOffsetWidth();
	    height = (int) RootLayoutPanel.get().getOffsetHeight();
	    boolean isMobile = width <= 768;
	    if (!hideMenu) {
	        int menuHeight = isMobile ? MOBILE_MENUBAR_HEIGHT : MENUBARHEIGHT;
	        layoutPanel.setWidgetSize(menuBar, menuHeight);
	        height -= menuHeight;
	    }
	    if (!hideSidebar)
	        RootPanel.getBodyElement().addClassName("has-simulation-controls");
	    if (!hideSidebar) {
	        VERTICALPANELWIDTH = isMobile ? 0 : Math.max(200, Math.min(width / 4, 350));
	        layoutPanel.setWidgetSize(verticalPanel, VERTICALPANELWIDTH);
	        if (!isMobile)
	            setMobileControlsOpen(false);
	        width -= VERTICALPANELWIDTH;
	    }

	    if (toolbarCheckItem.getState())
	        height -= TOOLBARHEIGHT;

	    width = Math.max(width, 0);
	    height = Math.max(height, 0);

	    if (cv != null) {
	        cv.setWidth(width + "PX");
	        cv.setHeight(height + "PX");
	        canvasWidth = width;
	        canvasHeight = height;
	        float scale = devicePixelRatio();
	        cv.setCoordinateSpaceWidth((int) (width * scale));
	        cv.setCoordinateSpaceHeight((int) (height * scale));
	    }

	    setCircuitArea();
	    
	    if (transform[0] == 0)
	        centreCircuit();

	    // Control mobile button visibility
	    if (mobileMenuBtn != null) {
	        mobileMenuBtn.setVisible(isMobile && !hideSidebar);
	    }
	}
    
    private void setMobileControlsOpen(boolean open) {
        if (open) {
            verticalPanel.addStyleName("visible");
            RootPanel.getBodyElement().addClassName("sidebar-open");
        } else {
            verticalPanel.removeStyleName("visible");
            RootPanel.getBodyElement().removeClassName("sidebar-open");
        }
        if (mobileMenuBtn != null)
            mobileMenuBtn.getElement().setAttribute("aria-expanded", Boolean.toString(open));
    }

    private double getScopeHeightFraction() {
        return scopeHeightFraction >= 0 ? scopeHeightFraction : isPortraitScopeLayout() ? 1.0 / 3 : .25;
    }

    private int scopeMeasurementsHeight;

    void setCircuitArea() {
        int height = canvasHeight;
        int width = canvasWidth;
        int h = (int) (height * getScopeHeightFraction());
        scopeMeasurementsHeight = 0;
        if (!hasScopePanel())
            h = probeViewer != null && hasTouchScope() && !hideMenu ? SCOPE_CONTROL_HEIGHT : 0;
        else if (scopeCollapsed)
            h = Math.min(height, SCOPE_CONTROL_HEIGHT);
        else {
            boolean waveform = probeViewer.useWaveform();
            int minimum = waveform ? 178 : 132;
            scopeMeasurementsHeight = waveform ? probeViewer.measurementsHeight() : 0;
            h = Math.min(Math.max(h, minimum), Math.max(0, height - 60 - scopeMeasurementsHeight))
                    + scopeMeasurementsHeight;
        }
        h = Math.min(height, h);
        circuitArea = new Rectangle(0, h, width, height - h);
    }

    private void setScopeCollapsed(boolean collapsed) {
        int previousTop = circuitArea.y;
        scopeCollapsed = collapsed;
        setCircuitArea();
        // The transform is relative to the circuit area. Keep the circuit
        // at the same screen position when the scope changes that origin.
        transform[5] += previousTop - circuitArea.y;
    }
    
    native String decompress(String dump) /*-{
        return $wnd.LZString.decompressFromEncodedURIComponent(dump);
    }-*/;

//    Circuit applet;

    CirSim() {
//	super("Circuit Simulator v1.6d");
//	applet = a;
//	useFrame = false;
	theSim = this;
    }

    String startCircuit = null;
    String startLabel = null;
    String startCircuitText = null;
    String startCircuitLink = null;
//    String baseURL = "http://www.falstad.com/circuit/";
    
    public void init() {

	boolean printable = false;
	boolean convention = true;
	boolean euroRes = false;
	boolean usRes = false;
	boolean running = true;
	boolean noEditing = false;
	boolean mouseWheelEdit = false;
	MenuBar m;

	CircuitElm.initClass(this);
	readRecovery();

	QueryParameters qp = new QueryParameters();
	String positiveColor = null;
	String negativeColor = null;
	String neutralColor = null;
	String selectColor = null;
	String currentColor = null;
	String mouseModeReq = null;
	boolean euroGates = false;

	try {
	    //baseURL = applet.getDocumentBase().getFile();
	    // look for circuit embedded in URL
	    //		String doc = applet.getDocumentBase().toString();
	    String cct=qp.getValue("cct");
	    if (cct!=null)
		startCircuitText = cct.replace("%24", "$");
	    if (startCircuitText == null)
		startCircuitText = getElectronStartCircuitText();
	    String ctz=qp.getValue("ctz");
	    if (ctz!= null)
		startCircuitText = decompress(ctz);
	    startCircuit = qp.getValue("startCircuit");
	    startLabel   = qp.getValue("startLabel");
	    startCircuitLink = qp.getValue("startCircuitLink");
	    euroRes = qp.getBooleanValue("euroResistors", false);
	    euroGates = qp.getBooleanValue("IECGates", getOptionFromStorage("euroGates", weAreInGermany()));
	    usRes = qp.getBooleanValue("usResistors",  false);
	    running = qp.getBooleanValue("running", true);
	    hideSidebar = qp.getBooleanValue("hideSidebar", false);
	    hideMenu = qp.getBooleanValue("hideMenu", false);
	    printable = qp.getBooleanValue("whiteBackground", getOptionFromStorage("whiteBackground", false));
	    convention = qp.getBooleanValue("conventionalCurrent",
		    getOptionFromStorage("conventionalCurrent", true));
	    noEditing = !qp.getBooleanValue("editable", true);
	    mouseWheelEdit = qp.getBooleanValue("mouseWheelEdit", getOptionFromStorage("mouseWheelEdit", true));
	    positiveColor = qp.getValue("positiveColor");
	    negativeColor = qp.getValue("negativeColor");
	    neutralColor = qp.getValue("neutralColor");
	    selectColor = qp.getValue("selectColor");
	    currentColor = qp.getValue("currentColor");
	    mouseModeReq = qp.getValue("mouseMode");
	    hideInfoBox = qp.getBooleanValue("hideInfoBox", false);
	} catch (Exception e) { }

	boolean euroSetting = false;
	if (euroRes)
	    euroSetting = true;
	else if (usRes)
	    euroSetting = false;
	else
	    euroSetting = getOptionFromStorage("euroResistors", !weAreInUS(true));

	transform = new double[6];
	String os = Navigator.getPlatform();
	isMac = (os.toLowerCase().contains("mac"));
	ctrlMetaKey = (isMac) ? Locale.LS("Cmd-") : Locale.LS("Ctrl-");

	shortcuts = new String[127];

	layoutPanel = new DockLayoutPanel(Unit.PX);

	fileMenuBar = new MenuBar(true);
	if (MobileProjects.isAvailable())
	    mobileProjects = new MobileProjects(this);
	if (isElectron())
	    fileMenuBar.addItem(menuItemWithShortcut("window", "New Window...", Locale.LS(ctrlMetaKey + "N"),
		    new MyCommand("file", "newwindow")));
	
	fileMenuBar.addItem(iconMenuItem("doc-new", "New Blank Circuit", new MyCommand("file", "newblankcircuit")));
	importFromLocalFileItem = menuItemWithShortcut("folder", "Open File...", Locale.LS(ctrlMetaKey + "O"),
		new MyCommand("file","importfromlocalfile"));
	importFromLocalFileItem.setEnabled(mobileProjects != null || LoadFile.isSupported());
	fileMenuBar.addItem(importFromLocalFileItem);
	importFromTextItem = iconMenuItem("doc-text", "Import From Text...", new MyCommand("file","importfromtext"));
	fileMenuBar.addItem(importFromTextItem);
	importFromDropboxItem = iconMenuItem("dropbox", "Import From Dropbox...", new MyCommand("file", "importfromdropbox"));
	fileMenuBar.addItem(importFromDropboxItem);
	if (isElectron() || mobileProjects != null) {
	    saveFileItem = fileMenuBar.addItem(menuItemWithShortcut("floppy", "Save", Locale.LS(ctrlMetaKey + "S"),
		    new MyCommand("file", "save")));
	    fileMenuBar.addItem(iconMenuItem("floppy", "Save As...", new MyCommand("file", "saveas")));
	} else {
	    exportAsLocalFileItem = menuItemWithShortcut("floppy", "Save As...", Locale.LS(ctrlMetaKey + "S"),
		    new MyCommand("file","exportaslocalfile"));
	    exportAsLocalFileItem.setEnabled(ExportAsLocalFileDialog.downloadIsSupported());
	    fileMenuBar.addItem(exportAsLocalFileItem);
	}
	exportAsUrlItem = iconMenuItem("export", "Export As Link...", new MyCommand("file","exportasurl"));
	fileMenuBar.addItem(exportAsUrlItem);
	exportAsTextItem = iconMenuItem("export", "Export As Text...", new MyCommand("file","exportastext"));
	fileMenuBar.addItem(exportAsTextItem);
	fileMenuBar.addItem(iconMenuItem("image", "Insert Image...", new MyCommand("file", "insertimage")));
	fileMenuBar.addItem(iconMenuItem("image", "Export As Image...", new MyCommand("file","exportasimage")));
	fileMenuBar.addItem(iconMenuItem("image", "Copy Circuit Image to Clipboard", new MyCommand("file","copypng")));
	fileMenuBar.addItem(iconMenuItem("image", "Export As SVG...", new MyCommand("file","exportassvg")));    	
	fileMenuBar.addItem(iconMenuItem("microchip", "Create Subcircuit...", new MyCommand("file","createsubcircuit")));
	fileMenuBar.addItem(iconMenuItem("magic", "Find DC Operating Point", new MyCommand("file", "dcanalysis")));
	recoverItem = iconMenuItem("back-in-time", "Recover Auto-Save", new MyCommand("file","recover"));
	recoverItem.setEnabled(recovery != null);
	fileMenuBar.addItem(recoverItem);
	printItem = menuItemWithShortcut("print", "Print...", Locale.LS(ctrlMetaKey + "P"), new MyCommand("file","print"));
	fileMenuBar.addItem(printItem);
	fileMenuBar.addSeparator();
	fileMenuBar.addItem(iconMenuItem("resize-full-alt", "Toggle Full Screen", new MyCommand("view", "fullscreen")));
	fileMenuBar.addSeparator();
	aboutItem = iconMenuItem("info-circled", "About...", (Command)null);
	fileMenuBar.addItem(aboutItem);
	aboutItem.setScheduledCommand(new MyCommand("file","about"));

	int width=(int)RootLayoutPanel.get().getOffsetWidth();
	VERTICALPANELWIDTH = width/4;  // Changed from /5 to /4 for 25%
	if (VERTICALPANELWIDTH > 350)  // Increased max width
	    VERTICALPANELWIDTH = 350;
	if (VERTICALPANELWIDTH < 200)  // Increased min width
	    VERTICALPANELWIDTH = 200;

	menuBar = new MenuBar();
	menuBar.addStyleName("circuit-menu");
	menuBar.addItem(Locale.LS("File"), fileMenuBar);
	verticalPanel=new FlowPanel();
	verticalPanel.setStyleName("circuit-sidebar");

	buttonPanel = new FlowPanel();
	buttonPanel.addStyleName("button-panel");
	
	m = new MenuBar(true);
	m.addItem(undoItem = menuItemWithShortcut("ccw", "Undo", Locale.LS(ctrlMetaKey + "Z"), new MyCommand("edit","undo")));
	m.addItem(redoItem = menuItemWithShortcut("cw", "Redo", Locale.LS(ctrlMetaKey + "Y"), new MyCommand("edit","redo")));
	m.addSeparator();
	m.addItem(cutItem = menuItemWithShortcut("scissors", "Cut", Locale.LS(ctrlMetaKey + "X"), new MyCommand("edit","cut")));
	m.addItem(copyItem = menuItemWithShortcut("copy", "Copy", Locale.LS(ctrlMetaKey + "C"), new MyCommand("edit","copy")));
	m.addItem(pasteItem = menuItemWithShortcut("paste", "Paste", Locale.LS(ctrlMetaKey + "V"), new MyCommand("edit","paste")));
	pasteItem.setEnabled(false);

	m.addItem(menuItemWithShortcut("clone", "Duplicate", Locale.LS(ctrlMetaKey + "D"), new MyCommand("edit","duplicate")));

	m.addSeparator();
	m.addItem(selectAllItem = menuItemWithShortcut("select-all", "Select All", Locale.LS(ctrlMetaKey + "A"), new MyCommand("edit","selectAll")));
	m.addSeparator();
	m.addItem(menuItemWithShortcut("search", "Find Component...", "/", new MyCommand("edit", "search")));
	m.addItem(iconMenuItem("target", weAreInUS(false) ? "Center Circuit" : "Centre Circuit", new MyCommand("edit", "centrecircuit")));
	m.addItem(menuItemWithShortcut("zoom-11", "Zoom 100%", "0", new MyCommand("zoom", "zoom100")));
	m.addItem(menuItemWithShortcut("zoom-in", "Zoom In", "+", new MyCommand("zoom", "zoomin")));
	m.addItem(menuItemWithShortcut("zoom-out", "Zoom Out", "-", new MyCommand("zoom", "zoomout")));
	m.addItem(flipXItem = iconMenuItem("flip-x", "Flip X", new MyCommand("edit", "flipx")));
	m.addItem(flipYItem = iconMenuItem("flip-y", "Flip Y", new MyCommand("edit", "flipy")));
	m.addItem(flipXYItem = iconMenuItem("flip-x-y", "Flip XY", new MyCommand("edit", "flipxy")));
	menuBar.addItem(Locale.LS("Edit"),m);

	MenuBar drawMenuBar = new MenuBar(true);
	drawMenuBar.setAutoOpen(true);

	menuBar.addItem(Locale.LS("Draw"), drawMenuBar);

	m = new MenuBar(true);
	m.addItem(stackAllItem = iconMenuItem("lines", "Stack All", new MyCommand("scopes", "stackAll")));
	m.addItem(unstackAllItem = iconMenuItem("columns", "Unstack All", new MyCommand("scopes", "unstackAll")));
	m.addItem(combineAllItem = iconMenuItem("object-group", "Combine All", new MyCommand("scopes", "combineAll")));
	m.addItem(separateAllItem = iconMenuItem("object-ungroup", "Separate All", new MyCommand("scopes", "separateAll")));
	menuBar.addItem(Locale.LS("Scopes"), m);

	optionsMenuBar = m = new MenuBar(true );
	menuBar.addItem(Locale.LS("Options"), optionsMenuBar);
	m.addItem(dotsCheckItem = new CheckboxMenuItem(Locale.LS("Show Current"), () -> {
	    syncSchematicDisplayControls();
	    circuitChanged();
	}));
	dotsCheckItem.setState(true);
	m.addItem(voltsCheckItem = new CheckboxMenuItem(Locale.LS("Show Voltage"),
		() -> changeSchematicColorMode(voltsCheckItem)));
	voltsCheckItem.setState(true);
	m.addItem(powerCheckItem = new CheckboxMenuItem(Locale.LS("Show Power"),
		() -> changeSchematicColorMode(powerCheckItem)));
	m.addItem(averagePowerLossCheckItem = new CheckboxMenuItem(Locale.LS("Show Average Power Loss"),
		() -> changeSchematicColorMode(averagePowerLossCheckItem)));
	m.addItem(showValuesCheckItem = new CheckboxMenuItem(Locale.LS("Show Values"),
		() -> setShowComponentValuesFromControl(showValuesCheckItem.getState())));
	setShowComponentValues(true);
	//m.add(conductanceCheckItem = getCheckItem(LS("Show Conductance")));
	m.addItem(smallGridCheckItem = new CheckboxMenuItem(Locale.LS("Small Grid"),
		new Command() { public void execute(){
		    setGrid();
                    circuitChanged();
		}
	}));
	m.addItem(toolbarCheckItem = new CheckboxMenuItem(Locale.LS("Toolbar"),
		new Command() { public void execute(){
		    setToolbar();
		}
	}));
	toolbarCheckItem.setState(!hideMenu && !noEditing && !hideSidebar && startCircuit == null && startCircuitText == null && startCircuitLink == null);
	m.addItem(crossHairCheckItem = new CheckboxMenuItem(Locale.LS("Show Cursor Cross Hairs"),
		new Command() { public void execute(){
		    setOptionInStorage("crossHair", crossHairCheckItem.getState());
		}
	}));
	crossHairCheckItem.setState(getOptionFromStorage("crossHair", false));
	m.addItem(euroResistorCheckItem = new CheckboxMenuItem(Locale.LS("European Resistors"),
		new Command() { public void execute(){
		    setOptionInStorage("euroResistors", euroResistorCheckItem.getState());
		    toolbar.setEuroResistors(euroResistorCheckItem.getState());
		}
	}));
	euroResistorCheckItem.setState(euroSetting);
	m.addItem(euroGatesCheckItem = new CheckboxMenuItem(Locale.LS("IEC Gates"),
		new Command() { public void execute(){
		    setOptionInStorage("euroGates", euroGatesCheckItem.getState());
		    int i;
		    for (i = 0; i != elmList.size(); i++)
			getElm(i).setPoints();
		}
	}));
	euroGatesCheckItem.setState(euroGates);
	m.addItem(printableCheckItem = new CheckboxMenuItem(Locale.LS("White Background"),
		new Command() { public void execute(){
		    int i;
		    for (i=0;i<scopeCount;i++)
			scopes[i].setRect(scopes[i].rect);
		    setOptionInStorage("whiteBackground", printableCheckItem.getState());
		}
	}));
	printableCheckItem.setState(printable);

	m.addItem(conventionCheckItem = new CheckboxMenuItem(Locale.LS("Conventional Current Motion"),
		new Command() { public void execute(){
		    setOptionInStorage("conventionalCurrent", conventionCheckItem.getState());
		    String cc = CircuitElm.currentColor.getHexValue();
		    // change the current color if it hasn't changed from the default
		    if (cc.equals("#ffff00") || cc.equals("#00ffff"))
			CircuitElm.currentColor = conventionCheckItem.getState() ? Color.yellow : Color.cyan;
		}
	}));
	conventionCheckItem.setState(convention);
	m.addItem(noEditCheckItem = new CheckboxMenuItem(Locale.LS("Disable Editing")));
	noEditCheckItem.setState(noEditing);

	m.addItem(mouseWheelEditCheckItem = new CheckboxMenuItem(Locale.LS("Edit Values With Mouse Wheel"),
		new Command() { public void execute(){
		    setOptionInStorage("mouseWheelEdit", mouseWheelEditCheckItem.getState());
		}
	}));
	mouseWheelEditCheckItem.setState(mouseWheelEdit);

	m.addItem(new CheckboxAlignedMenuItem(Locale.LS("Shortcuts..."), new MyCommand("options", "shortcuts")));
	m.addItem(new CheckboxAlignedMenuItem(Locale.LS("Subcircuits..."), new MyCommand("options", "subcircuits")));
	m.addItem(optionsItem = new CheckboxAlignedMenuItem(Locale.LS("Other Options..."), new MyCommand("options","other")));
	if (isElectron())
	    m.addItem(new CheckboxAlignedMenuItem(Locale.LS("Toggle Dev Tools"), new MyCommand("options","devtools")));

	mainMenuBar = new MenuBar(true);
	mainMenuBar.setAutoOpen(true);
	composeMainMenu(mainMenuBar, 0);
	composeMainMenu(drawMenuBar, 1);
	loadShortcuts();

	toolbar = new Toolbar();
	toolbar.setEuroResistors(euroSetting);
	if (!hideMenu)
	    layoutPanel.addNorth(menuBar, MENUBARHEIGHT);
	else
	    RootPanel.getBodyElement().addClassName("circuit-menu-hidden");

	layoutPanel.addSouth(toolbar, TOOLBARHEIGHT);

	// Add sidebar to layoutPanel initially (BEFORE canvas)
	if (!hideSidebar)
	    layoutPanel.addEast(verticalPanel, VERTICALPANELWIDTH);

	RootLayoutPanel.get().add(layoutPanel);

	cv =Canvas.createIfSupported();
	if (cv==null) {
	    RootPanel.get().add(new Label("Not working. You need a browser that supports the CANVAS element."));
	    return;
	}

	cvcontext=cv.getContext2d();
	canvasImages = new CanvasImages(this, cv.getCanvasElement());

	setToolbar();

	
	// Add canvas LAST (it becomes the CENTER widget)
	FlowPanel canvasPanel = new FlowPanel();
	canvasPanel.setStyleName("circuit-canvas");
	canvasPanel.add(cv);
	canvasModeLabel = new Label();
	canvasModeLabel.setStyleName("circuit-canvas-mode");
	canvasModeLabel.getElement().setAttribute("role", "status");
	canvasPanel.add(canvasModeLabel);
        simulationStopLabel = new Label();
        simulationStopLabel.setStyleName("circuit-simulation-stop");
        simulationStopLabel.getElement().setAttribute("role", "alert");
        simulationStopLabel.setVisible(false);
        canvasPanel.add(simulationStopLabel);
        scopeControls = new FlowPanel();
        scopeControls.setStyleName("circuit-scope-controls");
        scopeControls.setVisible(false);

        scopeTraceButton = new Button("Probes & Math");
        scopeTraceButton.setStyleName("circuit-scope-trace");
        scopeTraceButton.addClickHandler(event -> {
            if (scopeCollapsed) {
                setScopeCollapsed(false);
                repaint();
            }
            if (probeViewer.isOpen()) {
                probeViewer.close();
                return;
            }
            probeViewer.open(null);
        });
        scopeControls.add(scopeTraceButton);

        FlowPanel backdrop = new FlowPanel();
        backdrop.setStyleName("circuit-scope-dismiss");
        probeViewer = new ProbeViewer(this, scopeTraceButton, backdrop);
        backdrop.addDomHandler(event -> probeViewer.close(), ClickEvent.getType());

        scopeSettingsButton = new Button("⚙");
        scopeSettingsButton.setTitle(Locale.LS("Oscilloscope controls"));
        scopeSettingsButton.getElement().setAttribute("aria-label", Locale.LS("Oscilloscope controls"));
        scopeSettingsButton.addClickHandler(event -> {
            if (dialogIsShowing() || noEditCheckItem.getState())
                return;
            probeViewer.close();
            if (hasTouchScope() || probeViewer.useWaveform() || probeViewer.specialScope() != null)
                probeViewer.showProbeProperties(activeDockedScope);
            else if (activeDockedScope != null && activeDockedScope.canMenu())
                activeDockedScope.properties();
        });
        scopeControls.add(scopeSettingsButton);
        scopeControls.add(probeViewer.viewButton);

        scopeActions = new FlowPanel();
        scopeActions.setStyleName("probe-actions");
        scopeActions.add(scopeAction("Y+", "Increase vertical magnification", () -> probeViewer.scaleVertical(.5)));
        scopeActions.add(scopeAction("Y−", "Decrease vertical magnification", () -> probeViewer.scaleVertical(2)));
        scopeActions.add(scopeAction("≡", "Toggle measurements", () -> probeViewer.toggleMeasurements()));
        scopeActions.add(probeViewer.cursorButton);
        scopeActions.add(scopeAction("↺", "Reset scope zoom", () -> probeViewer.resetView()));
        scopeControls.add(scopeActions);

        scopeCollapseButton = new Button();
        scopeCollapseButton.addClickHandler(event -> {
            setScopeCollapsed(!scopeCollapsed);
            probeViewer.close();
            clearMouseElm();
            mouseCursorX = -1;
            mouseWasOverSplitter = false;
            repaint();
        });
        scopeControls.add(scopeCollapseButton);

        canvasPanel.add(backdrop);
        canvasPanel.add(scopeControls);
        canvasPanel.add(probeViewer.legend);
        canvasPanel.add(probeViewer.triggerHandle);
        canvasPanel.add(probeViewer.measurements);
        canvasPanel.add(probeViewer.traceMenu);
	layoutPanel.add(canvasPanel);
	updateToolbar();

	// Now call setCanvasSize to adjust for mobile/desktop
	setCanvasSize();

	Label controlsTitle = new Label(Locale.LS("Simulation Controls"));
	controlsTitle.setStyleName("controls-title");
	verticalPanel.add(controlsTitle);
	FlowPanel simulationControls = addControlsSection("Simulation");
	FlowPanel buttonRow = new FlowPanel();
	buttonRow.addStyleName("button-row");
	simulationControls.add(buttonRow);

	if (!hideSidebar) {
	    mobileMenuBtn = new Button("⚙");
	    mobileMenuBtn.addStyleName("mobile-menu-button");
	    mobileMenuBtn.setTitle(Locale.LS("Simulation Controls"));
	    mobileMenuBtn.getElement().setAttribute("aria-label", Locale.LS("Simulation Controls"));
	    mobileMenuBtn.addClickHandler(event -> setMobileControlsOpen(
	        !verticalPanel.getElement().hasClassName("visible")));
	    RootPanel.get().add(mobileMenuBtn);
	    setMobileControlsOpen(false);
	}

	componentToolsButton = new Button();
	componentToolsButton.setHTML("<span aria-hidden='true'>&#128295;&#65038;</span>");
	componentToolsButton.addStyleName("component-tools-button");
	componentToolsButton.setTitle(Locale.LS("Component Tools"));
	componentToolsButton.getElement().setAttribute("aria-label", Locale.LS("Component Tools"));
	componentToolsButton.getElement().setAttribute("aria-haspopup", "menu");
	componentToolsButton.setVisible(false);
	componentToolsButton.addClickHandler(event -> openComponentTools());
	canvasPanel.add(componentToolsButton);

	// Play/Pause button with label
	buttonRow.add(runStopButton = new Button());
	runStopButton.setHTML("<i class='button-icon fas fa-play'>▶</i><span>Play</span>");
	runStopButton.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent event) {
		setSimRunning(!simIsRunning());
	    }
	});

	// Reset button
	buttonRow.add(resetButton = new Button());
	resetButton.setHTML("<i class='button-icon fas fa-rotate-left'>⟲</i><span>Reset</span>");
	resetButton.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent event) {
		resetAction();
	    }
	});

	/*
	dumpMatrixButton = new Button("Dump Matrix");
	dumpMatrixButton.addClickHandler(new ClickHandler() {
	    public void onClick(ClickEvent event) { dumpMatrix = true; }});
	verticalPanel.add(dumpMatrixButton);// IES for debugging
	*/

	// Create LoadFile but don't add it to the panel (hides the Browse button)
	if (LoadFile.isSupported())
	    loadFileInput = new LoadFile(this);
	RootPanel.get().add(loadFileInput);

	simulationControls.add(new Label(Locale.LS("Simulation Speed")));
	simulationControls.add(speedBar = new HtmlSlider(0, 260, 3, this::circuitChanged));
	simulationControls.add(new Label(Locale.LS("Current Speed")));
	currentBar = new HtmlSlider(1, 100, 50, this::circuitChanged);
	simulationControls.add(currentBar);

	// Hide Power Brightness - create but don't add to panel
	powerLabel = new Label (Locale.LS("Power Brightness"));
	powerLabel.addStyleName("topSpace");
	powerBar = new HtmlSlider(1, 100, 50, this::circuitChanged);
	setPowerBarEnable();

	FlowPanel displayControls = addControlsSection("Display");
	voltageColorsCheckBox = new CheckBox(Locale.LS("Show Voltage Colors"));
	voltageColorsCheckBox.addValueChangeHandler(event -> {
	    if (event.getValue() != voltsCheckItem.getState())
		voltsCheckItem.execute();
	});
	displayControls.add(voltageColorsCheckBox);
	currentFlowCheckBox = new CheckBox(Locale.LS("Show Current Flow"));
	currentFlowCheckBox.addValueChangeHandler(event -> {
	    if (event.getValue() != dotsCheckItem.getState())
		dotsCheckItem.execute();
	});
	displayControls.add(currentFlowCheckBox);
	powerCheckBox = new CheckBox(Locale.LS("Show Power"));
	powerCheckBox.addValueChangeHandler(event -> {
	    if (event.getValue() != powerCheckItem.getState())
		powerCheckItem.execute();
	});
	displayControls.add(powerCheckBox);
	averagePowerLossCheckBox = new CheckBox(Locale.LS("Show Average Power Loss"));
	averagePowerLossCheckBox.addValueChangeHandler(event -> {
	    if (event.getValue() != averagePowerLossCheckItem.getState())
		averagePowerLossCheckItem.execute();
	});
	displayControls.add(averagePowerLossCheckBox);
	syncSchematicDisplayControls();

	Storage preferences = Storage.getLocalStorageIfSupported();
	showComponentLabels = preferences == null ||
		!"false".equals(preferences.getItem("showComponentLabels"));
	CheckBox labels = new CheckBox(Locale.LS("Show Component Labels"));
	labels.setValue(showComponentLabels);
	labels.addValueChangeHandler(event -> {
	    showComponentLabels = event.getValue();
	    if (preferences != null)
		preferences.setItem("showComponentLabels", Boolean.toString(showComponentLabels));
	    repaint();
	});
	displayControls.add(labels);
	componentValuesCheckBox = new CheckBox(Locale.LS("Show Component Values"));
	componentValuesCheckBox.setValue(showValuesCheckItem.getState());
	componentValuesCheckBox.addValueChangeHandler(event ->
		setShowComponentValuesFromControl(event.getValue()));
	displayControls.add(componentValuesCheckBox);

	circuitControls = addControlsSection("Current Circuit");
	circuitControls.add(titleLabel = new Label(""));
	titleLabel.addStyleName("controls-circuit-name");

	subcircuitActions = new FlowPanel();
	subcircuitActions.setStyleName("controls-section");
	FlowPanel subcircuitButtonRow = new FlowPanel();
	subcircuitButtonRow.setStyleName("button-row");
	subcircuitButtonRow.add(new Button(Locale.LS("Save and Return"), (ClickHandler) event -> saveSubcircuitAndReturn()));
	subcircuitButtonRow.add(new Button(Locale.LS("Cancel"), (ClickHandler) event -> returnFromSubcircuit(false)));
	subcircuitActions.add(subcircuitButtonRow);
	subcircuitActions.setVisible(false);
	verticalPanel.add(subcircuitActions);

	verticalPanel.add(iFrame = new Frame("iframe.html"));
	iFrame.setWidth(VERTICALPANELWIDTH+"px");
	iFrame.setHeight("100 px");
	iFrame.getElement().setAttribute("scrolling", "no");

	setGrid();
	elmList = new Vector<CircuitElm>();
	adjustables = new Vector<Adjustable>();
	//	setupList = new Vector();
	undoStack = new Vector<UndoItem>();
	redoStack = new Vector<UndoItem>();


	scopes = new Scope[20];
	scopeColCount = new int[20];
	scopeCount = 0;

	random = new Random();
	//	cv.setBackground(Color.black);
	//	cv.setForeground(Color.lightGray);

	elmMenuBar = new MenuBar(true);
	elmMenuBar.setAutoOpen(true);
	selectScopeMenuBar = new MenuBar(true) {
	    @Override
	    
	    // when mousing over scope menu item, select associated scope
	    public void onBrowserEvent(Event event) {
		int currentItem = -1;
		int i;
		for (i = 0; i != selectScopeMenuItems.size(); i++) {
		    MenuItem item = selectScopeMenuItems.get(i);
		    if (DOM.isOrHasChild(item.getElement(), DOM.eventGetTarget(event))) {
			//MenuItem found here
			currentItem = i;
		    }
		}
		switch (DOM.eventGetType(event)) {
		case Event.ONMOUSEOVER:
		    scopeMenuSelected = currentItem; 
		    break;              
		case Event.ONMOUSEOUT:
		    scopeMenuSelected = -1;
		    break;              
		}
		super.onBrowserEvent(event);
	    }
	};
	
	elmMenuBar.addItem(elmEditMenuItem = new MenuItem(Locale.LS("Edit..."),new MyCommand("elm","edit")));
	elmMenuBar.addItem(elmCouplingMenuItem = new MenuItem(Locale.LS("Couple Inductors..."), new MyCommand("elm", "coupleInductors")));
	elmMenuBar.addItem(elmScopeMenuItem = new MenuItem(Locale.LS("View in New Scope"), new MyCommand("elm","viewInScope")));
	elmMenuBar.addItem(elmFloatScopeMenuItem  = new MenuItem(Locale.LS("View in New Undocked Scope"), new MyCommand("elm","viewInFloatScope")));
	elmMenuBar.addItem(elmAddScopeMenuItem = new MenuItem(Locale.LS("Add to Existing Scope"), new MyCommand("elm", "addToScope0")));
	elmMenuBar.addItem(elmCutMenuItem = new MenuItem(Locale.LS("Cut"),new MyCommand("elm","cut")));
	elmMenuBar.addItem(elmCopyMenuItem = new MenuItem(Locale.LS("Copy"),new MyCommand("elm","copy")));
	elmMenuBar.addItem(elmDeleteMenuItem = new MenuItem(Locale.LS("Delete"),new MyCommand("elm","delete")));
	elmMenuBar.addItem(                    new MenuItem(Locale.LS("Duplicate"),new MyCommand("elm","duplicate")));
	elmMenuBar.addItem(elmSwapMenuItem = new MenuItem(Locale.LS("Swap Terminals"),new MyCommand("elm","flip")));
	elmMenuBar.addItem(elmFlipXMenuItem =  new MenuItem(Locale.LS("Flip X"),new MyCommand("elm","flipx")));
	elmMenuBar.addItem(elmFlipYMenuItem =  new MenuItem(Locale.LS("Flip Y"),new MyCommand("elm","flipy")));
	elmMenuBar.addItem(elmFlipXYMenuItem =  new MenuItem(Locale.LS("Flip XY"),new MyCommand("elm","flipxy")));
	elmMenuBar.addItem(elmSplitMenuItem = menuItemWithShortcut("", "Split Wire", Locale.LS(ctrlMetaKey + "click"), new MyCommand("elm","split")));
	elmMenuBar.addItem(elmSliderMenuItem = new MenuItem(Locale.LS("Sliders..."),new MyCommand("elm","sliders")));

	scopePopupMenu = new ScopePopupMenu();

	setColors(positiveColor, negativeColor, neutralColor, selectColor, currentColor);
	setWheelSensitivity();

	if (mobileProjects != null) {
	    getSetupList(false);
	    mobileProjects.restore();
	} else if (startCircuitText != null) {
	    getSetupList(false);
	    readCircuit(startCircuitText);
	    unsavedChanges = false;
	} else {
	    if (stopMessage == null && startCircuitLink!=null) {
		readCircuit("");
		getSetupList(false);
		ImportFromDropboxDialog.setSim(this);
		ImportFromDropboxDialog.doImportDropboxLink(startCircuitLink, false);
	    } else {
		readCircuit("");
		if (stopMessage == null && startCircuit != null) {
		    getSetupList(false);
		    readSetupFile(startCircuit, startLabel);
		}
		else
		    getSetupList(true);
	    }
	}

	if (mouseModeReq != null)
	    menuPerformed("main", mouseModeReq);

	enableUndoRedo();
	enablePaste();
	enableDisableMenuItems();
	setiFrameHeight();
	cv.addMouseDownHandler(this);
	cv.addMouseMoveHandler(this);
	cv.addMouseOutHandler(this);
	cv.addMouseUpHandler(this);
	cv.addClickHandler(this);
	cv.addDoubleClickHandler(this);
	doTouchHandlers(this, cv.getCanvasElement());
	cv.addDomHandler(this, ContextMenuEvent.getType());	
	menuBar.addDomHandler(new ClickHandler() {
	    public void onClick(ClickEvent event) {
		doMainMenuChecks();
	    }
	}, ClickEvent.getType());	
	Event.addNativePreviewHandler(this);
	cv.addMouseWheelHandler(this);

	Window.addResizeHandler(new ResizeHandler() {
	    public void onResize(ResizeEvent event) {
	        setCanvasSize();

	        // Delay full layout update
	        if (resizeTimer != null) {
	            resizeTimer.cancel();
	        }
	        resizeTimer = new Timer() {
	            public void run() {
	                if (transform[0] != 0) {
	                    centreCircuit();
	                }
	                repaint();
	            }
	        };
	        resizeTimer.schedule(300);
	    }
	});
	
	Window.addWindowClosingHandler(new Window.ClosingHandler() {
	    public void onWindowClosing(ClosingEvent event) {
		// there is a bug in electron that makes it impossible to close the app if this warning is given
		if (unsavedChanges && !isElectron() && mobileProjects == null)
		    event.setMessage(Locale.LS("Are you sure?  There are unsaved changes."));
	    }
	});
	setupJSInterface();
	
	setSimRunning(running);
    }

    void setColors(String positiveColor, String negativeColor, String neutralColor, String selectColor, String currentColor) {
        Storage stor = Storage.getLocalStorageIfSupported();
        if (stor != null) {
            if (positiveColor == null)
        	positiveColor = stor.getItem("positiveColor");
            if (negativeColor == null)
        	negativeColor = stor.getItem("negativeColor");
            if (neutralColor == null)
        	neutralColor = stor.getItem("neutralColor");
            if (selectColor == null)
        	selectColor = stor.getItem("selectColor");
            if (currentColor == null)
        	currentColor = stor.getItem("currentColor");
        }

	if (positiveColor != null)
	    CircuitElm.positiveColor = new Color(URL.decodeQueryString(positiveColor));
	else if (getOptionFromStorage("alternativeColor", false))
	    CircuitElm.positiveColor = Color.blue;
	
	if (negativeColor != null)
	    CircuitElm.negativeColor = new Color(URL.decodeQueryString(negativeColor));
	if (neutralColor != null)
	    CircuitElm.neutralColor = new Color(URL.decodeQueryString(neutralColor));

	if (selectColor != null)
	    CircuitElm.selectColor = new Color(URL.decodeQueryString(selectColor));
	else
	    CircuitElm.selectColor = Color.cyan;
	
	if (currentColor != null)
	    CircuitElm.currentColor = new Color(URL.decodeQueryString(currentColor));
	else
	    CircuitElm.currentColor = conventionCheckItem.getState() ? Color.yellow : Color.cyan;
	    
	CircuitElm.setColorScale();
    }
    
    void setWheelSensitivity() {
	wheelSensitivity = 1;
	try {
	    Storage stor = Storage.getLocalStorageIfSupported();
	    wheelSensitivity = Double.parseDouble(stor.getItem("wheelSensitivity"));
	} catch (Exception e) {}
    }

    MenuItem menuItemWithShortcut(String icon, String text, String shortcut, MyCommand cmd) {
	final String edithtml="<div style=\"white-space:nowrap\"><div style=\"display:inline-block;width:100%;\"><i class=\"cirjsicon-";
	String nbsp = "&nbsp;";
	if (icon=="") nbsp="";
	String sn=edithtml + icon + "\"></i>" + nbsp + Locale.LS(text) + "</div>" + shortcut + "</div>";
	return new MenuItem(SafeHtmlUtils.fromTrustedString(sn), cmd);
    }
    
    MenuItem iconMenuItem(String icon, String text, Command cmd) {
        String icoStr = "<i class=\"cirjsicon-" + icon + "\"></i>&nbsp;" + Locale.LS(text); //<i class="cirjsicon-"></i>&nbsp;
        return new MenuItem(SafeHtmlUtils.fromTrustedString(icoStr), cmd);
    }
    
    boolean getOptionFromStorage(String key, boolean val) {
        Storage stor = Storage.getLocalStorageIfSupported();
        if (stor == null)
            return val;
        String s = stor.getItem(key);
        if (s == null)
            return val;
        return s == "true";
    }

    void setOptionInStorage(String key, boolean val) {
        Storage stor = Storage.getLocalStorageIfSupported();
        if (stor == null)
            return;
        stor.setItem(key,  val ? "true" : "false");
    }
    
    // save shortcuts to local storage
    void saveShortcuts() {
        Storage stor = Storage.getLocalStorageIfSupported();
        if (stor == null)
            return;
        String str = "1";
        int i;
        // format: version;code1=ClassName;code2=ClassName;etc
        for (i = 0; i != shortcuts.length; i++) {
            String sh = shortcuts[i];
            if (sh == null)
        		continue;
            str += ";" + i + "=" + sh;
        }
        stor.setItem("shortcuts", str);
    }
    
    // load shortcuts from local storage
    void loadShortcuts() {
        Storage stor = Storage.getLocalStorageIfSupported();
        if (stor == null)
            return;
        String str = stor.getItem("shortcuts");
        if (str == null)
            return;
        String keys[] = str.split(";");

        // clear existing shortcuts
        int i;
        for (i = 0; i != shortcuts.length; i++)
            shortcuts[i] = null;

        // clear shortcuts from menu
        for (i = 0; i != mainMenuItems.size(); i++) {
            CheckboxMenuItem item = mainMenuItems.get(i);
            // stop when we get to drag menu items
            if (item.getShortcut().length() > 1)
        		break;
            item.setShortcut("");
        }

        // go through keys (skipping version at start)
        for (i = 1; i < keys.length; i++) {
            String arr[] = keys[i].split("=");
            if (arr.length != 2)
        	continue;
            int c = Integer.parseInt(arr[0]);
            String className = arr[1];
            shortcuts[c] = className;
            
            // find menu item and fix it
            int j;
            for (j = 0; j != mainMenuItems.size(); j++) {
        		if (mainMenuItemNames.get(j) == className) {
        		    CheckboxMenuItem item = mainMenuItems.get(j);
        		    item.setShortcut(Character.toString((char)c));
        		    break;
        		}
            }
        }
    }
    
    // install touch handlers
    // don't feel like rewriting this in java.  Anyway, java doesn't let us create mouse
    // events and dispatch them.
    native static void doTouchHandlers(CirSim sim, CanvasElement cv) /*-{
    var timer, lastDistance, tap, pinching, waveform, startX, startY;
    var navigate, movingComponent, mouseDown, lastX, lastY, cursorDrag, floatingCursorDrag, scopeMenuOpened;

    function local(x, y) {
        var bounds = cv.getBoundingClientRect();
        return [Math.round(x - bounds.left), Math.round(y - bounds.top)];
    }

    function press() {
        mouseDown = true;
        cv.dispatchEvent(new MouseEvent("mousedown", {clientX: startX, clientY: startY}));
    }

    function release() {
        if (mouseDown) {
            mouseDown = false;
            cv.dispatchEvent(new MouseEvent("mouseup", {}));
        }
    }

    cv.addEventListener("touchstart", function(e) {
        e.preventDefault();
        clearTimeout(timer);
        if (!pinching && e.touches.length === e.changedTouches.length) {
            var touch = e.touches[0];
            startX = touch.clientX;
            startY = touch.clientY;
            var origin = local(startX, startY);
            waveform = sim.@com.lushprojects.circuitjs1.client.CirSim::scopeTouchOrigin(II)(origin[0], origin[1]);
            navigate = sim.@com.lushprojects.circuitjs1.client.CirSim::shouldPanTouch(II)(origin[0], origin[1]);
            movingComponent = false;
            scopeMenuOpened = false;
            cursorDrag = e.touches.length === 1 && waveform &&
                sim.@com.lushprojects.circuitjs1.client.CirSim::startWaveformCursor(II)(origin[0], origin[1]);
            floatingCursorDrag = e.touches.length === 1 && navigate &&
                sim.@com.lushprojects.circuitjs1.client.CirSim::startFloatingCursor(II)(origin[0], origin[1]);
            if (cursorDrag || floatingCursorDrag)
                return;
        }
        if (e.touches.length === 1 && !pinching) {
            tap = true;
            lastX = startX;
            lastY = startY;
            if (navigate) {
                timer = setTimeout(function() {
                    var origin = local(startX, startY);
                    scopeMenuOpened = sim.@com.lushprojects.circuitjs1.client.CirSim::holdSchematic(II)(origin[0], origin[1]);
                    if (scopeMenuOpened)
                        tap = false;
                }, 500);
                return;
            }
            press();
            timer = setTimeout(function() {
                if (sim.@com.lushprojects.circuitjs1.client.CirSim::longPress()())
                    tap = false;
            }, 500);
        }
        if (e.touches.length < 2)
            return;
        if (!pinching) {
            tap = false;
            cursorDrag = false;
            floatingCursorDrag = false;
            pinching = true;
            release();
        }
        lastDistance = Math.hypot(e.touches[0].clientX - e.touches[1].clientX,
                                  e.touches[0].clientY - e.touches[1].clientY);
    }, {passive: false});

    function finish(e) {
        e.preventDefault();
        clearTimeout(timer);
        if (pinching) {
            if (!e.touches.length)
                pinching = false;
            lastDistance = 0;
            return;
        }
        if (!cursorDrag && !floatingCursorDrag && waveform && !scopeMenuOpened
                && e.type === "touchend" && e.changedTouches.length) {
            var touch = e.changedTouches[0];
            var down = touch.clientY - startY;
            if (@com.lushprojects.circuitjs1.client.CirSim::isScopeLayoutSwipe(DD)(touch.clientX - startX, down)) {
                var origin = local(startX, startY);
                if (sim.@com.lushprojects.circuitjs1.client.CirSim::arrangeWaveformAt(IIZ)(origin[0], origin[1], down > 0)) {
                    release();
                    tap = cursorDrag = false;
                    return;
                }
            }
        }
        if (cursorDrag || floatingCursorDrag) {
            cursorDrag = false;
            floatingCursorDrag = false;
            return;
        }
        if (tap && e.type === "touchend") {
            var origin = local(startX, startY);
            if (!sim.@com.lushprojects.circuitjs1.client.CirSim::toggleFloatingCursor(II)(origin[0], origin[1]) && navigate) {
                cv.dispatchEvent(new MouseEvent("mousemove", {clientX: startX, clientY: startY}));
                sim.@com.lushprojects.circuitjs1.client.CirSim::tapSchematic(II)(origin[0], origin[1]);
            }
        }
        release();
        tap = false;
    }
    cv.addEventListener("touchend", finish, {passive: false});
    cv.addEventListener("touchcancel", finish, {passive: false});
    cv.addEventListener("touchmove", function(e) {
        e.preventDefault();
        var a = e.touches[0];
        if (pinching) {
            if (e.touches.length < 2)
                return;
            var b = e.touches[1];
            var distance = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
            var center = local((a.clientX + b.clientX) / 2, (a.clientY + b.clientY) / 2);
            if (distance > 0 && lastDistance > 0)
                sim.@com.lushprojects.circuitjs1.client.CirSim::pinchZoom(ZDII)(waveform, distance / lastDistance, center[0], center[1]);
            lastDistance = distance;
            return;
        }
        if (cursorDrag) {
            var position = local(a.clientX, a.clientY);
            sim.@com.lushprojects.circuitjs1.client.CirSim::moveWaveformCursor(I)(position[0]);
            return;
        }
        if (floatingCursorDrag) {
            cv.dispatchEvent(new MouseEvent("mousemove", {clientX: a.clientX, clientY: a.clientY}));
            return;
        }
        if (scopeMenuOpened || (tap && Math.hypot(a.clientX - startX, a.clientY - startY) < 8))
            return;
        if (navigate && tap) {
            var origin = local(startX, startY);
            movingComponent = sim.@com.lushprojects.circuitjs1.client.CirSim::beginTouchComponentDrag(II)(origin[0], origin[1]);
            mouseDown = movingComponent;
        }
        tap = false;
        clearTimeout(timer);
        if (navigate && !movingComponent) {
            sim.@com.lushprojects.circuitjs1.client.CirSim::panTouch(DD)(a.clientX - lastX, a.clientY - lastY);
            lastX = a.clientX;
            lastY = a.clientY;
            return;
        }
        cv.dispatchEvent(new MouseEvent("mousemove", {clientX: a.clientX, clientY: a.clientY}));
    }, {passive: false});

    }-*/;
    
    boolean shown = false;
    
    // this is called twice, once for the Draw menu, once for the right mouse popup menu
    public void composeMainMenu(MenuBar mainMenuBar, int num) {
    	mainMenuBar.addItem(getClassCheckItem(Locale.LS("Add Wire"), "WireElm"));
    	mainMenuBar.addItem(getClassCheckItem(Locale.LS("Add Resistor"), "ResistorElm"));

    	MenuBar passMenuBar = new MenuBar(true);
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Capacitor"), "CapacitorElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Capacitor (polarized)"), "PolarCapacitorElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Inductor"), "InductorElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Switch"), "SwitchElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Push Switch"), "PushSwitchElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add SPDT Switch"), "Switch2Elm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add DPDT Switch"), "DPDTSwitchElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Make-Before-Break Switch"), "MBBSwitchElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Potentiometer"), "PotElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Transformer"), "TransformerElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Tapped Transformer"), "TappedTransformerElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Custom Transformer"), "CustomTransformerElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Transmission Line"), "TransLineElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Relay"), "RelayElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Relay Coil"), "RelayCoilElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Relay Contact"), "RelayContactElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Photoresistor"), "LDRElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Thermistor"), "ThermistorNTCElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Memristor"), "MemristorElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Spark Gap"), "SparkGapElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Fuse"), "FuseElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Crystal"), "CrystalElm"));
    	passMenuBar.addItem(getClassCheckItem(Locale.LS("Add Cross Switch"), "CrossSwitchElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Passive Components")), passMenuBar);

    	MenuBar inputMenuBar = new MenuBar(true);
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Ground"), "GroundElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Voltage Source (2-terminal)"), "DCVoltageElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add A/C Voltage Source (2-terminal)"), "ACVoltageElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Voltage Source (1-terminal)"), "RailElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add A/C Voltage Source (1-terminal)"), "ACRailElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Square Wave Source (1-terminal)"), "SquareRailElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Clock"), "ClockElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add A/C Sweep"), "SweepElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Variable Voltage"), "VarRailElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Antenna"), "AntennaElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add AM Source"), "AMElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add FM Source"), "FMElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Current Source"), "CurrentElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Noise Generator"), "NoiseElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Audio Input"), "AudioInputElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Data Input"), "DataInputElm"));
    	inputMenuBar.addItem(getClassCheckItem(Locale.LS("Add External Voltage (JavaScript)"), "ExtVoltageElm"));

    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Inputs and Sources")), inputMenuBar);
    	
    	MenuBar outputMenuBar = new MenuBar(true);
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Analog Output"), "OutputElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add LED"), "LEDElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Lamp"), "LampElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Text"), "TextElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Box"), "BoxElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Line"), "LineElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Labeled Node"), "LabeledNodeElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Voltmeter/Scope Probe"), "ProbeElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Ohmmeter"), "OhmMeterElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Ammeter"), "AmmeterElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Wattmeter"), "WattmeterElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Test Point"), "TestPointElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Decimal Display"), "DecimalDisplayElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add LED Array"), "LEDArrayElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Data Export"), "DataRecorderElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Audio Output"), "AudioOutputElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add Stop Trigger"), "StopTriggerElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add DC Motor"), "DCMotorElm"));
    	outputMenuBar.addItem(getClassCheckItem(Locale.LS("Add 3-Phase Motor"), "ThreePhaseMotorElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Outputs and Labels")), outputMenuBar);
    	
    	MenuBar activeMenuBar = new MenuBar(true);
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Diode"), "DiodeElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Zener Diode"), "ZenerElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Transistor (bipolar, NPN)"), "NTransistorElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Transistor (bipolar, PNP)"), "PTransistorElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add MOSFET (N-Channel)"), "NMosfetElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add MOSFET (P-Channel)"), "PMosfetElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add JFET (N-Channel)"), "NJfetElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add JFET (P-Channel)"), "PJfetElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add SCR"), "SCRElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add DIAC"), "DiacElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add TRIAC"), "TriacElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Darlington Pair (NPN)"), "NDarlingtonElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Darlington Pair (PNP)"), "PDarlingtonElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Varactor/Varicap"), "VaractorElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Tunnel Diode"), "TunnelDiodeElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Triode"), "TriodeElm"));
    	activeMenuBar.addItem(getClassCheckItem(Locale.LS("Add Unijunction Transistor"), "UnijunctionElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Active Components")), activeMenuBar);

    	MenuBar activeBlocMenuBar = new MenuBar(true);
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Op Amp (ideal, - on top)"), "OpAmpElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Op Amp (ideal, + on top)"), "OpAmpSwapElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Op Amp (real)"), "OpAmpRealElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Analog Switch (SPST)"), "AnalogSwitchElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Analog Switch (SPDT)"), "AnalogSwitch2Elm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Tristate Buffer"), "TriStateElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Schmitt Trigger"), "SchmittElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Schmitt Trigger (Inverting)"), "InvertingSchmittElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Delay Buffer"), "DelayBufferElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add CCII+"), "CC2Elm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add CCII-"), "CC2NegElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Comparator (Hi-Z/GND output)"), "ComparatorElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add OTA (LM13700 style)"), "OTAElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Voltage-Controlled Voltage Source (VCVS)"), "VCVSElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Voltage-Controlled Current Source (VCCS)"), "VCCSElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Current-Controlled Voltage Source (CCVS)"), "CCVSElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Current-Controlled Current Source (CCCS)"), "CCCSElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Optocoupler"), "OptocouplerElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Time Delay Relay"), "TimeDelayRelayElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add LM317"), "CustomCompositeElm:~LM317-v2"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add TL431"), "CustomCompositeElm:~TL431"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Motor Protection Switch"), "MotorProtectionSwitchElm"));
    	activeBlocMenuBar.addItem(getClassCheckItem(Locale.LS("Add Subcircuit Instance"), "CustomCompositeElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Active Building Blocks")), activeBlocMenuBar);
    	
    	MenuBar gateMenuBar = new MenuBar(true);
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add Logic Input"), "LogicInputElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add Logic Output"), "LogicOutputElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add Inverter"), "InverterElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add NAND Gate"), "NandGateElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add NOR Gate"), "NorGateElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add AND Gate"), "AndGateElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add OR Gate"), "OrGateElm"));
    	gateMenuBar.addItem(getClassCheckItem(Locale.LS("Add XOR Gate"), "XorGateElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Logic Gates, Input and Output")), gateMenuBar);

    	MenuBar chipMenuBar = new MenuBar(true);
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add D Flip-Flop"), "DFlipFlopElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add JK Flip-Flop"), "JKFlipFlopElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add T Flip-Flop"), "TFlipFlopElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add 7 Segment LED"), "SevenSegElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add 7 Segment Decoder"), "SevenSegDecoderElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Multiplexer"), "MultiplexerElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Demultiplexer"), "DeMultiplexerElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add SIPO shift register"), "SipoShiftElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add PISO shift register"), "PisoShiftElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Counter"), "CounterElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Counter w/ Load"), "Counter2Elm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Ring Counter"), "DecadeElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Latch"), "LatchElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Sequence generator"), "SeqGenElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Adder"), "FullAdderElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Half Adder"), "HalfAdderElm"));
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Custom Logic"), "UserDefinedLogicElm")); // don't change this, it will break people's saved shortcuts
    	chipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Static RAM"), "SRAMElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Digital Chips")), chipMenuBar);
    	
    	MenuBar achipMenuBar = new MenuBar(true);
    	achipMenuBar.addItem(getClassCheckItem(Locale.LS("Add 555 Timer"), "TimerElm"));
    	achipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Phase Comparator"), "PhaseCompElm"));
    	achipMenuBar.addItem(getClassCheckItem(Locale.LS("Add DAC"), "DACElm"));
    	achipMenuBar.addItem(getClassCheckItem(Locale.LS("Add ADC"), "ADCElm"));
    	achipMenuBar.addItem(getClassCheckItem(Locale.LS("Add VCO"), "VCOElm"));
    	achipMenuBar.addItem(getClassCheckItem(Locale.LS("Add Monostable"), "MonostableElm"));
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Analog and Hybrid Chips")), achipMenuBar);
    	
    	if (subcircuitMenuBar == null)
    	    subcircuitMenuBar = new MenuBar[2];
    	subcircuitMenuBar[num] = new MenuBar(true);
    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Subcircuits")), subcircuitMenuBar[num]);
    	
    	MenuBar otherMenuBar = new MenuBar(true);
    	CheckboxMenuItem mi;
    	otherMenuBar.addItem(mi=getClassCheckItem(Locale.LS("Drag All"), "DragAll"));
    	mi.setShortcut(Locale.LS("(Alt-drag)"));
    	otherMenuBar.addItem(mi=getClassCheckItem(Locale.LS("Drag Row"), "DragRow"));
    	mi.setShortcut(Locale.LS("(A-S-drag)"));
    	otherMenuBar.addItem(mi=getClassCheckItem(Locale.LS("Drag Column"), "DragColumn"));
    	mi.setShortcut(isMac ? Locale.LS("(A-Cmd-drag)") : Locale.LS("(A-M-drag)"));
    	otherMenuBar.addItem(getClassCheckItem(Locale.LS("Drag Selected"), "DragSelected"));
    	otherMenuBar.addItem(mi=getClassCheckItem(Locale.LS("Drag Post"), "DragPost"));
    	mi.setShortcut("(" + ctrlMetaKey + "drag)");

    	mainMenuBar.addItem(SafeHtmlUtils.fromTrustedString(CheckboxMenuItem.checkBoxHtml+Locale.LS("&nbsp;</div>Drag")), otherMenuBar);

    	mainMenuBar.addItem(mi=getClassCheckItem(Locale.LS("Select/Drag Sel"), "Select"));
	mi.setShortcut(Locale.LS("(space or Shift-drag)"));
    }
    
    void composeSubcircuitMenu() {
	if (subcircuitMenuBar == null)
	    return;
	int mi;
	
	// there are two menus to update: the one in the Draw menu, and the one in the right mouse menu
	for (mi = 0; mi != 2; mi++) {
	    MenuBar menu = subcircuitMenuBar[mi];
	    menu.clearItems();
	    Vector<CustomCompositeModel> list = CustomCompositeModel.getModelList();
	    int i;
	    for (i = 0; i != list.size(); i++) {
		String name = list.get(i).name;
		menu.addItem(getClassCheckItem(Locale.LS("Add ") + name, "CustomCompositeElm:" + name));
	    }
	}
	lastSubcircuitMenuUpdate = CustomCompositeModel.sequenceNumber;
    }
    
    public void composeSelectScopeMenu(MenuBar sb) {
	sb.clearItems();
	selectScopeMenuItems = new Vector<MenuItem>();
	for( int i = 0; i < scopeCount; i++) {
	    String s, l;
	    s = Locale.LS("Scope")+" "+ Integer.toString(i+1);
	    l=scopes[i].getScopeLabelOrText();
	    if (l!="")
		s+=" ("+SafeHtmlUtils.htmlEscape(l)+")";
	    selectScopeMenuItems.add(new MenuItem(s ,new MyCommand("elm", "addToScope"+Integer.toString(i))));
	}
	int c = countScopeElms();
	for (int j = 0; j < c; j++) {
	    String s,l;
	    s = Locale.LS("Undocked Scope")+" "+ Integer.toString(j+1);
	    l = getNthScopeElm(j).elmScope.getScopeLabelOrText();
	    if (l!="")
		s += " ("+SafeHtmlUtils.htmlEscape(l)+")";
	    selectScopeMenuItems.add(new MenuItem(s, new MyCommand("elm", "addToScope"+Integer.toString(scopeCount+j))));
	}
	for (MenuItem mi : selectScopeMenuItems)
	    sb.addItem(mi);
    }
    
    public void setiFrameHeight() {
    	if (iFrame==null)
    		return;
    	int i;
    	int cumheight=0;
    	for (i=0; i < verticalPanel.getWidgetIndex(iFrame); i++) {
    		if (verticalPanel.getWidget(i) !=loadFileInput) {
    			cumheight=cumheight+verticalPanel.getWidget(i).getOffsetHeight();
    			if (verticalPanel.getWidget(i).getStyleName().contains("topSpace"))
    					cumheight+=12;
    		}
    	}
        int ih = RootLayoutPanel.get().getOffsetHeight() - (hideMenu ? 0 : menuBar.getOffsetHeight()) - cumheight;
    	if (ih<0)
    		ih=0;
    	iFrame.setHeight(ih+"px");
    }
    


    


    CheckboxMenuItem getClassCheckItem(String s, String t) {
	if (classToLabelMap == null)
	    classToLabelMap = new HashMap<String, String>();
	classToLabelMap.put(t, s);

    	// try {
    	//   Class c = Class.forName(t);
    	String shortcut="";
    	CircuitElm elm = null;
    	try {
    	    elm = constructElement(t, 0, 0);
    	} catch (Exception e) {}
    	CheckboxMenuItem mi;
    	//  register(c, elm);
    	if ( elm!=null ) {
    		if (elm.needsShortcut() ) {
    			shortcut += (char)elm.getShortcut();
    			if (shortcuts[elm.getShortcut()] != null && !shortcuts[elm.getShortcut()].equals(t))
    			    console("already have shortcut for " + (char)elm.getShortcut() + " " + elm);
    			shortcuts[elm.getShortcut()]=t;
    		}
    		elm.delete();
    	}
//    	else
//    		GWT.log("Coudn't create class: "+t);
    	//	} catch (Exception ee) {
    	//	    ee.printStackTrace();
    	//	}
    	if (shortcut=="")
    		mi= new CheckboxMenuItem(s);
    	else
    		mi = new CheckboxMenuItem(s, shortcut);
    	mi.setScheduledCommand(new MyCommand("main", t) );
    	mainMenuItems.add(mi);
    	mainMenuItemNames.add(t);
    	return mi;
    }
    
    

    
    void centreCircuit() {
	if (elmList == null)  // avoid exception if called during initialization
	    return;
	
	Rectangle bounds = getCircuitBounds();
    	setCircuitArea();
	
    	double scale = 1;
    	int cheight = circuitArea.height;
    	
    	// if there's no scope, and the window isn't very wide, then don't use all of the circuit area when
    	// centering, because the info in the corner might not get in the way.  We still want circuitArea to be the full
    	// height though, to allow the user to put stuff there manually.
    	if (scopeCount == 0 && circuitArea.width < 800) {
            int h = (int) (cheight * getScopeHeightFraction());
    	    cheight -= h;
    	}
    	
    	if (bounds != null)
    	    // add some space on edges because bounds calculation is not perfect
    	    scale = Math.min(circuitArea.width /(double)(bounds.width+140),
    			     cheight/(double)(bounds.height+100));
    	scale = Math.min(scale, 1.5); // Limit scale so we don't create enormous circuits in big windows

    	// calculate transform so circuit fills most of screen
    	transform[0] = transform[3] = scale;
    	transform[1] = transform[2] = transform[4] = transform[5] = 0;
    	if (bounds != null) {
    	    transform[4] = (circuitArea.width -bounds.width *scale)/2 - bounds.x*scale;
    	    transform[5] = (cheight-bounds.height*scale)/2 - bounds.y*scale;
    	}
    }

    // get circuit bounds.  remember this doesn't use setBbox().  That is calculated when we draw
    // the circuit, but this needs to be ready before we first draw it, so we use this crude method
    Rectangle getCircuitBounds() {
    	int i;
    	int minx = 30000, maxx = -30000, miny = 30000, maxy = -30000;
    	for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		// centered text causes problems when trying to center the circuit,
    		// so we special-case it here
    		if (!ce.isCenteredText()) {
    			minx = min(ce.x, min(ce.x2, minx));
    			maxx = max(ce.x, max(ce.x2, maxx));
    		}
    		miny = min(ce.y, min(ce.y2, miny));
    		maxy = max(ce.y, max(ce.y2, maxy));
    	}
        Rectangle imageBounds = canvasImages.bounds();
        if (minx > maxx)
            return imageBounds;
        Rectangle circuitBounds = new Rectangle(minx, miny, maxx-minx, maxy-miny);
        return imageBounds == null ? circuitBounds : circuitBounds.union(imageBounds);
    }

    long lastTime = 0, lastIterTime;
    static CirSim theSim;

    
    public void setSimRunning(boolean s) {
	    if (s) {
	        if (stopMessage != null)
	            return;
	        simRunning = true;
	        draggingPausedWaveformCursor = false;
	        runStopButton.setHTML("<i class='button-icon fas fa-pause'>⏸</i><span>Pause</span>");
	        requestFrame();
	    } else {
	        simRunning = false;
	        runStopButton.setHTML("<i class='button-icon fas fa-play'>▶</i><span>Play</span>");
	        lastIterTime = 0;
	        repaint();
	    }
	}
    
    public boolean simIsRunning() {
    	return simRunning;
    }
    
    void repaint() {
        updateComponentToolsButton();
        requestFrame();
    }

    // *****************************************************************
    //                     UPDATE CIRCUIT
    
    public void updateCircuit() {
	    checkCanvasSize();
	    
	    // Analyze circuit
	    boolean didAnalyze = analyzeFlag;
	    if (analyzeFlag || dcAnalysisFlag) {
	        analyzeCircuit();
                probeViewer.refreshSignals();
	        analyzeFlag = false;
	    }
	    
	    // Stamp circuit
	    if (needsStamp && simRunning) {
	        try {
	            preStampAndStampCircuit();
	        } catch (Exception e) {
	            stop("Exception in stampCircuit()", null);
	            GWT.log("Exception in stampCircuit", e);
	        }
	    }
	    
	    if (stopElm != null && stopElm != mouseElm)
	        stopElm.setMouseElm(true);
	    
	    setupScopes();

	    // Run circuit
	    if (simRunning) {
	        if (needsStamp)
	            console("needsStamp while simRunning?");

	        try {                
	            runCircuit(didAnalyze, hasTouchScope() ? 10 : (int) (1000/minFrameRate));
	        } catch (Exception e) {
	            stop("Unexpected simulation error.", null);
	            GWT.log("Exception in runCircuit", e);
	        }
	    }

	    String stopStatus = stopMessage == null ? ""
	        : Locale.LS("Simulation stopped") + " · " + stopMessage + " (t = " + CircuitElm.getTimeText(t) + ")";
	    if (!simulationStopLabel.getText().equals(stopStatus))
	        simulationStopLabel.setText(stopStatus);
	    simulationStopLabel.setVisible(stopMessage != null);

	    long sysTime = System.currentTimeMillis();
	    if (simRunning) {
	        if (lastTime != 0) {
	            int inc = (int) (sysTime - lastTime);
	            double c = currentBar.getValue();
	            c = java.lang.Math.exp(c / 3.5 - 14.2);
	            CircuitElm.currentMult = 1.7 * inc * c;
	            if (!conventionCheckItem.getState())
	                CircuitElm.currentMult = -CircuitElm.currentMult;
	        }
	        lastTime = sysTime;
	    } else {
	        lastTime = 0;
	    }

	    CircuitElm.powerMult = Math.exp(powerBar.getValue() / 4.762 - 7);

	    Graphics g = new Graphics(cvcontext);
	    double scale = devicePixelRatio();
	    cvcontext.setTransform(scale, 0, 0, scale, 0, 0);
	    boolean printable = printableCheckItem.getState();
	    CircuitElm.whiteColor = printable ? Color.black : Color.white;
	    CircuitElm.lightGrayColor = printable ? Color.black : Color.lightGray;
	    g.setColor(printable ? Color.white : Color.black);
	    g.fillRect(0, 0, canvasWidth, canvasHeight);

	    g.setFont(CircuitElm.unitsFont);

	    g.context.setLineCap(LineCap.ROUND);

	    if (noEditCheckItem.getState())
	        g.drawLock(20, circuitArea.y + 30);
	    
	    g.setColor(Color.white);
	    
	    // Draw the circuit in circuit coordinates.
	    cvcontext.setTransform(transform[0] * scale, 0, 0, transform[3] * scale, transform[4] * scale, (circuitArea.y + transform[5]) * scale);

            canvasImages.draw(cvcontext, transform[0], transform[4], circuitArea.y + transform[5],
                circuitArea.y, circuitArea.width, circuitArea.height, true);

	    for (int i = 0; i != elmList.size(); i++) {
	        if (isPowerColorEnabled())
	            g.setColor(Color.gray);
	        
	        CircuitElm elm = getElm(i);
	        elm.draw(g);
	        elm.drawReference(g);
	    }

	    // Draw posts normally
	    if (mouseMode != CirSim.MODE_DRAG_ROW && mouseMode != CirSim.MODE_DRAG_COLUMN) {
	        for (int i = 0; i != postDrawList.size(); i++)
	            CircuitElm.drawPost(g, postDrawList.get(i));
	    }

	    // for some mouse modes, what matters is not the posts but the endpoints (which
	    // are only the same for 2-terminal elements). We draw those now if needed
	    if (tempMouseMode == MODE_DRAG_ROW || 
	        tempMouseMode == MODE_DRAG_COLUMN || 
	        tempMouseMode == MODE_DRAG_POST || 
	        tempMouseMode == MODE_DRAG_SELECTED) {
	        for (int i = 0; i != elmList.size(); i++) {

	            CircuitElm ce = getElm(i);
	            // ce.drawPost(g, ce.x , ce.y );
	            // ce.drawPost(g, ce.x2, ce.y2);
	            if (ce != mouseElm || tempMouseMode != MODE_DRAG_POST) {
	                g.setColor(Color.gray);
	                g.fillOval(ce.x - 3, ce.y - 3, 7, 7);
	                g.fillOval(ce.x2 - 3, ce.y2 - 3, 7, 7);
	            } else {
	                ce.drawHandles(g, CircuitElm.selectColor);
	            }
	        }
	    }
	    
	    // draw handles for elm we're creating
	    if (tempMouseMode == MODE_SELECT && mouseElm != null) {
	        mouseElm.drawHandles(g, CircuitElm.selectColor);
	    }

	    // draw handles for elm we're dragging
	    if (dragElm != null && (dragElm.x != dragElm.x2 || dragElm.y != dragElm.y2)) {
	        dragElm.draw(g);
	        dragElm.drawHandles(g, CircuitElm.selectColor);
	    }

	    // draw bad connections. do this last so they will not be overdrawn.
	    for (int i = 0; i != badConnectionList.size(); i++) {
	        Point cn = badConnectionList.get(i);
	        g.setColor(Color.red);
	        g.fillOval(cn.x - 3, cn.y - 3, 7, 7);
	    }

	    // draw the selection rect
	    if (selectedArea != null) {
	        g.setColor(CircuitElm.selectColor);
	        g.drawRect(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
	    }

	    // draw the crosshair cursor
	    if (crossHairCheckItem.getState() && mouseCursorX >= 0
	            && circuitArea.contains(mouseCursorX, mouseCursorY)) {
	        g.setColor(Color.gray);
	        int x = snapGrid(inverseTransformX(mouseCursorX));
	        int y = snapGrid(inverseTransformY(mouseCursorY));
	        g.drawLine(x, inverseTransformY(circuitArea.y), x, inverseTransformY(canvasHeight));
	        g.drawLine(inverseTransformX(0), y, inverseTransformX(circuitArea.width), y);
	    }

            if (!noEditCheckItem.getState() && mouseMode == MODE_SELECT)
                canvasImages.drawHandles(cvcontext);

	    // Restore canvas coordinates for overlays.
	    cvcontext.setTransform(scale, 0, 0, scale, 0, 0);

	    // Draw the docked scopes and simulation status in canvas coordinates.
	    drawScopeArea(g);
	    drawSelectedComponentInfo(g);

	    g.setColor(Color.white);
	    
	    if (stopElm != null && stopElm != mouseElm)
	        stopElm.setMouseElm(false);
	    
	    // if we did DC analysis, we need to re-analyze the circuit with that flag
	    // cleared.
	    if (dcAnalysisFlag) {
	        dcAnalysisFlag = false;
	        analyzeFlag = true;
	    }

	    // This should always be the last thing called
	    callUpdateHook();
	}

    void drawScopeArea(Graphics g) {
        probeViewer.cursorTime = Double.NaN;
	int leftX = 0;
	int h = 0;
	if (stopMessage == null && !hasScopePanel()) {
	    leftX = max(canvasWidth-infoWidth, 0);
	    int h0 = (int) (canvasHeight * getScopeHeightFraction());
	    h = Math.min(70, h0);
	    if (hideInfoBox)
		h = 0;
	}
	g.setColor(printableCheckItem.getState() ? "#eee" : "#111");
	if (hasScopePanel())
	    g.fillRect(0, 0, canvasWidth, circuitArea.y);
	else if (h > 0)
	    g.fillRect(leftX, canvasHeight - h, canvasWidth - leftX, h);
	g.setFont(CircuitElm.unitsFont);
	int ct = scopeCount;
	if (scopeCollapsed)
	    ct = 0;
	int i;
	Scope.clearCursorInfo();
	for (i = 0; i != ct; i++)
            if (isDockedScopeVisible(i))
                scopes[i].selectScope(mouseCursorX, mouseCursorY);
	if (scopeElmArr != null)
	    for (i=0; i != scopeElmArr.length; i++)
		scopeElmArr[i].selectScope(mouseCursorX, mouseCursorY);
	for (i = 0; i != ct; i++)
            if (isDockedScopeVisible(i))
                scopes[i].draw(g);
        if (!scopeCollapsed && hasScopePanel()) {
            if (probeViewer.useWaveform())
                probeViewer.draw(g, circuitArea.y, SCOPE_CONTROL_HEIGHT + 42);
            g.setColor(mouseWasOverSplitter ? CircuitElm.selectColor : Color.gray);
            g.setLineWidth(3);
            int middle = circuitArea.width / 2;
            g.drawLine(middle - 20, circuitArea.y - 4, middle + 20, circuitArea.y - 4);
            g.setLineWidth(1);
        }
	g.setColor(CircuitElm.whiteColor);

	if (stopMessage == null && !hideInfoBox && !(hasScopePanel() && (scopeCollapsed || probeViewer.useWaveform()))) {
	    // in JS it doesn't matter how big this is, there's no out-of-bounds exception
	    String info[] = new String[10];
	    info[0] = "t = " + CircuitElm.getTimeText(t);
	    double timerate = 160*getIterCount()*timeStep;
	    if (timerate >= .1)
		info[0] += " (" + CircuitElm.showFormat.format(timerate) + "x)";
	    info[1] = Locale.LS("time step = ") + CircuitElm.getTimeText(timeStep);

            int lines = 2;
            int badnodes = badConnectionList.size();
            if (badnodes > 0)
                info[lines++] = badnodes + (badnodes == 1
                        ? Locale.LS(" bad connection") : Locale.LS(" bad connections"));
            if (savedFlag)
                info[lines++] = "(saved)";

            if (ct != 0) {
                String status = info[0];
                for (i = 1; i < lines; i++)
                    status += "  ·  " + info[i];
                if (canvasWidth > 16)
                    g.context.fillText(status, 8, circuitArea.y - SCOPE_SPLITTER_HEIGHT - 5, canvasWidth - 16);
            } else {
                for (i = 0; i < lines; i++)
                    g.drawString(info[i], leftX + 5, canvasHeight - h + 15*(i+1));
            }
	}
    }
    
    private void drawSelectedComponentInfo(Graphics g) {
	if (hideInfoBox)
	    return;

	CircuitElm selected = null;
	for (CircuitElm elm : elmList) {
	    if (!elm.isSelected())
		continue;
	    if (selected != null)
		return;
	    selected = elm;
	}
	if (selected == null || selected instanceof ScopeElm)
	    return;

	String[] info = new String[10];
	selected.getInfo(info);
	if (info[0] == null)
	    return;
	boolean atCursor = !simIsRunning() && Double.isFinite(probeViewer.cursorTime);
	if (atCursor)
	    info = componentMeasurements.getInfo(selected, probeViewer.cursorTime, info[0]);
	info[0] = Locale.LS(info[0]);
	if (info[1] != null)
	    info[1] = Locale.LS(info[1]);

	int lines = 0;
	while (lines < info.length && info[lines] != null)
	    lines++;
	if (averagePowerLossCheckItem.getState() && lines < info.length
		&& selected.getPostCount() > 0 && !selected.isGraphicElmt()) {
	    double loss = averagePowerLoss.get(selected);
	    if (Double.isFinite(loss))
		info[lines++] = Locale.LS("Average loss") + " = " + CircuitElm.getUnitText(loss, "W");
	    else if (AveragePowerLoss.supports(selected))
		info[lines++] = Locale.LS("Collecting average power samples");
	    else
		info[lines++] = Locale.LS("Power loss model unavailable");
	}
	if (!atCursor && hintType != -1 && lines < info.length) {
	    String hint = getHint();
	    if (hint == null)
		hintType = -1;
	    else
		info[lines++] = hint;
	}

	int x = 16;
	int y = canvasModeLabel.getElement().getOffsetTop() + canvasModeLabel.getOffsetHeight() + 6;
	int lineHeight = 15;
	int maxTextWidth = circuitArea.width - 2*x;
	if (maxTextWidth <= 0 || y >= canvasHeight)
	    return;

	g.save();
	g.clipRect(0, circuitArea.y, circuitArea.width, circuitArea.height);
	g.setFont(CircuitElm.unitsFont);
	g.setColor(CircuitElm.whiteColor);
	for (int i = 0; i < lines; i++)
	    g.context.fillText(info[i], x, y + 20 + i*lineHeight, maxTextWidth);
	g.restore();
    }

    Color getBackgroundColor() {
	if (printableCheckItem.getState())
	    return Color.white;
	return Color.black;
    }
    
    private static final int SCOPE_CONTROL_HEIGHT = 44;
    private static final int SCOPE_STATUS_HEIGHT = 20;
    private static final int SCOPE_SPLITTER_HEIGHT = 12;
    private FlowPanel scopeControls, scopeActions;
    private Button scopeTraceButton, scopeSettingsButton, scopeCollapseButton;
    private ProbeViewer probeViewer;

    boolean highlightsProbeSource(CircuitElm elm) {
        return probeViewer != null && !scopeCollapsed && probeViewer.useWaveform() && probeViewer.highlights(elm);
    }
    private boolean scopeCollapsed;
    private Scope activeDockedScope;
    private boolean oldScopePanel;
    int oldScopeCount = -1;

    private boolean hasScopePanel() {
        return probeViewer != null && probeViewer.hasPanel();
    }

    private boolean isDockedScopeVisible(int index) {
        Scope special = probeViewer.specialScope();
        return special != null ? scopes[index] == special : !probeViewer.useWaveform();
    }

    private boolean isPortraitScopeLayout() {
        return hasTouchScope() && Window.getClientWidth() <= Window.getClientHeight();
    }

    native boolean hasTouchScope() /*-{
        return $wnd.innerWidth <= 768 || $wnd.matchMedia("(pointer: coarse)").matches;
    }-*/;

    void scopeModeChanged(Scope scope) {
        circuitChanged();
        probeViewer.scopeModeChanged(scope);
    }

    void scopeInfoChanged() {
        circuitChanged();
        probeViewer.scopeInfoChanged();
    }

    boolean openCombinedScopeProperties(Scope scope) {
        if (!hasTouchScope() && !probeViewer.useWaveform() && probeViewer.specialScope() == null)
            return false;
        for (int i = 0; i < scopeCount; i++) {
            if (scopes[i] == scope) {
                probeViewer.showProbeProperties(scope);
                return true;
            }
        }
        return false;
    }

    void selectDockedScope(Scope scope) {
        activeDockedScope = scope;
        clearMouseElm();
        mouseCursorX = -1;
    }

    private Button scopeAction(String label, String title, Runnable action) {
        Button button = new Button(label);
        button.addClickHandler(event -> action.run());
        button.setTitle(title);
        button.getElement().setAttribute("aria-label", title);
        return button;
    }

    private void updateScopeControls() {
        boolean panel = hasScopePanel();
        boolean showControls = panel || hasTouchScope() && !hideMenu;
        scopeControls.setVisible(showControls);
        if (!showControls && probeViewer.isOpen())
            probeViewer.close();

        boolean activeExists = false;
        for (int i = 0; i < scopeCount; i++)
            activeExists |= scopes[i] == activeDockedScope;
        if (!activeExists)
            activeDockedScope = scopeCount == 0 ? null : scopes[0];
        scopeSettingsButton.setVisible(showControls && panel && !probeViewer.hasTracePropertiesShortcut());
        scopeSettingsButton.setEnabled(!noEditCheckItem.getState());
        scopeActions.setVisible(panel && !scopeCollapsed && probeViewer.useWaveform());
        scopeCollapseButton.setVisible(panel);
        String collapseIcon = scopeCollapsed ? "▾" : "▴";
        if (!collapseIcon.equals(scopeCollapseButton.getText())) {
            scopeCollapseButton.setText(collapseIcon);
            String action = Locale.LS(scopeCollapsed ? "Expand oscilloscope" : "Collapse oscilloscope");
            scopeCollapseButton.setTitle(action);
            scopeCollapseButton.getElement().setAttribute("aria-label", action);
            scopeCollapseButton.getElement().setAttribute("aria-expanded", Boolean.toString(!scopeCollapsed));
        }
    }

    boolean scopeMenuIsSelected(Scope s) {
	if (scopeMenuSelected < 0)
	    return false;
	if (scopeMenuSelected < scopeCount)
	    return scopes[scopeMenuSelected] == s;
	return getNthScopeElm(scopeMenuSelected-scopeCount).elmScope == s; 
    }
    
    void setupScopes() {
    	int i;

    	// check scopes to make sure the elements still exist, and remove
    	// unused scopes/columns
    	int pos = -1;
    	for (i = 0; i < scopeCount; i++) {
    	    	if (scopes[i].needToRemove()) {
                System.arraycopy(scopes, i + 1, scopes, i, scopeCount - i - 1);
                scopes[--scopeCount] = null;
    			i--;
    			continue;
    		}
    		if (scopes[i].position > pos+1)
    			scopes[i].position = pos+1;
    		pos = scopes[i].position;
    	}
    	while (scopeCount > 0 && scopes[scopeCount-1].getElm() == null)
    		scopeCount--;
        if (oldScopeCount != scopeCount || oldScopePanel != hasScopePanel()) {
            oldScopeCount = scopeCount;
            oldScopePanel = hasScopePanel();
            setCircuitArea();
        }
        Scope previousSpecial = probeViewer.specialScope();
        probeViewer.syncScopes(!scopeCollapsed);
        if (previousSpecial != probeViewer.specialScope()
                || (hasScopePanel() && !scopeCollapsed && scopeMeasurementsHeight != probeViewer.measurementsHeight()))
            setCircuitArea();
        probeViewer.positionMeasurements(circuitArea.y);
        updateScopeControls();

        canvasModeLabel.getElement().getStyle().setTop(circuitArea.y + 8, Unit.PX);
        if (componentToolsButton != null)
            componentToolsButton.getElement().getStyle().setTop(circuitArea.y + 4, Unit.PX);

        int top = SCOPE_CONTROL_HEIGHT;
        int bottom = circuitArea.y - SCOPE_SPLITTER_HEIGHT - (hideInfoBox ? 0 : SCOPE_STATUS_HEIGHT);
        int h = Math.max(0, bottom - top);
        pos = 0;
        for (i = 0; i < scopeCount; i++)
            scopeColCount[i] = 0;
        for (i = 0; i < scopeCount; i++) {
            pos = Math.max(scopes[i].position, pos);
            scopeColCount[scopes[i].position]++;
        }
        int columns = pos + 1;
        boolean mobileDocked = hasTouchScope();
        int gap = 6;
        pos = -1;
    	int colh = 0;
    	int row = 0;
    	int speed = 0;
    	for (i = 0; i != scopeCount; i++) {
    		Scope s = scopes[i];
    		if (s.position > pos) {
    			pos = s.position;
    			colh = h / scopeColCount[pos];
    			row = 0;
    			speed = s.speed;
    		}
    		s.stackCount = scopeColCount[pos];
    		if (s.speed != speed) {
    			s.speed = speed;
    			s.resetGraph();
    		}
            if (scopeCollapsed)
                continue;
            boolean fullWidth = probeViewer.specialScope() == s;
            s.setMobileDocked(mobileDocked);
            int x = fullWidth ? 0 : canvasWidth * pos / columns;
            int right = fullWidth ? canvasWidth : canvasWidth * (pos + 1) / columns;
            int y = top + (fullWidth ? 0 : colh * row);
            int height = fullWidth ? h : colh;
            int headerHeight = height >= 76 ? 36 : height >= 38 ? 18 : 0;
            s.dockedHeaderHeight = headerHeight;
            Rectangle r = new Rectangle(x + gap, y + headerHeight,
                    Math.max(1, right - x - 2*gap), Math.max(1, height - headerHeight - 3));
            row++;
            if (!r.equals(s.rect))
                s.setRect(r);
        }
    }

    String getHint() {
	CircuitElm c1 = getElm(hintItem1);
	CircuitElm c2 = getElm(hintItem2);
	if (c1 == null || c2 == null)
	    return null;
	if (hintType == HINT_LC) {
	    if (!(c1 instanceof InductorElm))
		return null;
	    if (!(c2 instanceof CapacitorElm))
		return null;
	    InductorElm ie = (InductorElm) c1;
	    CapacitorElm ce = (CapacitorElm) c2;
	    return Locale.LS("res.f = ") + CircuitElm.getUnitText(1/(2*pi*Math.sqrt(ie.inductance*
						    ce.capacitance)), "Hz");
	}
	if (hintType == HINT_RC) {
	    if (!(c1 instanceof ResistorElm))
		return null;
	    if (!(c2 instanceof CapacitorElm))
		return null;
	    ResistorElm re = (ResistorElm) c1;
	    CapacitorElm ce = (CapacitorElm) c2;
	    return "RC = " + CircuitElm.getUnitText(re.resistance*ce.capacitance,
					 "s");
	}
	if (hintType == HINT_3DB_C) {
	    if (!(c1 instanceof ResistorElm))
		return null;
	    if (!(c2 instanceof CapacitorElm))
		return null;
	    ResistorElm re = (ResistorElm) c1;
	    CapacitorElm ce = (CapacitorElm) c2;
	    return Locale.LS("f.3db = ") +
		CircuitElm.getUnitText(1/(2*pi*re.resistance*ce.capacitance), "Hz");
	}
	if (hintType == HINT_3DB_L) {
	    if (!(c1 instanceof ResistorElm))
		return null;
	    if (!(c2 instanceof InductorElm))
		return null;
	    ResistorElm re = (ResistorElm) c1;
	    InductorElm ie = (InductorElm) c2;
	    return Locale.LS("f.3db = ") +
		CircuitElm.getUnitText(re.resistance/(2*pi*ie.inductance), "Hz");
	}
	if (hintType == HINT_TWINT) {
	    if (!(c1 instanceof ResistorElm))
		return null;
	    if (!(c2 instanceof CapacitorElm))
		return null;
	    ResistorElm re = (ResistorElm) c1;
	    CapacitorElm ce = (CapacitorElm) c2;
	    return Locale.LS("fc = ") +
		CircuitElm.getUnitText(1/(2*pi*re.resistance*ce.capacitance), "Hz");
	}
	return null;
    }

//    public void toggleSwitch(int n) {
//	int i;
//	for (i = 0; i != elmList.size(); i++) {
//	    CircuitElm ce = getElm(i);
//	    if (ce instanceof SwitchElm) {
//		n--;
//		if (n == 0) {
//		    ((SwitchElm) ce).toggle();
//		    analyzeFlag = true;
//		    cv.repaint();
//		    return;
//		}
//	    }
//	}
//    }
    
    private void changeSchematicColorMode(CheckboxMenuItem mode) {
	if (mode.getState()) {
	    if (mode != voltsCheckItem)
		voltsCheckItem.setState(false);
	    if (mode != powerCheckItem)
		powerCheckItem.setState(false);
	    if (mode != averagePowerLossCheckItem)
		averagePowerLossCheckItem.setState(false);
	}
	if (!averagePowerLossCheckItem.getState())
	    averagePowerLoss.clear();
	setPowerBarEnable();
	syncSchematicDisplayControls();
	circuitChanged();
    }

    private void syncSchematicDisplayControls() {
	voltageColorsCheckBox.setValue(voltsCheckItem.getState());
	currentFlowCheckBox.setValue(dotsCheckItem.getState());
	powerCheckBox.setValue(powerCheckItem.getState());
	averagePowerLossCheckBox.setValue(averagePowerLossCheckItem.getState());
    }

    boolean isPowerColorEnabled() {
	return powerCheckItem.getState() || isAveragePowerLossEnabled();
    }

    boolean isAveragePowerLossEnabled() {
	return averagePowerLossCheckItem.getState();
    }

    double getAveragePowerLoss(CircuitElm element) {
	return averagePowerLoss.get(element);
    }

    private void setShowComponentValues(boolean show) {
	showValuesCheckItem.setState(show);
	if (componentValuesCheckBox != null)
	    componentValuesCheckBox.setValue(show);
    }

    private void setShowComponentValuesFromControl(boolean show) {
	setShowComponentValues(show);
	circuitChanged();
	repaint();
    }

    void circuitChanged() {
        if (mobileProjects != null)
            mobileProjects.circuitChanged();
    }

    void needAnalyze() {
        circuitChanged();
	componentMeasurements.clear();
	averagePowerLoss.clear();
	analyzeFlag = true;
    	repaint();
	enableDisableMenuItems();
    }
    
    Vector<CircuitNode> nodeList;
    Vector<Point> postDrawList = new Vector<Point>();
    Vector<Point> badConnectionList = new Vector<Point>();
    CircuitElm voltageSources[];

    public CircuitNode getCircuitNode(int n) {
	if (n >= nodeList.size())
	    return null;
	return nodeList.elementAt(n);
    }

    public CircuitElm getElm(int n) {
	if (n >= elmList.size())
	    return null;
	return elmList.elementAt(n);
    }
    
    public Adjustable findAdjustable(CircuitElm elm, int item) {
	int i;
	for (i = 0; i != adjustables.size(); i++) {
	    Adjustable a = adjustables.get(i);
	    if (a.elm == elm && a.editItem == item)
		return a;
	}
	return null;
    }
    
    public static native void console(String text)
    /*-{
	    console.log(text);
	}-*/;

    public static native void debugger() /*-{ debugger; }-*/;
    
    class NodeMapEntry {
	int node;
	NodeMapEntry() { node = -1; }
	NodeMapEntry(int n) { node = n; }
    }
    // map points to node numbers
    HashMap<Point,NodeMapEntry> nodeMap;
    
    class WireInfo {
	CircuitElm wire;
	Vector<CircuitElm> neighbors;
	int post;
	WireInfo(CircuitElm w) {
	    wire = w;
	}
    }
    
    // info about each wire and its neighbors, used to calculate wire currents
    Vector<WireInfo> wireInfoList;
    
    // find groups of nodes connected by wire equivalents and map them to the same node.  this speeds things
    // up considerably by reducing the size of the matrix.  We do this for wires, labeled nodes, and ground.
    // The actual node we map to is not assigned yet.  Instead we map to the same NodeMapEntry.
    void calculateWireClosure() {
	int i;
	LabeledNodeElm.resetNodeList();
	GroundElm.resetNodeList();
	nodeMap = new HashMap<Point,NodeMapEntry>();
//	int mergeCount = 0;
	wireInfoList = new Vector<WireInfo>();
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    if (!ce.isRemovableWire())
		continue;
	    ce.hasWireInfo = false;
	    wireInfoList.add(new WireInfo(ce));
	    Point p0 = ce.getPost(0);
	    NodeMapEntry cn  = nodeMap.get(p0);
	    
	    // what post are we connected to
	    Point p1 = ce.getConnectedPost();
	    if (p1 == null) {
		// no connected post (true for labeled node the first time it's encountered, or ground)
		if (cn == null) {
		    cn = new NodeMapEntry();
		    nodeMap.put(p0, cn);
		}
		continue;
	    }
	    NodeMapEntry cn2 = nodeMap.get(p1);
	    if (cn != null && cn2 != null) {
		// merge nodes; go through map and change all keys pointing to cn2 to point to cn
		for (Map.Entry<Point, NodeMapEntry> entry : nodeMap.entrySet()) {
		    if (entry.getValue() == cn2)
			entry.setValue(cn);
		}
//		mergeCount++;
		continue;
	    }
	    if (cn != null) {
		nodeMap.put(p1, cn);
		continue;
	    }
	    if (cn2 != null) {
		nodeMap.put(p0, cn2);
		continue;
	    }
	    // new entry
	    cn = new NodeMapEntry();
	    nodeMap.put(p0, cn);
	    nodeMap.put(p1, cn);
	}
	
//	console("got " + (groupCount-mergeCount) + " groups with " + nodeMap.size() + " nodes " + mergeCount);
    }
    
    // generate info we need to calculate wire currents.  Most other elements calculate currents using
    // the voltage on their terminal nodes.  But wires have the same voltage at both ends, so we need
    // to use the neighbors' currents instead.  We used to treat wires as zero voltage sources to make
    // this easier, but this is very inefficient, since it makes the matrix 2 rows bigger for each wire.
    // We create a list of WireInfo objects instead to help us calculate the wire currents instead,
    // so we make the matrix less complex, and we only calculate the wire currents when we need them
    // (once per frame, not once per subiteration).  We need the WireInfos arranged in the correct order,
    // each one containing a list of neighbors and which end to use (since one end may be ready before
    // the other)
    boolean calcWireInfo() {
	int i;
	int moved = 0;
	
	for (i = 0; i != wireInfoList.size(); i++) {
	    WireInfo wi = wireInfoList.get(i);
	    CircuitElm wire = wi.wire;
	    CircuitNode cn1 = nodeList.get(wire.getNode(0));  // both ends of wire have same node #
	    int j;

	    Vector<CircuitElm> neighbors0 = new Vector<CircuitElm>();
	    Vector<CircuitElm> neighbors1 = new Vector<CircuitElm>();
	    
	    // assume each end is ready (except ground nodes which have one end)
	    // labeled nodes are treated as having 2 terminals, see below
	    boolean isReady0 = true, isReady1 = !(wire instanceof GroundElm);

	    // go through elements sharing a node with this wire (may be connected indirectly
	    // by other wires, but at least it's faster than going through all elements)
	    for (j = 0; j != cn1.links.size(); j++) {
		CircuitNodeLink cnl = cn1.links.get(j);
		CircuitElm ce = cnl.elm;
		if (ce == wire)
		    continue;
		Point pt = ce.getPost(cnl.num);
		
		// is this a wire that doesn't have wire info yet?  If so we can't use it yet.
		// That would create a circular dependency.  So that side isn't ready.
		boolean notReady = (ce.isRemovableWire() && !ce.hasWireInfo);
		
		// which post does this element connect to, if any?
		if (pt.x == wire.x && pt.y == wire.y) {
		    neighbors0.add(ce);
		    if (notReady) isReady0 = false;
		} else if (wire.getPostCount() > 1) {
		    Point p2 = wire.getConnectedPost();
		    if (pt.x == p2.x && pt.y == p2.y) { 
			neighbors1.add(ce);
			if (notReady) isReady1 = false;
		    }
		} else if (ce instanceof LabeledNodeElm && wire instanceof LabeledNodeElm &&
			((LabeledNodeElm) ce).text == ((LabeledNodeElm) wire).text) {
		    // ce and wire are both labeled nodes with matching labels.  treat them as neighbors
		    neighbors1.add(ce);
		    if (notReady) isReady1 = false;
		}
	    }

	    // does one of the posts have all information necessary to calculate current?
	    if (isReady0) {
		wi.neighbors = neighbors0;
		wi.post = 0;
		wire.hasWireInfo = true;
		moved = 0;
	    } else if (isReady1) {
		wi.neighbors = neighbors1;
		wi.post = 1;
		wire.hasWireInfo = true;
		moved = 0;
	    } else {
		// no, so move to the end of the list and try again later
		wireInfoList.add(wireInfoList.remove(i--));
		moved++;
		if (moved > wireInfoList.size() * 2) {
		    stop("wire loop detected", wire);
		    return false;
		}
	    }
	}
	
	return true;
    }

    // find or allocate ground node
    void setGroundNode(boolean subcircuit) {
	int i;
	boolean gotGround = false;
	boolean gotRail = false;
	CircuitElm volt = null;
	    
	//System.out.println("ac1");
	// look for voltage or ground element
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    if (ce instanceof GroundElm) {
		gotGround = true;
		
		// set ground node to 0
		NodeMapEntry nme = nodeMap.get(ce.getPost(0));
		nme.node = 0;
		break;
	    }
	    if (ce instanceof RailElm)
	    	gotRail = true;
	    if (volt == null && ce instanceof VoltageElm)
	    	volt = ce;
	}

	// if no ground, and no rails, then the voltage elm's first terminal
	// is ground (but not for subcircuits)
	if (!subcircuit && !gotGround && volt != null && !gotRail) {
	    CircuitNode cn = new CircuitNode();
	    Point pt = volt.getPost(0);
	    nodeList.addElement(cn);

	    // update node map
	    NodeMapEntry cln = nodeMap.get(pt);
	    if (cln != null)
		cln.node = 0;
	    else
		nodeMap.put(pt, new NodeMapEntry(0));
	} else {
	    // otherwise allocate extra node for ground
	    CircuitNode cn = new CircuitNode();
	    nodeList.addElement(cn);
	}
    }

    // make list of nodes
    void makeNodeList() {
	int i, j;
	int vscount = 0;
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    int inodes = ce.getInternalNodeCount();
	    int ivs = ce.getVoltageSourceCount();
	    int posts = ce.getPostCount();
	    
	    // allocate a node for each post and match posts to nodes
	    for (j = 0; j != posts; j++) {
		Point pt = ce.getPost(j);
		NodeMapEntry cln = nodeMap.get(pt);
		
		// is this node not in map yet?  or is the node number unallocated?
		// (we don't allocate nodes before this because changing the allocation order
		// of nodes changes circuit behavior and breaks backward compatibility;
		// the code below to connect unconnected nodes may connect a different node to ground) 
		if (cln == null || cln.node == -1) {
		    CircuitNode cn = new CircuitNode();
		    CircuitNodeLink cnl = new CircuitNodeLink();
		    cnl.num = j;
		    cnl.elm = ce;
		    cn.links.addElement(cnl);
		    ce.setNode(j, nodeList.size());
		    if (cln != null)
			cln.node = nodeList.size();
		    else
			nodeMap.put(pt, new NodeMapEntry(nodeList.size()));
		    nodeList.addElement(cn);
		} else {
		    int n = cln.node;
		    CircuitNodeLink cnl = new CircuitNodeLink();
		    cnl.num = j;
		    cnl.elm = ce;
		    getCircuitNode(n).links.addElement(cnl);
		    ce.setNode(j, n);
		    // if it's the ground node, make sure the node voltage is 0,
		    // cause it may not get set later
		    if (n == 0)
			ce.setNodeVoltage(j, 0);
		}
	    }
	    for (j = 0; j != inodes; j++) {
		CircuitNode cn = new CircuitNode();
		cn.internal = true;
		CircuitNodeLink cnl = new CircuitNodeLink();
		cnl.num = j+posts;
		cnl.elm = ce;
		cn.links.addElement(cnl);
		ce.setNode(cnl.num, nodeList.size());
		nodeList.addElement(cn);
	    }
	    
	    // also count voltage sources so we can allocate array
	    vscount += ivs;
	}
	
        voltageSources = new CircuitElm[vscount];
    }
    
    Vector<Integer> unconnectedNodes;
    Vector<CircuitElm> nodesWithGroundConnection;
    int nodesWithGroundConnectionCount;
    
    void findUnconnectedNodes() {
	int i, j;
	
	// determine nodes that are not connected indirectly to ground.
	// all nodes must be connected to ground somehow, or else we
	// will get a matrix error.
	boolean closure[] = new boolean[nodeList.size()];
	boolean changed = true;
	unconnectedNodes = new Vector<Integer>();
	nodesWithGroundConnection = new Vector<CircuitElm>();
	closure[0] = true;
	while (changed) {
	    changed = false;
	    for (i = 0; i != elmList.size(); i++) {
		CircuitElm ce = getElm(i);
		if (ce instanceof WireElm)
		    continue;
		// loop through all ce's nodes to see if they are connected
		// to other nodes not in closure
		boolean hasGround = false;
		for (j = 0; j < ce.getConnectionNodeCount(); j++) {
		    boolean hg = ce.hasGroundConnection(j);
		    if (hg)
			hasGround = true;
		    if (!closure[ce.getConnectionNode(j)]) {
			if (hg)
			    closure[ce.getConnectionNode(j)] = changed = true;
			continue;
		    }
		    int k;
		    for (k = 0; k != ce.getConnectionNodeCount(); k++) {
			if (j == k)
			    continue;
			int kn = ce.getConnectionNode(k);
			if (ce.getConnection(j, k) && !closure[kn]) {
			    closure[kn] = true;
			    changed = true;
			}
		    }
		}
		if (hasGround)
		    nodesWithGroundConnection.add(ce);
	    }
	    if (changed)
		continue;

	    // connect one of the unconnected nodes to ground with a big resistor, then try again
	    for (i = 0; i != nodeList.size(); i++)
		if (!closure[i] && !getCircuitNode(i).internal) {
		    unconnectedNodes.add(i);
		    console("node " + i + " unconnected");
//		    stampResistor(0, i, 1e8);   // do this later in connectUnconnectedNodes()
		    closure[i] = true;
		    changed = true;
		    break;
		}
	}
    }
    
    // take list of unconnected nodes, which we identified earlier, and connect them to ground
    // with a big resistor.  otherwise we will get matrix errors.  The resistor has to be big,
    // otherwise circuits like 555 Square Wave will break
    void connectUnconnectedNodes() {
	int i;
	for (i = 0; i != unconnectedNodes.size(); i++) {
	    int n = unconnectedNodes.get(i);
	    stampResistor(0, n, 1e8);
	}
    }
    
    boolean validateCircuit() {
	int i, j;
	
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    // look for inductors with no current path
	    if (ce instanceof InductorElm) {
		FindPathInfo fpi = new FindPathInfo(FindPathInfo.INDUCT, ce,
						    ce.getNode(1));
		if (!fpi.findPath(ce.getNode(0))) {
//		    console(ce + " no path");
		    ce.reset();
		}
	    }
	    // look for current sources with no current path
	    if (ce instanceof CurrentElm) {
		CurrentElm cur = (CurrentElm) ce;
		FindPathInfo fpi = new FindPathInfo(FindPathInfo.INDUCT, ce,
						    ce.getNode(1));
		cur.setBroken(!fpi.findPath(ce.getNode(0)));
	    }
	    if (ce instanceof VCCSElm) {
		VCCSElm cur = (VCCSElm) ce;
		FindPathInfo fpi = new FindPathInfo(FindPathInfo.INDUCT, ce,
						    cur.getOutputNode(0));
		if (cur.hasCurrentOutput() && !fpi.findPath(cur.getOutputNode(1))) {
		    cur.broken = true;
		} else
		    cur.broken = false;
	    }
	    
	    // look for voltage source or wire loops.  we do this for voltage sources
	    if (ce.getPostCount() == 2) {
		if (ce instanceof VoltageElm) {
		    FindPathInfo fpi = new FindPathInfo(FindPathInfo.VOLTAGE, ce,
						    ce.getNode(1));
		    if (fpi.findPath(ce.getNode(0))) {
			stop("Voltage source/wire loop with no resistance!", ce);
			return false;
		    }
		}
	    }

	    // look for path from rail to ground
	    if (ce instanceof RailElm || ce instanceof LogicInputElm) {
		FindPathInfo fpi = new FindPathInfo(FindPathInfo.VOLTAGE, ce, ce.getNode(0));
		if (fpi.findPath(0)) {
		    stop("Path to ground with no resistance!", ce);
		    return false;
		}
	    }
	    
	    // look for shorted caps, or caps w/ voltage but no R
	    if (ce.isIdealCapacitor()) {
		FindPathInfo fpi = new FindPathInfo(FindPathInfo.SHORT, ce,
						    ce.getNode(1));
		if (fpi.findPath(ce.getNode(0))) {
		    console(ce + " shorted");
		    ((CapacitorElm) ce).shorted();
		} else {
		    fpi = new FindPathInfo(FindPathInfo.CAP_V, ce, ce.getNode(1));
		    if (fpi.findPath(ce.getNode(0))) {
			// loop of ideal capacitors; set a small series resistance to avoid
			// oscillation in case one of them has voltage on it
			((CapacitorElm) ce).setSeriesResistance(.1);

			// return false to re-stamp the circuit
			return false;
		    }
		}
	    }
	}
	return true;
    }
    
    // analyze the circuit when something changes, so it can be simulated.
    // Most of this has been moved to preStampCircuit() so it can be avoided if the simulation is stopped.
    void analyzeCircuit() {
	stopMessage = null;
	stopElm = null;
	if (elmList.isEmpty()) {
	    postDrawList = new Vector<Point>();
	    badConnectionList = new Vector<Point>();
	    return;
	}
	makePostDrawList();

	needsStamp = true;
    }

    // do the rest of the pre-stamp circuit analysis
    boolean preStampCircuit(boolean subcircuit) {
	InductorCoupling.prepare(elmList);
	int i, j;
	nodeList = new Vector<CircuitNode>();

	calculateWireClosure();
	setGroundNode(subcircuit);

	// allocate nodes and voltage sources
	makeNodeList();
	
	if (!calcWireInfo())
	    return false;
	nodeMap = null; // done with this
	
	int vscount = 0;
	circuitNonLinear = false;

	// determine if circuit is nonlinear.  also set voltage sources
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    if (ce.nonLinear())
		circuitNonLinear = true;
	    int ivs = ce.getVoltageSourceCount();
	    for (j = 0; j != ivs; j++) {
		voltageSources[vscount] = ce;
		ce.setVoltageSource(j, vscount++);
	    }
	}
	voltageSourceCount = vscount;

	// show resistance in voltage sources if there's only one.
	// can't use voltageSourceCount here since that counts internal voltage sources, like the one in GroundElm
	boolean gotVoltageSource = false;
	showResistanceInVoltageSources = true;
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    if (ce instanceof VoltageElm) {
		if (gotVoltageSource)
		    showResistanceInVoltageSources = false;
		else
		    gotVoltageSource = true;
	    }
	}

	findUnconnectedNodes();
	if (!validateCircuit())
	    return false;
	
	nodesWithGroundConnectionCount = nodesWithGroundConnection.size();
	// only need this for validation
	nodesWithGroundConnection = null;
	
	timeStep = maxTimeStep;
	needsStamp = true;
	
	callAnalyzeHook();
	return true;
    }

    // do pre-stamping and then stamp circuit
    void preStampAndStampCircuit() {
	int i;

	// preStampCircuit returns false if there's an error.  It can return false if we have capacitor loops
	// but we just need to try again in that case.  Try again 10 times to avoid infinite loop.
	for (i = 0; i != 10; i++)
	    if (preStampCircuit(false) || stopMessage != null)
		break;
	if (stopMessage != null)
	    return;
	if (i == 10) {
	    stop("failed to stamp circuit", null);
	    return;
	}

	stampCircuit();
    }

    // stamp the matrix, meaning populate the matrix as required to simulate the circuit (for all linear elements, at least).
    // this gets called after something changes in the circuit, and also when auto-adjusting timestep
    void stampCircuit() {
	int i;
	int matrixSize = nodeList.size()-1 + voltageSourceCount;
	circuitMatrix = new double[matrixSize][matrixSize];
	circuitRightSide = new double[matrixSize];
	nodeVoltages = new double[nodeList.size()-1];
	origMatrix = new double[matrixSize][matrixSize];
	origRightSide = new double[matrixSize];
	circuitMatrixSize = circuitMatrixFullSize = matrixSize;
	circuitRowInfo = new RowInfo[matrixSize];
	circuitPermute = new int[matrixSize];
	for (i = 0; i != matrixSize; i++)
	    circuitRowInfo[i] = new RowInfo();
	circuitNeedsMap = false;
	
	connectUnconnectedNodes();

	// stamp linear circuit elements
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    ce.setParentList(elmList);
	    ce.stamp();
	}

	if (!simplifyMatrix(matrixSize))
	    return;
	
	// check if we called stop()
	if (circuitMatrix == null)
	    return;
	
	// if a matrix is linear, we can do the lu_factor here instead of
	// needing to do it every frame
	if (!circuitNonLinear) {
	    if (!lu_factor(circuitMatrix, circuitMatrixSize, circuitPermute)) {
		stop("Singular matrix!", null);
		return;
	    }
	}
	
	// copy elmList to an array to avoid a bunch of calls to canCast() when doing simulation
	elmArr = new CircuitElm[elmList.size()];
	int scopeElmCount = 0;
	for (i = 0; i != elmList.size(); i++) {
	    elmArr[i] = elmList.get(i);
	    if (elmArr[i] instanceof ScopeElm)
		scopeElmCount++;
	}
	
	// copy ScopeElms to an array to avoid a second pass over entire list of elms during simulation
	scopeElmArr = new ScopeElm[scopeElmCount];
	int j = 0;
	for (i = 0; i != elmList.size(); i++) {
	    if (elmArr[i] instanceof ScopeElm)
		scopeElmArr[j++] = (ScopeElm) elmArr[i];
	}	

	needsStamp = false;
    }

    // simplify the matrix; this speeds things up quite a bit, especially for digital circuits.
    // or at least it did before we added wire removal
    boolean simplifyMatrix(int matrixSize) {
	int i, j;
	for (i = 0; i != matrixSize; i++) {
	    int qp = -1;
	    double qv = 0;
	    RowInfo re = circuitRowInfo[i];
	    /*System.out.println("row " + i + " " + re.lsChanges + " " + re.rsChanges + " " +
			       re.dropRow);*/
	    
	    //if (qp != -100) continue;   // uncomment this line to disable matrix simplification for debugging purposes
	    
	    if (re.lsChanges || re.dropRow || re.rsChanges)
		continue;
	    double rsadd = 0;

	    // see if this row can be removed
	    for (j = 0; j != matrixSize; j++) {
		double q = circuitMatrix[i][j];
		if (circuitRowInfo[j].type == RowInfo.ROW_CONST) {
		    // keep a running total of const values that have been
		    // removed already
		    rsadd -= circuitRowInfo[j].value*q;
		    continue;
		}
		// ignore zeroes
		if (q == 0)
		    continue;
		// keep track of first nonzero element that is not ROW_CONST
		if (qp == -1) {
		    qp = j;
		    qv = q;
		    continue;
		}
		// more than one nonzero element?  give up
		break;
	    }
	    if (j == matrixSize) {
		if (qp == -1) {
		    // probably a singular matrix, try disabling matrix simplification above to check this
		    stop("Matrix error", null);
		    return false;
		}
		RowInfo elt = circuitRowInfo[qp];
		// we found a row with only one nonzero nonconst entry; that value
		// is a constant
		if (elt.type != RowInfo.ROW_NORMAL) {
		    System.out.println("type already " + elt.type + " for " + qp + "!");
		    continue;
		}
		elt.type = RowInfo.ROW_CONST;
//		console("ROW_CONST " + i + " " + rsadd);
		elt.value = (circuitRightSide[i]+rsadd)/qv;
		circuitRowInfo[i].dropRow = true;
		// find first row that referenced the element we just deleted
		for (j = 0; j != i; j++)
		    if (circuitMatrix[j][qp] != 0)
			break;
		// start over just before that
		i = j-1;
	    }
	}
	//System.out.println("ac7");

	// find size of new matrix
	int nn = 0;
	for (i = 0; i != matrixSize; i++) {
	    RowInfo elt = circuitRowInfo[i];
	    if (elt.type == RowInfo.ROW_NORMAL) {
		elt.mapCol = nn++;
		//System.out.println("col " + i + " maps to " + elt.mapCol);
		continue;
	    }
	    if (elt.type == RowInfo.ROW_CONST)
		elt.mapCol = -1;
	}

	// make the new, simplified matrix
	int newsize = nn;
	double newmatx[][] = new double[newsize][newsize];
	double newrs  []   = new double[newsize];
	int ii = 0;
	for (i = 0; i != matrixSize; i++) {
	    RowInfo rri = circuitRowInfo[i];
	    if (rri.dropRow) {
		rri.mapRow = -1;
		continue;
	    }
	    newrs[ii] = circuitRightSide[i];
	    rri.mapRow = ii;
	    //System.out.println("Row " + i + " maps to " + ii);
	    for (j = 0; j != matrixSize; j++) {
		RowInfo ri = circuitRowInfo[j];
		if (ri.type == RowInfo.ROW_CONST)
		    newrs[ii] -= ri.value*circuitMatrix[i][j];
		else
		    newmatx[ii][ri.mapCol] += circuitMatrix[i][j];
	    }
	    ii++;
	}

//	console("old size = " + matrixSize + " new size = " + newsize);
	
	circuitMatrix = newmatx;
	circuitRightSide = newrs;
	matrixSize = circuitMatrixSize = newsize;
	for (i = 0; i != matrixSize; i++)
	    origRightSide[i] = circuitRightSide[i];
	for (i = 0; i != matrixSize; i++)
	    for (j = 0; j != matrixSize; j++)
		origMatrix[i][j] = circuitMatrix[i][j];
	circuitNeedsMap = true;
	return true;
    }
    
    // make list of posts we need to draw.  posts shared by 2 elements should be hidden, all
    // others should be drawn.  We can't use the node list for this purpose anymore because wires
    // have the same node number at both ends.
    void makePostDrawList() {
        HashMap<Point,Integer> postCountMap = new HashMap<Point,Integer>();
	int i, j;
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    int posts = ce.getPostCount();
	    for (j = 0; j != posts; j++) {
		Point pt = ce.getPost(j);
		Integer g = postCountMap.get(pt);
		postCountMap.put(pt, g == null ? 1 : g+1);
	    }
	}

	postDrawList = new Vector<Point>();
	badConnectionList = new Vector<Point>();
	for (Map.Entry<Point, Integer> entry : postCountMap.entrySet()) {
	    if (entry.getValue() != 2)
		postDrawList.add(entry.getKey());
	    
	    // look for bad connections, posts not connected to other elements which intersect
	    // other elements' bounding boxes
	    if (entry.getValue() == 1) {
		boolean bad = false;
		Point cn = entry.getKey();
		for (j = 0; j != elmList.size() && !bad; j++) {
		    CircuitElm ce = getElm(j);
		    if ( ce instanceof GraphicElm )
			continue;
		    // does this post intersect elm's bounding box?
		    if (!ce.boundingBox.contains(cn.x, cn.y))
			continue;
		    int k;
		    // does this post belong to the elm?
		    int pc = ce.getPostCount();
		    for (k = 0; k != pc; k++)
			if (ce.getPost(k).equals(cn))
			    break;
		    if (k == pc)
			bad = true;
		}
		if (bad)
		    badConnectionList.add(cn);
	    }
	}
    }

    class FindPathInfo {
	static final int INDUCT  = 1;
	static final int VOLTAGE = 2;
	static final int SHORT   = 3;
	static final int CAP_V   = 4;
	boolean visited[];
	int dest;
	CircuitElm firstElm;
	int type;

	// State object to help find loops in circuit subject to various conditions (depending on type_)
	// elm_ = source and destination element.  dest_ = destination node.
	FindPathInfo(int type_, CircuitElm elm_, int dest_) {
	    dest = dest_;
	    type = type_;
	    firstElm = elm_;
	    visited  = new boolean[nodeList.size()];
	}

	// look through circuit for loop starting at node n1 of firstElm, for a path back to
	// dest node of firstElm
	boolean findPath(int n1) {
	    if (n1 == dest)
		return true;

	    // depth first search, don't need to revisit already visited nodes!
	    if (visited[n1])
		return false;

	    visited[n1] = true;
	    CircuitNode cn = getCircuitNode(n1);
	    int i;
	    if (cn == null)
		return false;
	    for (i = 0; i != cn.links.size(); i++) {
		CircuitNodeLink cnl = cn.links.get(i);
		CircuitElm ce = cnl.elm;
		if (checkElm(n1, ce))
		    return true;
	    }
	    if (n1 == 0) {
		for (i = 0; i != nodesWithGroundConnection.size(); i++)
		    if (checkElm(0, nodesWithGroundConnection.get(i)))
			return true;
	    }
	    return false;
	}
	
	boolean checkElm(int n1, CircuitElm ce) {
		if (ce == firstElm)
		    return false;
		if (type == INDUCT) {
		    // inductors need a path free of current sources
		    if (ce instanceof CurrentElm)
			return false;
		}
		if (type == VOLTAGE) {
		    // when checking for voltage loops, we only care about voltage sources/wires/ground
		    if (!(ce.isWireEquivalent() || ce instanceof VoltageElm || ce instanceof GroundElm))
			return false;
		}
		// when checking for shorts, just check wires
		if (type == SHORT && !ce.isWireEquivalent())
		    return false;
		if (type == CAP_V) {
		    // checking for capacitor/voltage source loops
		    if (!(ce.isWireEquivalent() || ce.isIdealCapacitor() || ce instanceof VoltageElm))
			return false;
		}
		if (n1 == 0) {
		    // look for posts which have a ground connection;
		    // our path can go through ground
		    int j;
		    for (j = 0; j != ce.getConnectionNodeCount(); j++)
			if (ce.hasGroundConnection(j) && findPath(ce.getConnectionNode(j)))
			    return true;
		}
		int j;
		for (j = 0; j != ce.getConnectionNodeCount(); j++) {
		    if (ce.getConnectionNode(j) == n1) {
			if (ce.hasGroundConnection(j) && findPath(0))
			    return true;
			if (type == INDUCT && ce instanceof InductorElm) {
			    // inductors can use paths with other inductors of matching current
			    double c = ce.getCurrent();
			    if (j == 0)
				c = -c;
			    if (Math.abs(c-firstElm.getCurrent()) > 1e-10)
				continue;
			}
			int k;
			for (k = 0; k != ce.getConnectionNodeCount(); k++) {
			    if (j == k)
				continue;
			    if (ce.getConnection(j, k) && findPath(ce.getConnectionNode(k))) {
				//System.out.println("got findpath " + n1);
				return true;
			    }
			}
		    }
		}
	    return false;
	}
    }

    void stop(String s, CircuitElm ce) {
	stopMessage = Locale.LS(s);
	circuitMatrix = null;  // causes an exception
	stopElm = ce;
	setSimRunning(false);
	analyzeFlag = false;
//	cv.repaint();
    }
    
    // control voltage source vs with voltage from n1 to n2 (must
    // also call stampVoltageSource())
    void stampVCVS(int n1, int n2, double coef, int vs) {
	int vn = nodeList.size()+vs;
	stampMatrix(vn, n1, coef);
	stampMatrix(vn, n2, -coef);
    }
    
    // stamp independent voltage source #vs, from n1 to n2, amount v
    void stampVoltageSource(int n1, int n2, int vs, double v) {
	int vn = nodeList.size()+vs;
	stampMatrix(vn, n1, -1);
	stampMatrix(vn, n2, 1);
	stampRightSide(vn, v);
	stampMatrix(n1, vn, 1);
	stampMatrix(n2, vn, -1);
    }

    // use this if the amount of voltage is going to be updated in doStep(), by updateVoltageSource()
    void stampVoltageSource(int n1, int n2, int vs) {
	int vn = nodeList.size()+vs;
	stampMatrix(vn, n1, -1);
	stampMatrix(vn, n2, 1);
	stampRightSide(vn);
	stampMatrix(n1, vn, 1);
	stampMatrix(n2, vn, -1);
    }
    
    // update voltage source in doStep()
    void updateVoltageSource(int n1, int n2, int vs, double v) {
	int vn = nodeList.size()+vs;
	stampRightSide(vn, v);
    }
    
    void stampResistor(int n1, int n2, double r) {
	double r0 = 1/r;
	if (Double.isNaN(r0) || Double.isInfinite(r0)) {
	    System.out.print("bad resistance " + r + " " + r0 + "\n");
	    int a = 0;
	    a /= a;
	}
	stampMatrix(n1, n1, r0);
	stampMatrix(n2, n2, r0);
	stampMatrix(n1, n2, -r0);
	stampMatrix(n2, n1, -r0);
    }

    void stampConductance(int n1, int n2, double r0) {
	stampMatrix(n1, n1, r0);
	stampMatrix(n2, n2, r0);
	stampMatrix(n1, n2, -r0);
	stampMatrix(n2, n1, -r0);
    }

    // specify that current from cn1 to cn2 is equal to voltage from vn1 to 2, divided by g
    void stampVCCurrentSource(int cn1, int cn2, int vn1, int vn2, double g) {
	stampMatrix(cn1, vn1, g);
	stampMatrix(cn2, vn2, g);
	stampMatrix(cn1, vn2, -g);
	stampMatrix(cn2, vn1, -g);
    }

    void stampCurrentSource(int n1, int n2, double i) {
	stampRightSide(n1, -i);
	stampRightSide(n2, i);
    }

    // stamp a current source from n1 to n2 depending on current through vs
    void stampCCCS(int n1, int n2, int vs, double gain) {
	int vn = nodeList.size()+vs;
	stampMatrix(n1, vn, gain);
	stampMatrix(n2, vn, -gain);
    }

    // stamp value x in row i, column j, meaning that a voltage change
    // of dv in node j will increase the current into node i by x dv.
    // (Unless i or j is a voltage source node.)
    void stampMatrix(int i, int j, double x) {
	if (Double.isInfinite(x))
	    debugger();
	if (i > 0 && j > 0) {
	    if (circuitNeedsMap) {
		i = circuitRowInfo[i-1].mapRow;
		RowInfo ri = circuitRowInfo[j-1];
		if (ri.type == RowInfo.ROW_CONST) {
		    //System.out.println("Stamping constant " + i + " " + j + " " + x);
		    circuitRightSide[i] -= x*ri.value;
		    return;
		}
		j = ri.mapCol;
		//System.out.println("stamping " + i + " " + j + " " + x);
	    } else {
		i--;
		j--;
	    }
	    circuitMatrix[i][j] += x;
	}
    }

    // stamp value x on the right side of row i, representing an
    // independent current source flowing into node i
    void stampRightSide(int i, double x) {
	if (i > 0) {
	    if (circuitNeedsMap) {
		i = circuitRowInfo[i-1].mapRow;
		//System.out.println("stamping " + i + " " + x);
	    } else
		i--;
	    circuitRightSide[i] += x;
	}
    }

    // indicate that the value on the right side of row i changes in doStep()
    void stampRightSide(int i) {
	//System.out.println("rschanges true " + (i-1));
	if (i > 0)
	    circuitRowInfo[i-1].rsChanges = true;
    }
    
    // indicate that the values on the left side of row i change in doStep()
    void stampNonLinear(int i) {
	if (i > 0)
	    circuitRowInfo[i-1].lsChanges = true;
    }

    double getIterCount() {
    	// IES - remove interaction
	if (speedBar.getValue() == 0)
	   return 0;

	 return .1*Math.exp((speedBar.getValue()-61)/24.);

    }

    // we need to calculate wire currents for every iteration if someone is viewing a wire in the
    // scope.  Otherwise we can do it only once per frame.
    boolean canDelayWireProcessing() {
	int i;
	for (i = 0; i != scopeCount; i++)
	    if (scopes[i].viewingWire())
		return false;
	for (i=0; i != elmList.size(); i++)
	    if (getElm(i) instanceof ScopeElm && ((ScopeElm)getElm(i)).elmScope.viewingWire())
		return false;
	return true;
    }
    
    boolean converged;
    int subIterations;
    
    void runCircuit(boolean didAnalyze, int frameTimeLimit) {
	if (circuitMatrix == null || elmList.size() == 0) {
	    circuitMatrix = null;
	    return;
	}
	int iter;
	//int maxIter = getIterCount();
	boolean debugprint = dumpMatrix;
	dumpMatrix = false;
	long steprate = (long) (160*getIterCount());
	long tm = System.currentTimeMillis();
	long lit = lastIterTime;
	if (lit == 0) {
	    lastIterTime = tm;
	    return;
	}
	
	// Check if we don't need to run simulation (for very slow simulation speeds).
	// If the circuit changed, do at least one iteration to make sure everything is consistent.
	if (1000 >= steprate*(tm-lastIterTime) && !didAnalyze)
	    return;
	
	boolean recordComponentMeasurements = hasScopePanel() && probeViewer.useWaveform();
	boolean delayWireProcessing = !recordComponentMeasurements && canDelayWireProcessing();
	
	int timeStepCountAtFrameStart = timeStepCount;
	
	long deadline = tm + frameTimeLimit;
	final int subiterCount = 5000;
	
	for (iter = 1; ; iter++) {
	    int i, j, subiter;
	    for (i = 0; i != elmArr.length; i++)
		elmArr[i].startIteration();
	    for (subiter = 0; subiter != subiterCount; subiter++) {
		converged = true;
		subIterations = subiter;
		for (i = 0; i != circuitMatrixSize; i++)
		    circuitRightSide[i] = origRightSide[i];
		if (circuitNonLinear) {
		    for (i = 0; i != circuitMatrixSize; i++)
			for (j = 0; j != circuitMatrixSize; j++)
			    circuitMatrix[i][j] = origMatrix[i][j];
		}
		for (i = 0; i != elmArr.length; i++)
		    elmArr[i].doStep();
		if (stopMessage != null)
		    return;
		boolean printit = debugprint;
		debugprint = false;
		if (circuitMatrixSize < 8) {
		    // we only need this for debugging purposes, so skip it for large matrices 
		    for (j = 0; j != circuitMatrixSize; j++) {
			for (i = 0; i != circuitMatrixSize; i++) {
			    double x = circuitMatrix[i][j];
			    if (Double.isNaN(x) || Double.isInfinite(x)) {
				stop("nan/infinite matrix!", null);
				console("circuitMatrix " + i + " " + j + " is " + x);
				return;
			    }
			}
		    }
		}
		if (printit) {
		    for (j = 0; j != circuitMatrixSize; j++) {
			String x = "";
			for (i = 0; i != circuitMatrixSize; i++)
			    x += circuitMatrix[j][i] + ",";
			x += "\n";
			console(x);
		    }
		    console("done");
		}
		if (circuitNonLinear) {
		    // stop if converged (elements check for convergence in doStep())
		    if (converged && subiter > 0)
			break;
		    if (!lu_factor(circuitMatrix, circuitMatrixSize,
				  circuitPermute)) {
			stop("Singular matrix!", null);
			return;
		    }
		}
		lu_solve(circuitMatrix, circuitMatrixSize, circuitPermute,
			 circuitRightSide);
		applySolvedRightSide(circuitRightSide);
		if (!circuitNonLinear)
		    break;
	    }
	    if (subiter == subiterCount) {
		console("convergence failed after " + subiter + " iterations");
		stop("Convergence failed!", null);
		break;
	    }
	    if (subiter > 5)
		console("converged after " + subiter + " iterations, timeStep = " + timeStep);
	    t += timeStep;
	    timeStepAccum += timeStep;
	    if (timeStepAccum >= maxTimeStep) {
		timeStepAccum -= maxTimeStep;
		timeStepCount++;
	    }
	    for (i = 0; i != elmArr.length; i++)
		elmArr[i].stepFinished();
	    if (!delayWireProcessing)
		calcWireCurrents();
	    if (recordComponentMeasurements)
	        componentMeasurements.add(t, elmArr);
	    if (averagePowerLossCheckItem.getState())
	        averagePowerLoss.add(elmArr, timeStep);
            probeViewer.sampleProbes(t);
	    for (i = 0; i != scopeCount; i++)
	    	scopes[i].timeStep();
            probeViewer.sampleSaved(t);
	    for (i=0; i != scopeElmArr.length; i++)
		scopeElmArr[i].stepScope();
	    callTimeStepHook();
		
	    tm = System.currentTimeMillis();
	    lit = tm;
	    // Check whether enough time has elapsed to perform an *additional* iteration after
	    // those we have already completed, while leaving time to draw and handle input.
	    if ((timeStepCount-timeStepCountAtFrameStart)*1000 >= steprate*(tm-lastIterTime) || tm >= deadline)
		break;
	    if (!simRunning)
		break;
	} // for (iter = 1; ; iter++)
	lastIterTime = lit;
	if (delayWireProcessing)
	    calcWireCurrents();
    }

    // set node voltages given right side found by solving matrix
    void applySolvedRightSide(double rs[]) {
//	console("setvoltages " + rs);
	int j;
	for (j = 0; j != circuitMatrixFullSize; j++) {
	    RowInfo ri = circuitRowInfo[j];
	    double res = 0;
	    if (ri.type == RowInfo.ROW_CONST)
		res = ri.value;
	    else
		res = rs[ri.mapCol];
	    if (Double.isNaN(res)) {
		converged = false;
		break;
	    }
	    if (j < nodeList.size()-1) {
		nodeVoltages[j] = res;
	    } else {
		int ji = j-(nodeList.size()-1);
		voltageSources[ji].setCurrent(ji, res);
	    }
	}
	
	setNodeVoltages(nodeVoltages);
    }
    
    // set node voltages in each element given an array of node voltages
    void setNodeVoltages(double nv[]) {
	int j, k;
	for (j = 0; j != nv.length; j++) {
	    double res = nv[j];
	    CircuitNode cn = getCircuitNode(j+1);
	    for (k = 0; k != cn.links.size(); k++) {
		CircuitNodeLink cnl = cn.links.elementAt(k);
		cnl.elm.setNodeVoltage(cnl.num, res);
	    }
	}
    }
    
    // we removed wires from the matrix to speed things up.  in order to display wire currents,
    // we need to calculate them now.
    void calcWireCurrents() {
	int i;
	
	// for debugging
	//for (i = 0; i != wireInfoList.size(); i++)
	 //   wireInfoList.get(i).wire.setCurrent(-1, 1.23);
	
	for (i = 0; i != wireInfoList.size(); i++) {
	    WireInfo wi = wireInfoList.get(i);
	    double cur = 0;
	    int j;
	    Point p = wi.wire.getPost(wi.post);
	    for (j = 0; j != wi.neighbors.size(); j++) {
		CircuitElm ce = wi.neighbors.get(j);
		int n = ce.getNodeAtPoint(p.x, p.y);
		cur += ce.getCurrentIntoNode(n);
	    }
	    // get correct current polarity
	    // (LabeledNodes may have wi.post == 1, in which case we flip the current sign)
	    if (wi.post == 0 || (wi.wire instanceof LabeledNodeElm))
		wi.wire.setCurrent(-1, cur);
	    else
		wi.wire.setCurrent(-1, -cur);
	}
    }
    
    int min(int a, int b) { return (a < b) ? a : b; }
    int max(int a, int b) { return (a > b) ? a : b; }
    
    public void resetAction(){
        componentMeasurements.clear();
        averagePowerLoss.clear();
        if (probeViewer != null)
            probeViewer.clearHistory();
    	int i;
    	analyzeFlag = true;
    	if (t == 0)
    	    setSimRunning(true);
    	t = timeStepAccum = 0;
    	timeStepCount = 0;
    	for (i = 0; i != elmList.size(); i++)
		getElm(i).reset();
	for (i = 0; i != scopeCount; i++)
		scopes[i].resetGraph(true);
    	repaint();
    }
    
    static void electronSaveAsCallback(String s) {
	s = s.substring(s.lastIndexOf('/')+1);
	s = s.substring(s.lastIndexOf('\\')+1);
	theSim.setCircuitTitle(s);
	theSim.allowSave(true);
	theSim.savedFlag = true;
	theSim.repaint();
    }

    static void electronSaveCallback() {
	theSim.savedFlag = true;
	theSim.repaint();
    }

    static native void electronSaveAs(String dump) /*-{
        $wnd.showSaveDialog().then(function (file) {
            if (file.canceled)
            	return;
            $wnd.saveFile(file, dump);
            @com.lushprojects.circuitjs1.client.CirSim::electronSaveAsCallback(Ljava/lang/String;)(file.filePath.toString());
        });
    }-*/;

    static native void electronSave(String dump) /*-{
        $wnd.saveFile(null, dump);
        @com.lushprojects.circuitjs1.client.CirSim::electronSaveCallback()();
    }-*/;
    
    static void electronOpenFileCallback(String text, String name) {
	LoadFile.doLoadCallback(text, name);
	theSim.allowSave(true);
    }
    
    static native void electronOpenFile() /*-{
        $wnd.openFile(function (text, name) {
            @com.lushprojects.circuitjs1.client.CirSim::electronOpenFileCallback(Ljava/lang/String;Ljava/lang/String;)(text, name);
        });
    }-*/;
    
    static native void toggleDevTools() /*-{
        $wnd.toggleDevTools();
    }-*/;
    
    static native boolean isElectron() /*-{
        return ($wnd.openFile != undefined);
    }-*/;    

    static native String getElectronStartCircuitText() /*-{
    	return $wnd.startCircuitText;
    }-*/;    
    
    void allowSave(boolean b) {
	if (saveFileItem != null && mobileProjects == null)
	    saveFileItem.setEnabled(b);
    }
    
    public void menuPerformed(String menu, String item) {
	if (multiplePlacement)
	    resetToSelectMode();
	if ((menu=="edit" || menu=="main" || menu=="scopes") && noEditCheckItem.getState()) {
	    Window.alert(Locale.LS("Editing disabled.  Re-enable from the Options menu."));
	    return;
	}
    	if (item=="about")
    		aboutBox = new AboutBox(circuitjs1.versionString);
        if (item=="importfromlocalfile") {
            if (mobileProjects != null)
                mobileProjects.open();
            else if (isElectron()) {
                pushUndo();
                electronOpenFile();
            } else {
                pushUndo();
                loadFileInput.click();
            }
        }
        if (item=="newwindow") {
            Window.open(Document.get().getURL(), "_blank", "");
        }
        if (item=="save" || item=="saveas") {
            if (isEditingSubcircuit())
                saveSubcircuitAndReturn();
            else if (mobileProjects != null)
                mobileProjects.save(item=="saveas", () -> {});
            else if (item=="save")
                electronSave(dumpCircuit());
            else
                electronSaveAs(dumpCircuit());
        }
    	if (item=="importfromtext") {
    		dialogShowing = new ImportFromTextDialog(this);
    	}
    	if (item=="importfromdropbox") {
    		dialogShowing = new ImportFromDropboxDialog(this);
    	}
    	if (item=="exportasurl") {
    		doExportAsUrl();
    		unsavedChanges = false;
    	}
    	if (item=="exportaslocalfile") {
            if (isEditingSubcircuit())
                saveSubcircuitAndReturn();
            else {
                doExportAsLocalFile();
                unsavedChanges = false;
            }
    	}
    	if (item=="exportastext") {
    		doExportAsText();
    		unsavedChanges = false;
    	}
        if (item=="insertimage" && !isEditingSubcircuit() && !noEditCheckItem.getState()) {
            resetToSelectMode();
            canvasImages.choose();
        }
    	if (item=="exportasimage")
		doExportAsImage();
    	if (item=="copypng") {
		doImageToClipboard();
    		if (contextPanel!=null)
			contextPanel.hide();
    	}
    	if (item=="exportassvg")
		doExportAsSVG();
    	if (item=="createsubcircuit")
		doCreateSubcircuit();
    	if (item=="dcanalysis")
    	    	doDCAnalysis();
    	if (item=="print")
    	    	doPrint();
    	if (item=="recover")
    	    	doRecover();

    	if ((menu=="elm" || menu=="scopepop") && contextPanel!=null)
    		contextPanel.hide();
    	if (menu=="options" && item=="shortcuts") {
    	    	dialogShowing = new ShortcutsDialog(this);
    	    	dialogShowing.show();
    	}
    	if (menu=="options" && item=="subcircuits") {
    	    	dialogShowing = new SubcircuitDialog(this);
    	    	dialogShowing.show();
    	}
    	if (item=="search") {
    	    	dialogShowing = new SearchDialog(this);
    	    	dialogShowing.show();
    	}
    	if (menu=="options" && item=="other")
    		doEdit(new EditOptions(this));
    	if (item=="devtools")
    	    toggleDevTools();
    	if (item=="undo")
    		doUndo();
    	if (item=="redo")
    		doRedo();
    	
    	// if the mouse is hovering over an element, and a shortcut key is pressed, operate on that element (treat it like a context menu item selection)
    	if (menu == "key" && mouseElm != null) {
    	    menuElm = mouseElm;
    	    menu = "elm";
    	}
	if (menu != "elm")
		menuElm = null;

    	if (item == "cut") {
    		doCut();
    	}
    	if (item == "copy") {
    		doCopy();
    	}
    	if (item=="paste")
    		doPaste(null);
    	if (item=="duplicate") {
    	    	doDuplicate();
    	}
    	if (item=="flip")
    	    doFlip();
    	if (item=="split")
    	    doSplit(menuElm);
    	if (item=="selectAll")
    		doSelectAll();
    	//	if (e.getSource() == exitItem) {
    	//	    destroyFrame();
    	//	    return;
    	//	}
    	
    	if (item=="centrecircuit") {
    		pushUndo();
    		centreCircuit();
    	}
    	if (item=="flipx") {
	    pushUndo();
	    flipX();
    	}
    	if (item=="flipy") {
	    pushUndo();
	    flipY();
    	}
    	if (item=="flipxy") {
	    pushUndo();
	    flipXY();
    	}
        if (item=="stackAll" || item=="unstackAll" || item=="combineAll") {
            if (item=="combineAll" && hasFloatingScopes())
                combineFloatingScopes();
            else if (probeViewer.useWaveform())
                probeViewer.arrangeTraces(item.substring(0, item.length() - 3));
            else if (item=="stackAll")
                stackAll();
            else if (item=="unstackAll")
                unstackAll();
            else
                combineAll();
            circuitChanged();
        }
        if (item=="separateAll") {
            separateAll();
            circuitChanged();
        }
    	if (item=="zoomin")
    	    zoomCircuit(20, true);
    	if (item=="zoomout")
    	    zoomCircuit(-20, true);
    	if (item=="zoom100")
    	    setCircuitScale(1, true);
    	if (menu=="elm" && item=="edit")
    		doEdit(menuElm);
        if (menu == "elm" && item == "coupleInductors" && menuElm instanceof InductorElm) {
            dialogShowing = new InductorCouplingDialog(this, (InductorElm) menuElm);
            dialogShowing.center();
        }
    	if (item=="delete") {
    		if (menu!="elm")
    			menuElm = null;
    		pushUndo();
    		doDelete(true);
    	}
    	if (item=="sliders")
    	    doSliders(menuElm);

    	if (item=="viewInScope" && menuElm != null) {
    		int i;
    		for (i = 0; i != scopeCount; i++)
    			if (scopes[i].getElm() == null)
    				break;
    		if (i == scopeCount) {
    			if (scopeCount == scopes.length)
    				return;
    			scopeCount++;
    			scopes[i] = new Scope(this);
    			scopes[i].position = i;
    			//handleResize();
    		}
    		scopes[i].setElm(menuElm);
    		if (i > 0)
    		    scopes[i].speed = scopes[i-1].speed;
                circuitChanged();
    	}
    	
    	if (item=="viewInFloatScope" && menuElm != null) {
    	    ScopeElm newScope = new ScopeElm(snapGrid(menuElm.x+50), snapGrid(menuElm.y+50));
    	    elmList.addElement(newScope);
    	    newScope.setScopeElm(menuElm);
    	    
    	    // need to rebuild scopeElmArr
    	    needAnalyze();
	}
    	
    	if (item.startsWith("addToScope") && menuElm != null) {
    	    int n;
    	    n = Integer.parseInt(item.substring(10));
    	    if (n < scopeCount + countScopeElms()) {
    		if (n < scopeCount )
    		    scopes[n].addElm(menuElm);
    		else
    		    getNthScopeElm(n-scopeCount).elmScope.addElm(menuElm);
    	    }
    	    scopeMenuSelected = -1;
            circuitChanged();
    	}
    	
    	if (menu=="scopepop") {
    		pushUndo();
    		Scope s;
		if (menuScope != -1 )
		    	s= scopes[menuScope];
		else
		    	s= ((ScopeElm)mouseElm).elmScope;

    		if (item=="dock") {
            		if (scopeCount == scopes.length)
            			return;
            		scopes[scopeCount] = ((ScopeElm)mouseElm).elmScope;
            		((ScopeElm)mouseElm).clearElmScope();
            		scopes[scopeCount].position = scopeCount;
            		scopeCount++;
            		doDelete(false);
    		}
    		if (item=="undock") {
		    CircuitElm elm = s.getElm();
    	    	    ScopeElm newScope = new ScopeElm(snapGrid(elm.x+50), snapGrid(elm.y+50));
    	    	    elmList.addElement(newScope);
    	    	    newScope.setElmScope(scopes[menuScope]);
    	    	    
    	    	    int i;
    	    	    // remove scope from list.  setupScopes() will fix the positions
                    for (i = menuScope; i < scopeCount - 1; i++)
    	    		scopes[i] = scopes[i+1];
                    scopes[--scopeCount] = null;

    	            needAnalyze();      // need to rebuild scopeElmArr
    		}
    		if (item=="remove")
    		    	s.setElm(null);  // setupScopes() will clean this up
    		if (item=="removeplot")
			s.removePlot(menuPlot);
    		if (item=="speed2")
    			s.speedUp();
    		if (item=="speed1/2")
    			s.slowDown();
//    		if (item=="scale")
//    			scopes[menuScope].adjustScale(.5);
                if (menuScope < 0 || !probeViewer.applySavedTraceOption(s, menuPlot, item)) {
                    if (item=="maxscale")
                        s.maxScale();
                    if (item=="stack")
                        stackScope(menuScope);
                    if (item=="unstack")
                        unstackScope(menuScope);
                    if (item=="combine")
                        combineScope(menuScope);
                }
    		if (item=="selecty")
    			s.selectY();
                if (item=="reset") {
                    s.resetGraph(true);
                    probeViewer.resetSavedHistory(s);
                }
    		if (item=="properties")
			s.properties();
                else if (item != "reset")
                    circuitChanged();
    		deleteUnusedScopeElms();
    	}
    	if (menu=="circuits" && item.indexOf("setup ") ==0) {
            if (mobileProjects == null)
                pushUndo();
    		int sp = item.indexOf(' ', 6);
    		readSetupFile(item.substring(6, sp), item.substring(sp+1));
    	}
        if (item=="newblankcircuit") {
            if (mobileProjects != null)
                mobileProjects.newBlank();
            else {
                pushUndo();
                readSetupFile("blank.txt", "Blank Circuit");
            }
        }

    	//	if (ac.indexOf("setup ") == 0) {
    	//	    pushUndo();
    	//	    readSetupFile(ac.substring(6),
    	//			  ((MenuItem) e.getSource()).getLabel());
    	//	}

    	// IES: Moved from itemStateChanged()
    	if (menu=="main") {
    		if (contextPanel!=null)
    			contextPanel.hide();
    		//	MenuItem mmi = (MenuItem) mi;
    		//		int prevMouseMode = mouseMode;
    		setMouseMode(MODE_ADD_ELM);
    		String s = item;
    		if (s.length() > 0)
    			mouseModeStr = s;
    		if (s.compareTo("DragAll") == 0)
    			setMouseMode(MODE_DRAG_ALL);
    		else if (s.compareTo("DragRow") == 0)
    			setMouseMode(MODE_DRAG_ROW);
    		else if (s.compareTo("DragColumn") == 0)
    			setMouseMode(MODE_DRAG_COLUMN);
    		else if (s.compareTo("DragSelected") == 0)
    			setMouseMode(MODE_DRAG_SELECTED);
    		else if (s.compareTo("DragPost") == 0)
    			setMouseMode(MODE_DRAG_POST);
    		else if (s.compareTo("Select") == 0)
    			setMouseMode(MODE_SELECT);

		updateToolbar();

    		//		else if (s.length() > 0) {
    		//			try {
    		//				addingClass = Class.forName(s);
    		//			} catch (Exception ee) {
    		//				ee.printStackTrace();
    		//			}
    		//		}
    		//		else
    		//			setMouseMode(prevMouseMode);
    		tempMouseMode = mouseMode;
    	}
    	if (item=="fullscreen") {
    	    if (! Graphics.isFullScreen)
    		Graphics.viewFullScreen();
    	    else
    		Graphics.exitFullScreen();
    	    centreCircuit();
    	}
    
	repaint();
    }
    
    int countScopeElms() {
	int c = 0;
	for (int i = 0; i != elmList.size(); i++) {
	    if ( elmList.get(i) instanceof ScopeElm)
		c++;
	}
	return c;
    }
    
    ScopeElm getNthScopeElm(int n) {
	for (int i = 0; i != elmList.size(); i++) {
	    if ( elmList.get(i) instanceof ScopeElm) {
		n--;
		if (n<0)
		    return (ScopeElm) elmList.get(i);
	    }
	}
	return (ScopeElm) null;
    }
    
    
    boolean canStackScope(int s) {
	if (scopeCount < 2) 
	    return false;
	if (s==0)
	    s=1;
    	if (scopes[s].position == scopes[s-1].position)
    	    return false;
	return true;
    }
    
    boolean canCombineScope(int s) {
	return scopeCount >=2;
    }
    
    boolean canUnstackScope(int s) {
	if (scopeCount < 2) 
	    return false;
	if (s==0)
	    s=1;
    	if (scopes[s].position != scopes[s-1].position) {
        	if ( s + 1 < scopeCount && scopes[s+1].position == scopes[s].position) // Allow you to unstack by selecting the top scope in the stack
        	    return true;
        	else
        	    return false;
    	}
	return true;
    }

    void stackScope(int s) {
	if (! canStackScope(s) )
	    return;
    	if (s == 0) {
    		s = 1;
    	}
    	scopes[s].position = scopes[s-1].position;
    	for (s++; s < scopeCount; s++)
    		scopes[s].position--;
    }

    void unstackScope(int s) {
	if (! canUnstackScope(s) )
	    return;
    	if (s == 0) {
    		s = 1;
    	}
    	if (scopes[s].position != scopes[s-1].position) // Allow you to unstack by selecting the top scope in the stack
    	    s++;
    	for (; s < scopeCount; s++)
    		scopes[s].position++;
    }

    void combineScope(int s) {
	if (! canCombineScope(s))
	    return;
    	if (s == 0) {
    		s = 1;
    	}
    	scopes[s-1].combine(scopes[s]);
    	scopes[s].setElm(null);
    }
    

    void stackAll() {
    	int i;
    	for (i = 0; i != scopeCount; i++) {
    		scopes[i].position = 0;
    		scopes[i].showMax = scopes[i].showMin = false;
    	}
    }

    void unstackAll() {
    	int i;
    	for (i = 0; i != scopeCount; i++) {
    		scopes[i].position = i;
    		scopes[i].showMax = true;
    	}
    }

    void combineAll() {
    	int i;
    	for (i = scopeCount-2; i >= 0; i--) {
    	    scopes[i].combine(scopes[i+1]);
    	    scopes[i+1].setElm(null);
    	}
    }
    
    private boolean hasFloatingScopes() {
        for (CircuitElm elm : elmList) {
            if (elm instanceof ScopeElm)
                return true;
        }
        return false;
    }

    private void combineFloatingScopes() {
        pushUndo();
        Vector<Scope> docked = new Vector<Scope>();
        Vector<Scope> timeScopes = new Vector<Scope>();
        for (int i = 0; i < scopeCount; i++) {
            Scope scope = scopes[i];
            if (scope.needToRemove())
                continue;
            if (scope.plot2d || scope.showFFT)
                docked.add(scope);
            else
                timeScopes.add(scope);
        }

        Vector<ScopeElm> floating = new Vector<ScopeElm>();
        // Leave one docked slot available for the combined time traces.
        int specialLimit = scopes.length - 1;
        for (CircuitElm elm : elmList) {
            if (!(elm instanceof ScopeElm))
                continue;
            ScopeElm window = (ScopeElm) elm;
            Scope scope = window.elmScope;
            if (!scope.needToRemove()) {
                if (scope.plot2d || scope.showFFT) {
                    if (docked.size() >= specialLimit)
                        continue;
                    docked.add(scope);
                } else {
                    if (docked.size() == scopes.length)
                        continue;
                    timeScopes.add(scope);
                }
            }
            floating.add(window);
        }

        if (!timeScopes.isEmpty()) {
            Scope combined = timeScopes.firstElement();
            if (timeScopes.size() > 1)
                combined.combineTimeTraces(timeScopes);
            docked.insertElementAt(combined, 0);
        }

        for (ScopeElm window : floating) {
            if (mouseElm == window)
                setMouseElm(null);
            window.delete();
            elmList.remove(window);
        }
        Arrays.fill(scopes, null);
        scopeCount = docked.size();
        for (int i = 0; i < scopeCount; i++) {
            scopes[i] = docked.get(i);
            scopes[i].position = i;
        }
        scopeSelected = scopeMenuSelected = -1;
        scopeCollapsed = false;
        setCircuitArea();
        probeViewer.syncScopes(true);
        probeViewer.arrangeTraces("combine");
        needAnalyze();
        unsavedChanges = true;
        updateScopeMenuItems();
        writeRecoveryToStorage();
    }


    void clearDockedScopes() {
        if (scopeCount == 0)
            return;
        pushUndo();
        Arrays.fill(scopes, 0, scopeCount, null);
        scopeCount = 0;
        scopeSelected = scopeMenuSelected = -1;
        activeDockedScope = null;
        unsavedChanges = true;
        circuitChanged();
        updateScopeMenuItems();
        writeRecoveryToStorage();
    }

    void separateAll() {
        if (scopeCount == 0)
            return;
        pushUndo();
        Vector<Scope> separated = new Vector<Scope>();
        for (int i = 0; i < scopeCount; i++) {
            if (!scopes[i].needToRemove())
                scopes[i].separateInto(separated);
        }

        Arrays.fill(scopes, 0, scopeCount, null);
        scopeCount = 0;
        scopeSelected = scopeMenuSelected = -1;
        int previousTop = circuitArea.y;
        setCircuitArea();
        transform[5] += previousTop - circuitArea.y;
        Vector<Rectangle> occupied = new Vector<Rectangle>();
        for (CircuitElm elm : elmList) {
            if (elm instanceof ScopeElm)
                occupied.add(new Rectangle(min(elm.x, elm.x2), min(elm.y, elm.y2),
                        Math.abs(elm.x2 - elm.x), Math.abs(elm.y2 - elm.y)));
        }

        for (Scope scope : separated) {
            scope.setMobileDocked(false);
            Rectangle bounds = scope.getElm().getBoundingBox();
            Rectangle placement = placeFloatingScope(bounds, occupied);
            ScopeElm floating = new ScopeElm(placement.x, placement.y);
            floating.setElmScope(scope);
            elmList.add(floating);
            occupied.add(placement);
        }
        needAnalyze();
        unsavedChanges = true;
        updateScopeMenuItems();
        writeRecoveryToStorage();
    }

    private Rectangle placeFloatingScope(Rectangle bounds, Vector<Rectangle> occupied) {
        int width = ScopeElm.DEFAULT_WIDTH, height = ScopeElm.DEFAULT_HEIGHT;
        int gap = 2 * gridSize;
        int x = snapGrid(bounds.x + (bounds.width - width) / 2);
        int y = snapGrid(bounds.y + (bounds.height - height) / 2);
        Rectangle fallback = null;
        // Try the four sides of the component, expanding outward for crowded areas.
        for (int ring = 0; ring <= occupied.size(); ring++) {
            Rectangle[] candidates = {
                new Rectangle(x, snapGrid(bounds.y + bounds.height + gap) + ring * (height + gap), width, height),
                new Rectangle(snapGrid(bounds.x + bounds.width + gap) + ring * (width + gap), y, width, height),
                new Rectangle(x, snapGrid(bounds.y - height - gap) - ring * (height + gap), width, height),
                new Rectangle(snapGrid(bounds.x - width - gap) - ring * (width + gap), y, width, height)
            };
            for (Rectangle candidate : candidates) {
                if (occupied.stream().anyMatch(candidate::intersects))
                    continue;
                if (fallback == null)
                    fallback = candidate;
                if (circuitArea.contains(transformX(candidate.x), transformY(candidate.y))
                        && circuitArea.contains(transformX(candidate.x + width), transformY(candidate.y + height)))
                    return candidate;
            }
        }
        return fallback;
    }


    void doEdit(Editable eable) {
    	clearSelection();
    	pushUndo();
    	if (editDialog != null) {
    //		requestFocus();
    		editDialog.setVisible(false);
    		editDialog = null;
    	}
    	editDialog = new EditDialog(eable, this);
    	editDialog.show();
    }
    
    void doSliders(CircuitElm ce) {
	clearSelection();
	pushUndo();
	dialogShowing = new SliderDialog(ce, this);
	dialogShowing.show();
    }


    void doExportAsUrl()
    {
    	String dump = dumpCircuit();
	dialogShowing = new ExportAsUrlDialog(dump);
	dialogShowing.show();
    }
    
    void doExportAsText()
    {
    	String dump = dumpCircuit();
    	dialogShowing = new ExportAsTextDialog(this, dump);
    	dialogShowing.show();
    }

    void doExportAsImage()
    {
    	dialogShowing = new ExportAsImageDialog(CAC_IMAGE);
    	dialogShowing.show();
    }

    private static native void clipboardWriteImage(CanvasElement cv) /*-{
	cv.toBlob(function(blob) {
	    var promise = parent.navigator.clipboard.write([new ClipboardItem({ "image/png": blob })]);
	    promise.then(function(x) { console.log(x); });
	});
    }-*/;

    void doImageToClipboard()
    {
	Canvas cv = CirSim.theSim.getCircuitAsCanvas(CAC_IMAGE);
	clipboardWriteImage(cv.getCanvasElement());
    }
    
    boolean isEditingSubcircuit() {
	return subcircuitEdit != null;
    }

    boolean useUpdatedSubcircuit(String name) {
	return staleSubcircuitStates.contains(name);
    }

    void openSubcircuitEditor(CustomCompositeModel model) {
	if (mobileProjects != null && !isEditingSubcircuit())
	    mobileProjects.flushSession();
	subcircuitEdit = new SubcircuitEdit(model);
	undoStack = new Vector<UndoItem>();
	redoStack = new Vector<UndoItem>();
	updateSubcircuitActions();
	readCircuit(model.modelCircuit, RC_SUBCIRCUIT_EDITOR);
	setCircuitTitle(model.name);
	enableUndoRedo();
    }

    void saveSubcircuitAndReturn() {
	if (!isEditingSubcircuit())
	    return;
	CustomCompositeModel model = subcircuitEdit.model;
	for (CircuitElm elm : elmList) {
	    if (containsSubcircuit(elm, model.name)) {
		Window.alert(Locale.LS("A subcircuit cannot contain itself."));
		return;
	    }
	}
	CustomCompositeModel updated = getCircuitAsComposite(false);
	if (updated == null) {
	    needAnalyze();
	    return;
	}

	// Keep the existing pin order and positions so connected instances keep their wiring.
	Vector<ExtListEntry> pins = new Vector<ExtListEntry>();
	for (ExtListEntry oldPin : model.extList) {
	    for (ExtListEntry pin : updated.extList) {
		if (oldPin.name.equals(pin.name)) {
		    pins.add(new ExtListEntry(oldPin.name, pin.node, oldPin.pos, oldPin.side));
		    break;
		}
	    }
	}
	HashSet<Integer> pinNodes = new HashSet<Integer>();
	for (ExtListEntry pin : pins)
	    pinNodes.add(pin.node);
	if (pins.size() != model.extList.size() || pins.size() != updated.extList.size()
	        || pinNodes.size() != pins.size()) {
	    Window.alert(Locale.LS("Keep the existing external pin labels when updating this subcircuit."));
	    needAnalyze();
	    return;
	}

	updated.name = model.name;
	updated.flags = model.flags;
	updated.sizeX = model.sizeX;
	updated.sizeY = model.sizeY;
	updated.extList = pins;
	updated.modelCircuit = dumpCircuit();
	try {
	    updated.setSaved(true);
	} catch (RuntimeException e) {
	    Window.alert(Locale.LS("The subcircuit model could not be saved."));
	    needAnalyze();
	    return;
	}
	model.nodeList = updated.nodeList;
	model.elmDump = updated.elmDump;
	model.extList = updated.extList;
	model.modelCircuit = updated.modelCircuit;
	returnFromSubcircuit(true);
    }

    private boolean containsSubcircuit(CircuitElm elm, String name) {
	if (elm instanceof CustomCompositeElm && name.equals(((CustomCompositeElm) elm).modelName))
	    return true;
	if (elm instanceof CompositeElm)
	    for (CircuitElm child : ((CompositeElm) elm).compElmList)
		if (containsSubcircuit(child, name))
		    return true;
	return false;
    }

    void returnFromSubcircuit(boolean saved) {
	if (!isEditingSubcircuit())
	    return;
	SubcircuitEdit edit = subcircuitEdit;
	subcircuitEdit = edit.parent;
	if (saved && subcircuitEdit != null)
	    subcircuitEdit.childUpdated = true;
	boolean changed = saved || edit.childUpdated;
	undoStack = edit.undo;
	redoStack = edit.redo;
	loadUndoItem(edit.schematic, RC_SUBCIRCUIT_EDITOR, changed);
	setCircuitTitle(edit.title);
	savedFlag = edit.saved && !changed;
	unsavedChanges = edit.unsaved || changed;
	updateSubcircuitActions();
	enableUndoRedo();
	if (changed)
	    writeRecoveryToStorage();
    }

    private void updateSubcircuitActions() {
	subcircuitActions.setVisible(isEditingSubcircuit());
    }

    void doCreateSubcircuit()
    {
    	EditCompositeModelDialog dlg = new EditCompositeModelDialog();
    	if (!dlg.createModel())
    	    return;
    	dlg.createDialog();
    	dialogShowing = dlg;
    	dialogShowing.show();
    }
    
    void doExportAsLocalFile() {
    	String dump = dumpCircuit();
    	dialogShowing = new ExportAsLocalFileDialog(dump);
    	dialogShowing.show();
    }

    boolean importMobileCircuit(String text, String title, boolean subcircuitsOnly) {
        if (mobileProjects == null)
            return false;
        if (text != null && text.startsWith("\uFEFF"))
            text = text.substring(1);
        if (text == null || (subcircuitsOnly ? !text.matches("(?s).*?(?:^|[\\r\\n])\\s*\\.\\s+.*")
                                           : !NetlistImport.looksLikeCircuitJS(text))) {
            showInvalidCircuit();
            return true;
        }
        String circuit = text;
        if (subcircuitsOnly)
            applyMobileImport(circuit, title, true);
        else
            mobileProjects.beforeReplacing(() -> applyMobileImport(circuit, title, false));
        return true;
    }

    private void applyMobileImport(String text, String title, boolean subcircuitsOnly) {
        UndoItem previous = new UndoItem(dumpCircuit());
        int flags = subcircuitsOnly ? RC_SUBCIRCUITS | RC_RETAIN : 0;
        if (!readCircuit(text.getBytes(), flags | RC_STRICT)) {
            loadUndoItem(previous);
            showInvalidCircuit();
            return;
        }
        pushUndo(previous);
        if (!subcircuitsOnly) {
            titleLabel.setText(title);
            detachMobileProject();
        }
    }

    private void showInvalidCircuit() {
        Window.alert(Locale.LS("Could not import the circuit. Check that the file contains valid CircuitJS text."));
    }

    public void importCircuitFromText(String circuitText, boolean subcircuitsOnly) {
		int flags = subcircuitsOnly ? (CirSim.RC_SUBCIRCUITS | CirSim.RC_RETAIN) : 0;
		if (circuitText != null) {
			readCircuit(circuitText, flags);
			if (!subcircuitsOnly)
			    detachMobileProject();
			allowSave(false);
		}
    }

    String dumpOptions() {
	int f = (dotsCheckItem.getState()) ? 1 : 0;
	f |= (smallGridCheckItem.getState()) ? 2 : 0;
	f |= (voltsCheckItem.getState()) ? 0 : 4;
	f |= (powerCheckItem.getState()) ? 8 : 0;
	f |= (showValuesCheckItem.getState()) ? 0 : 16;
	// 32 = linear scale in afilter
	// 64 = legacy adaptive time step
	f |= averagePowerLossCheckItem.getState() ? SHOW_AVERAGE_POWER_LOSS : 0;
	String dump = "$ " + f + " " +
	    maxTimeStep + " " + getIterCount() + " " +
	    currentBar.getValue() + " " + CircuitElm.voltageRange + " " +
	    powerBar.getValue() + "\n";
	return dump;
    }
    
    String dumpCircuit() {
	int i;
	CustomLogicModel.clearDumpedFlags();
	CustomCompositeModel.clearDumpedFlags();
	DiodeModel.clearDumpedFlags();
	TransistorModel.clearDumpedFlags();
	
	String dump = dumpOptions() + canvasImages.dump();
		
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    String m = ce.dumpModel();
	    if (m != null && !m.isEmpty())
		dump += m + "\n";
	    dump += ce.dump();
	    if (ce.referenceNumber > 0)
		dump += " @ref=" + ce.referenceNumber;
	    dump += "\n";
	}
	for (i = 0; i != scopeCount; i++) {
	    String d = scopes[i].dump();
	    if (d != null)
		dump += d + "\n";
	}
	for (i = 0; i != adjustables.size(); i++) {
	    Adjustable adj = adjustables.get(i);
	    dump += "38 " + adj.dump() + "\n";
	}
	if (hintType != -1)
	    dump += "h " + hintType + " " + hintItem1 + " " +
		hintItem2 + "\n";
	return dump;
    }

    void getSetupList(final boolean openDefault) {

    	String url;
    	url = GWT.getModuleBaseURL()+"setuplist.txt"; // +"?v="+random.nextInt();
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					if (!hideMenu)
					    Window.alert(Locale.LS("Can't load circuit list!"));
					GWT.log("File Error Response", exception);
				}

				public void onResponseReceived(Request request, Response response) {
					// processing goes here
					if (response.getStatusCode()==Response.SC_OK) {
					String text = response.getText();
					processSetupList(text.getBytes(), openDefault);
					// end or processing
					}
					else { 
						Window.alert(Locale.LS("Can't load circuit list!"));
						GWT.log("Bad file server response:"+response.getStatusText() );
					}
				}
			});
		} catch (RequestException e) {
			GWT.log("failed file reading", e);
		}
    }
		
    void processSetupList(byte b[], final boolean openDefault) {
	int len = b.length;
    	MenuBar currentMenuBar;
    	MenuBar stack[] = new MenuBar[6];
    	int stackptr = 0;
    	currentMenuBar=new MenuBar(true);
    	currentMenuBar.setAutoOpen(true);
    	menuBar.addItem(Locale.LS("Circuits"), currentMenuBar);
    	stack[stackptr++] = currentMenuBar;
    	int p;
    	for (p = 0; p < len; ) {
    		int l;
    		for (l = 0; l != len-p; l++)
    			if (b[l+p] == '\n' || b[l+p] == '\r') {
    				l++;
    				break;
    			}
    		String line = new String(b, p, l-1);
    		if (line.isEmpty() || line.charAt(0) == '#')
    			;
    		else if (line.charAt(0) == '+') {
    		//	MenuBar n = new Menu(line.substring(1));
    			MenuBar n = new MenuBar(true);
    			n.setAutoOpen(true);
    			currentMenuBar.addItem(Locale.LS(line.substring(1)),n);
    			currentMenuBar = stack[stackptr++] = n;
    		} else if (line.charAt(0) == '-') {
    			currentMenuBar = stack[--stackptr-1];
    		} else {
    			int i = line.indexOf(' ');
    			if (i > 0) {
    				String title = Locale.LS(line.substring(i+1));
    				boolean first = false;
    				if (line.charAt(0) == '>')
    					first = true;
    				String file = line.substring(first ? 1 : 0, i);
    				currentMenuBar.addItem(new MenuItem(title,
    					new MyCommand("circuits", "setup "+file+" " + title)));
    				if (file.equals(startCircuit) && startLabel == null) {
    				    startLabel = title;
    				    titleLabel.setText(title);
    				}
    				if (first && startCircuit == null) {
    					startCircuit = file;
    					startLabel = title;
    					if (openDefault && stopMessage == null)
    						readSetupFile(startCircuit, startLabel);
    				}
    			}
    		}
    		p += l;
    	}
}

    void readCircuit(String text, int flags) {
	readCircuit(text.getBytes(), flags);
	if ((flags & RC_KEEP_TITLE) == 0)
	    titleLabel.setText(null);
    }

    void readCircuit(String text) {
	readCircuit(text.getBytes(), 0);
	titleLabel.setText(null);
    }

    void setCircuitTitle(String s) {
	if (s != null)
	    titleLabel.setText(s);
    }

    String getCircuitTitle() {
	return titleLabel.getText();
    }

    void detachMobileProject() {
	if (mobileProjects != null)
	    mobileProjects.detachProject();
    }

    void updateSavedProjectHistory(String file, String circuit) {
	for (UndoItem item : undoStack)
	    if (file.equals(item.file))
		item.dirty = !circuit.equals(item.dump);
	for (UndoItem item : redoStack)
	    if (file.equals(item.file))
		item.dirty = !circuit.equals(item.dump);
    }
    
	void readSetupFile(String str, String title) {
		System.out.println(str);
		// don't avoid caching here, it's unnecessary and makes offline PWA's not work
		String url=GWT.getModuleBaseURL()+"circuits/"+str; // +"?v="+random.nextInt(); 
        if (mobileProjects != null) {
            loadFileFromURL(url, title);
            return;
        }
		loadFileFromURL(url, null);
		if (title != null)
		    titleLabel.setText(title);
		unsavedChanges = false;
		ExportAsLocalFileDialog.setLastFileName(null);
	}
	
	void loadFileFromURL(String url, String title) {
	    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
	    
	    try {
		requestBuilder.sendRequest(null, new RequestCallback() {
		    public void onError(Request request, Throwable exception) {
			Window.alert(Locale.LS("Can't load circuit!"));
			GWT.log("File Error Response", exception);
		    }

		    public void onResponseReceived(Request request, Response response) {
			if (response.getStatusCode()==Response.SC_OK) {
			    String text = response.getText();
                            if (importMobileCircuit(text, title, false))
                                return;
			    readCircuit(text, RC_KEEP_TITLE);
			    detachMobileProject();
			    allowSave(false);
			    unsavedChanges = false;
			}
			else { 
			    Window.alert(Locale.LS("Can't load circuit!"));
			    GWT.log("Bad file server response:"+response.getStatusText() );
			}
		    }
		});
	    } catch (RequestException e) {
		GWT.log("failed file reading", e);
	    }

	}

    static final int RC_RETAIN = 1;
    static final int RC_NO_CENTER = 2;
    static final int RC_SUBCIRCUITS = 4;
    static final int RC_KEEP_TITLE = 8;
    static final int RC_SUBCIRCUIT_EDITOR = 16;
    private static final int RC_STRICT = 32;

    void assignComponentReferences() {
	HashSet<String> used = new HashSet<String>();
	for (CircuitElm elm : elmList) {
	    if (elm.referenceNumber > 0 && !used.add(elm.referencePrefix() + elm.referenceNumber))
		elm.referenceNumber = 0;
	}

	Map<String, Integer> next = new HashMap<String, Integer>();
	for (CircuitElm elm : elmList) {
	    if (elm.referenceNumber > 0)
		continue;
	    String prefix = elm.referencePrefix();
	    if (prefix == null)
		continue;
	    int number = next.containsKey(prefix) ? next.get(prefix) : 1;
	    while (!used.add(prefix + number))
		number++;
	    elm.referenceNumber = number;
	    next.put(prefix, number + 1);
	}
    }

    int nextInductorCouplingId() {
        int max = 0;
        for (CircuitElm element : elmList) {
            if (element instanceof InductorElm)
                max = Math.max(max, ((InductorElm) element).couplingId);
        }
        return max + 1;
    }

    boolean readCircuit(byte b[], int flags) {
        boolean strict = (flags & RC_STRICT) != 0;
	int i;
	staleSubcircuitStates.clear();
	if (isEditingSubcircuit() && (flags & (RC_RETAIN | RC_SUBCIRCUIT_EDITOR)) == 0) {
	    subcircuitEdit = null;
	    updateSubcircuitActions();
	}
        HashMap<Integer, Integer> importedCouplings = new HashMap<Integer, Integer>();
        int nextCouplingId = (flags & RC_RETAIN) != 0 ? nextInductorCouplingId() : 1;
	int len = b.length;
	if ((flags & RC_RETAIN) == 0) {
            canvasImages.clear();
            if (probeViewer != null)
                probeViewer.clearCircuit();
	    clearMouseElm();
	    for (i = 0; i != elmList.size(); i++) {
		CircuitElm ce = getElm(i);
		ce.delete();
	    }
	    t = timeStepAccum = 0;
	    elmList.removeAllElements();
	    hintType = -1;
	    maxTimeStep = 5e-6;
	    dotsCheckItem.setState(false);
	    smallGridCheckItem.setState(false);
	    powerCheckItem.setState(false);
	    averagePowerLossCheckItem.setState(false);
	    voltsCheckItem.setState(true);
	    syncSchematicDisplayControls();
	    setShowComponentValues(true);
	    setGrid();
	    speedBar.setValue(117); // 57
	    currentBar.setValue(50);
	    powerBar.setValue(50);
	    CircuitElm.voltageRange = 5;
	    scopeCount = 0;
	    lastIterTime = 0;
	}
	boolean subs = (flags & RC_SUBCIRCUITS) != 0;
	//cv.repaint();
	int p;
	for (p = 0; p < len; ) {
	    int l;
	    int linelen = len-p; // IES - changed to allow the last line to not end with a delim.
	    for (l = 0; l != len-p; l++)
		if (b[l+p] == '\n' || b[l+p] == '\r') {
		    linelen = l++;
		    if (l+p < b.length && b[l+p] == '\n')
			l++;
		    break;
		}
	    String line = new String(b, p, linelen);
            if (line.startsWith("I ")) {
                if (!subs && !canvasImages.restore(line.substring(2)) && strict)
                    return false;
                p += l;
                continue;
            }
	    StringTokenizer st = new StringTokenizer(line, " +\t\n\r\f");
	    while (st.hasMoreTokens()) {
		String type = st.nextToken();
		int tint = type.charAt(0);
		try {
                    if (subs && tint != '.')
                        break;
                    if (strict && type.length() != 1 && !type.matches("[0-9]+"))
                        return false;
		    if (tint == 'o') {
			Scope sc = new Scope(this);
			sc.position = scopeCount;
			sc.undump(st);
			scopes[scopeCount++] = sc;
			break;
		    }
		    if (tint == 'h') {
			readHint(st);
			break;
		    }
		    if (tint == '$') {
			readOptions(st, flags);
			break;
		    }
		    if (tint == '!') {
			CustomLogicModel.undumpModel(st);
			break;
		    }
		    if (tint == '%' || tint == '?' || tint == 'B') {
			// ignore afilter-specific stuff
			break;
		    }
		    // do not add new symbols here without testing export as link
		    
		    // if first character is a digit then parse the type as a number
		    if (tint >= '0' && tint <= '9')
			tint = new Integer(type).intValue();
		    
		    if (tint == 34) {
			DiodeModel.undumpModel(st);
			break;
		    }
		    if (tint == 32) {
			TransistorModel.undumpModel(st);
			break;
		    }
		    if (tint == 38) {
			Adjustable adj = new Adjustable(st, this);
			if (adj.elm != null)
			    adjustables.add(adj);
			break;
		    }
		    if (tint == '.') {
			CustomCompositeModel.undumpModel(st, staleSubcircuitStates);
			break;
		    }
		    int referenceNumber = 0;
		    int referenceStart = line.lastIndexOf(" @ref=");
		    if (referenceStart >= 0 && CircuitElm.referencePrefix(tint) != null) {
			String number = line.substring(referenceStart + 6).trim();
			if (number.matches("[1-9][0-9]{0,8}")) {
			    referenceNumber = Integer.parseInt(number);
			    st = new StringTokenizer(line.substring(0, referenceStart), " +\t\n\r\f");
			    st.nextToken(); // Skip the element type again.
			}
		    }
		    int x1 = new Integer(st.nextToken()).intValue();
		    int y1 = new Integer(st.nextToken()).intValue();
		    int x2 = new Integer(st.nextToken()).intValue();
		    int y2 = new Integer(st.nextToken()).intValue();
		    int f  = new Integer(st.nextToken()).intValue();
		    
		    CircuitElm newce = createCe(tint, x1, y1, x2, y2, f, st);
		    if (newce==null) {
                        if (strict)
                            return false;
			System.out.println("unrecognized dump type: " + type);
			break;
		    }
		    /*
		     * debug code to check if allocNodes() is called in constructor.  It gets called in
		     * setPoints() but that doesn't get called for subcircuits.
		    double vv[] = newce.volts;
		    int vc = newce.getPostCount() + newce.getInternalNodeCount();
		    if (vv.length != vc)
			console("allocnodes not called! " + tint);
		     */
		    newce.setPoints();
		    if ((flags & RC_RETAIN) == 0 && newce.referencePrefix() != null)
			newce.referenceNumber = referenceNumber;
		    if ((flags & RC_RETAIN) != 0 && newce instanceof InductorElm) {
                        InductorElm winding = (InductorElm) newce;
                        if (winding.couplingId > 0) {
                            Integer remapped = importedCouplings.get(winding.couplingId);
                            if (remapped == null) {
                                remapped = nextCouplingId++;
                                importedCouplings.put(winding.couplingId, remapped);
                            }
                            winding.couplingId = remapped;
                        }
                    }
		    elmList.addElement(newce);
		} catch (Exception ee) {
		    ee.printStackTrace();
		    console("exception while undumping " + ee);
                    if (strict)
                        return false;
		    break;
		}
		break;
	    }
	    p += l;
	    
	}
	InductorCoupling.clearSingletons(elmList);
	assignComponentReferences();
	setPowerBarEnable();
	enableItems();
	if ((flags & RC_RETAIN) == 0) {
	    // create sliders as needed
	    for (i = 0; i < adjustables.size(); i++) {
		if (!adjustables.get(i).createSlider(this))
		    adjustables.remove(i--);
	    }
	}
//	if (!retain)
	//    handleResize(); // for scopes
	needAnalyze();
	if ((flags & RC_NO_CENTER) == 0)
		centreCircuit();
	if ((flags & RC_SUBCIRCUITS) != 0)
	    updateModels();
	
	AudioInputElm.clearCache();  // to save memory
	DataInputElm.clearCache();  // to save memory
	staleSubcircuitStates.clear();
        return true;
    }

    // delete sliders for an element
    void deleteSliders(CircuitElm elm) {
	int i;
	if (adjustables == null)
	    return;
	for (i = adjustables.size()-1; i >= 0; i--) {
	    Adjustable adj = adjustables.get(i);
	    if (adj.elm == elm) {
		adj.deleteSlider(this);
		adjustables.remove(i);
	    }
	}
    }
    
    void readHint(StringTokenizer st) {
	hintType  = new Integer(st.nextToken()).intValue();
	hintItem1 = new Integer(st.nextToken()).intValue();
	hintItem2 = new Integer(st.nextToken()).intValue();
    }

    void readOptions(StringTokenizer st, int importFlags) {
	int flags = new Integer(st.nextToken()).intValue();
	
	if ((importFlags & RC_RETAIN) != 0) {
            // need to set small grid if pasted circuit uses it
	    if ((flags & 2) != 0)
		smallGridCheckItem.setState(true);
	    return;
	}
	
	dotsCheckItem.setState((flags & 1) != 0);
	smallGridCheckItem.setState((flags & 2) != 0);
	voltsCheckItem.setState((flags & 4) == 0);
	powerCheckItem.setState((flags & 8) == 8);
	averagePowerLossCheckItem.setState((flags & SHOW_AVERAGE_POWER_LOSS) != 0);
	if (averagePowerLossCheckItem.getState()) {
	    voltsCheckItem.setState(false);
	    powerCheckItem.setState(false);
	}
	syncSchematicDisplayControls();
	setShowComponentValues((flags & 16) == 0);
	maxTimeStep = timeStep = new Double (st.nextToken()).doubleValue();
	double sp = new Double(st.nextToken()).doubleValue();
	int sp2 = (int) (Math.log(10*sp)*24+61.5);
	//int sp2 = (int) (Math.log(sp)*24+1.5);
	speedBar.setValue(sp2);
	currentBar.setValue(new Integer(st.nextToken()).intValue());
	CircuitElm.voltageRange = new Double (st.nextToken()).doubleValue();

	try {
	    powerBar.setValue(new Integer(st.nextToken()).intValue());
	} catch (Exception e) {
	}
	setGrid();
    }
    
    int snapGrid(int x) {
	return (x+gridRound) & gridMask;
    }

	boolean doSwitch(int x, int y) {
		if (mouseElm == null || !(mouseElm instanceof SwitchElm))
			return false;
		SwitchElm se = (SwitchElm) mouseElm;
		if (!se.getSwitchRect().contains(x, y))
		    return false;
		toggleSwitch(se);
		return true;
	}

	private void toggleSwitch(SwitchElm se) {
		se.toggle();
		if (se.momentary)
		    heldSwitchElm = se;
		if (!(se instanceof LogicInputElm))
		    needAnalyze();
	}

    int locateElm(CircuitElm elm) {
	int i;
	for (i = 0; i != elmList.size(); i++)
	    if (elm == elmList.elementAt(i))
		return i;
	return -1;
    }
    
    public void mouseDragged(MouseMoveEvent e) {
    	// ignore right mouse button with no modifiers (needed on PC)
    	if (e.getNativeButton()==NativeEvent.BUTTON_RIGHT) {
    		if (!(e.isMetaKeyDown() ||
    				e.isShiftKeyDown() ||
    				e.isControlKeyDown() ||
    				e.isAltKeyDown()))
    			return;
    	}
    	
    	if (tempMouseMode==MODE_DRAG_SPLITTER) {
    		dragSplitter(e.getX(), e.getY());
    		return;
    	}
    	int gx = inverseTransformX(e.getX());
    	int gy = inverseTransformY(e.getY());
    	if (!circuitArea.contains(e.getX(), e.getY()))
    	    return;
    	boolean changed = false;
    	if (dragElm != null)
    	    dragElm.drag(gx, gy);
    	boolean success = true;
    	switch (tempMouseMode) {
    	case MODE_DRAG_ALL:
    		dragAll(e.getX(), e.getY());
    		break;
    	case MODE_DRAG_ROW:
    		dragRow(snapGrid(gx), snapGrid(gy));
    		changed = true;
    		break;
    	case MODE_DRAG_COLUMN:
		dragColumn(snapGrid(gx), snapGrid(gy));
    		changed = true;
    		break;
    	case MODE_DRAG_POST:
    		if (mouseElm != null) {
    		    dragPost(snapGrid(gx), snapGrid(gy), e.isShiftKeyDown());
    		    changed = true;
    		}
    		break;
    	case MODE_SELECT:
    		if (mouseElm == null)
    		    selectArea(gx, gy, e.isShiftKeyDown());
    		else if (!noEditCheckItem.getState()) {
    		    // wait short delay before dragging.  This is to fix problem where switches were accidentally getting
    		    // dragged when tapped on mobile devices
    		    if (System.currentTimeMillis()-mouseDownTime < 150)
    			return;
    		
    		    tempMouseMode = MODE_DRAG_SELECTED;
    		    changed = success = dragSelected(gx, gy);
    		}
    		break;
    	case MODE_DRAG_SELECTED:
    		changed = success = dragSelected(gx, gy);
    		break;

    	}
    	dragging = true;
    	if (success) {
    	    dragScreenX = e.getX();
    	    dragScreenY = e.getY();
    //	    console("setting dragGridx in mousedragged");
    	    dragGridX = inverseTransformX(dragScreenX);
    	    dragGridY = inverseTransformY(dragScreenY);
    	    if (!(tempMouseMode == MODE_DRAG_SELECTED && onlyGraphicsElmsSelected())) {
    		dragGridX = snapGrid(dragGridX);
    		dragGridY = snapGrid(dragGridY);
    	    }
   	}
    	if (changed)
    	    writeRecoveryToStorage();
    	repaint();
    }
    
    void dragSplitter(int x, int y) {
    	double h = (double) canvasHeight;
    	if (h<1)
    		h=1;
        int measurements = probeViewer.measurementsHeight();
        scopeHeightFraction = (y - measurements) / h;
    	if (scopeHeightFraction<0.1)
    		scopeHeightFraction=0.1;
    	if (scopeHeightFraction>0.9)
    		scopeHeightFraction=0.9;
    	setCircuitArea();
    	repaint();
    }

    void dragAll(int x, int y) {
    	int dx = x-dragScreenX;
    	int dy = y-dragScreenY;
    	if (dx == 0 && dy == 0)
    		return;
    	transform[4] += dx;
    	transform[5] += dy;
    	dragScreenX = x;
    	dragScreenY = y;
    }

    void dragRow(int x, int y) {
    	int dy = y-dragGridY;
    	if (dy == 0)
    		return;
    	int i;
    	for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		if (ce.y  == dragGridY)
    			ce.movePoint(0, 0, dy);
    		if (ce.y2 == dragGridY)
    			ce.movePoint(1, 0, dy);
    	}
    	removeZeroLengthElements();
    }

    void dragColumn(int x, int y) {
    	int dx = x-dragGridX;
    	if (dx == 0)
    		return;
    	int i;
    	for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		if (ce.x  == dragGridX)
    			ce.movePoint(0, dx, 0);
    		if (ce.x2 == dragGridX)
    			ce.movePoint(1, dx, 0);
    	}
    	removeZeroLengthElements();
    }

    boolean onlyGraphicsElmsSelected() {
	if (mouseElm!=null && !(mouseElm instanceof GraphicElm))
	    return false;
    	int i;
    	for (i = 0; i != elmList.size(); i++) {
    	    CircuitElm ce = getElm(i);
    	    if ( ce.isSelected() && !(ce instanceof GraphicElm) )
    		return false;
    	}
    	return true;
    }
    
    boolean dragSelected(int x, int y) {
    	boolean me = false;
    	int i;
    	if (mouseElm != null && !mouseElm.isSelected())
    	    mouseElm.setSelected(me = true);

    	if (! onlyGraphicsElmsSelected()) {
    //	    console("Snapping x and y");
    	    x = snapGrid(x);
    	    y = snapGrid(y);
    	}

    	int dx = x-dragGridX;
  //  	console("dx="+dx+"dragGridx="+dragGridX);
    	int dy = y-dragGridY;
    	if (dx == 0 && dy == 0) {
    	    // don't leave mouseElm selected if we selected it above
    	    if (me)
    		mouseElm.setSelected(false);
    	    return false;
    	}
    	boolean allowed = true;

    	// check if moves are allowed
    	for (i = 0; allowed && i != elmList.size(); i++) {
    	    CircuitElm ce = getElm(i);
    	    if (ce.isSelected() && !ce.allowMove(dx, dy))
    		allowed = false;
    	}

    	if (allowed) {
    	    for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		if (ce.isSelected())
    		    ce.move(dx, dy);
    	    }
    	    needAnalyze();
    	}

    	// don't leave mouseElm selected if we selected it above
    	if (me)
    		mouseElm.setSelected(false);

    	return allowed;
    }

    void dragPost(int x, int y, boolean all) {
    	if (draggingPost == -1) {
    		draggingPost =
    				(Graphics.distanceSq(mouseElm.x , mouseElm.y , x, y) >
    				Graphics.distanceSq(mouseElm.x2, mouseElm.y2, x, y)) ? 1 : 0;
    	}
    	int dx = x-dragGridX;
    	int dy = y-dragGridY;
    	if (dx == 0 && dy == 0)
    		return;
    	
    	if (all) {
    	    // go through all elms
    	    int i;
    	    for (i = 0; i != elmList.size(); i++) {
    		CircuitElm e = elmList.get(i);
    		
    		// which post do we move?
    		int p = 0;
    		if (e.x == dragGridX && e.y == dragGridY)
    		    p = 0;
    		else if (e.x2 == dragGridX && e.y2 == dragGridY)
    		    p = 1;
    		else
    		    continue;
    		e.movePoint(p, dx, dy);
    	    }
    	} else
    	    mouseElm.movePoint(draggingPost, dx, dy);
    	needAnalyze();
    }

    void doFlip() {
	if (menuElm instanceof InductorElm && ((InductorElm) menuElm).couplingId > 0)
	    pushUndo();
	menuElm.flipPosts();
    	needAnalyze();
    }
    
    void doSplit(CircuitElm ce) {
	int x = snapGrid(inverseTransformX(menuX));
	int y = snapGrid(inverseTransformY(menuY));
	if (ce == null || !(ce instanceof WireElm))
	    return;
	if (ce.x == ce.x2)
	    x = ce.x;
	else
	    y = ce.y;
	
	if (splitWireAt((WireElm) ce, x, y))
	    needAnalyze();
    }

    private void splitWiresAt(int x, int y) {
	// Start with the existing wires only. Splitting appends a new segment.
	for (int i = elmList.size() - 1; i >= 0; i--) {
	    CircuitElm elm = getElm(i);
	    if (elm instanceof WireElm)
		splitWireAt((WireElm) elm, x, y);
	}
    }

    private boolean splitWireAt(WireElm wire, int x, int y) {
	long dx = (long) wire.x2 - wire.x;
	long dy = (long) wire.y2 - wire.y;
	long offsetX = (long) x - wire.x;
	long offsetY = (long) y - wire.y;
	long projection = offsetX * dx + offsetY * dy;
	if (offsetX * dy != offsetY * dx || projection <= 0 || projection >= dx * dx + dy * dy)
	    return false;

	WireElm newWire = new WireElm(x, y, wire.x2, wire.y2, 0, null);
	newWire.setPoints();
	wire.movePoint(1, x - wire.x2, y - wire.y2);
	elmList.addElement(newWire);
	return true;
    }
    
    void selectArea(int x, int y, boolean add) {
    	int x1 = min(x, initDragGridX);
    	int x2 = max(x, initDragGridX);
    	int y1 = min(y, initDragGridY);
    	int y2 = max(y, initDragGridY);
    	selectedArea = new Rectangle(x1, y1, x2-x1, y2-y1);
    	int i;
    	for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		ce.selectRect(selectedArea, add);
    	}
	enableDisableMenuItems();
    }

    void enableDisableMenuItems() {
	boolean canFlipX = true;
	boolean canFlipY = true;
	boolean canFlipXY = true;
	int selCount = countSelected();
	for (CircuitElm elm : elmList)
	    if (elm.isSelected() || selCount == 0) {
		if (!elm.canFlipX())
		    canFlipX = false;
		if (!elm.canFlipY())
		    canFlipY = false;
		if (!elm.canFlipXY())
		    canFlipXY = false;
	    }
	cutItem.setEnabled(selCount > 0);
	copyItem.setEnabled(selCount > 0);
	flipXItem.setEnabled(canFlipX);
	flipYItem.setEnabled(canFlipY);
	flipXYItem.setEnabled(canFlipXY);
    }

    void setMouseElm(CircuitElm ce) {
    	if (ce!=mouseElm) {
    		if (mouseElm!=null)
    			mouseElm.setMouseElm(false);
    		if (ce!=null)
    			ce.setMouseElm(true);
    		mouseElm=ce;
    		int i;
    		for (i = 0; i < adjustables.size(); i++)
    		    adjustables.get(i).setMouseElm(ce);
    	}
    }

    void removeZeroLengthElements() {
    	int i;
    	boolean changed = false;
    	for (i = elmList.size()-1; i >= 0; i--) {
    		CircuitElm ce = getElm(i);
    		if (ce.x == ce.x2 && ce.y == ce.y2) {
    			elmList.removeElementAt(i);
    			ce.delete();
    			changed = true;
    		}
    	}
    	needAnalyze();
    }
    
    boolean mouseIsOverSplitter(int x, int y) {
    	boolean isOverSplitter;
        if (!hasScopePanel())
            return false;
        isOverSplitter = !scopeCollapsed && x >= 0 && x < circuitArea.width
                && y >= circuitArea.y - SCOPE_SPLITTER_HEIGHT && y < circuitArea.y;
    	if (isOverSplitter!=mouseWasOverSplitter){
    		if (isOverSplitter)
    			setCursorStyle("cursorSplitter");
    		else
    			setMouseMode(mouseMode);
    	}
    	mouseWasOverSplitter=isOverSplitter;
    	return isOverSplitter;
    }

    public void onMouseMove(MouseMoveEvent e) {
    	e.preventDefault();
        if (draggingPausedWaveformCursor) {
            moveWaveformCursor(e.getX());
            return;
        }
    	mouseCursorX=e.getX();
    	mouseCursorY=e.getY();
    	if (mouseDragging) {
    		mouseDragged(e);
    		return;
    	}
    	mouseSelect(e);
    	scopeMenuSelected = -1;
    }
    
    // convert screen coordinates to grid coordinates by inverting circuit transform
    int inverseTransformX(double x) {
	return (int) ((x-transform[4])/transform[0]);
    }

    int inverseTransformY(double y) {
	return (int) ((y-circuitArea.y-transform[5])/transform[3]);
    }
    
    // convert grid coordinates to screen coordinates
    int transformX(double x) {
	return (int) ((x*transform[0]) + transform[4]);
    }
    
    int transformY(double y) {
	return (int) ((y*transform[3]) + transform[5] + circuitArea.y);
    }
    
    

    // need to break this out into a separate routine to handle selection,
    // since we don't get mouse move events on mobile
    public void mouseSelect(MouseEvent<?> e) {
    	//	The following is in the original, but seems not to work/be needed for GWT
    	//    	if (e.getNativeButton()==NativeEvent.BUTTON_LEFT)
    	//	    return;
    	CircuitElm newMouseElm=null;
    	mouseCursorX=e.getX();
    	mouseCursorY=e.getY();
    	int sx = e.getX();
    	int sy = e.getY();
    	int gx = inverseTransformX(sx);
    	int gy = inverseTransformY(sy);
   // 	console("Settingd draggridx in mouseEvent");
    	dragGridX = snapGrid(gx);
    	dragGridY = snapGrid(gy);
    	dragScreenX = sx;
    	dragScreenY = sy;
    	draggingPost = -1;
    	int i;
    	//	CircuitElm origMouse = mouseElm;

    	mousePost = -1;
    	plotXElm = plotYElm = null;
    	
    	if (mouseIsOverSplitter(sx, sy)) {
    		setMouseElm(null);
    		return;
    	}
    	
    	if (circuitArea.contains(sx, sy)) {
    	    if (mouseElm!=null && ( mouseElm.getHandleGrabbedClose(gx, gy, POSTGRABSQ, MINPOSTGRABSIZE)>=0)) {
    		newMouseElm=mouseElm;
    	    } else {
    		int bestDist = 100000000;
    		for (i = 0; i != elmList.size(); i++) {
		    CircuitElm ce = getElm(i);
		    if (ce.boundingBox.contains(gx, gy)) {
			int dist = ce.getMouseDistance(gx, gy);
			if (dist >= 0 && dist < bestDist) {
			    bestDist = dist;
			    newMouseElm = ce;
			}
		    }
    		} // for
    	    }
    	}
    	scopeSelected = -1;
    	if (newMouseElm == null) {
    	    for (i = 0; i != scopeCount; i++) {
    		Scope s = scopes[i];
            if (!scopeCollapsed && isDockedScopeVisible(i) && s.rect.contains(sx, sy)) {
    		    newMouseElm=s.getElm();
    		    if (s.plotXY) {
    			plotXElm = s.getXElm();
    			plotYElm = s.getYElm();
    		    }
    		    scopeSelected = i;
    		}
    	    }
    		//	    // the mouse pointer was not in any of the bounding boxes, but we
    		//	    // might still be close to a post
                for (i = 0; circuitArea.contains(sx, sy) && i != elmList.size(); i++) {
    			CircuitElm ce = getElm(i);
    			if (mouseMode==MODE_DRAG_POST ) {
    				if (ce.getHandleGrabbedClose(gx, gy, POSTGRABSQ, 0)> 0)
    				{
    					newMouseElm = ce;
    					break;
    				}
    			}
    			int j;
    			int jn = ce.getPostCount();
    			for (j = 0; j != jn; j++) {
    				Point pt = ce.getPost(j);
    				//   int dist = Graphics.distanceSq(x, y, pt.x, pt.y);
    				if (Graphics.distanceSq(pt.x, pt.y, gx, gy) < 26) {
    					newMouseElm = ce;
    					mousePost = j;
    					break;
    				}
    			}
    		}
    	} else {
    		mousePost = -1;
    		// look for post close to the mouse pointer
    		for (i = 0; i != newMouseElm.getPostCount(); i++) {
    			Point pt = newMouseElm.getPost(i);
    			if (Graphics.distanceSq(pt.x, pt.y, gx, gy) < 26)
    				mousePost = i;
    		}
    	}
    	repaint();
    	setMouseElm(newMouseElm);
    }



    public void onContextMenu(ContextMenuEvent e) {
    	e.preventDefault();
    	if (!dialogIsShowing()) {
        	menuClientX = e.getNativeEvent().getClientX();
        	menuClientY = e.getNativeEvent().getClientY();
        	doPopupMenu();
    	}
    }
    
    void openDockedScopeMenu(Scope scope, int plot, int x, int y) {
        for (int index = 0; index < scopeCount; index++) {
            if (scopes[index] != scope)
                continue;
            menuScope = index;
            menuPlot = plot;
            boolean waveform = probeViewer.useWaveform();
            boolean arrange = probeViewer.canArrangeTraces();
            scopePopupMenu.doScopePopupChecks(false, waveform ? arrange : canStackScope(index),
                    waveform ? arrange : canCombineScope(index), waveform ? arrange : canUnstackScope(index),
                    scope, plot);
            if (waveform)
                scopePopupMenu.setMaxScaleChecked(probeViewer.savedTraceHoldsScale(scope, plot));
            showScopePopup(x, y);
            return;
        }
    }

    private void showScopePopup(int x, int y) {
        showComponentTools(scopePopupMenu.getMenuBar(), x, y);
    }

    PopupPanel showComponentTools(MenuBar menu, int x, int y) {
        PopupPanel popup = new PopupPanel(true);
        popup.addStyleName("component-tools-popup");
        popup.add(menu);
        contextPanel = popup;
        popup.setPopupPositionAndShow((width, height) -> popup.setPopupPosition(
                Math.max(0, Math.min(x, Window.getClientWidth() - width)),
                Math.max(0, Math.min(y, Window.getClientHeight() - height))));
        return popup;
    }

    void undockProbe(CircuitElm elm, int value) {
        pushUndo();
        Scope scope = new Scope(this);
        scope.setValue(value, elm);
        if (value == Scope.VAL_VOLTAGE)
            scope.showCurrent(false);
        ScopeElm floating = new ScopeElm(snapGrid(elm.x + 50), snapGrid(elm.y + 50));
        elmList.addElement(floating);
        floating.setElmScope(scope);
        needAnalyze();
        unsavedChanges = true;
        writeRecoveryToStorage();
        repaint();
    }

    @SuppressWarnings("deprecation")
    void doPopupMenu() {
	if (noEditCheckItem.getState() || dialogIsShowing())
	    return;
    	menuElm = mouseElm;
    	menuScope=-1;
    	menuPlot=-1;
    	int x, y;
    	if (scopeSelected!=-1) {
    	    	if (scopes[scopeSelected].canMenu()) {
                    openDockedScopeMenu(scopes[scopeSelected], scopes[scopeSelected].selectedPlot,
                            menuClientX, menuClientY);
    		}
    	} else if (mouseElm != null) {
    	    	if (! (mouseElm instanceof ScopeElm)) {
    	    	    elmScopeMenuItem.setEnabled(mouseElm.canViewInScope());
    	    	    elmFloatScopeMenuItem.setEnabled(mouseElm.canViewInScope());
    	    	    if ((scopeCount + countScopeElms()) <= 1) {
    	    		elmAddScopeMenuItem.setCommand(new MyCommand("elm", "addToScope0"));
    	    		elmAddScopeMenuItem.setSubMenu(null);
    	    	    	elmAddScopeMenuItem.setEnabled(mouseElm.canViewInScope() && (scopeCount + countScopeElms())> 0);
    	    	    }
    	    	    else {
    	    		composeSelectScopeMenu(selectScopeMenuBar);
    	    		elmAddScopeMenuItem.setCommand(null);
    	    		elmAddScopeMenuItem.setSubMenu(selectScopeMenuBar);
    	    	    	elmAddScopeMenuItem.setEnabled(mouseElm.canViewInScope() );
    	    	    }
    	    	    elmEditMenuItem .setEnabled(mouseElm.getEditInfo(0) != null);
                    elmCouplingMenuItem.setVisible(mouseElm instanceof InductorElm);
		    elmSwapMenuItem .setEnabled(mouseElm.getPostCount() == 2);
    	    	    elmSplitMenuItem.setEnabled(canSplit(mouseElm));
    	    	    elmSliderMenuItem.setEnabled(sliderItemEnabled(mouseElm));
		    boolean canFlipX = mouseElm.canFlipX();
		    boolean canFlipY = mouseElm.canFlipY();
		    boolean canFlipXY = mouseElm.canFlipXY();
		    for (CircuitElm elm : elmList)
			if (elm.isSelected()) {
			    if (!elm.canFlipX())
				canFlipX = false;
			    if (!elm.canFlipY())
				canFlipY = false;
			    if (!elm.canFlipXY())
				canFlipXY = false;
			}
    	    	    elmFlipXMenuItem.setEnabled(canFlipX);
    	    	    elmFlipYMenuItem.setEnabled(canFlipY);
    	    	    elmFlipXYMenuItem.setEnabled(canFlipXY);
                    showComponentTools(elmMenuBar, menuClientX, menuClientY);
    	    	} else {
    	    	    ScopeElm s = (ScopeElm) mouseElm;
    	    	    if (s.elmScope.canMenu()) {
    	    		menuPlot = s.elmScope.selectedPlot;
                        scopePopupMenu.doScopePopupChecks(true, false, false, false, s.elmScope, menuPlot);
                        showScopePopup(menuClientX, menuClientY);
    	    	    }
    	    	}
    	} else {
    		doMainMenuChecks();
    		contextPanel=new PopupPanel(true);
    		contextPanel.add(mainMenuBar);
    		x=Math.max(0, Math.min(menuClientX, canvasWidth-400));
    		y=Math.max(0, Math.min(menuClientY, canvasHeight-450));
    		contextPanel.setPopupPosition(x,y);
    		contextPanel.show();
    	}
    }

    boolean canSplit(CircuitElm ce) {
	if (!(ce instanceof WireElm))
	    return false;
	WireElm we = (WireElm) ce;
	if (we.x == we.x2 || we.y == we.y2)
	    return true;
	return false;
    }
    
    // check if the user can create sliders for this element
    boolean sliderItemEnabled(CircuitElm elm) {
	int i;
	
	// prevent confusion
	if (elm instanceof VarRailElm || elm instanceof PotElm)
	    return false;
	
	for (i = 0; ; i++) {
	    EditInfo ei = elm.getEditInfo(i);
	    if (ei == null)
		return false;
	    if (ei.canCreateAdjustable())
		return true;
	}
    }

    boolean longPress() {
        if (mouseElm != null && !(mouseElm instanceof ScopeElm))
            return false;
        menuClientX = cv.getAbsoluteLeft() + menuX;
        menuClientY = cv.getAbsoluteTop() + menuY;
        doPopupMenu();
        return true;
    }

    void setTouchSelectionEnabled(boolean enabled) {
        touchSelectionEnabled = enabled;
        clearSelection();
        repaint();
    }

    boolean canEditCanvasImage(int x, int y) {
        return mouseMode == MODE_SELECT && !noEditCheckItem.getState()
            && !dialogIsShowing() && circuitArea.contains(x, y);
    }

    boolean canvasComponentAt(int x, int y) {
        return floatingScopeAt(x, y) != null || nearestTouchElement(inverseTransformX(x), inverseTransformY(y)) != null;
    }

    void canvasImageChanged() {
        unsavedChanges = true;
        writeRecoveryToStorage();
        repaint();
    }

    boolean shouldPanTouch(int x, int y) {
        return mouseMode == MODE_SELECT && !touchSelectionEnabled && circuitArea.contains(x, y);
    }

    void panTouch(double dx, double dy) {
        transform[4] += dx;
        transform[5] += dy;
        repaint();
    }

    private static final int TOUCH_HIT_RADIUS = 12;
    private static final int TOUCH_SWITCH_RADIUS = 8;

    private double touchBoundsDistanceSq(Rectangle bounds, int gx, int gy) {
        double dx = Math.max(0, Math.max(bounds.x - gx, gx - (bounds.x + bounds.width)));
        double dy = Math.max(0, Math.max(bounds.y - gy, gy - (bounds.y + bounds.height)));
        return dx * dx + dy * dy;
    }

    private double touchElementDistanceSq(CircuitElm elm, int gx, int gy) {
        if (!(elm instanceof WireElm) && !(elm instanceof LineElm))
            return touchBoundsDistanceSq(elm.boundingBox, gx, gy);

        // Use a finite segment so a tap beyond a wire's end measures from the endpoint.
        double dx = elm.x2 - elm.x;
        double dy = elm.y2 - elm.y;
        double lengthSq = dx * dx + dy * dy;
        double along = lengthSq == 0 ? 0 : ((gx - elm.x) * dx + (gy - elm.y) * dy) / lengthSq;
        along = Math.max(0, Math.min(1, along));
        double offsetX = gx - elm.x - along * dx;
        double offsetY = gy - elm.y - along * dy;
        return offsetX * offsetX + offsetY * offsetY;
    }

    private CircuitElm nearestTouchElement(int gx, int gy) {
        double radius = TOUCH_HIT_RADIUS / transform[0];
        double bestDistance = radius * radius;
        CircuitElm best = null;
        for (int i = 0; i < elmList.size(); i++) {
            CircuitElm elm = getElm(i);
            if (elm instanceof ScopeElm)
                continue;
            double distance = touchElementDistanceSq(elm, gx, gy);
            if (distance < bestDistance) {
                best = elm;
                bestDistance = distance;
            }
        }
        return best;
    }

    private ScopeElm floatingScopeAt(int x, int y) {
        // Later elements are drawn on top when floating scopes overlap.
        for (int i = elmList.size() - 1; i >= 0; i--) {
            CircuitElm elm = getElm(i);
            if (elm instanceof ScopeElm) {
                ScopeElm scope = (ScopeElm) elm;
                scope.setScopeRect();
                if (scope.elmScope.rect.contains(x, y) || scope.touchResizeHandle(x, y) >= 0
                        || scope.cursorButtonBounds().contains(x, y))
                    return scope;
            }
        }
        return null;
    }

    boolean toggleFloatingCursor(int x, int y) {
        if (dialogIsShowing() || !circuitArea.contains(x, y) || mouseMode != MODE_SELECT)
            return false;
        ScopeElm scope = floatingScopeAt(x, y);
        if (scope == null || scope.touchResizeHandle(x, y) >= 0 || !scope.cursorButtonBounds().contains(x, y))
            return false;
        scope.cursorEnabled = !scope.cursorEnabled;
        setMouseElm(scope);
        repaint();
        return true;
    }

    boolean startFloatingCursor(int x, int y) {
        if (dialogIsShowing())
            return false;
        ScopeElm scope = floatingScopeAt(x, y);
        if (scope == null || !scope.cursorEnabled || scope.touchResizeHandle(x, y) >= 0
                || scope.cursorButtonBounds().contains(x, y) || !scope.elmScope.rect.contains(x, y))
            return false;
        mouseCursorX = x;
        mouseCursorY = y;
        if (scope.elmScope.cursorInSettingsWheel())
            return false;
        setMouseElm(scope);
        repaint();
        return true;
    }

    boolean holdSchematic(int x, int y) {
        if (noEditCheckItem.getState() || dialogIsShowing())
            return false;
        ScopeElm scope = floatingScopeAt(x, y);
        CircuitElm target = scope != null ? scope : nearestTouchElement(inverseTransformX(x), inverseTransformY(y));
        if (target == null || (scope != null && !scope.elmScope.canMenu()))
            return false;
        setMouseElm(target);
        scopeSelected = -1;
        if (scope != null)
            scope.selectScope(x, y);
        menuClientX = cv.getAbsoluteLeft() + x;
        menuClientY = cv.getAbsoluteTop() + y;
        doPopupMenu();
        return true;
    }

    boolean beginTouchComponentDrag(int x, int y) {
        if (noEditCheckItem.getState() || dialogIsShowing())
            return false;
        ScopeElm scope = floatingScopeAt(x, y);
        CircuitElm elm = scope != null ? scope : touchSelectedComponent;
        if (elm == null || (scope == null && (!elm.isSelected() || !elmList.contains(elm))))
            return false;

        int gx = inverseTransformX(x);
        int gy = inverseTransformY(y);
        double handleRadius = Math.min(10 / transform[0], Math.hypot(elm.x2 - elm.x, elm.y2 - elm.y) / 3);
        handleRadius = Math.max(Math.sqrt(POSTGRABSQ), handleRadius);
        int handle = scope != null ? scope.touchResizeHandle(x, y)
                : elm.getHandleGrabbedClose(gx, gy, (int) (handleRadius * handleRadius), MINPOSTGRABSIZE);
        if (scope == null && handle < 0 && nearestTouchElement(gx, gy) != elm)
            return false;

        pushUndo();
        if (scope != null)
            clearSelection();
        setMouseElm(elm);
        mouseDragging = dragging = true;
        tempMouseMode = handle >= 0 ? MODE_DRAG_POST : MODE_DRAG_SELECTED;
        draggingPost = handle;
        dragGridX = snapGrid(gx);
        dragGridY = snapGrid(gy);
        return true;
    }

    void tapSchematic(int x, int y) {
        if (dialogIsShowing())
            return;
        cv.setFocus(true);
        int gx = inverseTransformX(x);
        int gy = inverseTransformY(y);
        ScopeElm scopeHit = floatingScopeAt(x, y);
        if (scopeHit != null)
            setMouseElm(scopeHit);
        else if (mouseElm == null || mouseElm instanceof ScopeElm)
            setMouseElm(nearestTouchElement(gx, gy));
        if (simIsRunning() && mouseElm instanceof SwitchElm) {
            SwitchElm elm = (SwitchElm) mouseElm;
            double radius = TOUCH_SWITCH_RADIUS / transform[0];
            if (touchBoundsDistanceSq(elm.getSwitchRect(), gx, gy) <= radius * radius) {
                toggleSwitch(elm);
                if (releaseHeldSwitch()) {
                    needAnalyze();
                    pushUndo();
                }
            }
        }
        if (noEditCheckItem.getState())
            return;

        clearSelection();
        if (mouseElm instanceof ScopeElm) {
            Scope scope = ((ScopeElm) mouseElm).elmScope;
            if (scope.cursorInSettingsWheel())
                scope.properties();
        } else if (mouseElm != null) {
            touchSelectedComponent = mouseElm;
            touchSelectedComponent.setSelected(true);
            enableDisableMenuItems();
        }
        repaint();
    }

    private void updateComponentToolsButton() {
        if (componentToolsButton == null)
            return;
        if (touchSelectedComponent != null && (!touchSelectedComponent.isSelected()
                || !elmList.contains(touchSelectedComponent) || noEditCheckItem.getState()
                || mouseMode != MODE_SELECT || touchSelectionEnabled))
            touchSelectedComponent = null;
        boolean visible = touchSelectedComponent != null;
        if (componentToolsButton.isVisible() != visible)
            componentToolsButton.setVisible(visible);
    }

    private void openComponentTools() {
        updateComponentToolsButton();
        if (touchSelectedComponent == null || dialogIsShowing())
            return;
        setMouseElm(touchSelectedComponent);
        scopeSelected = -1;
        menuClientX = componentToolsButton.getAbsoluteLeft();
        menuClientY = componentToolsButton.getAbsoluteTop() + componentToolsButton.getOffsetHeight();
        doPopupMenu();
    }

    boolean scopeTouchOrigin(int x, int y) {
        return hasScopePanel() && !scopeCollapsed
                && x >= 0 && x < circuitArea.width && y >= SCOPE_CONTROL_HEIGHT
                && y < circuitArea.y - SCOPE_SPLITTER_HEIGHT;
    }

    static boolean isScopeLayoutSwipe(double dx, double dy) {
        return Math.abs(dy) >= 56 && Math.abs(dy) >= 2 * Math.abs(dx);
    }

    boolean arrangeWaveformAt(int x, int y, boolean separate) {
        return probeViewer.arrangeAt(x, y, separate);
    }

    boolean startWaveformCursor(int x, int y) {
        return probeViewer.startCursorTouch(x, y);
    }

    void moveWaveformCursor(int x) {
        probeViewer.moveCursorTouch(x);
    }

    void pinchZoom(boolean waveform, double factor, int x, int y) {
        if (waveform) {
            if (probeViewer.useWaveform())
                probeViewer.zoom(factor);
            return;
        }
        mouseCursorX = x;
        mouseCursorY = y;
        setCircuitScale(Math.max(.2, Math.min(2.5, transform[0] * factor)), false);
        repaint();
    }

//    public void mouseClicked(MouseEvent e) {
    public void onClick(ClickEvent e) {
        e.preventDefault();
        if (e.getNativeButton() == NativeEvent.BUTTON_LEFT && toggleFloatingCursor(e.getX(), e.getY()))
            return;
        if (e.getNativeButton() == NativeEvent.BUTTON_LEFT && !e.isShiftKeyDown()
                && !e.isControlKeyDown() && !e.isMetaKeyDown() && !e.isAltKeyDown()
                && Math.abs(e.getX() - menuX) < 4 && Math.abs(e.getY() - menuY) < 4
                && mouseMode == MODE_SELECT && !noEditCheckItem.getState() && !dialogIsShowing()
                && scopeSelected == -1 && !mouseWasOverSplitter
                && (simIsRunning() || circuitArea.contains(e.getX(), e.getY()))) {
            clearSelection();
            if (mouseElm != null && !(mouseElm instanceof ScopeElm)) {
                mouseElm.setSelected(true);
                enableDisableMenuItems();
            }
            repaint();
        }
        if (e.getNativeButton() == NativeEvent.BUTTON_MIDDLE)
            scrollValues(e.getNativeEvent().getClientX(), e.getNativeEvent().getClientY(), 0);
    }

    public void onDoubleClick(DoubleClickEvent e){
    	e.preventDefault();
 //   	if (!didSwitch && mouseElm != null)
    	if (mouseElm != null && !(mouseElm instanceof SwitchElm) && !noEditCheckItem.getState())
    		doEdit(mouseElm);
    }
    
//    public void mouseEntered(MouseEvent e) {
//    }
    
    public void onMouseOut(MouseOutEvent e) {
        draggingPausedWaveformCursor = false;
    	mouseCursorX=-1;
    }

    void clearMouseElm() {
    	scopeSelected = -1;
    	setMouseElm(null);
    	plotXElm = plotYElm = null;
    }
    
    int menuClientX, menuClientY;
    int menuX, menuY;
    
    public void onMouseDown(MouseDownEvent e) {
//    public void mousePressed(MouseEvent e) {
    	e.preventDefault();
    	
    	// make sure canvas has focus, not stop button or something else, so all shortcuts work
    	cv.setFocus(true);
    	
	stopElm = null; // if stopped, allow user to select other elements to fix circuit 
    	menuX = menuClientX = e.getX();
    	menuY = menuClientY = e.getY();
    	mouseDownTime = System.currentTimeMillis();
    	
    	// maybe someone did copy in another window?  should really do this when
    	// window receives focus
    	enablePaste();
    	
    	if (e.getNativeButton() != NativeEvent.BUTTON_LEFT && e.getNativeButton() != NativeEvent.BUTTON_MIDDLE)
    		return;

        if (!simIsRunning() && e.getNativeButton() == NativeEvent.BUTTON_LEFT
                && scopeTouchOrigin(e.getX(), e.getY()) && startWaveformCursor(e.getX(), e.getY())) {
            draggingPausedWaveformCursor = true;
            return;
        }
    	
    	// set mouseElm in case we are on mobile
    	mouseSelect(e);
    	
    	mouseDragging=true;
    	didSwitch = false;
	
    	if (mouseWasOverSplitter) {
    		tempMouseMode = MODE_DRAG_SPLITTER;
    		return;
    	}
	if (e.getNativeButton() == NativeEvent.BUTTON_LEFT) {
//	    // left mouse
	    tempMouseMode = mouseMode;
	    if (e.isAltKeyDown() && e.isMetaKeyDown())
		tempMouseMode = MODE_DRAG_COLUMN;
	    else if (e.isAltKeyDown() && e.isShiftKeyDown())
		tempMouseMode = MODE_DRAG_ROW;
	    else if (e.isShiftKeyDown())
		tempMouseMode = MODE_SELECT;
	    else if (e.isAltKeyDown())
		tempMouseMode = MODE_DRAG_ALL;
	    else if (e.isControlKeyDown() || e.isMetaKeyDown())
		tempMouseMode = MODE_DRAG_POST;
	} else
	    tempMouseMode = MODE_DRAG_ALL;
	

	if (noEditCheckItem.getState())
	    tempMouseMode = MODE_SELECT;
	
	if (!(dialogIsShowing()) && ((scopeSelected != -1 && scopes[scopeSelected].cursorInSettingsWheel()) ||
		( scopeSelected == -1 && mouseElm instanceof ScopeElm && ((ScopeElm)mouseElm).elmScope.cursorInSettingsWheel()))){
	    if (noEditCheckItem.getState())
		return;
	    Scope s;
	    if (scopeSelected != -1)
		s=scopes[scopeSelected];
	    else 
		s=((ScopeElm)mouseElm).elmScope;
	    s.properties();
	    clearSelection();
	    mouseDragging=false;
	    return;
	}

	int gx = inverseTransformX(e.getX());
	int gy = inverseTransformY(e.getY());
	if (tempMouseMode != MODE_ADD_ELM && (simIsRunning() || tempMouseMode != MODE_SELECT)
	        && circuitArea.contains(e.getX(), e.getY()) && doSwitch(gx, gy)) {
	    // do this BEFORE we change the mouse mode to MODE_DRAG_POST!  Or else logic inputs
	    // will add dots to the whole circuit when we click on them!
            didSwitch = true;
	    return;
	}
	
	// IES - Grab resize handles in select mode if they are far enough apart and you are on top of them
	if (tempMouseMode == MODE_SELECT && mouseElm!=null && !noEditCheckItem.getState() &&
		mouseElm.getHandleGrabbedClose(gx, gy, POSTGRABSQ, MINPOSTGRABSIZE) >=0 &&
		!anySelectedButMouse())
	    tempMouseMode = MODE_DRAG_POST;
	
	if (tempMouseMode != MODE_SELECT && tempMouseMode != MODE_DRAG_SELECTED)
	    clearSelection();

	pushUndo();
	initDragGridX = gx;
	initDragGridY = gy;
	dragging = true;
	if (tempMouseMode !=MODE_ADD_ELM)
		return;
//	
	int x0 = snapGrid(gx);
	int y0 = snapGrid(gy);
	if (!circuitArea.contains(e.getX(), e.getY()))
	    return;

	try {
	    dragElm = constructElement(mouseModeStr, x0, y0);
	} catch (Exception ex) {
	    debugger();
	}
    }

    static int lastSubcircuitMenuUpdate;
    
    // check/uncheck/enable/disable menu items as appropriate when menu bar clicked on, or when
    // right mouse menu accessed.  also displays shortcuts as a side effect
    void doMainMenuChecks() {
    	int c = mainMenuItems.size();
    	int i;
    	for (i=0; i<c ; i++) {
    	    	String s = mainMenuItemNames.get(i);
    		mainMenuItems.get(i).setState(s==mouseModeStr);

	        // Code to disable draw menu items when cct is not editable, but no used in this version as it
	        // puts up a dialog box instead (see menuPerformed).
    		//if (s.length() > 3 && s.substring(s.length()-3)=="Elm")
    		    //mainMenuItems.get(i).setEnabled(!noEditCheckItem.getState());
    	}
        updateScopeMenuItems();

    	// also update the subcircuit menu if necessary
    	if (lastSubcircuitMenuUpdate != CustomCompositeModel.sequenceNumber)
    	    composeSubcircuitMenu();
    }
    
 
    private void updateScopeMenuItems() {
        boolean waveform = probeViewer.useWaveform();
        boolean arrange = waveform && probeViewer.canArrangeTraces();
        boolean multipleDocked = scopeCount > 1;
        stackAllItem.setEnabled(waveform ? arrange : multipleDocked && scopes[scopeCount - 1].position > 0);
        unstackAllItem.setEnabled(waveform ? arrange : multipleDocked && scopes[scopeCount - 1].position != scopeCount - 1);
        combineAllItem.setEnabled(hasFloatingScopes() || (waveform ? arrange : multipleDocked));
        separateAllItem.setEnabled(scopeCount > 0);
    }

    public void onMouseUp(MouseUpEvent e) {
    	e.preventDefault();
        if (draggingPausedWaveformCursor) {
            draggingPausedWaveformCursor = false;
            return;
        }
    	mouseDragging=false;
    	
    	// click to clear selection
    	if (tempMouseMode == MODE_SELECT && selectedArea == null)
    	    clearSelection();

    	// cmd-click = split wire
    	if (tempMouseMode == MODE_DRAG_POST && draggingPost == -1)
    	    doSplit(mouseElm);
    	
    	tempMouseMode = mouseMode;
    	selectedArea = null;
    	dragging = false;
        boolean circuitChanged = releaseHeldSwitch();
    	if (dragElm != null) {
    		// if the element is zero size then don't create it
    		// IES - and disable any previous selection
    	    	if (dragElm.creationFailed()) {
    			dragElm.delete();
    			if (mouseMode == MODE_SELECT || mouseMode == MODE_DRAG_SELECTED)
    				clearSelection();
    		}
    		else {
			if (dragElm instanceof WireElm) {
			    splitWiresAt(dragElm.x, dragElm.y);
			    splitWiresAt(dragElm.x2, dragElm.y2);
			}
    			elmList.addElement(dragElm);
			if (dragElm.referencePrefix() != null)
			    assignComponentReferences();
    			dragElm.draggingDone();
    			circuitChanged = true;
    			writeRecoveryToStorage();
    			unsavedChanges = true;
                        if (!multiplePlacement && !(dragElm instanceof WireElm))
                            resetToSelectMode();
    		}
    		dragElm = null;
    	}
    	if (circuitChanged) {
    	    needAnalyze();
    	    pushUndo();
    	}
    	if (dragElm != null)
    		dragElm.delete();
    	dragElm = null;
    	repaint();
    }

    private boolean releaseHeldSwitch() {
        if (heldSwitchElm == null)
            return false;
        heldSwitchElm.mouseUp();
        heldSwitchElm = null;
        return true;
    }
    
    public void onMouseWheel(MouseWheelEvent e) {
    	e.preventDefault();
    	
    	// once we start zooming, don't allow other uses of mouse wheel for a while
    	// so we don't accidentally edit a resistor value while zooming
    	boolean zoomOnly = System.currentTimeMillis() < zoomTime+1000;
    	
    	if (noEditCheckItem.getState() || !mouseWheelEditCheckItem.getState())
    	    zoomOnly = true;
    	
    	if (!zoomOnly)
    	    scrollValues(e.getNativeEvent().getClientX(), e.getNativeEvent().getClientY(), e.getDeltaY());
    	
    	if (mouseElm instanceof MouseWheelHandler && !zoomOnly)
    		((MouseWheelHandler) mouseElm).onMouseWheel(e);
    	else if (scopeSelected != -1 && !zoomOnly)
    	    scopes[scopeSelected].onMouseWheel(e);
    	else if (!dialogIsShowing()) {
    	    mouseCursorX=e.getX();
    	    mouseCursorY=e.getY();
    	    zoomCircuit(-e.getDeltaY()*wheelSensitivity, false);
    	    zoomTime = System.currentTimeMillis();
   	}
    	repaint();
    }

    void zoomCircuit(double dy) { zoomCircuit(dy, false); }

    void zoomCircuit(double dy, boolean menu) {
	double newScale;
    	double oldScale = transform[0];
    	double val = dy*.01;
    	newScale = Math.max(oldScale+val, .2);
    	newScale = Math.min(newScale, 2.5);
    	setCircuitScale(newScale, menu);
    }
    
    void setCircuitScale(double newScale, boolean menu) {
	int constX = !menu ? mouseCursorX : circuitArea.width/2;
	int constY = !menu ? mouseCursorY : circuitArea.y + circuitArea.height/2;
	int cx = inverseTransformX(constX);
	int cy = inverseTransformY(constY);
	transform[0] = transform[3] = newScale;

	// adjust translation to keep center of screen constant
	// inverse transform = (x-t4)/t0
	transform[4] = constX - cx*newScale;
	transform[5] = constY - circuitArea.y - cy*newScale;
    }
    
    void setPowerBarEnable() {
	if (isPowerColorEnabled()) {
    	    powerLabel.setStyleName("disabled", false);
    	    powerBar.enable();
    	} else {
    	    powerLabel.setStyleName("disabled", true);
    	    powerBar.disable();
    	}
    }

    void scrollValues(int x, int y, int deltay) {
    	if (mouseElm!=null && !dialogIsShowing() && scopeSelected == -1)
    		if (mouseElm instanceof ResistorElm || mouseElm instanceof CapacitorElm ||  mouseElm instanceof InductorElm) {
    			scrollValuePopup = new ScrollValuePopup(x, y, deltay, mouseElm, this);
    		}
    }
    
    void enableItems() {
    }
    
    void setGrid() {
	gridSize = (smallGridCheckItem.getState()) ? 8 : 16;
	gridMask = ~(gridSize-1);
	gridRound = gridSize/2-1;
    }

    void setToolbar() {
	layoutPanel.setWidgetHidden(toolbar, !toolbarCheckItem.getState());
	setCanvasSize();
    }

    void pushUndo() {
        pushUndo(new UndoItem(dumpCircuit()));
    }

    private void pushUndo(UndoItem item) {
        redoStack.removeAllElements();
        if (undoStack.size() > 0 && item.matches(undoStack.lastElement()))
            return;
        undoStack.add(item);
        enableUndoRedo();
        savedFlag = false;
    }

    void doUndo() {
    	if (undoStack.size() == 0)
    		return;
    	redoStack.add(new UndoItem(dumpCircuit()));
    	UndoItem ui = undoStack.remove(undoStack.size()-1);
    	loadUndoItem(ui);
    	enableUndoRedo();
    }

    void doRedo() {
    	if (redoStack.size() == 0)
    		return;
    	undoStack.add(new UndoItem(dumpCircuit()));
    	UndoItem ui = redoStack.remove(redoStack.size()-1);
    	loadUndoItem(ui);
    	enableUndoRedo();
    }

    void loadUndoItem(UndoItem ui) {
	loadUndoItem(ui, isEditingSubcircuit() ? RC_SUBCIRCUIT_EDITOR : 0, false);
    }

    private void loadUndoItem(UndoItem ui, int flags, boolean changed) {
	readCircuit(ui.dump, RC_NO_CENTER | flags);
	transform[0] = transform[3] = ui.scale;
	transform[4] = ui.transform4;
	transform[5] = ui.transform5;
	if (mobileProjects != null && !isEditingSubcircuit())
	    mobileProjects.restoreProject(ui.file, ui.title, ui.dirty || changed);
	else if (isEditingSubcircuit())
	    setCircuitTitle(subcircuitEdit.model.name);
    }
    
    void doRecover() {
        if (mobileProjects != null) {
            mobileProjects.beforeReplacing(this::restoreRecoveryCircuit);
            return;
        }
        restoreRecoveryCircuit();
    }

    private void restoreRecoveryCircuit() {
	pushUndo();
	readCircuit(recovery);
	detachMobileProject();
	allowSave(false);
	recoverItem.setEnabled(false);
    }
    
    void enableUndoRedo() {
    	redoItem.setEnabled(redoStack.size() > 0);
    	undoItem.setEnabled(undoStack.size() > 0);
    }

    void setMouseMode(int mode)
    {
    	mouseMode = mode;
    	if ( mode == MODE_ADD_ELM ) {
    		setCursorStyle("cursorCross");
    	} else {
    		setCursorStyle("cursorPointer");
    	}
    }
    
    void setCursorStyle(String s) {
    	if (lastCursorStyle!=null)
    		cv.removeStyleName(lastCursorStyle);
    	cv.addStyleName(s);
    	lastCursorStyle=s;
    }
    


    void setMenuSelection() {
    	if (menuElm != null) {
    		if (menuElm.selected)
    			return;
    		clearSelection();
    		menuElm.setSelected(true);
    	}
    }

    int countSelected() {
	int count = 0;
	for (CircuitElm ce: elmList)
	    if (ce.isSelected())
		count++;
	return count;
    }

    class FlipInfo { public int cx, cy, count; }

    FlipInfo prepareFlip() {
    	int i;
    	pushUndo();
    	setMenuSelection();
    	int minx = 30000, maxx = -30000;
    	int miny = 30000, maxy = -30000;
	int count = countSelected();
    	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    if (ce.isSelected() || count == 0) {
		minx = min(ce.x, min(ce.x2, minx));
		maxx = max(ce.x, max(ce.x2, maxx));
		miny = min(ce.y, min(ce.y2, miny));
		maxy = max(ce.y, max(ce.y2, maxy));
	    }
    	}
	FlipInfo fi = new FlipInfo();
	fi.cx = (minx+maxx)/2;
	fi.cy = (miny+maxy)/2;
	fi.count = count;	
	return fi;
    }

    void flipX() {
	FlipInfo fi = prepareFlip();
	int center2 = fi.cx*2;
	for (CircuitElm ce : elmList) {
	    if (ce.isSelected() || fi.count == 0)
		ce.flipX(center2, fi.count);
    	}
	needAnalyze();
    }

    void flipY() {
	FlipInfo fi = prepareFlip();
	int center2 = fi.cy*2;
	for (CircuitElm ce : elmList) {
	    if (ce.isSelected() || fi.count == 0)
		ce.flipY(center2, fi.count);
    	}
	needAnalyze();
    }

    void flipXY() {
	FlipInfo fi = prepareFlip();
	int xmy = snapGrid(fi.cx-fi.cy);
	console("xmy " + xmy + " grid " + gridSize + " " + fi.cx + " " + fi.cy);
	for (CircuitElm ce : elmList) {
	    if (ce.isSelected() || fi.count == 0)
		ce.flipXY(xmy, fi.count);
    	}
	needAnalyze();
    }

    void doCut() {
    	int i;
    	pushUndo();
    	setMenuSelection();
    	clipboard = "";
    	for (i = elmList.size()-1; i >= 0; i--) {
    		CircuitElm ce = getElm(i);
    		// ScopeElms don't cut-paste well because their reference to a parent
    		// elm by number get's messed up in the dump. For now we will just ignore them
    		// until I can be bothered to come up with something better
    		if (willDelete(ce) && !(ce instanceof ScopeElm) ) {
    			clipboard += ce.dump() + "\n";
    		}
    	}
    	writeClipboardToStorage();
    	doDelete(true);
    	enablePaste();
    }

    void writeClipboardToStorage() {
    	Storage stor = Storage.getLocalStorageIfSupported();
    	if (stor == null)
    		return;
    	stor.setItem("circuitClipboard", clipboard);
    }
    
    void readClipboardFromStorage() {
    	Storage stor = Storage.getLocalStorageIfSupported();
    	if (stor == null)
    		return;
    	clipboard = stor.getItem("circuitClipboard");
    }

    void writeRecoveryToStorage() {
	if (isEditingSubcircuit())
	    return;
        if (mobileProjects != null) {
            circuitChanged();
            return;
        }
        writeRecoveryToStorage(null);
    }

    void writeRecoveryToStorage(String circuit) {
    	Storage stor = Storage.getLocalStorageIfSupported();
    	if (stor == null)
    		return;
        try {
            stor.setItem("circuitRecovery", circuit == null ? dumpCircuit() : circuit);
        } catch (RuntimeException error) {
            // Embedded images can exceed the browser's recovery storage quota.
            if (!canvasImages.hasImages() || mobileProjects != null)
                throw error;
        }
    }

    void readRecovery() {
	Storage stor = Storage.getLocalStorageIfSupported();
	if (stor == null)
		return;
	recovery = stor.getItem("circuitRecovery");
    }


    void deleteUnusedScopeElms() {
	// Remove any scopeElms for elements that no longer exist
	for (int i = elmList.size()-1; i >= 0; i--) {
    		CircuitElm ce = getElm(i);
    		if (ce instanceof ScopeElm && (((ScopeElm) ce).elmScope.needToRemove() )) {
    			ce.delete();
    			elmList.removeElementAt(i);
    			
    			// need to rebuild scopeElmArr
    			needAnalyze();
    		}
    	}
	
    }
    
    void doDelete(boolean pushUndoFlag) {
    	int i;
    	if (pushUndoFlag)
    	    pushUndo();
    	boolean hasDeleted = false;

    	for (i = elmList.size()-1; i >= 0; i--) {
    		CircuitElm ce = getElm(i);
    		if (willDelete(ce)) {
    		    	if (ce.isMouseElm())
    		    	    setMouseElm(null);
    			ce.delete();
    			elmList.removeElementAt(i);
    			hasDeleted = true;
    		}
    	}
    	if ( hasDeleted ) {
            InductorCoupling.clearSingletons(elmList);
    	    deleteUnusedScopeElms();
    	    needAnalyze();
    	    writeRecoveryToStorage();
    	}    
    }
    
    boolean willDelete( CircuitElm ce ) {
	// Is this element in the list to be deleted.
	// This changes the logic from the previous version which would initially only
	// delete selected elements (which could include the mouseElm) and then delete the 
	// mouseElm if there were no selected elements. Not really sure this added anything useful
	// to the user experience.
	//
	// BTW, the old logic could also leave mouseElm pointing to a deleted element.
	return ce.isSelected() || ce.isMouseElm();
    }
    
    String copyOfSelectedElms() {
	String r = dumpOptions();
	CustomLogicModel.clearDumpedFlags();
	CustomCompositeModel.clearDumpedFlags();
	DiodeModel.clearDumpedFlags();
	TransistorModel.clearDumpedFlags();
	for (int i = elmList.size()-1; i >= 0; i--) {
	    CircuitElm ce = getElm(i);
	    String m = ce.dumpModel();
	    if (m != null && !m.isEmpty())
		r += m + "\n";
	    // See notes on do cut why we don't copy ScopeElms.
	    if (ce.isSelected() && !(ce instanceof ScopeElm))
		r += ce.dump() + "\n";
	}
	return r;
    }
    
    void doCopy() {
    	// clear selection when we're done if we're copying a single element using the context menu
    	boolean clearSel = (menuElm != null && !menuElm.selected);
    	
    	setMenuSelection();
    	clipboard=copyOfSelectedElms();
    	
    	if (clearSel)
    	    clearSelection();
    	
    	writeClipboardToStorage();
    	enablePaste();
    }

    void enablePaste() {
    	if (clipboard == null || clipboard.length() == 0)
    		readClipboardFromStorage();
    	pasteItem.setEnabled(clipboard != null && clipboard.length() > 0);
    }

    void doDuplicate() {
    	String s;
    	setMenuSelection();
    	s=copyOfSelectedElms();
    	doPaste(s);
    }
    
    void doPaste(String dump) {
    	pushUndo();
    	clearSelection();
    	int i;
    	Rectangle oldbb = null;
    	
    	// get old bounding box
    	for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		Rectangle bb = ce.getBoundingBox();
    		if (oldbb != null)
    			oldbb = oldbb.union(bb);
    		else
    			oldbb = bb;
    	}
    	
    	// add new items
    	int oldsz = elmList.size();
    	int flags = RC_RETAIN;
    	
    	// don't recenter circuit if we're going to paste in place because that will change the transform
//	if (mouseCursorX > 0 && circuitArea.contains(mouseCursorX, mouseCursorY))
    	
    	// in fact, don't ever recenter circuit, unless old circuit was empty
    	if (oldsz > 0)
    	    flags |= RC_NO_CENTER;
	
    	if (dump != null)
    	    readCircuit(dump, flags);
    	else {
    	    readClipboardFromStorage();
    	    readCircuit(clipboard, flags);
    	}

    	// select new items and get their bounding box
    	Rectangle newbb = null;
    	for (i = oldsz; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		ce.setSelected(true);
    		Rectangle bb = ce.getBoundingBox();
    		if (newbb != null)
    			newbb = newbb.union(bb);
    		else
    			newbb = bb;
    	}
    	
    	if (oldbb != null && newbb != null /*&& oldbb.intersects(newbb)*/) {
    		// find a place on the edge for new items
    		int dx = 0, dy = 0;
    		int spacew = circuitArea.width - oldbb.width - newbb.width;
    		int spaceh = circuitArea.height - oldbb.height - newbb.height;
    		
    		if (!oldbb.intersects(newbb)) {
    		    // old coordinates may be really far away so move them to same origin as current circuit
    		    dx = snapGrid(oldbb.x - newbb.x);
    		    dy = snapGrid(oldbb.y - newbb.y);
    		}
    		
    		if (spacew > spaceh) {
    			dx = snapGrid(oldbb.x + oldbb.width  - newbb.x + gridSize);
    		} else {
    			dy = snapGrid(oldbb.y + oldbb.height - newbb.y + gridSize);
    		}
    		
    		// move new items near the mouse if possible
    		if (mouseCursorX > 0 && circuitArea.contains(mouseCursorX, mouseCursorY)) {
    	    	    int gx = inverseTransformX(mouseCursorX);
    	    	    int gy = inverseTransformY(mouseCursorY);
    	    	    int mdx = snapGrid(gx-(newbb.x+newbb.width/2));
    	    	    int mdy = snapGrid(gy-(newbb.y+newbb.height/2));
    	    	    for (i = oldsz; i != elmList.size(); i++) {
    	    		if (!getElm(i).allowMove(mdx, mdy))
    	    		    break;
    	    	    }
    	    	    if (i == elmList.size()) {
    	    		dx = mdx;
    	    		dy = mdy;
    	    	    }
    		}
    		
    		// move the new items
    		for (i = oldsz; i != elmList.size(); i++) {
    			CircuitElm ce = getElm(i);
    			ce.move(dx, dy);
    		}
    		
    		// center circuit
    	//	handleResize();
    	}
    	needAnalyze();
    	writeRecoveryToStorage();
    }

    void clearSelection() {
	touchSelectedComponent = null;
	updateComponentToolsButton();
	int i;
	for (i = 0; i != elmList.size(); i++) {
	    CircuitElm ce = getElm(i);
	    ce.setSelected(false);
	}
	enableDisableMenuItems();
    }
    
    void doSelectAll() {
    	int i;
    	for (i = 0; i != elmList.size(); i++) {
    		CircuitElm ce = getElm(i);
    		ce.setSelected(true);
    	}
	enableDisableMenuItems();
    }
    
    boolean anySelectedButMouse() {
    	for (int i=0; i != elmList.size(); i++)
    		if (getElm(i)!= mouseElm && getElm(i).selected)
    			return true;
    	return false;
    }

//    public void keyPressed(KeyEvent e) {}
//    public void keyReleased(KeyEvent e) {}
    
    boolean dialogIsShowing() {
    	if (editDialog!=null && editDialog.isShowing())
    		return true;
        if (customLogicEditDialog!=null && customLogicEditDialog.isShowing())
                return true;
        if (diodeModelEditDialog!=null && diodeModelEditDialog.isShowing())
                return true;
       	if (dialogShowing != null && dialogShowing.isShowing())
       		return true;
    	if (contextPanel!=null && contextPanel.isShowing())
    		return true;
    	if (scrollValuePopup != null && scrollValuePopup.isShowing())
    		return true;
    	if (aboutBox !=null && aboutBox.isShowing())
    		return true;
    	return false;
    }
    
    public void onPreviewNativeEvent(NativePreviewEvent e) {
    	int cc=e.getNativeEvent().getCharCode();
    	int t=e.getTypeInt();
    	int code=e.getNativeEvent().getKeyCode();
        if (multiplePlacement && t == Event.ONCLICK
                && com.google.gwt.dom.client.Element.is(e.getNativeEvent().getEventTarget())
                && !cv.getElement().isOrHasChild(
                        com.google.gwt.dom.client.Element.as(e.getNativeEvent().getEventTarget()))) {
            Scheduler.get().scheduleDeferred(() -> {
                if (multiplePlacement)
                    resetToSelectMode();
            });
        }
        if (probeViewer != null && probeViewer.isOpen()) {
            if (t == Event.ONKEYDOWN && code == KEY_ESCAPE) {
                probeViewer.close();
                scopeTraceButton.setFocus(true);
                e.cancel();
            }
            return;
        }
        if (scopeControls != null && com.google.gwt.dom.client.Element.is(e.getNativeEvent().getEventTarget())
                && scopeControls.getElement().isOrHasChild(
                        com.google.gwt.dom.client.Element.as(e.getNativeEvent().getEventTarget())))
            return;
    	if (dialogIsShowing()) {
    		if (scrollValuePopup != null && scrollValuePopup.isShowing() &&
    				(t & Event.ONKEYDOWN)!=0) {
    			if (code==KEY_ESCAPE || code==KEY_SPACE)
    				scrollValuePopup.close(false);
    			if (code==KEY_ENTER)
    				scrollValuePopup.close(true);
    		}
    		
    		// process escape/enter for dialogs
    		// multiple edit dialogs could be displayed at once, pick the one in front
    		Dialog dlg = editDialog;
    		if (diodeModelEditDialog != null)
    		    dlg = diodeModelEditDialog;
    		if (customLogicEditDialog != null)
    		    dlg = customLogicEditDialog;
    		if (dialogShowing != null)
    		    dlg = dialogShowing;
    		if (dlg!=null && dlg.isShowing() &&
    				(t & Event.ONKEYDOWN)!=0) {
    			if (code==KEY_ESCAPE)
    			    dlg.closeDialog();
    			if (code==KEY_ENTER)
    			    dlg.enterPressed();
    		}
    		return;
    	}
    	
    	if ((t&Event.ONKEYPRESS)!=0) {
		if (cc=='-') {
    		    menuPerformed("key", "zoomout");
    		    e.cancel();
    		}
    		if (cc=='+' || cc == '=') {
    		    menuPerformed("key", "zoomin");
    		    e.cancel();
    		}
		if (cc=='0') {
    		    menuPerformed("key", "zoom100");
    		    e.cancel();
		}
		if (cc=='/' && shortcuts['/'] == null) {
		    menuPerformed("key", "search");
		    e.cancel();
		}
    	}
    	
    	// all other shortcuts are ignored when editing disabled
    	if (noEditCheckItem.getState())
    	    return;

    	if ((t & Event.ONKEYDOWN)!=0) {
    		if (code==KEY_BACKSPACE || code==KEY_DELETE) {
    		    if (scopeSelected != -1) {
    			// Treat DELETE key with scope selected as "remove scope", not delete
    			scopes[scopeSelected].setElm(null);
    			scopeSelected = -1;
    		    } else {
    		    	menuElm = null;
    		    	pushUndo();
    			doDelete(true);
    			e.cancel();
    		    }
    		}
    		if (code==KEY_ESCAPE){
                        resetToSelectMode();
    			e.cancel();
    		}

    		if (e.getNativeEvent().getCtrlKey() || e.getNativeEvent().getMetaKey()) {
    			if (code==KEY_C) {
    				menuPerformed("key", "copy");
    				e.cancel();
    			}
    			if (code==KEY_X) {
    				menuPerformed("key", "cut");
    				e.cancel();
    			}
    			if (code==KEY_V) {
    				menuPerformed("key", "paste");
    				e.cancel();
    			}
    			if (code==KEY_Z) {
    				menuPerformed("key", "undo");
    				e.cancel();
    			}
    			if (code==KEY_Y) {
    				menuPerformed("key", "redo");
    				e.cancel();
    			}
    			if (code==KEY_D) {
    			    	menuPerformed("key", "duplicate");
    			    	e.cancel();
    			}
    			if (code==KEY_A) {
    				menuPerformed("key", "selectAll");
    				e.cancel();
    			}
    			if (code==KEY_P) {
				menuPerformed("key", "print");
				e.cancel();
			}
    			if (code==KEY_N && isElectron()) {
				menuPerformed("key", "newwindow");
				e.cancel();
			}
                if (code==KEY_S) {
                        String cmd = "exportaslocalfile";
                        if (mobileProjects != null)
                            cmd = "save";
                        else if (isElectron())
                            cmd = saveFileItem.isEnabled() ? "save" : "saveas";
                menuPerformed("key", cmd);
                e.cancel();
            }
    			if (code==KEY_O) {
				menuPerformed("key", "importfromlocalfile");
				e.cancel();
			}    			
    		}
    	}
    	if ((t&Event.ONKEYPRESS)!=0) {
    		if (cc>32 && cc<127){
    			String c=shortcuts[cc];
    			e.cancel();
    			if (c==null)
    				return;
                        multiplePlacement = false;
    			setMouseMode(MODE_ADD_ELM);
    			mouseModeStr=c;
			updateToolbar();
    			tempMouseMode = mouseMode;
    		}
    		if (cc==32) {
		    resetToSelectMode();
		    e.cancel();
    		}
    	}
    }
    
    void enableMultiplePlacement() {
        multiplePlacement = mouseMode == MODE_ADD_ELM;
        updateToolbar();
    }

    private void resetToSelectMode() {
        multiplePlacement = false;
        setMouseMode(MODE_SELECT);
        tempMouseMode = mouseMode;
        mouseModeStr = "Select";
        updateToolbar();
    }

    void updateToolbar() {
	String mode = classToLabelMap.get(mouseModeStr);
	String label = Locale.LS("Mode: ") + (mode == null ? Locale.LS("Select") : mode);
	canvasModeLabel.setText(multiplePlacement ? label + " (" + Locale.LS("Multiple") + ")" : label);
	toolbar.highlightButton(mouseModeStr);
    }

    String getLabelTextForClass(String cls) {
	return classToLabelMap.get(cls);
    }

    // factors a matrix into upper and lower triangular matrices by
    // gaussian elimination.  On entry, a[0..n-1][0..n-1] is the
    // matrix to be factored.  ipvt[] returns an integer vector of pivot
    // indices, used in the lu_solve() routine.
    static boolean lu_factor(double a[][], int n, int ipvt[]) {
	int i,j,k;
	
	// check for a possible singular matrix by scanning for rows that
	// are all zeroes
	for (i = 0; i != n; i++) { 
	    boolean row_all_zeros = true;
	    for (j = 0; j != n; j++) {
		if (a[i][j] != 0) {
		    row_all_zeros = false;
		    break;
		}
	    }
	    // if all zeros, it's a singular matrix
	    if (row_all_zeros)
		return false;
	}
	
        // use Crout's method; loop through the columns
	for (j = 0; j != n; j++) {
	    
	    // calculate upper triangular elements for this column
	    for (i = 0; i != j; i++) {
		double q = a[i][j];
		for (k = 0; k != i; k++)
		    q -= a[i][k]*a[k][j];
		a[i][j] = q;
	    }

	    // calculate lower triangular elements for this column
	    double largest = 0;
	    int largestRow = -1;
	    for (i = j; i != n; i++) {
		double q = a[i][j];
		for (k = 0; k != j; k++)
		    q -= a[i][k]*a[k][j];
		a[i][j] = q;
		double x = Math.abs(q);
		if (x >= largest) {
		    largest = x;
		    largestRow = i;
		}
	    }
	    
	    // pivoting
	    if (j != largestRow) {
		if (largestRow == -1) {
		    console("largestRow == -1");
		    return false;
		}
		double x;
		for (k = 0; k != n; k++) {
		    x = a[largestRow][k];
		    a[largestRow][k] = a[j][k];
		    a[j][k] = x;
		}
	    }

	    // keep track of row interchanges
	    ipvt[j] = largestRow;

	    // check for zeroes; if we find one, it's a singular matrix.
	    // we used to avoid them, but that caused weird bugs.  For example,
	    // two inverters with outputs connected together should be flagged
	    // as a singular matrix, but it was allowed (with weird currents)
	    if (a[j][j] == 0.0) {
		console("didn't avoid zero");
//		a[j][j]=1e-18;
		return false;
	    }

	    if (j != n-1) {
		double mult = 1.0/a[j][j];
		for (i = j+1; i != n; i++)
		    a[i][j] *= mult;
	    }
	}
	return true;
    }

    // Solves the set of n linear equations using a LU factorization
    // previously performed by lu_factor.  On input, b[0..n-1] is the right
    // hand side of the equations, and on output, contains the solution.
    static void lu_solve(double a[][], int n, int ipvt[], double b[]) {
	int i;

	// find first nonzero b element
	for (i = 0; i != n; i++) {
	    int row = ipvt[i];

	    double swap = b[row];
	    b[row] = b[i];
	    b[i] = swap;
	    if (swap != 0)
		break;
	}
	
	int bi = i++;
	for (; i < n; i++) {
	    int row = ipvt[i];
	    int j;
	    double tot = b[row];
	    
	    b[row] = b[i];
	    // forward substitution using the lower triangular matrix
	    for (j = bi; j < i; j++)
		tot -= a[i][j]*b[j];
	    b[i] = tot;
	}
	for (i = n-1; i >= 0; i--) {
	    double tot = b[i];
	    
	    // back-substitution using the upper triangular matrix
	    int j;
	    for (j = i+1; j != n; j++)
		tot -= a[i][j]*b[j];
	    b[i] = tot/a[i][i];
	}
    }

    
    private FlowPanel addControlsSection(String title) {
	FlowPanel section = new FlowPanel();
	section.setStyleName("controls-section");
	Label heading = new Label(Locale.LS(title));
	heading.setStyleName("controls-section-title");
	section.add(heading);
	verticalPanel.add(section);
	return section;
    }

    void addWidgetToVerticalPanel(Widget w) {
	circuitControls.add(w);
	setiFrameHeight();
    }
    
    void removeWidgetFromVerticalPanel(Widget w) {
	circuitControls.remove(w);
	setiFrameHeight();
    }
    
    public static CircuitElm createCe(int tint, int x1, int y1, int x2, int y2, int f, StringTokenizer st) {
	switch (tint) {
    	case 'A': return new AntennaElm(x1, y1, x2, y2, f, st);
    	case 'I': return new InverterElm(x1, y1, x2, y2, f, st);
    	case 'L': return new LogicInputElm(x1, y1, x2, y2, f, st);
    	case 'M': return new LogicOutputElm(x1, y1, x2, y2, f, st);
    	case 'O': return new OutputElm(x1, y1, x2, y2, f, st);
    	case 'R': return new RailElm(x1, y1, x2, y2, f, st);
    	case 'S': return new Switch2Elm(x1, y1, x2, y2, f, st);
    	case 'T': return new TransformerElm(x1, y1, x2, y2, f, st);
    	case 'a': return new OpAmpElm(x1, y1, x2, y2, f, st);
    	case 'b': return new BoxElm(x1, y1, x2, y2, f, st);
    	case 'c': return new CapacitorElm(x1, y1, x2, y2, f, st);   	
    	case 'd': return new DiodeElm(x1, y1, x2, y2, f, st);
    	case 'f': return new MosfetElm(x1, y1, x2, y2, f, st);
    	case 'g': return new GroundElm(x1, y1, x2, y2, f, st);
    	case 'i': return new CurrentElm(x1, y1, x2, y2, f, st);
    	case 'j': return new JfetElm(x1, y1, x2, y2, f, st);
    	case 'l': return new InductorElm(x1, y1, x2, y2, f, st);
    	case 'm': return new MemristorElm(x1, y1, x2, y2, f, st);
    	case 'n': return new NoiseElm(x1, y1, x2, y2, f, st);
    	case 'p': return new ProbeElm(x1, y1, x2, y2, f, st);
    	case 'r': return new ResistorElm(x1, y1, x2, y2, f, st);
    	case 's': return new SwitchElm(x1, y1, x2, y2, f, st);
    	case 't': return new TransistorElm(x1, y1, x2, y2, f, st);
    	case 'v': return new VoltageElm(x1, y1, x2, y2, f, st);
    	case 'w': return new WireElm(x1, y1, x2, y2, f, st);
    	case 'x': return new TextElm(x1, y1, x2, y2, f, st);
    	case 'z': return new ZenerElm(x1, y1, x2, y2, f, st);
    	case 150: return new AndGateElm(x1, y1, x2, y2, f, st);
    	case 151: return new NandGateElm(x1, y1, x2, y2, f, st);
    	case 152: return new OrGateElm(x1, y1, x2, y2, f, st);
    	case 153: return new NorGateElm(x1, y1, x2, y2, f, st);
    	case 154: return new XorGateElm(x1, y1, x2, y2, f, st);
    	case 155: return new DFlipFlopElm(x1, y1, x2, y2, f, st);
    	case 156: return new JKFlipFlopElm(x1, y1, x2, y2, f, st);
    	case 157: return new SevenSegElm(x1, y1, x2, y2, f, st);
    	case 158: return new VCOElm(x1, y1, x2, y2, f, st);
    	case 159: return new AnalogSwitchElm(x1, y1, x2, y2, f, st);
    	case 160: return new AnalogSwitch2Elm(x1, y1, x2, y2, f, st);
    	case 161: return new PhaseCompElm(x1, y1, x2, y2, f, st);
    	case 162: return new LEDElm(x1, y1, x2, y2, f, st);
    	case 163: return new RingCounterElm(x1, y1, x2, y2, f, st);
    	case 164: return new CounterElm(x1, y1, x2, y2, f, st);
    	case 165: return new TimerElm(x1, y1, x2, y2, f, st);
    	case 166: return new DACElm(x1, y1, x2, y2, f, st);
    	case 167: return new ADCElm(x1, y1, x2, y2, f, st);
    	case 168: return new LatchElm(x1, y1, x2, y2, f, st);
    	case 169: return new TappedTransformerElm(x1, y1, x2, y2, f, st);
    	case 170: return new SweepElm(x1, y1, x2, y2, f, st);
    	case 171: return new TransLineElm(x1, y1, x2, y2, f, st);
    	case 172: return new VarRailElm(x1, y1, x2, y2, f, st);
    	case 173: return new TriodeElm(x1, y1, x2, y2, f, st);
    	case 174: return new PotElm(x1, y1, x2, y2, f, st);
    	case 175: return new TunnelDiodeElm(x1, y1, x2, y2, f, st);
    	case 176: return new VaractorElm(x1, y1, x2, y2, f, st);
    	case 177: return new SCRElm(x1, y1, x2, y2, f, st);
    	case 178: return new RelayElm(x1, y1, x2, y2, f, st);
    	case 179: return new CC2Elm(x1, y1, x2, y2, f, st);
    	case 180: return new TriStateElm(x1, y1, x2, y2, f, st);
    	case 181: return new LampElm(x1, y1, x2, y2, f, st);
    	case 182: return new SchmittElm(x1, y1, x2, y2, f, st);
    	case 183: return new InvertingSchmittElm(x1, y1, x2, y2, f, st);
    	case 184: return new MultiplexerElm(x1, y1, x2, y2, f, st);
    	case 185: return new DeMultiplexerElm(x1, y1, x2, y2, f, st);
    	case 186: return new PisoShiftElm(x1, y1, x2, y2, f, st);
    	case 187: return new SparkGapElm(x1, y1, x2, y2, f, st);
    	case 188: return new SeqGenElm(x1, y1, x2, y2, f, st);
    	case 189: return new SipoShiftElm(x1, y1, x2, y2, f, st);
    	case 193: return new TFlipFlopElm(x1, y1, x2, y2, f, st);
    	case 194: return new MonostableElm(x1, y1, x2, y2, f, st);
    	case 195: return new HalfAdderElm(x1, y1, x2, y2, f, st);
    	case 196: return new FullAdderElm(x1, y1, x2, y2, f, st);
    	case 197: return new SevenSegDecoderElm(x1, y1, x2, y2, f, st);
    	case 200: return new AMElm(x1, y1, x2, y2, f, st);
    	case 201: return new FMElm(x1, y1, x2, y2, f, st);
    	case 203: return new DiacElm(x1, y1, x2, y2, f, st);
    	case 206: return new TriacElm(x1, y1, x2, y2, f, st);
    	case 207: return new LabeledNodeElm(x1, y1, x2, y2, f, st);
    	case 208: return new CustomLogicElm(x1, y1, x2, y2, f, st);
    	case 209: return new PolarCapacitorElm(x1, y1, x2, y2, f, st);   	
    	case 210: return new DataRecorderElm(x1, y1, x2, y2, f, st);
    	case 211: return new AudioOutputElm(x1, y1, x2, y2, f, st);
    	case 212: return new VCVSElm(x1, y1, x2, y2, f, st);
    	case 213: return new VCCSElm(x1, y1, x2, y2, f, st);
    	case 214: return new CCVSElm(x1, y1, x2, y2, f, st);
    	case 215: return new CCCSElm(x1, y1, x2, y2, f, st);
    	case 216: return new OhmMeterElm(x1, y1, x2, y2, f, st);
	case 350: return new ThermistorNTCElm(x1, y1, x2, y2, f, st);
    	case 368: return new TestPointElm(x1, y1, x2, y2, f, st);
    	case 370: return new AmmeterElm(x1, y1, x2, y2, f, st);
	case 374: return new LDRElm(x1, y1, x2, y2, f, st);
    	case 400: return new DarlingtonElm(x1, y1, x2, y2, f, st);
    	case 401: return new ComparatorElm(x1, y1, x2, y2, f, st);
    	case 402: return new OTAElm(x1, y1, x2, y2, f, st);
    	case 403: return new ScopeElm(x1, y1, x2, y2, f, st);
    	case 404: return new FuseElm(x1, y1, x2, y2, f, st);
    	case 405: return new LEDArrayElm(x1, y1, x2, y2, f, st);
    	case 406: return new CustomTransformerElm(x1, y1, x2, y2, f, st);
    	case 407: return new OptocouplerElm(x1, y1, x2, y2, f, st);
    	case 408: return new StopTriggerElm(x1, y1, x2, y2, f, st);
    	case 409: return new OpAmpRealElm(x1, y1, x2, y2, f, st);
    	case 410: return new CustomCompositeElm(x1, y1, x2, y2, f, st);
    	case 411: return new AudioInputElm(x1, y1, x2, y2, f, st);
    	case 412: return new CrystalElm(x1, y1, x2, y2, f, st);
    	case 413: return new SRAMElm(x1, y1, x2, y2, f, st);
    	case 414: return new TimeDelayRelayElm(x1, y1, x2, y2, f, st);
	case 415: return new DCMotorElm(x1, y1, x2, y2, f, st);
	case 416: return new MBBSwitchElm(x1, y1, x2, y2, f, st);
    	case 417: return new UnijunctionElm(x1, y1, x2, y2, f, st);
    	case 418: return new ExtVoltageElm(x1, y1, x2, y2, f, st);
    	case 419: return new DecimalDisplayElm(x1, y1, x2, y2, f, st);
    	case 420: return new WattmeterElm(x1, y1, x2, y2, f, st);
    	case 421: return new Counter2Elm(x1, y1, x2, y2, f, st);
    	case 422: return new DelayBufferElm(x1, y1, x2, y2, f, st);
    	case 423: return new LineElm(x1, y1, x2, y2, f, st);
    	case 424: return new DataInputElm(x1, y1, x2, y2, f, st);
    	case 425: return new RelayCoilElm(x1, y1, x2, y2, f, st);
    	case 426: return new RelayContactElm(x1, y1, x2, y2, f, st);
    	case 427: return new ThreePhaseMotorElm(x1, y1, x2, y2, f, st);
    	case 428: return new MotorProtectionSwitchElm(x1, y1, x2, y2, f, st);
    	case 429: return new DPDTSwitchElm(x1, y1, x2, y2, f, st);
    	case 430: return new CrossSwitchElm(x1, y1, x2, y2, f, st);
        }
    	return null;
    }

    public static CircuitElm constructElement(String n, int x1, int y1){
    	if (n=="GroundElm")
    		return (CircuitElm) new GroundElm(x1, y1);
    	if (n=="ResistorElm")
    		return (CircuitElm) new ResistorElm(x1, y1);
    	if (n=="RailElm")
    		return (CircuitElm) new RailElm(x1, y1);
    	if (n=="SwitchElm")
    		return (CircuitElm) new SwitchElm(x1, y1);
    	if (n=="Switch2Elm")
    		return (CircuitElm) new Switch2Elm(x1, y1);
    	if (n=="MBBSwitchElm")
    		return (CircuitElm) new MBBSwitchElm(x1, y1);
    	if (n=="NTransistorElm" || n == "TransistorElm")
    		return (CircuitElm) new NTransistorElm(x1, y1);
    	if (n=="PTransistorElm")
    		return (CircuitElm) new PTransistorElm(x1, y1);
    	if (n=="WireElm")
    		return (CircuitElm) new WireElm(x1, y1);
    	if (n=="CapacitorElm")
    		return (CircuitElm) new CapacitorElm(x1, y1);
    	if (n=="PolarCapacitorElm")
		return (CircuitElm) new PolarCapacitorElm(x1, y1);
    	if (n=="InductorElm")
    		return (CircuitElm) new InductorElm(x1, y1);
    	if (n=="DCVoltageElm" || n=="VoltageElm")
    		return (CircuitElm) new DCVoltageElm(x1, y1);
    	if (n=="VarRailElm")
    		return (CircuitElm) new VarRailElm(x1, y1);
    	if (n=="PotElm")
    		return (CircuitElm) new PotElm(x1, y1);
    	if (n=="OutputElm")
    		return (CircuitElm) new OutputElm(x1, y1);
    	if (n=="CurrentElm")
    		return (CircuitElm) new CurrentElm(x1, y1);
    	if (n=="ProbeElm")
    		return (CircuitElm) new ProbeElm(x1, y1);
    	if (n=="DiodeElm")
    		return (CircuitElm) new DiodeElm(x1, y1);
    	if (n=="ZenerElm")
    		return (CircuitElm) new ZenerElm(x1, y1);
    	if (n=="ACVoltageElm")
    		return (CircuitElm) new ACVoltageElm(x1, y1);
    	if (n=="ACRailElm")
    		return (CircuitElm) new ACRailElm(x1, y1);
    	if (n=="SquareRailElm")
    		return (CircuitElm) new SquareRailElm(x1, y1);
    	if (n=="SweepElm")
    		return (CircuitElm) new SweepElm(x1, y1);
    	if (n=="LEDElm")
    		return (CircuitElm) new LEDElm(x1, y1);
    	if (n=="AntennaElm")
    		return (CircuitElm) new AntennaElm(x1, y1);
    	if (n=="LogicInputElm")
    		return (CircuitElm) new LogicInputElm(x1, y1);
    	if (n=="LogicOutputElm")
    		return (CircuitElm) new LogicOutputElm(x1, y1);
    	if (n=="TransformerElm")
    		return (CircuitElm) new TransformerElm(x1, y1);
    	if (n=="TappedTransformerElm")
    		return (CircuitElm) new TappedTransformerElm(x1, y1);
    	if (n=="TransLineElm")
    		return (CircuitElm) new TransLineElm(x1, y1);
    	if (n=="RelayElm")
    		return (CircuitElm) new RelayElm(x1, y1);
    	if (n=="RelayCoilElm")
    		return (CircuitElm) new RelayCoilElm(x1, y1);
    	if (n=="RelayContactElm")
    		return (CircuitElm) new RelayContactElm(x1, y1);
    	if (n=="ThreePhaseMotorElm")
    		return (CircuitElm) new ThreePhaseMotorElm(x1, y1);
    	if (n=="MemristorElm")
    		return (CircuitElm) new MemristorElm(x1, y1);
    	if (n=="SparkGapElm")
    		return (CircuitElm) new SparkGapElm(x1, y1);
    	if (n=="ClockElm")
    		return (CircuitElm) new ClockElm(x1, y1);
    	if (n=="AMElm")
    		return (CircuitElm) new AMElm(x1, y1);
    	if (n=="FMElm")
    		return (CircuitElm) new FMElm(x1, y1);
    	if (n=="LampElm")
    		return (CircuitElm) new LampElm(x1, y1);
    	if (n=="PushSwitchElm")
    		return (CircuitElm) new PushSwitchElm(x1, y1);
    	if (n=="OpAmpElm")
    		return (CircuitElm) new OpAmpElm(x1, y1);
    	if (n=="OpAmpSwapElm")
    		return (CircuitElm) new OpAmpSwapElm(x1, y1);
    	if (n=="NMosfetElm" || n == "MosfetElm")
    		return (CircuitElm) new NMosfetElm(x1, y1);
    	if (n=="PMosfetElm")
    		return (CircuitElm) new PMosfetElm(x1, y1);
    	if (n=="NJfetElm" || n == "JfetElm")
    		return (CircuitElm) new NJfetElm(x1, y1);
    	if (n=="PJfetElm")
    		return (CircuitElm) new PJfetElm(x1, y1);
    	if (n=="AnalogSwitchElm")
    		return (CircuitElm) new AnalogSwitchElm(x1, y1);
    	if (n=="AnalogSwitch2Elm")
    		return (CircuitElm) new AnalogSwitch2Elm(x1, y1);
    	if (n=="SchmittElm")
    		return (CircuitElm) new SchmittElm(x1, y1);
    	if (n=="InvertingSchmittElm")
    		return (CircuitElm) new InvertingSchmittElm(x1, y1);
    	if (n=="TriStateElm")
    		return (CircuitElm) new TriStateElm(x1, y1);
    	if (n=="SCRElm")
    		return (CircuitElm) new SCRElm(x1, y1);
    	if (n=="DiacElm")
    		return (CircuitElm) new DiacElm(x1, y1);
    	if (n=="TriacElm")
    		return (CircuitElm) new TriacElm(x1, y1);
    	if (n=="TriodeElm")
    		return (CircuitElm) new TriodeElm(x1, y1);
    	if (n=="VaractorElm")
    	    	return (CircuitElm) new VaractorElm(x1, y1);
    	if (n=="TunnelDiodeElm")
    		return (CircuitElm) new TunnelDiodeElm(x1, y1);
    	if (n=="CC2Elm")
    		return (CircuitElm) new CC2Elm(x1, y1);
    	if (n=="CC2NegElm")
    		return (CircuitElm) new CC2NegElm(x1, y1);
    	if (n=="InverterElm")
    		return (CircuitElm) new InverterElm(x1, y1);
    	if (n=="NandGateElm")
    		return (CircuitElm) new NandGateElm(x1, y1);
    	if (n=="NorGateElm")
    		return (CircuitElm) new NorGateElm(x1, y1);
    	if (n=="AndGateElm")
    		return (CircuitElm) new AndGateElm(x1, y1);
    	if (n=="OrGateElm")
    		return (CircuitElm) new OrGateElm(x1, y1);
    	if (n=="XorGateElm")
    		return (CircuitElm) new XorGateElm(x1, y1);
    	if (n=="DFlipFlopElm")
    		return (CircuitElm) new DFlipFlopElm(x1, y1);
    	if (n=="JKFlipFlopElm")
    		return (CircuitElm) new JKFlipFlopElm(x1, y1);
    	if (n=="SevenSegElm")
    		return (CircuitElm) new SevenSegElm(x1, y1);
    	if (n=="MultiplexerElm")
    		return (CircuitElm) new MultiplexerElm(x1, y1);
    	if (n=="DeMultiplexerElm")
    		return (CircuitElm) new DeMultiplexerElm(x1, y1);
    	if (n=="SipoShiftElm")
    		return (CircuitElm) new SipoShiftElm(x1, y1);
    	if (n=="PisoShiftElm")
    		return (CircuitElm) new PisoShiftElm(x1, y1);
    	if (n=="PhaseCompElm")
    		return (CircuitElm) new PhaseCompElm(x1, y1);
    	if (n=="CounterElm")
    		return (CircuitElm) new CounterElm(x1, y1);
    	
	// if you take out RingCounterElm, it will break subcircuits
    	// if you take out DecadeElm, it will break the menus and people's saved shortcuts
    	if (n=="DecadeElm" || n=="RingCounterElm")
    		return (CircuitElm) new RingCounterElm(x1, y1);
    	
    	if (n=="TimerElm")
    		return (CircuitElm) new TimerElm(x1, y1);
    	if (n=="DACElm")
    		return (CircuitElm) new DACElm(x1, y1);
    	if (n=="ADCElm")
    		return (CircuitElm) new ADCElm(x1, y1);
    	if (n=="LatchElm")
    		return (CircuitElm) new LatchElm(x1, y1);
    	if (n=="SeqGenElm")
    		return (CircuitElm) new SeqGenElm(x1, y1);
    	if (n=="VCOElm")
    		return (CircuitElm) new VCOElm(x1, y1);
    	if (n=="BoxElm")
    		return (CircuitElm) new BoxElm(x1, y1);
    	if (n=="LineElm")
    		return (CircuitElm) new LineElm(x1, y1);
    	if (n=="TextElm")
    		return (CircuitElm) new TextElm(x1, y1);
    	if (n=="TFlipFlopElm")
    		return (CircuitElm) new TFlipFlopElm(x1, y1);
    	if (n=="SevenSegDecoderElm")
    		return (CircuitElm) new SevenSegDecoderElm(x1, y1);
    	if (n=="FullAdderElm")
    		return (CircuitElm) new FullAdderElm(x1, y1);
    	if (n=="HalfAdderElm")
    		return (CircuitElm) new HalfAdderElm(x1, y1);
    	if (n=="MonostableElm")
    		return (CircuitElm) new MonostableElm(x1, y1);
    	if (n=="LabeledNodeElm")
    		return (CircuitElm) new LabeledNodeElm(x1, y1);
    	
    	// if you take out UserDefinedLogicElm, it will break people's saved shortcuts
    	if (n=="UserDefinedLogicElm" || n=="CustomLogicElm")
    	    	return (CircuitElm) new CustomLogicElm(x1, y1);
    	
    	if (n=="TestPointElm")
    	    	return new TestPointElm(x1, y1);
    	if (n=="AmmeterElm")
	    	return new AmmeterElm(x1, y1);
    	if (n=="DataRecorderElm")
		return (CircuitElm) new DataRecorderElm(x1, y1);
    	if (n=="AudioOutputElm")
		return (CircuitElm) new AudioOutputElm(x1, y1);
    	if (n=="NDarlingtonElm" || n == "DarlingtonElm")
		return (CircuitElm) new NDarlingtonElm(x1, y1);
    	if (n=="PDarlingtonElm")
		return (CircuitElm) new PDarlingtonElm(x1, y1);
    	if (n=="ComparatorElm")
		return (CircuitElm) new ComparatorElm(x1, y1);
    	if (n=="OTAElm")
		return (CircuitElm) new OTAElm(x1, y1);
    	if (n=="NoiseElm")
		return (CircuitElm) new NoiseElm(x1, y1);
    	if (n=="VCVSElm")
		return (CircuitElm) new VCVSElm(x1, y1);
    	if (n=="VCCSElm")
		return (CircuitElm) new VCCSElm(x1, y1);
    	if (n=="CCVSElm")
		return (CircuitElm) new CCVSElm(x1, y1);
    	if (n=="CCCSElm")
		return (CircuitElm) new CCCSElm(x1, y1);
    	if (n=="OhmMeterElm")
		return (CircuitElm) new OhmMeterElm(x1, y1);
    	if (n=="ScopeElm")
    	    	return (CircuitElm) new ScopeElm(x1,y1);
    	if (n=="FuseElm")
	    	return (CircuitElm) new FuseElm(x1,y1);
    	if (n=="LEDArrayElm")
    	    	return (CircuitElm) new LEDArrayElm(x1, y1);
    	if (n=="CustomTransformerElm")
    	    	return (CircuitElm) new CustomTransformerElm(x1, y1);
    	if (n=="OptocouplerElm")
		return (CircuitElm) new OptocouplerElm(x1, y1);
    	if (n=="StopTriggerElm")
		return (CircuitElm) new StopTriggerElm(x1, y1);
    	if (n=="OpAmpRealElm")
		return (CircuitElm) new OpAmpRealElm(x1, y1);
    	if (n=="CustomCompositeElm")
		return (CircuitElm) new CustomCompositeElm(x1, y1);
    	if (n=="AudioInputElm")
		return (CircuitElm) new AudioInputElm(x1, y1);
    	if (n=="CrystalElm")
		return (CircuitElm) new CrystalElm(x1, y1);
    	if (n=="SRAMElm")
		return (CircuitElm) new SRAMElm(x1, y1);
    	if (n=="TimeDelayRelayElm")
		return (CircuitElm) new TimeDelayRelayElm(x1, y1);
    	if (n=="DCMotorElm")
		return (CircuitElm) new DCMotorElm(x1, y1);
    	if (n=="LDRElm")
		return (CircuitElm) new LDRElm(x1, y1);
    	if (n=="ThermistorNTCElm")
		return (CircuitElm) new ThermistorNTCElm(x1, y1);
    	if (n=="UnijunctionElm")
		return (CircuitElm) new UnijunctionElm(x1, y1);
    	if (n=="ExtVoltageElm")
		return (CircuitElm) new ExtVoltageElm(x1, y1);
    	if (n=="DecimalDisplayElm")
		return (CircuitElm) new DecimalDisplayElm(x1, y1);
    	if (n=="WattmeterElm")
		return (CircuitElm) new WattmeterElm(x1, y1);
    	if (n=="Counter2Elm")
		return (CircuitElm) new Counter2Elm(x1, y1);
    	if (n=="DelayBufferElm")
		return (CircuitElm) new DelayBufferElm(x1, y1);
    	if (n=="DataInputElm")
		return (CircuitElm) new DataInputElm(x1, y1);
    	if (n=="MotorProtectionSwitchElm")
		return (CircuitElm) new MotorProtectionSwitchElm(x1, y1);
    	if (n=="DPDTSwitchElm")
		return (CircuitElm) new DPDTSwitchElm(x1, y1);
    	if (n=="CrossSwitchElm")
		return (CircuitElm) new CrossSwitchElm(x1, y1);
    	
    	// handle CustomCompositeElm:modelname
    	if (n.startsWith("CustomCompositeElm:")) {
    	    int ix = n.indexOf(':')+1;
    	    String name = n.substring(ix);
    	    return (CircuitElm) new CustomCompositeElm(x1, y1, name);
    	}
    	return null;
    }
    
    public void updateModels() {
	int i;
	for (i = 0; i != elmList.size(); i++)
	    elmList.get(i).updateModels();
    }
    

    
    
    native boolean weAreInUS(boolean orCanada) /*-{
    try {
	l = navigator.languages ? navigator.languages[0] : (navigator.language || navigator.userLanguage) ;  
    	if (l.length > 2) {
    		l = l.slice(-2).toUpperCase();
    		return (l == "US" || (l=="CA" && orCanada));
    	} else {
    		return 0;
    	}

    } catch (e) { return 0;
    }
    }-*/;

    native boolean weAreInGermany() /*-{
    try {
	l = navigator.languages ? navigator.languages[0] : (navigator.language || navigator.userLanguage) ;
	return (l.toUpperCase().startsWith("DE"));
    } catch (e) { return 0;
    }
    }-*/;
    
    // For debugging
    void dumpNodelist() {

	CircuitNode nd;
	CircuitElm e;
	int i,j;
	String s;
	String cs;
//
//	for(i=0; i<nodeList.size(); i++) {
//	    s="Node "+i;
//	    nd=nodeList.get(i);
//	    for(j=0; j<nd.links.size();j++) {
//		s=s+" " + nd.links.get(j).num + " " +nd.links.get(j).elm.getDumpType();
//	    }
//	    console(s);
//	}
	console("Elm list Dump");
	for (i=0;i<elmList.size(); i++) {
	    e=elmList.get(i);
	    cs = e.getDumpClass().toString();
	    int p = cs.lastIndexOf('.');
	    cs = cs.substring(p+1);
	    if (cs=="WireElm") 
		continue;
	    if (cs=="LabeledNodeElm")
		cs = cs+" "+((LabeledNodeElm)e).text;
	    if (cs=="TransistorElm") {
		if (((TransistorElm)e).pnp == -1)
		    cs= "PTransistorElm";
		else
		    cs = "NTransistorElm";
	    }
	    s=cs;
	    for(j=0; j<e.getPostCount(); j++) {
		s=s+" "+e.nodes[j];
	    }
	    console(s);
	}
    }
    
	native void printCanvas(CanvasElement cv) /*-{
	    var img    = cv.toDataURL("image/png");
	    var win = window.open("", "print", "height=500,width=500,status=yes,location=no");
	    win.document.title = "Print Circuit";
	    win.document.open();
	    win.document.write('<img src="'+img+'"/>');
	    win.document.close();
	    setTimeout(function(){win.print();},1000);
	}-*/;

	void doDCAnalysis() {
	    dcAnalysisFlag = true;
	    resetAction();
	}
	
	void doPrint() {
	    Canvas cv = getCircuitAsCanvas(CAC_PRINT);
	    printCanvas(cv.getCanvasElement());
	}

	boolean loadedCanvas2SVG = false;

	boolean initializeSVGScriptIfNecessary(final String followupAction) {
		// load canvas2svg if we haven't already
		if (!loadedCanvas2SVG) {
			ScriptInjector.fromUrl("canvas2svg.js").setCallback(new Callback<Void,Exception>() {
				public void onFailure(Exception reason) {
					Window.alert("Can't load canvas2svg.js.");
				}
				public void onSuccess(Void result) {
					loadedCanvas2SVG = true;
					if (followupAction.equals("doExportAsSVG")) {
						doExportAsSVG();
					} else if (followupAction.equals("doExportAsSVGFromAPI")) {
						doExportAsSVGFromAPI();
					}
				}
			}).inject();
			return false;
		}
		return true;
	}

	void doExportAsSVG() {
		if (!initializeSVGScriptIfNecessary("doExportAsSVG")) {
			return;
		}
		dialogShowing = new ExportAsImageDialog(CAC_SVG);
		dialogShowing.show();
	}

	public void doExportAsSVGFromAPI() {
		if (!initializeSVGScriptIfNecessary("doExportAsSVGFromAPI")) {
			return;
		}
		String svg = getCircuitAsSVG();
		callSVGRenderedHook(svg);
	}

	static final int CAC_PRINT = 0;
	static final int CAC_IMAGE = 1;
	static final int CAC_SVG   = 2;
	
	public Canvas getCircuitAsCanvas(int type) {
	    	// create canvas to draw circuit into
	    	Canvas cv = Canvas.createIfSupported();
	    	Rectangle bounds = getCircuitBounds();
	    	
		// add some space on edges because bounds calculation is not perfect
	    	int wmargin = 140;
	    	int hmargin = 100;
	    	int w = (bounds.width*2+wmargin) ;
	    	int h = (bounds.height*2+hmargin) ;
	    	cv.setCoordinateSpaceWidth(w);
	    	cv.setCoordinateSpaceHeight(h);
	    
		Context2d context = cv.getContext2d();
		drawCircuitInContext(context, type, bounds, w, h);
		return cv;
	}
	
	// create SVG context using canvas2svg
	native static Context2d createSVGContext(int w, int h) /*-{
	    return new C2S(w, h);
	}-*/;
	
	native static String getSerializedSVG(Context2d context) /*-{
	    return context.getSerializedSvg();
	}-*/;
	
	public String getCircuitAsSVG() {
	    Rectangle bounds = getCircuitBounds();

	    // add some space on edges because bounds calculation is not perfect
	    int wmargin = 140;
	    int hmargin = 100;
	    int w = (bounds.width+wmargin) ;
	    int h = (bounds.height+hmargin) ;
	    Context2d context = createSVGContext(w, h);
	    drawCircuitInContext(context, CAC_SVG, bounds, w, h);
	    return getSerializedSVG(context);
	}
	
	void drawCircuitInContext(Context2d context, int type, Rectangle bounds, int w, int h) {
		Graphics g = new Graphics(context);
		context.setTransform(1, 0, 0, 1, 0, 0);
	    	double oldTransform[] = Arrays.copyOf(transform, 6);
	        
	        double scale = 1;
	        
		// turn on white background, turn off current display
		boolean p = printableCheckItem.getState();
		boolean c = dotsCheckItem.getState();
		boolean print = (type == CAC_PRINT);
		if (print)
		    printableCheckItem.setState(true);
	        if (printableCheckItem.getState()) {
	            CircuitElm.whiteColor = Color.black;
	            CircuitElm.lightGrayColor = Color.black;
	            g.setColor(Color.white);
	        } else {
	            CircuitElm.whiteColor = Color.white;
	            CircuitElm.lightGrayColor = Color.lightGray;
	            g.setColor(Color.black);
	        }
	        g.fillRect(0, 0, w, h);
		dotsCheckItem.setState(false);

	    	int wmargin = 140;
	    	int hmargin = 100;
	        if (bounds != null)
	            scale = Math.min(w /(double)(bounds.width+wmargin),
	                             h/(double)(bounds.height+hmargin));
	        
	        // ScopeElms need the transform array to be updated
		transform[0] = transform[3] = scale;
		transform[4] = -(bounds.x-wmargin/2);
		transform[5] = -(bounds.y-hmargin/2);
		context.scale(scale, scale);
		context.translate(transform[4], transform[5]);
		context.setLineCap(Context2d.LineCap.ROUND);
                canvasImages.draw(context, scale, 0, 0, 0, w, h, false);
		
		// draw elements
		int i;
		for (i = 0; i != elmList.size(); i++) {
		    CircuitElm elm = getElm(i);
		    elm.draw(g);
		    elm.drawReference(g);
		}
		for (i = 0; i != postDrawList.size(); i++) {
		    CircuitElm.drawPost(g, postDrawList.get(i));
		}

		// restore everything
		printableCheckItem.setState(p);
		dotsCheckItem.setState(c);
		transform = oldTransform;
	}
	
	boolean isSelection() {
	    for (int i = 0; i != elmList.size(); i++)
		if (getElm(i).isSelected())
		    return true;
	    return false;
	}
	
	public CustomCompositeModel getCircuitAsComposite() {
	    return getCircuitAsComposite(isSelection());
	}

	private CustomCompositeModel getCircuitAsComposite(boolean sel) {
	    int i;
	    String nodeDump = "";
	    String dump = "";
//	    String models = "";
	    CustomLogicModel.clearDumpedFlags();
	    DiodeModel.clearDumpedFlags();
	    TransistorModel.clearDumpedFlags();
            Vector<LabeledNodeElm> sideLabels[] = new Vector[] {
                new Vector<LabeledNodeElm>(), new Vector<LabeledNodeElm>(),
                new Vector<LabeledNodeElm>(), new Vector<LabeledNodeElm>()
            };
	    Vector<ExtListEntry> extList = new Vector<ExtListEntry>();
	    // redo node allocation to avoid auto-assigning ground
	    if (!preStampCircuit(true))
		return null;
	    boolean used[] = new boolean[nodeList.size()];
	    boolean extnodes[] = new boolean[nodeList.size()];

	    // find all the labeled nodes, get a list of them, and create a node number map
	    for (i = 0; i != elmList.size(); i++) {
		CircuitElm ce = getElm(i);
		if (sel && !ce.isSelected())
		    continue;
		if (ce instanceof LabeledNodeElm) {
		    LabeledNodeElm lne = (LabeledNodeElm) ce;
		    String label = lne.text;
		    if (lne.isInternal())
			continue;
		    
		    // already added to list?
		    if (extnodes[ce.getNode(0)])
			continue;
		    
                    int side = ChipElm.SIDE_W;
                    if (Math.abs(ce.dx) >= Math.abs(ce.dy) && ce.dx > 0) side = ChipElm.SIDE_E;
                    if (Math.abs(ce.dx) <= Math.abs(ce.dy) && ce.dy < 0) side = ChipElm.SIDE_N;
                    if (Math.abs(ce.dx) <= Math.abs(ce.dy) && ce.dy > 0) side = ChipElm.SIDE_S;
                    
		    // create ext list entry for external nodes
                    sideLabels[side].add(lne);
		    extnodes[ce.getNode(0)] = true;
		    if (ce.getNode(0) == 0) {
		        Window.alert("Node \"" + lne.text + "\" can't be connected to ground");
			return null;
		    }
		}
	    }
	    
            Collections.sort(sideLabels[ChipElm.SIDE_W], (LabeledNodeElm a, LabeledNodeElm b) -> Integer.signum(a.y - b.y));
            Collections.sort(sideLabels[ChipElm.SIDE_E], (LabeledNodeElm a, LabeledNodeElm b) -> Integer.signum(a.y - b.y));
            Collections.sort(sideLabels[ChipElm.SIDE_N], (LabeledNodeElm a, LabeledNodeElm b) -> Integer.signum(a.x - b.x));
            Collections.sort(sideLabels[ChipElm.SIDE_S], (LabeledNodeElm a, LabeledNodeElm b) -> Integer.signum(a.x - b.x));

            for (int side = 0; side < sideLabels.length; side++) {
                for (int pos = 0; pos < sideLabels[side].size(); pos++) {
                    LabeledNodeElm lne = sideLabels[side].get(pos);
                    ExtListEntry ent = new ExtListEntry(lne.text, lne.getNode(0), pos, side);
                    extList.add(ent);
                }
            }

	    // output all the elements
	    for (i = 0; i != elmList.size(); i++) {
		CircuitElm ce = getElm(i);
		if (sel && !ce.isSelected())
		    continue;
		// don't need these elements dumped
		if (ce instanceof WireElm || ce instanceof LabeledNodeElm || ce instanceof ScopeElm)
		    continue;
		if (ce instanceof GraphicElm || ce instanceof GroundElm)
		    continue;
		int j;
		if (nodeDump.length() > 0)
		    nodeDump += "\r";
		nodeDump += ce.getClass().getSimpleName();
		for (j = 0; j != ce.getPostCount(); j++) {
		    int n = ce.getNode(j);
		    used[n] = true;
		    nodeDump += " " + n;
		}
		
	        // save positions
                int x1 = ce.x;  int y1 = ce.y;
                int x2 = ce.x2; int y2 = ce.y2;
                
                // set them to 0 so they're easy to remove
                ce.x = ce.y = ce.x2 = ce.y2 = 0;

                String tstring = ce.dump();
                tstring = tstring.replaceFirst("[A-Za-z0-9]+ 0 0 0 0 ", ""); // remove unused tint_x1 y1 x2 y2 coords for internal components
                
                // restore positions
                ce.x = x1; ce.y = y1; ce.x2 = x2; ce.y2 = y2;
                if (dump.length() > 0)
                    dump += " ";
                dump += CustomLogicModel.escape(tstring);
	    }
	    
	    for (i = 0; i != extList.size(); i++) {
		ExtListEntry ent = extList.get(i);
		if (!used[ent.node]) {
		    Window.alert("Node \"" + ent.name + "\" is not used!");
		    return null;
		}
	    }
	
	    boolean first = true;
	    for (i = 0; i != unconnectedNodes.size(); i++) {
		int q = unconnectedNodes.get(i);
		if (!extnodes[q] && used[q]) {
		    if (nodesWithGroundConnectionCount == 0 && first) {
			first = false;
			continue;
		    }
		    Window.alert("Some nodes are unconnected!");
		    return null;
		}
	    }	    

	    CustomCompositeModel ccm = new CustomCompositeModel();
	    ccm.nodeList = nodeDump;
	    ccm.elmDump = dump;
	    ccm.extList = extList;
	    return ccm;
	}
	
	static void invertMatrix(double a[][], int n) {
	    int ipvt[] = new int[n];
	    lu_factor(a, n, ipvt);
	    int i, j;
	    double b[] = new double[n];
	    double inva[][] = new double[n][n];
	    
	    // solve for each column of identity matrix
	    for (i = 0; i != n; i++) {
		for (j = 0; j != n; j++)
		    b[j] = 0;
		b[i] = 1;
		lu_solve(a, n, ipvt, b);
		for (j = 0; j != n; j++)
		    inva[j][i] = b[j];
	    }
	    
	    // return in original matrix
	    for (i = 0; i != n; i++)
		for (j = 0; j != n; j++)
		    a[i][j] = inva[i][j];
	}
	
	double getLabeledNodeVoltage(String name) {
	    Integer node = LabeledNodeElm.getByName(name);
	    if (node == null || node == 0)
		return 0;
	    // subtract one because ground is not included in nodeVoltages[]
	    return nodeVoltages[node.intValue()-1];
	}
	
	void setExtVoltage(String name, double v) {
	    int i;
	    for (i = 0; i != elmList.size(); i++) {
		CircuitElm ce = getElm(i);
		if (ce instanceof ExtVoltageElm) {
		    ExtVoltageElm eve = (ExtVoltageElm) ce;
		    if (eve.getName().equals(name))
			eve.setVoltage(v);
		}
	    }
	}

	native JsArray<JavaScriptObject> getJSArray() /*-{ return []; }-*/;
	
	JsArray<JavaScriptObject> getJSElements() {
	    int i;
	    JsArray<JavaScriptObject> arr = getJSArray();
	    for (i = 0; i != elmList.size(); i++) {
		CircuitElm ce = getElm(i);
		ce.addJSMethods();
		arr.push(ce.getJavaScriptObject());
	    }
	    return arr;
	}
	
	native void setupJSInterface() /*-{
	    var that = this;
	    $wnd.CircuitJS1 = {
	        setSimRunning: $entry(function(run) { that.@com.lushprojects.circuitjs1.client.CirSim::setSimRunning(Z)(run); } ),
	        getTime: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::t; } ),
	        getTimeStep: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::timeStep; } ),
	        setTimeStep: $entry(function(ts) { that.@com.lushprojects.circuitjs1.client.CirSim::timeStep = ts; } ), // don't use this, see #843
	        getMaxTimeStep: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::maxTimeStep; } ),
	        setMaxTimeStep: $entry(function(ts) { that.@com.lushprojects.circuitjs1.client.CirSim::maxTimeStep = 
                                                      that.@com.lushprojects.circuitjs1.client.CirSim::timeStep = ts; } ),
	        isRunning: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::simIsRunning()(); } ),
	        getNodeVoltage: $entry(function(n) { return that.@com.lushprojects.circuitjs1.client.CirSim::getLabeledNodeVoltage(Ljava/lang/String;)(n); } ),
	        setExtVoltage: $entry(function(n, v) { that.@com.lushprojects.circuitjs1.client.CirSim::setExtVoltage(Ljava/lang/String;D)(n, v); } ),
	        getElements: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::getJSElements()(); } ),
	        getCircuitAsSVG: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::doExportAsSVGFromAPI()(); } ),
	        exportCircuit: $entry(function() { return that.@com.lushprojects.circuitjs1.client.CirSim::dumpCircuit()(); } ),
	        importCircuit: $entry(function(circuit, subcircuitsOnly) { return that.@com.lushprojects.circuitjs1.client.CirSim::importCircuitFromText(Ljava/lang/String;Z)(circuit, subcircuitsOnly); })
	    };
	    var hook = $wnd.oncircuitjsloaded;
	    if (hook)
	    	hook($wnd.CircuitJS1);
	}-*/;
	
	native void callUpdateHook() /*-{
	    var hook = $wnd.CircuitJS1.onupdate;
	    if (hook)
	    	hook($wnd.CircuitJS1);
	}-*/;
	
        native void callAnalyzeHook() /*-{
            var hook = $wnd.CircuitJS1.onanalyze;
            if (hook)
                hook($wnd.CircuitJS1);
    	}-*/;
    

	native void callTimeStepHook() /*-{
	    var hook = $wnd.CircuitJS1.ontimestep;
	    if (hook)
	    	hook($wnd.CircuitJS1);
	}-*/;
	
	native void callSVGRenderedHook(String svgData) /*-{
		var hook = $wnd.CircuitJS1.onsvgrendered;
		if (hook)
			hook($wnd.CircuitJS1, svgData);
	}-*/;

	private class SubcircuitEdit {
	    final SubcircuitEdit parent = subcircuitEdit;
	    final UndoItem schematic = new UndoItem(dumpCircuit());
	    final Vector<UndoItem> undo = undoStack, redo = redoStack;
	    final String title = getCircuitTitle();
	    final boolean saved = savedFlag, unsaved = unsavedChanges;
	    final CustomCompositeModel model;
	    boolean childUpdated;

	    SubcircuitEdit(CustomCompositeModel model) {
		this.model = model;
	    }
	}

	class UndoItem {
	    public String dump;
	    public double scale, transform4, transform5;
	    String file, title;
	    boolean dirty;
	    UndoItem(String d) {
		dump = d;
		scale = transform[0];
		transform4 = transform[4];
		transform5 = transform[5];
		if (mobileProjects != null) {
		    file = mobileProjects.projectName();
		    title = getCircuitTitle();
		    dirty = mobileProjects.hasUnsavedEdits();
		}
	    }

	    boolean matches(UndoItem other) {
		return dump.equals(other.dump)
		    && (mobileProjects == null || (java.util.Objects.equals(file, other.file)
		        && title.equals(other.title) && dirty == other.dirty));
	    }
	}

}

