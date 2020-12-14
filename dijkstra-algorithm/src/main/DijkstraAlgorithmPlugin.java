package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.SystemColor;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import lavesdk.LAVESDKV;
import lavesdk.algorithm.AlgorithmExercise;
import lavesdk.algorithm.AlgorithmStateAttachment;
import lavesdk.algorithm.RTEvent;
import lavesdk.algorithm.AlgorithmRTE;
import lavesdk.algorithm.AlgorithmState;
import lavesdk.algorithm.plugin.AlgorithmPlugin;
import lavesdk.algorithm.plugin.PluginHost;
import lavesdk.algorithm.plugin.ResourceLoader;
import lavesdk.algorithm.plugin.enums.MessageIcon;
import lavesdk.algorithm.plugin.extensions.CircleLayoutToolBarExtension;
import lavesdk.algorithm.plugin.extensions.MatrixToGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.RandomGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultGraphView;
import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.LegendView;
import lavesdk.algorithm.plugin.views.TextAreaView;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.dialogs.enums.AllowedGraphType;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.widgets.BooleanProperty;
import lavesdk.gui.widgets.BooleanPropertyGroup;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.ExecutionTableBorder;
import lavesdk.gui.widgets.ExecutionTableColumn;
import lavesdk.gui.widgets.ExecutionTableGroup;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.Mask;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.NumericTextField;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.gui.widgets.Symbol;
import lavesdk.gui.widgets.Symbol.PredefinedSymbol;
import lavesdk.language.LanguageFile;
import lavesdk.math.ElementParser;
import lavesdk.math.Set;
import lavesdk.math.Set.StringElementParser;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.Walk;
import lavesdk.utils.GraphUtils;
import lavesdk.utils.MathUtils;

/**
 * Plugin that visualizes and teaches users the algorithm of Dijkstra.
 * 
 * @author jdornseifer
 * @version 1.3
 */
public class DijkstraAlgorithmPlugin implements AlgorithmPlugin {
	
	/** the host */
	private PluginHost host;
	/** the configuration of the plugin */
	private Configuration config;
	/** the language file of the plugin */
	private LanguageFile langFile;
	/** the language id the plugin should use */
	private String langID;
	/** the .vgf file filter */
	private FileNameExtensionFilter vgfFileFilter;
	/** the .png file filter */
	private FileNameExtensionFilter pngFileFilter;
	/** the algorithm text */
	private AlgorithmText algoText;
	/** the graph view that visualizes the graph */
	private DefaultGraphView graphView;
	/** the view that displays the algorithm text */
	private AlgorithmTextView algoTextView;
	/** the table that visualizes execution data */
	private ExecutionTableView execTableView;
	/** the view that visualizes the sets */
	private TextAreaView setsView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of dijkstra's algorithm */
	private DijkstraRTE rte;
	/** toolbar extension to create graphs from adjacency matrices */
	private MatrixToGraphToolBarExtension<Vertex, Edge> matrixToGraph;
	/** toolbar extension to create a random graph */
	private RandomGraphToolBarExtension<Vertex, Edge> randomGraph;
	/** toolbar extension to layout a graph in a circle */
	private CircleLayoutToolBarExtension<Vertex, Edge> circleLayoutExt;
	/** the string describing the directed creator preference */
	private String creatorPrefsDirected;
	/** the string describing the undirected creator preference */
	private String creatorPrefsUndirected;
	/** the value of the directed creator preference */
	private boolean creatorPrefsDirectedValue;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for D and E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup de;
	/** the view group for A,B,C,D and E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcde;
	
	// modifiable visualization data
	/** color to visualize the vertices of set A */
	private Color colorSetA;
	/** color to visualize the vertices of set B */
	private Color colorSetB;
	/** color to visualize the vertices of set C */
	private Color colorSetC;
	/** color to visualize the vertex with the minimal distance */
	private Color colorVertexMinDist;
	/** color to visualize the elements which are under investigation */
	private Color colorSetHighlight;
	/** color to visualize the current element under investigation */
	private Color colorSetCurrElemHighlight;
	/** color to visualize modified objects */
	private Color colorModified;
	/** line with of the starting vertex */
	private int lineWidthStartVertex;
	/** line with of the current edge under investigation */
	private int lineWidthCurrEdge;
	/** line with of the vertex with the minimal distance */
	private int lineWidthVertexMinDist;
	
	/** configuration key for the {@link #creatorPrefsDirectedValue} */
	private static final String CFGKEY_CREATORPROP_DIRECTED = "creatorPropDirected";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #colorSetHighlight} */
	private static final String CFGKEY_COLOR_SETHIGHLIGHT = "colorSetHighlight";
	/** configuration key for the {@link #colorSetCurrElemHighlight} */
	private static final String CFGKEY_COLOR_SETCURRELEMHIGHLIGHT = "colorSetCurrElemHighlight";
	/** configuration key for the {@link #colorSetA} */
	private static final String CFGKEY_COLOR_SETA = "colorSetA";
	/** configuration key for the {@link #colorSetB} */
	private static final String CFGKEY_COLOR_SETB = "colorSetB";
	/** configuration key for the {@link #colorSetC} */
	private static final String CFGKEY_COLOR_SETC = "colorSetC";
	/** configuration key for the {@link #colorVertexMinDist} */
	private static final String CFGKEY_COLOR_VERTEXMINDIST = "colorVertexMinDist";
	/** configuration key for the {@link #lineWidthStartVertex} */
	private static final String CFGKEY_LINEWIDTH_STARTVERTEX = "lineWidthStartVertex";
	/** configuration key for the {@link #lineWidthCurrEdge} */
	private static final String CFGKEY_LINEWIDTH_CURREDGE = "lineWidthCurrEdge";
	/** configuration key for the {@link #lineWidthVertexMinDist} */
	private static final String CFGKEY_LINEWIDTH_VERTEXMINDIST = "lineWidthVertexMinDist";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langDijkstra.txt"));
			// include the language file of the host to only use one language file
			this.langFile.include(host.getLanguageFile());
		} catch (IOException e) {
			this.langFile = null;
		}
		this.langID = host.getLanguageID();
		
		// create plugin
		this.host = host;
		this.config = (config != null) ? config : new Configuration();
		this.vgfFileFilter = new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf");
		this.pngFileFilter = new FileNameExtensionFilter("Portable Network Graphic (*.png)", "png");
		this.graphView = new DefaultGraphView(LanguageFile.getLabel(langFile, "VIEW_GRAPH_TITLE", langID, "Graph"), new SimpleGraph<>(false), null, true, langFile, langID);
		this.execTableView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_EXECTABLE_TITLE", langID, "Execution Table"), true, langFile, langID);
		this.setsView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_SETS_TITLE", langID, "Set Overview"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new DijkstraRTE();
		this.matrixToGraph = new MatrixToGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.BOTH, langFile, langID, true);
		this.randomGraph = new RandomGraphToolBarExtension<>(host, graphView, AllowedGraphType.BOTH, langFile, langID, false);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, false);
		this.creatorPrefsDirected = LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED", langID, "directed");
		this.creatorPrefsUndirected = LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED", langID, "undirected");
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		execTableView.setAutoRepaint(true);
		setsView.setAutoRepaint(true);
		
		// load the creator preference data from the configuration
		creatorPrefsDirectedValue = this.config.getBoolean(CFGKEY_CREATORPROP_DIRECTED, false);
		
		// load the visualization colors from the configuration of the plugin
		colorSetA = this.config.getColor(CFGKEY_COLOR_SETA, new Color(180, 210, 230));
		colorSetB = this.config.getColor(CFGKEY_COLOR_SETB, new Color(255, 220, 80));
		colorSetC = this.config.getColor(CFGKEY_COLOR_SETC, Color.white);
		colorVertexMinDist = this.config.getColor(CFGKEY_COLOR_VERTEXMINDIST, new Color(120, 210, 80));
		colorSetHighlight = this.config.getColor(CFGKEY_COLOR_SETHIGHLIGHT, new Color(225, 235, 240));
		colorSetCurrElemHighlight = this.config.getColor(CFGKEY_COLOR_SETCURRELEMHIGHLIGHT, new Color(195, 210, 225));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		lineWidthStartVertex = this.config.getInt(CFGKEY_LINEWIDTH_STARTVERTEX, 2);
		lineWidthVertexMinDist = this.config.getInt(CFGKEY_LINEWIDTH_VERTEXMINDIST, 2);
		lineWidthCurrEdge = this.config.getInt(CFGKEY_LINEWIDTH_CURREDGE, 2);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		execTableView.loadConfiguration(config, "execTableView");
		setsView.loadConfiguration(config, "setsView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Dijkstra's Algorithm");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds the shortest path between a starting vertex and another (or every other) vertex.");
	}
	
	@Override
	public String getType() {
		return LanguageFile.getLabel(langFile, "ALGO_TYPE", langID, "Exact algorithm");
	}
	
	@Override
	public String getAuthor() {
		return "Jan Dornseifer";
	}
	
	@Override
	public String getAuthorContact() {
		return "jan.dornseifer@student.uni-siegen.de";
	}
	
	@Override
	public String getAssumptions() {
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A simple, non-negative weighted graph G = (V, E) and a starting vertex v<sub>1</sub>.");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Shortest path problem");
	}

	@Override
	public String getSubject() {
		return LanguageFile.getLabel(langFile, "ALGO_SUBJECT", langID, "Logistics");
	}
	
	@Override
	public String getInstructions() {
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extension to create a graph by use of an adjacency matrix.<br><br><b>Starting the algorithm</b>:<br>Before you start the algorithm select a vertex v<sub>1</sub> the algorithm should begin with.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
	}

	@Override
	public String getVersion() {
		return "1.4";
	}

	@Override
	public LAVESDKV getUsedSDKVersion() {
		return new LAVESDKV(1, 4);
	}

	@Override
	public AlgorithmRTE getRuntimeEnvironment() {
		return rte;
	}
	
	@Override
	public AlgorithmText getText() {
		return algoText.getBaseCopy();
	}

	@Override
	public Configuration getConfiguration() {
		return config;
	}

	@Override
	public boolean hasExerciseMode() {
		return true;
	}

	@Override
	public boolean hasCreatorPreferences() {
		return true;
	}

	@Override
	public void loadCreatorPreferences(PropertiesListModel plm) {
		final BooleanPropertyGroup group = new BooleanPropertyGroup(plm);
		plm.add(new BooleanProperty(creatorPrefsDirected, LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED_DESC", langID, "Apply algorithm to a directed graph"), creatorPrefsDirectedValue, group));
		plm.add(new BooleanProperty(creatorPrefsUndirected, LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED_DESC", langID, "Apply algorithm to an undirected graph"), !creatorPrefsDirectedValue, group));
	}

	@Override
	public void onCreate(ViewContainer container, PropertiesListModel creatorProperties) {
		creatorPrefsDirectedValue = (creatorProperties != null) ? creatorProperties.getBooleanProperty(creatorPrefsDirected).getValue() : false;
		
		// update the configuration
		config.addBoolean(CFGKEY_CREATORPROP_DIRECTED, creatorPrefsDirectedValue);
		
		// change the graph in the view
		graphView.setGraph(new SimpleGraph<Vertex, Edge>(creatorPrefsDirectedValue));
		graphView.repaint();
		// change the graph type the user can create with the toolbar extension
		matrixToGraph.setAllowedGraphType(creatorPrefsDirectedValue ? AllowedGraphType.DIRECTED_ONLY : AllowedGraphType.UNDIRECTED_ONLY);
		randomGraph.setAllowedGraphType(creatorPrefsDirectedValue ? AllowedGraphType.DIRECTED_ONLY : AllowedGraphType.UNDIRECTED_ONLY);
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|/////|///
		 * /A/|/   /|/D/	A = algorithm text view
		 * ///|/   /|///	B = legend view
		 * ---|/ C /|---	C = graph view
		 * ///|/   /|///	D = execution table view
		 * /B/|/   /|/E/	E = text area view (set view)
		 * ///|/////|///
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		de = new ViewGroup(ViewGroup.VERTICAL);
		abcde = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// right group for D and E
		de.add(execTableView);
		de.add(setsView);
		de.restoreWeights(config, "weights_de", new float[] { 0.7f, 0.3f });
		
		// group for (A,B), C and (D,E)
		abcde.add(ab);
		abcde.add(graphView);
		abcde.add(de);
		abcde.restoreWeights(config, "weights_abcde", new float[] { 0.4f, 0.4f, 0.2f });
		
		container.setLayout(new BorderLayout());
		container.add(abcde, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		execTableView.saveConfiguration(config, "execTableView");
		setsView.saveConfiguration(config, "setsView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(de != null)
			de.storeWeights(config, "weights_de");
		if(abcde != null)
			abcde.storeWeights(config, "weights_abcde");
		
		// reset view content where it is necessary
		graphView.reset();
		execTableView.reset();
		setsView.reset();
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETA, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SETA", langID, "Background color of the vertices of set A"), colorSetA));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETB, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SETB", langID, "Background color of the vertices of set B"), colorSetB));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETC, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SETC", langID, "Background color of the vertices of set C"), colorSetC));
		plm.add(new ColorProperty(CFGKEY_COLOR_VERTEXMINDIST, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MINDISTVERTEX", langID, "Background color of the vertex v<sub>a</sub>"), colorVertexMinDist));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETHIGHLIGHT, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURRSETELEMS", langID, "Background color of the vertices in the execution table that are under investigation"), colorSetHighlight));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETCURRELEMHIGHLIGHT, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURRVERTEX", langID, "Background color of the vertex in the execution table that is currently investigated"), colorSetCurrElemHighlight));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwStartVertex = new NumericProperty(CFGKEY_LINEWIDTH_STARTVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_STARTVERTEX", langID, "Line width of the starting vertex"), lineWidthStartVertex, true);
		lwStartVertex.setMinimum(1);
		lwStartVertex.setMaximum(5);
		plm.add(lwStartVertex);
		final NumericProperty lwCurrEdge = new NumericProperty(CFGKEY_LINEWIDTH_CURREDGE, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_CURREDGE", langID, "Line with of the edge that is currently investigated"), lineWidthCurrEdge, true);
		lwCurrEdge.setMinimum(1);
		lwCurrEdge.setMaximum(5);
		plm.add(lwCurrEdge);
		final NumericProperty lwVertexMinDist = new NumericProperty(CFGKEY_LINEWIDTH_VERTEXMINDIST, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_VERTEXMINDIST", langID, "Line with of the vertex v<sub>a</sub>"), lineWidthVertexMinDist, true);
		lwVertexMinDist.setMinimum(1);
		lwVertexMinDist.setMaximum(5);
		plm.add(lwVertexMinDist);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorSetA = config.addColor(CFGKEY_COLOR_SETA, plm.getColorProperty(CFGKEY_COLOR_SETA).getValue());
		colorSetB = config.addColor(CFGKEY_COLOR_SETB, plm.getColorProperty(CFGKEY_COLOR_SETB).getValue());
		colorSetC = config.addColor(CFGKEY_COLOR_SETC, plm.getColorProperty(CFGKEY_COLOR_SETC).getValue());
		colorVertexMinDist = config.addColor(CFGKEY_COLOR_VERTEXMINDIST, plm.getColorProperty(CFGKEY_COLOR_VERTEXMINDIST).getValue());
		colorSetHighlight = config.addColor(CFGKEY_COLOR_SETHIGHLIGHT, plm.getColorProperty(CFGKEY_COLOR_SETHIGHLIGHT).getValue());
		colorSetCurrElemHighlight = config.addColor(CFGKEY_COLOR_SETCURRELEMHIGHLIGHT, plm.getColorProperty(CFGKEY_COLOR_SETCURRELEMHIGHLIGHT).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthStartVertex = config.addInt(CFGKEY_LINEWIDTH_STARTVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_STARTVERTEX).getValue().intValue());
		lineWidthVertexMinDist = config.addInt(CFGKEY_LINEWIDTH_VERTEXMINDIST, plm.getNumericProperty(CFGKEY_LINEWIDTH_VERTEXMINDIST).getValue().intValue());
		lineWidthCurrEdge = config.addInt(CFGKEY_LINEWIDTH_CURREDGE, plm.getNumericProperty(CFGKEY_LINEWIDTH_CURREDGE).getValue().intValue());
		
		// recreate the legend
		createLegend();
	}
	
	@Override
	public ToolBarExtension[] getToolBarExtensions() {
		return new ToolBarExtension[] { matrixToGraph, randomGraph, circleLayoutExt };
	}

	@Override
	public void save(File file) {
		try {
			if(vgfFileFilter.accept(file))
				graphView.save(file);
			else if(pngFileFilter.accept(file))
				graphView.saveAsPNG(file);
		}
		catch(IOException e) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_ERROR_SAVEFILE", langID, "File could not be saved!") + "\n\n" + e.getMessage(), LanguageFile.getLabel(langFile, "MSG_ERROR_SAVEFILE_TITLE", langID, "Save File"), MessageIcon.ERROR);
		}
	}

	@Override
	public void open(File file) {
		try {
			if(vgfFileFilter.accept(file))
				graphView.load(file);
		}
		catch(IOException e) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_ERROR_OPENFILE", langID, "File could not be opened!") + "\n\n" + e.getMessage(), LanguageFile.getLabel(langFile, "MSG_ERROR_OPENFILE_TITLE", langID, "Open File"), MessageIcon.ERROR);
		}
	}

	@Override
	public FileNameExtensionFilter[] getSaveFileFilters() {
		return new FileNameExtensionFilter[] { vgfFileFilter, pngFileFilter };
	}

	@Override
	public FileNameExtensionFilter[] getOpenFileFilters() {
		return new FileNameExtensionFilter[] { vgfFileFilter };
	}

	@Override
	public void beforeStart(RTEvent e) {
		ExecutionTableColumn column;
		
		// the algorithm needs a starting vertex as input and the graph may not have negative weights
		if(graphView.getSelectedVertexCount() != 1) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTVERTEX", langID, "Please select the starting vertex in the graph!"), LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTVERTEX_TITLE", langID, "Select starting vertex"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(containsGraphNegativeWeights()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS", langID, "The created graph contains edges with a negative weight!\nDijkstra's algorithm can only be applied to non-negative weighted graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS_TITLE", langID, "Negative weights"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			// get the starting vertex
			final Vertex startVertex = graphView.getSelectedVertex(0).getVertex();
			
			graphView.deselectAll();
			graphView.setEditable(false);
			
			// set the start vertex (after disable the edit mode) because the start mode should be visualized
			rte.setStartVertex(startVertex);
			
			// initialize the execution table by creating columns and groups
			execTableView.reset();
			
			// first column displays only d(v) or p(v)
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_EXECTABLE_FIRSTCOL", langID, "Vertices:"), 0);
			column.addMask(new Mask(1.0f, "d(v)"));
			execTableView.add(column);
			
			// create the columns for all vertices add masks to display the infinity symbol instead of the float constant
			// and make this columns editable (for exercise mode)
			column = new ExecutionTableColumn(startVertex.getCaption(), startVertex.getID());
			column.addMask(new Mask(Float.POSITIVE_INFINITY, Symbol.getPredefinedSymbol(PredefinedSymbol.INFINITY)));
			column.setEditable(true);
			execTableView.add(column);
			for(int i = 0; i < graphView.getVisualVertexCount(); i++) {
				if(graphView.getVisualVertex(i).getVertex() != startVertex) {
					column = new ExecutionTableColumn(graphView.getVisualVertex(i).getVertex().getCaption(), graphView.getVisualVertex(i).getVertex().getID());
					column.addMask(new Mask(Float.POSITIVE_INFINITY, Symbol.getPredefinedSymbol(PredefinedSymbol.INFINITY)));
					column.setEditable(true);
					execTableView.add(column);
				}
			}
			
			// create the groups to separate items and columns for a better look and feel
			execTableView.addColumnGroup(new ExecutionTableGroup(new ExecutionTableBorder(2, Color.black), 0));
			execTableView.addItemGroup(new ExecutionTableGroup(new ExecutionTableBorder(2, Color.black), 0, 2, true));
			
			// clear the view that displays the sets
			setsView.reset();
			
			// if we start the algorithm in exercise mode then add a final exercise with random vertices
			if(rte.isExerciseModeEnabled()) {
				// get a random target vertex except the starting vertex
				final Random rnd = new Random();
				final Set<Vertex> vertices = graphView.getGraph().getVertexSet();
				vertices.remove(startVertex);
				final Vertex rndTargetVertex = vertices.get(rnd.nextInt(vertices.size()));
				
				String finalExerciseText = LanguageFile.getLabel(langFile, "FINAL_EXERCISE", langID, "What is the path from vertex &v_1& to vertex &v_i& and how long is the path?");
				finalExerciseText = finalExerciseText.replaceAll("&v_1&", startVertex.getCaption());
				finalExerciseText = finalExerciseText.replaceAll("&v_i&", rndTargetVertex.getCaption());
				algoText.setFinalExercise(new AlgorithmExercise<Object>(finalExerciseText, 3.0f) {
					
					@Override
					protected Object[] requestSolution() {
						final SolutionEntry<JTextField> entryPath = new SolutionEntry<JTextField>(LanguageFile.getLabel(langFile, "FINAL_EXERCISE_PATH", langID, "Path ="), new JTextField());
						final SolutionEntry<NumericTextField> entryLength = new SolutionEntry<NumericTextField>(LanguageFile.getLabel(langFile, "FINAL_EXERCISE_LENGTH", langID, "Length ="), new NumericTextField());
						
						if(!SolveExercisePane.showDialog(DijkstraAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryPath,  entryLength }, DijkstraAlgorithmPlugin.this.langFile, DijkstraAlgorithmPlugin.this.langID, LanguageFile.getLabel(DijkstraAlgorithmPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", DijkstraAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
							return null;
						
						final Walk<Vertex> w = GraphUtils.toWalk(entryPath.getComponent().getText(), DijkstraAlgorithmPlugin.this.graphView.getGraph());
						Number length = null;
						try {
							length = NumberFormat.getInstance().parse(entryLength.getComponent().getText());
						} catch (ParseException e) {
							length = null;
						}
						
						return new Object[] { w, length };
					}
					
					@Override
					protected boolean examine(Object[] results, AlgorithmState state) {
						@SuppressWarnings("unchecked")
						final Walk<Vertex> w = (Walk<Vertex>)results[0];
						final Number length = (Number)results[1];
						final Map<Integer, Float> d = state.getMap("d");
						final Map<Integer, String> p = state.getMap("p");
						
						if(w == null || length == null)
							return false;
						else if(length.floatValue() != d.get(rndTargetVertex.getID()).floatValue())
							return false;
						
						final Graph<Vertex, Edge> graph = DijkstraAlgorithmPlugin.this.graphView.getGraph();
						final Walk<Vertex> idealWalk = new Walk<Vertex>(graph);
						Vertex v = rndTargetVertex;
						
						while(v != null) {
							idealWalk.add(0, v);
							v = graph.getVertexByCaption(p.get(v.getID()));
						}
						
						return w.equals(idealWalk);
					}
				});
			}
		}
	}

	@Override
	public void beforeResume(RTEvent e) {
	}

	@Override
	public void beforePause(RTEvent e) {
	}

	@Override
	public void onStop() {
		graphView.setEditable(true);
		
		// remove a possible final exercise
		algoText.setFinalExercise(null);
	}

	@Override
	public void onRunning() {
	}
	
	@Override
	public void onPause() {
	}
	
	/**
	 * Creates the algorithm text.
	 * 
	 * @return the algorithm text
	 * @since 1.0
	 */
	private AlgorithmText loadAlgorithmText() {
		AlgorithmStep step;
		
		final AlgorithmText text = new AlgorithmText();
		
		// create paragraphs
		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_INITIALIZATION", langID, "1. Initialization:"), 1);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOPCRITERION", langID, "2. Stop criterion:"), 2);
		final AlgorithmParagraph itParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ITERATION", langID, "3. Iteration:"), 3);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INITSETS", langID, "Set _latex{$A := \\{v_1\\}$}, _latex{$B := \\{v_i \\in V | (v_1,v_i) \\in E \\}$} and _latex{$C := V \\setminus (A \\cup B)$}.\n"), 1);
		step.setExercise(new AlgorithmExercise<Set<?>>(LanguageFile.getLabel(langFile, "EXERCISE_SETP1", langID, "Specify the sets A, B and C."), 3.0f) {

			@Override
			protected Set<?>[] requestSolution() {
				final SolutionEntry<JTextField> entryA = new SolutionEntry<JTextField>("A=", new JTextField());
				final SolutionEntry<JTextField> entryB = new SolutionEntry<JTextField>("B=", new JTextField());
				final SolutionEntry<JTextField> entryC = new SolutionEntry<JTextField>("C=", new JTextField());
				
				if(!SolveExercisePane.showDialog(DijkstraAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryA,  entryB,  entryC }, DijkstraAlgorithmPlugin.this.langFile, DijkstraAlgorithmPlugin.this.langID, LanguageFile.getLabel(DijkstraAlgorithmPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", DijkstraAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				
				final ElementParser<String> parser = new StringElementParser();
				final Set<String> A = Set.parse(entryA.getComponent().getText(), parser);
				final Set<String> B = Set.parse(entryB.getComponent().getText(), parser);
				final Set<String> C = Set.parse(entryC.getComponent().getText(), parser);
				
				return new Set<?>[] { A, B, C };
			}
			
			@Override
			protected String getResultAsString(Set<?> result, int index) {
				String setName = "";
				
				switch(index) {
					case 0:
						setName = "A=";
						break;
					case 1:
						setName = "B=";
						break;
					case 2:
						setName = "C=";
						break;
				}
				
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return setName + super.getResultAsString(result, index);
			}

			@Override
			protected boolean examine(Set<?>[] results, AlgorithmState state) {
				// convert the input sets to sets with the identifiers of the vertices to use auto examination
				final Set<Integer> A = DijkstraAlgorithmPlugin.this.toIDs(results[0]);
				final Set<Integer> B = DijkstraAlgorithmPlugin.this.toIDs(results[1]);
				final Set<Integer> C = DijkstraAlgorithmPlugin.this.toIDs(results[2]);
				
				return doAutoExamine(state, new String[] { "A",  "B", "C" }, new Set<?>[] { A, B, C });
			}
		});
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP2_INITD", langID, "Set _latex{$d(v_1) := 0$}, _latex{$d(v_i) := c(v_1,v_i) \\; \\forall v_i \\in B$} and _latex{$d(v_i) := \\infty \\; \\forall v_i \\in C$}.\n"), 2);
		step.setExercise(new AlgorithmExercise<Map<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Specify d(v) in the execution table (<i>use \"-\" as infinity</i>)."), 3.0f, execTableView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				final ExecutionTableItem item = new ExecutionTableItem(new String[] { "d(v)" });
				item.setDefaultInputParser(new ExecutionTableItem.NumericInputParser());
				item.setEditable(true);
				item.setBorder(new ExecutionTableBorder(2, SystemColor.textHighlight));
				DijkstraAlgorithmPlugin.this.execTableView.add(item);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
			}
			
			@Override
			protected Map<?, ?>[] requestSolution() {
				final ExecutionTableItem item = DijkstraAlgorithmPlugin.this.execTableView.getLastItem();
				final Map<Integer, Float> d = new HashMap<Integer, Float>();
				d.put(0, 1.0f);
				
				for(int i = 1; i < DijkstraAlgorithmPlugin.this.execTableView.getColumnCount(); i++) {
					final Object o = item.getCellObject(i);
					d.put(DijkstraAlgorithmPlugin.this.execTableView.getColumn(i).getID(), (o != null) ? ((Number)o).floatValue() : 0.0f);
				}
				
				return new Map<?, ?>[] { d };
			}
			
			@Override
			protected String getResultAsString(Map<?, ?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					final Iterator<?> it = result.keySet().iterator();
					final StringBuilder res = new StringBuilder();
					GraphView<? extends Vertex, ? extends Edge>.VisualVertex vv;
					int c = 0;
					
					while(it.hasNext()) {
						final Integer id = (Integer)it.next();
						// the mask should not be in the result string
						if(id.intValue() == 0)
							continue;
						
						vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(id);
						
						if(vv != null) {
							if(c > 0)
								res.append(", ");
							res.append("(" + vv.getVertex().getCaption() + ", " + MathUtils.formatFloat((Float)result.get(id)) + ")");
						}
						
						c++;
					}
					
					return "d(v)=[" + res.toString() + "]";
				}
			}
			
			@Override
			protected boolean examine(Map<?, ?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "d" }, results);
			}
		});
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP3_INITP", langID, "Set _latex{$p(v_i) := v_1 \\; \\forall v_i \\in B$}.\n\n"), 3);
		step.setExercise(new AlgorithmExercise<Map<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Specify p(v) in the execution table."), 2.0f, execTableView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				final ExecutionTableItem item = new ExecutionTableItem(new String[] { "p(v)" });
				item.setDefaultInputParser(new ExecutionTableItem.StringInputParser());
				item.setEditable(true);
				item.setBorder(new ExecutionTableBorder(2, SystemColor.textHighlight));
				DijkstraAlgorithmPlugin.this.execTableView.add(item);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
			}
			
			@Override
			protected Map<?, ?>[] requestSolution() {
				final ExecutionTableItem item = DijkstraAlgorithmPlugin.this.execTableView.getLastItem();
				final Map<Integer, String> p = new HashMap<Integer, String>();
				p.put(0, "p(v)");
				
				for(int i = 1; i < DijkstraAlgorithmPlugin.this.execTableView.getColumnCount(); i++) {
					final Object o = item.getCellObject(i);
					p.put(DijkstraAlgorithmPlugin.this.execTableView.getColumn(i).getID(), (o != null && !o.toString().isEmpty()) ? o.toString() : null);
				}
				
				return new Map<?, ?>[] { p };
			}
			
			@Override
			protected String getResultAsString(Map<?, ?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					final Iterator<?> it = result.keySet().iterator();
					final StringBuilder res = new StringBuilder();
					GraphView<? extends Vertex, ? extends Edge>.VisualVertex vv;
					int c = 0;
					Object o;
					
					while(it.hasNext()) {
						final Integer id = (Integer)it.next();
						// the mask should not be in the result string
						if(id.intValue() == 0)
							continue;
						
						vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(id);
						
						if(vv != null) {
							o = result.get(id);
							if(o == null)
								o = "";
							
							if(c > 0)
								res.append(", ");
							res.append("(" + vv.getVertex().getCaption() + ", " + o + ")");
						}
						
						c++;
					}
					
					return "p(v)=[" + res.toString() + "]";
				}
			}

			@Override
			protected boolean examine(Map<?, ?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "p" }, results);
			}
		});
		
		// 2. stop criterion
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP4_STOP", langID, "If _latex{$A = V$} or _latex{$B = \\emptyset$} then stop. Otherwise go to step 3.\n\n"), 4);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "Does the algorithm terminate or does he resume with 3.?"), 1.0f) {
			
			private final String labelTerminate = LanguageFile.getLabel(langFile, "EXERCISE_STEP4_OPTIONTERMINATE", langID, "Algorithm terminates");
			private final String labelResume = LanguageFile.getLabel(langFile, "EXERCISE_STEP4_OPTIONRESUME", langID, "Algorithm resumes with step 3");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelTerminate);
				final JRadioButton rdobtn2 = new JRadioButton(labelResume);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryFinish = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryResume = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(DijkstraAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryFinish,  entryResume }, DijkstraAlgorithmPlugin.this.langFile, DijkstraAlgorithmPlugin.this.langID))
					return null;
				
				return new Boolean[] { (!rdobtn1.isSelected() && !rdobtn2.isSelected()) ? null : rdobtn1.isSelected() };
			}
			
			@Override
			protected String getResultAsString(Boolean result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return (result == Boolean.TRUE) ? labelTerminate : labelResume;
			}
			
			@Override
			protected boolean examine(Boolean[] results, AlgorithmState state) {
				final Set<Integer> A = state.getSet("A");
				final Set<Integer> V = DijkstraAlgorithmPlugin.this.graphView.getGraph().getVertexByIDSet();
				
				return results[0] != null && results[0] == A.equals(V);
			}
		});
		
		// 3. iteration
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP5_DETERMINEVERTEXMINDIST", langID, "Determine _latex{$v_a \\in \\; \\underset{v_i \\in B}{argmin} \\; d(v_i)$} "), 5);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP5", langID, "Choose the vertex in the graph with a minimum value in d(v)."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				DijkstraAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				DijkstraAlgorithmPlugin.this.graphView.deselectAll();
				DijkstraAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(DijkstraAlgorithmPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { DijkstraAlgorithmPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "v_a" }, results);
			}
		});
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP6_UPDATESETS", langID, "and set _latex{$A = A \\cup \\{v_a\\}$} and _latex{$B = B \\setminus \\{v_a\\}$}.\n"), 6);
		step.setExercise(new AlgorithmExercise<Set<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP6", langID, "What are the sets A and B?"), 2.0f) {
			
			@Override
			protected Set<?>[] requestSolution() {
				final SolutionEntry<JTextField> entryA = new SolutionEntry<JTextField>("A=", new JTextField());
				final SolutionEntry<JTextField> entryB = new SolutionEntry<JTextField>("B=", new JTextField());
				
				if(!SolveExercisePane.showDialog(DijkstraAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryA,  entryB }, DijkstraAlgorithmPlugin.this.langFile, DijkstraAlgorithmPlugin.this.langID, LanguageFile.getLabel(DijkstraAlgorithmPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", DijkstraAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				
				final ElementParser<String> parser = new StringElementParser();
				final Set<String> A = Set.parse(entryA.getComponent().getText(), parser);
				final Set<String> B = Set.parse(entryB.getComponent().getText(), parser);
				
				return new Set<?>[] { A, B };
			}
			
			@Override
			protected String getResultAsString(Set<?> result, int index) {
				String setName = "";
				
				switch(index) {
					case 0:
						setName = "A=";
						break;
					case 1:
						setName = "B=";
						break;
				}
				
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return setName + super.getResultAsString(result, index);
			}
			
			@Override
			protected boolean examine(Set<?>[] results, AlgorithmState state) {
				// convert the input sets to sets with the identifiers of the vertices to use auto examination
				final Set<Integer> A = DijkstraAlgorithmPlugin.this.toIDs(results[0]);
				final Set<Integer> B = DijkstraAlgorithmPlugin.this.toIDs(results[1]);
				
				return doAutoExamine(state, new String[] { "A",  "B" }, new Set<?>[] { A, B });
			}
		});
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP7_UPDATEDANDPFORALLB", langID, "For all _latex{$v_i \\in B$} with _latex{$(v_a,v_i) \\in E$}:\nIf _latex{$d(v_a) + c(v_a,v_i) < d(v_i)$} then _latex{$d(v_i) = d(v_a) + c(v_a,v_i)$} and _latex{$p(v_i) = v_a$}\n"), 7);
		step.setExercise(new AlgorithmExercise<Map<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP7", langID, "What are d(v) and p(v) (<i>use \"-\" as infinity</i>)?"), 5.0f, execTableView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				final Map<Integer, Float> d = state.getMap("d");
				final Map<Integer, String> p = state.getMap("p");
				final ExecutionTableItem itemD = new ExecutionTableItem(d, true);
				final ExecutionTableItem itemP = new ExecutionTableItem(p, true);
				itemD.setDefaultInputParser(new ExecutionTableItem.NumericInputParser());
				itemP.setDefaultInputParser(new ExecutionTableItem.StringInputParser());
				itemD.setEditable(true);
				itemP.setEditable(true);
				itemD.setBorder(new ExecutionTableBorder(2, SystemColor.textHighlight));
				itemP.setBorder(new ExecutionTableBorder(2, SystemColor.textHighlight));
				DijkstraAlgorithmPlugin.this.execTableView.add(itemD);
				DijkstraAlgorithmPlugin.this.execTableView.add(itemP);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
				DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
			}
			
			@Override
			protected Map<?, ?>[] requestSolution() {
				final ExecutionTableItem itemD = DijkstraAlgorithmPlugin.this.execTableView.getItem(DijkstraAlgorithmPlugin.this.execTableView.getItemCount() - 2);
				final Map<Integer, Float> d = new HashMap<Integer, Float>();
				d.put(0, 1.0f);
				
				for(int i = 1; i < DijkstraAlgorithmPlugin.this.execTableView.getColumnCount(); i++) {
					final Object o = itemD.getCellObject(i);
					d.put(DijkstraAlgorithmPlugin.this.execTableView.getColumn(i).getID(), (o != null) ? ((Number)o).floatValue() : 0.0f);
				}
				
				final ExecutionTableItem itemP = DijkstraAlgorithmPlugin.this.execTableView.getLastItem();
				final Map<Integer, String> p = new HashMap<Integer, String>();
				p.put(0, "p(v)");
				
				for(int i = 1; i < DijkstraAlgorithmPlugin.this.execTableView.getColumnCount(); i++) {
					final Object o = itemP.getCellObject(i);
					p.put(DijkstraAlgorithmPlugin.this.execTableView.getColumn(i).getID(), (o != null && !o.toString().isEmpty()) ? o.toString() : null);
				}
				
				return new Map<?, ?>[] { d, p };
			}
			
			@Override
			protected String getResultAsString(Map<?, ?> result, int index) {
				String pre = "";
				
				switch(index) {
					case 0:
						pre = "d(v)";
						break;
					case 1:
						pre = "p(v)";
						break;
				}
				
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					final Iterator<?> it = result.keySet().iterator();
					final StringBuilder res = new StringBuilder();
					GraphView<? extends Vertex, ? extends Edge>.VisualVertex vv;
					int c = 0;
					Object o;
					
					while(it.hasNext()) {
						final Integer id = (Integer)it.next();
						// the mask should not be in the result string
						if(id.intValue() == 0)
							continue;
						
						vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(id);
						
						if(vv != null) {
							o = result.get(id);
							if(o == null)
								o = "";
							else
								o = (index == 0) ? MathUtils.formatFloat((Float)o) : o;
							
							if(c > 0)
								res.append(", ");
							res.append("(" + vv.getVertex().getCaption() + ", " + o + ")");
						}
						
						c++;
					}
					
					return pre + "=[" + res.toString() + "]";
				}
			}
			
			@Override
			protected boolean examine(Map<?, ?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "d", "p" }, results);
			}
		});
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_SETP8_UPDATEDANDPFORALLC", langID, "For all _latex{$v_j \\in C$} with _latex{$(v_a,v_j) \\in E$}:\nSet _latex{$d(v_j) = d(v_a) + c(v_a,v_j)$}, _latex{$p(v_j) = v_a$}, _latex{$B = B \\cup \\{v_j\\}$} and _latex{$C = C \\setminus \\{v_j\\}$}.\n"), 8);
		step.setExercise(new AlgorithmExercise<Map<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP8", langID, "What are d(v) and p(v) (<i>use \"-\" as infinity</i>)?"), 5.0f, execTableView) {
			
			private ExecutionTableItem itemD;
			private ExecutionTableItem itemP;
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				itemD = DijkstraAlgorithmPlugin.this.execTableView.getItem(DijkstraAlgorithmPlugin.this.execTableView.getItemCount() - 2);
				itemP = DijkstraAlgorithmPlugin.this.execTableView.getLastItem();
				itemD.setDefaultInputParser(new ExecutionTableItem.NumericInputParser());
				itemP.setDefaultInputParser(new ExecutionTableItem.StringInputParser());
				itemD.setEditable(true);
				itemP.setEditable(true);
				itemD.setBorder(new ExecutionTableBorder(2, SystemColor.textHighlight));
				itemP.setBorder(new ExecutionTableBorder(2, SystemColor.textHighlight));
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				itemD.setEditable(false);
				itemP.setEditable(false);
				itemD.setBorder(null);
				itemP.setBorder(null);
			}
			
			@Override
			protected Map<?, ?>[] requestSolution() {
				final Map<Integer, Float> d = new HashMap<Integer, Float>();
				d.put(0, 1.0f);
				
				for(int i = 1; i < DijkstraAlgorithmPlugin.this.execTableView.getColumnCount(); i++) {
					final Object o = itemD.getCellObject(i);
					d.put(DijkstraAlgorithmPlugin.this.execTableView.getColumn(i).getID(), (o != null) ? ((Number)o).floatValue() : 0.0f);
				}
				
				final Map<Integer, String> p = new HashMap<Integer, String>();
				p.put(0, "p(v)");
				
				for(int i = 1; i < DijkstraAlgorithmPlugin.this.execTableView.getColumnCount(); i++) {
					final Object o = itemP.getCellObject(i);
					p.put(DijkstraAlgorithmPlugin.this.execTableView.getColumn(i).getID(), (o != null && !o.toString().isEmpty()) ? o.toString() : null);
				}
				
				return new Map<?, ?>[] { d, p };
			}
			
			@Override
			protected String getResultAsString(Map<?, ?> result, int index) {
				String pre = "";
				
				switch(index) {
					case 0:
						pre = "d(v)";
						break;
					case 1:
						pre = "p(v)";
						break;
				}
				
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					final Iterator<?> it = result.keySet().iterator();
					final StringBuilder res = new StringBuilder();
					GraphView<? extends Vertex, ? extends Edge>.VisualVertex vv;
					int c = 0;
					Object o;
					
					while(it.hasNext()) {
						final Integer id = (Integer)it.next();
						// the mask should not be in the result string
						if(id.intValue() == 0)
							continue;
						
						vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(id);
						
						if(vv != null) {
							o = result.get(id);
							if(o == null)
								o = "";
							else
								o = (index == 0) ? MathUtils.formatFloat((Float)o) : o;
							
							if(c > 0)
								res.append(", ");
							res.append("(" + vv.getVertex().getCaption() + ", " + o + ")");
						}
						
						c++;
					}
					
					return pre + "=[" + res.toString() + "]";
				}
			}
			
			@Override
			protected boolean examine(Map<?, ?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "d", "p" }, results);
			}
		});
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP9_GOTO", langID, "Go to step 2."), 9);
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_STARTVERTEX", langID, "starting vertex v<sub>1</sub>"), LegendItem.createCircleIcon(colorSetA, Color.black, lineWidthStartVertex)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SETA", langID, "Set A of vertices where a shortest path is known"), LegendItem.createCircleIcon(colorSetA, Color.black, 1)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SETB", langID, "Set B of vertices that are not contained in A but connected with a vertex of set A"), LegendItem.createCircleIcon(colorSetB, Color.black, 1)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SETC", langID, "Set C of vertices that are not in set A and B meaning C = V \\ (A &cup; B)"), LegendItem.createCircleIcon(colorSetC, Color.black, 1)));
		legendView.add(new LegendItem("item5", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_MINDISTVERTEX", langID, "The current vertex v<sub>a</sub>"), LegendItem.createCircleIcon(colorVertexMinDist, Color.black, 1)));
		legendView.add(new LegendItem("item6", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURREDGE", langID, "The current edge that is under investigation"), LegendItem.createLineIcon(colorModified, lineWidthCurrEdge)));
		legendView.add(new LegendItem("item7", execTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_EXECTABLE_CURRSETELEMS", langID, "Set of vertices that are under investigation"), LegendItem.createRectangleIcon(colorSetHighlight, colorSetHighlight, 1)));
		legendView.add(new LegendItem("item8", execTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_EXECTABLE_CURRVERTEX", langID, "The current vertex that is under investigation"), LegendItem.createRectangleIcon(colorSetCurrElemHighlight, colorSetCurrElemHighlight, 1)));
		legendView.add(new LegendItem("item9", execTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_EXECTABLE_CURRMINDISTVERTEX", langID, "The vertex of the investigated set with a current minimum value in d(v)"), LegendItem.createRectangleIcon(Color.white, colorVertexMinDist, lineWidthVertexMinDist)));
		legendView.add(new LegendItem("item10", execTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_EXECTABLE_MINDISTVERTEX", langID, "The current vertex v<sub>a</sub>"), LegendItem.createRectangleIcon(colorVertexMinDist, colorVertexMinDist, 1)));
		legendView.add(new LegendItem("item11", execTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_EXECTABLE_MODIFICATIONS", langID, "Changes in d(v) and/or p(v)"), LegendItem.createRectangleIcon(colorModified, colorModified, 1)));
		legendView.add(new LegendItem("item12", setsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_SETS_MODIFICATIONS", langID, "Changes in the sets A, B or C"), LegendItem.createRectangleIcon(colorModified, colorModified, 1)));
	}
	
	/**
	 * Converts the given set of vertex captions to a set with the identifiers of the vertices.
	 * 
	 * @param set the set
	 * @return the set with the vertex identifiers
	 * @since 1.0
	 */
	private Set<Integer> toIDs(final Set<?> set) {
		final Graph<Vertex, Edge> graph = graphView.getGraph();
		final Set<Integer> res = new Set<Integer>(set.size());
		Vertex v;
		
		for(Object caption : set) {
			v = graph.getVertexByCaption(caption.toString());
			
			// if the user entered an invalid caption then add an invalid id so that the set could not be correct
			if(v == null)
				res.add(-1);
			else
				res.add(v.getID());
		}
		
		return res;
	}
	
	/**
	 * Indicates whether the graph the user has created contains edges with a negative weight.
	 * 
	 * @return <code>true</code> if their are negative weights otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean containsGraphNegativeWeights() {
		final Graph<Vertex, Edge> graph = graphView.getGraph();
		
		for(int i = 0; i < graph.getSize(); i++) {
			if(graph.getEdge(i).getWeight() < 0.0f)
				return true;
		}
		
		return false;
	}
	
	/**
	 * The runtime environment of Dijkstra's algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class DijkstraRTE extends AlgorithmRTE {

		/** the set of the vertices that have a shortest path from the starting vertex to the specific vertex */
		private Set<Integer> A;
		/** the set of vertices with an edge to a vertex of set A */
		private Set<Integer> B;
		/** the set of vertices that are neither in A nor in B */
		private Set<Integer> C;
		/** the distances of the vertices from the starting vertex */
		private Map<Integer, Float> d;
		/** the predecessor of a vertex on the shortest path from the starting vertex to himself */
		private Map<Integer, String> p;
		/** the id of the vertex with the minimal distance */
		private int v_a;
		/** the starting vertex */
		private Vertex v_1;
		
		/**
		 * Creates the runtime environment.
		 * 
		 * @since 1.0
		 */
		public DijkstraRTE() {
			super(DijkstraAlgorithmPlugin.this, DijkstraAlgorithmPlugin.this.algoText);
			
			v_1 = null;
		}
		
		/**
		 * Sets the starting vertex of the algorithm and visualizes the vertex.
		 * 
		 * @param v the vertex
		 * @since 1.0
		 */
		public void setStartVertex(final Vertex v) {
			v_1 = v;
			visualizeVertices();
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) {
			final Graph<Vertex, Edge> graph = graphView.getGraph();
			GraphView<Vertex, Edge>.VisualEdge ve = null;
			int nextStep = -1;
			
			switch(stepID) {
				case 1:
					// create the sets A, B and C
					A.add(v_1.getID());
					
					for(int i = 0; i < v_1.getOutgoingEdgeCount(); i++)
						B.add(v_1.getOutgoingEdge(i).getSuccessor(v_1).getID());
					
					C = Set.complement(graph.getVertexByIDSet(), Set.union(A, B));
					
					// visualize the step meaning display the sets and the appearance in the graph
					sleep(250);
					visualizeSets();
					sleep(500);
					visualizeVertices();
					sleep(1000);
					
					nextStep = 2;
					break;
				case 2:
					// initialize the distances
					d.put(v_1.getID(), 0.0f);
					for(int i : B)
						d.put(i, graph.getEdge(v_1.getID(), i).getWeight());
					for(int j : C)
						d.put(j, Float.POSITIVE_INFINITY);
					
					// only indicates that the first cell should show a "d(v)" (defined by a column mask for column id=0)
					d.put(0, 1.0f);
					
					// visualize the distances using a new item in the execution table that displays the "d(v)"
					DijkstraAlgorithmPlugin.this.execTableView.add(new ExecutionTableItem(d, true));
					sleep(1000);
					
					nextStep = 3;
					break;
				case 3:
					// initialize the paths
					for(int i : B)
						p.put(i, v_1.getCaption());
					
					// for the other vertices we add "null" so that we can roll back p
					p.put(v_1.getID(), null);
					for(int j : C)
						p.put(j, null);
					
					// the first cell should show a "p(v)" (column id=0)
					p.put(0, "p(v)");
					
					// visualize the paths using a new item in the execution table that displays the "p(v)"
					DijkstraAlgorithmPlugin.this.execTableView.add(new ExecutionTableItem(p, true));
					sleep(1000);
					
					nextStep = 4;
					break;
				case 4:
					// check step criterion
					
					sleep(1000);
					
					if(A.equals(graphView.getGraph().getVertexByIDSet()) || B.isEmpty())
						nextStep = -1;
					else
						nextStep = 5;
					break;
				case 5:
					final ExecutionTableItem item = DijkstraAlgorithmPlugin.this.execTableView.getItem(DijkstraAlgorithmPlugin.this.execTableView.getItemCount() - 2);
					float minD = Float.MAX_VALUE;
					v_a = 0;
					
					// highlight the elements of the set B that are used to perform this step
					sleep(250);
					for(int i : B)
						item.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetHighlight);
					
					// find the vertex of set B with the minimal distance
					for(int i : B) {
						// highlight current vertex
						item.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetCurrElemHighlight);
						sleep(500);
						
						if(d.get(i) < minD) {
							// remove the border from the last vertex with a minimal distance
							if(v_a > 0)
								item.setCellBorderByID(v_a, (Color)null);
							
							minD = d.get(i);
							v_a = i;
							
							// mark the vertex as the one that has currently the minimal distance
							item.setCellBorderByID(v_a, DijkstraAlgorithmPlugin.this.colorVertexMinDist, DijkstraAlgorithmPlugin.this.lineWidthVertexMinDist);
							sleep(1000);
						}
						
						// the current vertex is visited so reset the visual appearance
						item.setCellBackgroundByID(i, Color.white);
					}
					
					// if their is no v_a then quit the algorithm
					if(v_a < 1)
						nextStep = -1;
					else {
						// remove the border from the vertex with the minimal distance and highlight him green
						item.setCellBorderByID(v_a, (Color)null);
						item.setCellBackgroundByID(v_a, DijkstraAlgorithmPlugin.this.colorVertexMinDist);
						
						// update the vertex visualization
						visualizeVertices();
						sleep(1000);
						
						// clear the highlight in the table
						item.setCellBackgroundByID(v_a, Color.white);
						
						nextStep = 6;
					}
					break;
				case 6:
					sleep(250);
					
					// highlight the text area to visualize the modification
					DijkstraAlgorithmPlugin.this.setsView.setBackground(DijkstraAlgorithmPlugin.this.colorModified);
					
					// add the vertex with the minimal distance to set A and remove it from set B
					A.add(v_a);
					B.remove(v_a);
					
					// visualize the modification of the sets
					sleep(500);
					visualizeSets();
					visualizeVertices();
					sleep(1000);
					
					// reset the highlight
					DijkstraAlgorithmPlugin.this.setsView.setBackground(Color.white);
					
					nextStep = 7;
					break;
				case 7:
					final ExecutionTableItem itemD = new ExecutionTableItem(d, true);
					final ExecutionTableItem itemP = new ExecutionTableItem(p, true);
					DijkstraAlgorithmPlugin.this.execTableView.add(itemD);
					DijkstraAlgorithmPlugin.this.execTableView.add(itemP);
					
					// highlight the elements of the set B that are used to perform this step
					sleep(250);
					for(int i : B) {
						itemD.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetHighlight);
						itemP.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetHighlight);
					}
					
					// update the distances and paths of the vertices of set B
					for(int i : B) {
						final Edge e = graph.getEdge(v_a, i);
						
						// highlight the current cell
						itemD.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetCurrElemHighlight);
						itemP.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetCurrElemHighlight);
						sleep(500);
						
						if(e != null) {
							// highlight the current edge
							ve = DijkstraAlgorithmPlugin.this.graphView.getVisualEdgeByID(e.getID());
							ve.setColor(DijkstraAlgorithmPlugin.this.colorModified);
							ve.setLineWidth(2);
							DijkstraAlgorithmPlugin.this.graphView.repaint();
							
							if(d.get(v_a) + e.getWeight() < d.get(i)) {
								d.put(i, d.get(v_a) + e.getWeight());
								p.put(i, DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v_a).getVertex().getCaption());
								
								// highlight the changed cells
								itemD.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorModified);
								itemP.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorModified);
								sleep(250);
								
								// the weight changed so apply the new data to the items
								itemD.setCellDataByID(d);
								sleep(250);
								itemP.setCellDataByID(p);
								sleep(750);
							}
							
							// reset the highlighting of the edge
							ve.setColor(GraphView.DEF_EDGECOLOR);
							ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
							DijkstraAlgorithmPlugin.this.graphView.repaint();
						}
						
						// reset the highlighting of the cell
						itemD.setCellBackgroundByID(i, Color.white);
						itemP.setCellBackgroundByID(i, Color.white);
					}
					
					nextStep = 8;
					break;
				case 8:
					final ExecutionTableItem itemD2 = DijkstraAlgorithmPlugin.this.execTableView.getItem(DijkstraAlgorithmPlugin.this.execTableView.getItemCount() - 2);
					final ExecutionTableItem itemP2 = DijkstraAlgorithmPlugin.this.execTableView.getItem(DijkstraAlgorithmPlugin.this.execTableView.getItemCount() - 1);
					
					// highlight the elements of the set C that are used to perform this step
					sleep(250);
					for(int i : C) {
						itemD2.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetHighlight);
						itemP2.setCellBackgroundByID(i, DijkstraAlgorithmPlugin.this.colorSetHighlight);
					}
					
					// update the distances and paths of the vertices of set C
					for(int i = C.size() - 1, j; i >= 0; i--) {
						j = C.get(i);
						final Edge e = graph.getEdge(v_a, j);
						
						// highlight the current cell
						itemD2.setCellBackgroundByID(j, DijkstraAlgorithmPlugin.this.colorSetCurrElemHighlight);
						itemP2.setCellBackgroundByID(j, DijkstraAlgorithmPlugin.this.colorSetCurrElemHighlight);
						sleep(500);
						
						if(e != null) {
							// highlight the current edge
							ve = DijkstraAlgorithmPlugin.this.graphView.getVisualEdgeByID(e.getID());
							ve.setColor(DijkstraAlgorithmPlugin.this.colorModified);
							ve.setLineWidth(2);
							DijkstraAlgorithmPlugin.this.graphView.repaint();
							
							d.put(j, d.get(v_a) + e.getWeight());
							p.put(j, DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v_a).getVertex().getCaption());
							B.add(j);
							C.remove(j);
							
							// visualize the modification
							itemD2.setCellBackgroundByID(j, DijkstraAlgorithmPlugin.this.colorModified);
							itemP2.setCellBackgroundByID(j, DijkstraAlgorithmPlugin.this.colorModified);
							sleep(250);
							
							// update the cell data
							itemD2.setCellDataByID(d);
							sleep(250);
							itemP2.setCellDataByID(p);
							sleep(500);
							
							// highlight the text area to visualize the modification
							DijkstraAlgorithmPlugin.this.setsView.setBackground(DijkstraAlgorithmPlugin.this.colorModified);
							
							sleep(250);
							visualizeSets();
							visualizeVertices();
							sleep(250);
							
							// reset the highlight of the text area
							DijkstraAlgorithmPlugin.this.setsView.setBackground(Color.white);
							sleep(750);
							
							// reset the highlighting
							ve.setColor(GraphView.DEF_EDGECOLOR);
							ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
							DijkstraAlgorithmPlugin.this.graphView.repaint();
						}
						
						// reset the cell highlighting
						itemD2.setCellBackgroundByID(j, Color.white);
						itemP2.setCellBackgroundByID(j, Color.white);
					}
					
					nextStep = 9;
					break;
				case 9:
					// go to step 2 (stepid=4)
					
					sleep(750);
					
					nextStep = 4;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addSet("A", A);
			state.addSet("B", B);
			state.addSet("C", C);
			state.addMap("d", d);
			state.addMap("p", p);
			state.addInt("v_a", v_a);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			A = state.getSet("A");
			B = state.getSet("B");
			C = state.getSet("C");
			d = state.getMap("d");
			p = state.getMap("p");
			v_a = state.getInt("v_a");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			A = state.addSet("A", new Set<Integer>());
			B = state.addSet("B", new Set<Integer>());
			C = state.addSet("C", new Set<Integer>());
			d = state.addMap("d", new HashMap<Integer, Float>());
			p = state.addMap("p", new HashMap<Integer, String>());
			v_a = state.addInt("v_a", 0);
		}
		
		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			// remove created items from the execution table if the related step is rolled back
			switch(stepID) {
				case 1:
				case 5:
				case 6:
					// update the visual appearance of the vertices and the sets
					visualizeVertices();
					visualizeSets();
					break;
				case 2:
				case 3:
					DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
					break;
				case 7:
					DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
					DijkstraAlgorithmPlugin.this.execTableView.remove(DijkstraAlgorithmPlugin.this.execTableView.getLastItem());
					break;
				case 8:
					// reset the items
					DijkstraAlgorithmPlugin.this.execTableView.getLastItem().setCellDataByID(p);
					DijkstraAlgorithmPlugin.this.execTableView.getItem(DijkstraAlgorithmPlugin.this.execTableView.getItemCount() - 2).setCellDataByID(d);
					
					// update the visual appearance of the vertices and the sets
					visualizeVertices();
					visualizeSets();
					break;
			}
		}
		
		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			// not necessary because we do not override getApplySolutionToAlgorithm() in the exercises of the algorithm
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { DijkstraAlgorithmPlugin.this.graphView, DijkstraAlgorithmPlugin.this.execTableView, DijkstraAlgorithmPlugin.this.setsView };
		}
		
		/**
		 * Updates the display of the sets.
		 * 
		 * @since 1.0
		 */
		private void visualizeSets() {
			DijkstraAlgorithmPlugin.this.setsView.setText("A=" + toCaptions(A) + "\nB=" + toCaptions(B) + "\nC=" + toCaptions(C));
		}
		
		/**
		 * Updates the visual appearance of the vertices in the graph using the information of the sets {@link #A}, {@link #B} and {@link #C},
		 * the starting vertex {@link #v_1} and the vertex with the minimal distance {@link #v_a}.
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			GraphView<? extends Vertex, ? extends Edge>.VisualVertex vv;
			
			// reset the appearance of all vertices when their are no one
			if(A.isEmpty())
				DijkstraAlgorithmPlugin.this.graphView.resetVisualAppearance();
			
			// colorize the starting vertex
			vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v_1.getID());
			vv.setBackground(DijkstraAlgorithmPlugin.this.colorSetA);
			vv.setEdgeWidth(DijkstraAlgorithmPlugin.this.lineWidthStartVertex);
			
			// colorize all vertices of set A
			for(int v : A) {
				if(v == v_1.getID())
					continue;
				
				vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v);
				vv.setBackground(DijkstraAlgorithmPlugin.this.colorSetA);
				vv.setEdgeWidth(1);
			}
			
			// colorize all vertices of set B
			for(int v : B) {
				vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v);
				vv.setBackground(DijkstraAlgorithmPlugin.this.colorSetB);
				vv.setEdgeWidth(1);
			}
			
			// colorize all vertices of set C
			for(int v : C) {
				vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v);
				vv.setBackground(DijkstraAlgorithmPlugin.this.colorSetC);
				vv.setEdgeWidth(1);
			}
			
			// colorize the vertex with the min d(v)
			vv = DijkstraAlgorithmPlugin.this.graphView.getVisualVertexByID(v_a);
			if(vv != null) {
				vv.setBackground(DijkstraAlgorithmPlugin.this.colorVertexMinDist);
				vv.setEdgeWidth(1);
			}
			
			// show the visualization
			graphView.repaint();
		}
		
		/**
		 * Converts the given set of vertex identifiers to a set of related vertex captions.
		 * 
		 * @param set the set of vertex identifiers
		 * @return the converted set
		 * @since 1.0
		 */
		private Set<String> toCaptions(final Set<Integer> set) {
			final Graph<Vertex, Edge> graph = DijkstraAlgorithmPlugin.this.graphView.getGraph();
			final Set<String> res = new Set<String>(set.size());
			
			for(Integer id : set)
				res.add(graph.getVertexByID(id).getCaption());
			
			return res;
		}
		
	}

}
