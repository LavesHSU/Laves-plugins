package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import lavesdk.LAVESDKV;
import lavesdk.algorithm.AlgorithmExercise;
import lavesdk.algorithm.AlgorithmRTE;
import lavesdk.algorithm.AlgorithmState;
import lavesdk.algorithm.AlgorithmStateAttachment;
import lavesdk.algorithm.RTEvent;
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
import lavesdk.algorithm.plugin.views.MatrixView;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.dialogs.enums.AllowedGraphType;
import lavesdk.gui.widgets.BooleanProperty;
import lavesdk.gui.widgets.BooleanPropertyGroup;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.ExecutionTableBorder;
import lavesdk.gui.widgets.ExecutionTableColumn;
import lavesdk.gui.widgets.ExecutionTableGroup;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.Mask;
import lavesdk.gui.widgets.MatrixEditor;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.NumericTextField;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.gui.widgets.Symbol;
import lavesdk.gui.widgets.Symbol.PredefinedSymbol;
import lavesdk.language.LanguageFile;
import lavesdk.math.Matrix;
import lavesdk.math.NumericMatrix;
import lavesdk.math.ObjectMatrix;
import lavesdk.math.Set;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.Walk;
import lavesdk.math.graph.enums.Type;
import lavesdk.utils.GraphUtils;
import lavesdk.utils.MathUtils;

/**
 * Plugin that visualizes and teaches users the Floyd-Warshall algorithm.
 * 
 * @author jdornseifer
 * @version 1.4
 */
public class TripelAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the graph view that displays the graph */
	private DefaultGraphView graphView;
	/** the view that displays the algorithm text */
	private AlgorithmTextView algoTextView;
	/** the table that visualizes assistant data */
	private ExecutionTableView assistantTableView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the view to show the matrix d */
	private MatrixView<Float> matrixViewD;
	/** the view to show the matrix p */
	private MatrixView<String> matrixViewP;
	/** the runtime environment of the algorithm */
	private TripelRTE rte;
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
	/** the view group for C and E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ce;
	/** the view group for D and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup df;
	/** the view group for (C,E) and (D,F) (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cedf;
	/** the view group for A,B,C,D,E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcdef;
	
	// modifiable visualization data
	/** color to visualize the source vertex */
	private Color colorSourceVertex;
	/** color to visualize the transit vertex */
	private Color colorTransitVertex;
	/** color to visualize the target vertex */
	private Color colorTargetVertex;
	/** color to visualize the current highlighted vertices */
	private Color colorHighlightedVertices;
	/** color to visualize the current highlighted edge */
	private Color colorHighlightedEdge;
	/** color to visualize a smaller distance */
	private Color colorSmallerDist;
	/** color to visualize modified objects */
	private Color colorModified;
	/** the line width of the source vertex */
	private int lineWidthSourceVertex;
	/** the line width of the transit vertex */
	private int lineWidthTransitVertex;
	/** the line width of the target vertex */
	private int lineWidthTargetVertex;
	
	/** configuration key for the {@link #creatorPrefsDirectedValue} */
	private static final String CFGKEY_CREATORPROP_DIRECTED = "creatorPropDirected";
	/** configuration key for the {@link #colorSourceVertex} */
	private static final String CFGKEY_COLOR_SOURCEVERTEX = "colorSourceVertex";
	/** configuration key for the {@link #colorTransitVertex} */
	private static final String CFGKEY_COLOR_TRANSITVERTEX = "colorTransitVertex";
	/** configuration key for the {@link #colorTargetVertex} */
	private static final String CFGKEY_COLOR_TARGETVERTEX = "colorTargetVertex";
	/** configuration key for the {@link #colorHighlightedVertices} */
	private static final String CFGKEY_COLOR_HIGHLIGHTEDVERTICES = "colorHighlightedVertices";
	/** configuration key for the {@link #colorHighlightedEdge} */
	private static final String CFGKEY_COLOR_HIGHLIGHTEDEDGE = "colorHighlightedEdge";
	/** configuration key for the {@link #colorSmallerDist} */
	private static final String CFGKEY_COLOR_SMALLERDIST = "colorSmallerDist";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #lineWidthSourceVertex} */
	private static final String CFGKEY_LINEWIDTH_SOURCEVERTEX = "lineWidthSourceVertex";
	/** configuration key for the {@link #lineWidthTransitVertex} */
	private static final String CFGKEY_LINEWIDTH_TRANSITVERTEX = "lineWidthTransitVertex";
	/** configuration key for the {@link #lineWidthTargetVertex} */
	private static final String CFGKEY_LINEWIDTH_TARGETVERTEX = "lineWidthTargetVertex";
	
	private static final String COLUMN_CHANGE_TICK = "t";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langTripel.txt"));
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
		this.assistantTableView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_ASSISTANTTABLE_TITLE", langID, "Assistant Table"), true, langFile, langID);
		this.matrixViewD = new MatrixView<Float>(LanguageFile.getLabel(langFile, "VIEW_MATRIXD_TITLE", langID, "Matrix D (distance matrix)"), new MatrixEditor.FloatElementFormat(), true, langFile, langID);
		this.matrixViewP = new MatrixView<String>(LanguageFile.getLabel(langFile, "VIEW_MATRIXP_TITLE", langID, "Matrix P (predecessor matrix)"), new MatrixEditor.StringElementFormat(), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new TripelRTE();
		this.matrixToGraph = new MatrixToGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.BOTH, langFile, langID, true);
		this.randomGraph = new RandomGraphToolBarExtension<>(host, graphView, AllowedGraphType.BOTH, langFile, langID, false);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, false);
		this.creatorPrefsDirected = LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED", langID, "directed");
		this.creatorPrefsUndirected = LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED", langID, "undirected");
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		assistantTableView.setAutoRepaint(true);
		matrixViewD.setAutoRepaint(true);
		matrixViewP.setAutoRepaint(true);
		
		// the column widths of the assistant table should be set manually
		assistantTableView.setAutoResizeColumns(false);
		
		// load infinity mask for the matrix D
		matrixViewD.addMask(new Mask(Float.POSITIVE_INFINITY, Symbol.getPredefinedSymbol(PredefinedSymbol.INFINITY)));
		
		// load the creator preference data from the configuration
		creatorPrefsDirectedValue = this.config.getBoolean(CFGKEY_CREATORPROP_DIRECTED, false);
		
		// load the visualization colors from the configuration of the plugin
		colorSourceVertex = this.config.getColor(CFGKEY_COLOR_SOURCEVERTEX, new Color(180, 210, 230));
		colorTransitVertex = this.config.getColor(CFGKEY_COLOR_TRANSITVERTEX, new Color(110, 190, 110));
		colorTargetVertex = this.config.getColor(CFGKEY_COLOR_TARGETVERTEX, new Color(255, 220, 80));
		colorHighlightedVertices = this.config.getColor(CFGKEY_COLOR_HIGHLIGHTEDVERTICES, new Color(200, 145, 145));
		colorHighlightedEdge = this.config.getColor(CFGKEY_COLOR_HIGHLIGHTEDEDGE, new Color(200, 145, 145));
		colorSmallerDist = this.config.getColor(CFGKEY_COLOR_SMALLERDIST, new Color(120, 210, 80));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		lineWidthSourceVertex = this.config.getInt(CFGKEY_LINEWIDTH_SOURCEVERTEX, 2);
		lineWidthTransitVertex = this.config.getInt(CFGKEY_LINEWIDTH_TRANSITVERTEX, 2);
		lineWidthTargetVertex = this.config.getInt(CFGKEY_LINEWIDTH_TARGETVERTEX, 2);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		assistantTableView.loadConfiguration(config, "assistantTableView");
		matrixViewD.loadConfiguration(config, "matrixViewD");
		matrixViewP.loadConfiguration(config, "matrixViewP");
		legendView.loadConfiguration(config, "legendView");
		
		// load legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Floyd-Warshall algorithm (Tripel algorithm)");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds the shortest paths between all pairs of vertices in a graph.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A simple, weighted graph G = (V, E) without circles of negative length.");
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
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extension to create a graph by use of an adjacency matrix.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
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
	public boolean hasExerciseMode() {
		return true;
	}

	@Override
	public Configuration getConfiguration() {
		return config;
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
		 * ///|/////|/////
		 * /A/|/ C /|/ E /	A = algorithm text view
		 * ///|/////|/////	B = legend view
		 * ---|-----|-----	C = graph view
		 * ///|/////|/////	D = matrix view D
		 * /B/|/ D /|/ F /	E = execution table view
		 * ///|/////|/////	F = matrix view P
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		ce = new ViewGroup(ViewGroup.HORIZONTAL);
		df = new ViewGroup(ViewGroup.HORIZONTAL);
		cedf = new ViewGroup(ViewGroup.VERTICAL);
		abcdef = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// top right group for C and E
		ce.add(graphView);
		ce.add(assistantTableView);
		ce.restoreWeights(config, "weights_ce", new float[] { 0.7f, 0.3f });
		
		// bottom right group for D and F
		df.add(matrixViewD);
		df.add(matrixViewP);
		df.restoreWeights(config, "weights_df", new float[] { 0.5f, 0.5f });
		df.setWeights(new float[] { 0.5f, 0.5f });
		
		// right group for (C,E) and (D,F)
		cedf.add(ce);
		cedf.add(df);
		cedf.restoreWeights(config, "weights_cedf", new float[] { 0.7f, 0.3f });
		
		// group for (A,B) and ((C,E),(D,F))
		abcdef.add(ab);
		abcdef.add(cedf);
		abcdef.restoreWeights(config, "weights_abcdef", new float[] { 0.4f, 0.6f });
		
		container.setLayout(new BorderLayout());
		container.add(abcdef, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		assistantTableView.saveConfiguration(config, "assistantTableView");
		matrixViewD.saveConfiguration(config, "matrixViewD");
		matrixViewP.saveConfiguration(config, "matrixViewP");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(ce != null)
			ce.storeWeights(config, "weights_ce");
		if(df != null)
			df.storeWeights(config, "weights_df");
		if(cedf != null)
			cedf.storeWeights(config, "weights_cedf");
		if(abcdef != null)
			abcdef.storeWeights(config, "weights_abcdef");
		
		// reset view content where it is necessary
		graphView.reset();
		assistantTableView.reset();
		matrixViewD.reset();
		matrixViewP.reset();
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_TRANSITVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_TRANSITVERTEX", langID, "Background color of the transit vertex"), colorTransitVertex));
		plm.add(new ColorProperty(CFGKEY_COLOR_SOURCEVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SOURCEVERTEX", langID, "Background color of the source vertex"), colorSourceVertex));
		plm.add(new ColorProperty(CFGKEY_COLOR_TARGETVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_TARGETVERTEX", langID, "Background color of the target vertex"), colorTargetVertex));
		plm.add(new ColorProperty(CFGKEY_COLOR_HIGHLIGHTEDVERTICES, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_HIGHLIGHTEDVERTICES", langID, "Background color of the vertices that are currently investigated"), colorHighlightedVertices));
		plm.add(new ColorProperty(CFGKEY_COLOR_HIGHLIGHTEDEDGE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_HIGHLIGHTEDEDGE", langID, "Color of the edge that is currently investigated"), colorHighlightedEdge));
		plm.add(new ColorProperty(CFGKEY_COLOR_SMALLERDIST, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SMALLERDIST", langID, "Background color of an assistant table cell that indicates a smaller distance"), colorSmallerDist));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwTransitVertex = new NumericProperty(CFGKEY_LINEWIDTH_TRANSITVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_TRANSITVERTEX", langID, "Line width of the transit vertex"), lineWidthTransitVertex, true);
		lwTransitVertex.setMinimum(1);
		lwTransitVertex.setMaximum(5);
		plm.add(lwTransitVertex);
		final NumericProperty lwSourceVertex = new NumericProperty(CFGKEY_LINEWIDTH_SOURCEVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_SOURCEVERTEX", langID, "Line width of the source vertex"), lineWidthSourceVertex, true);
		lwSourceVertex.setMinimum(1);
		lwSourceVertex.setMaximum(5);
		plm.add(lwSourceVertex);
		final NumericProperty lwTargetVertex = new NumericProperty(CFGKEY_LINEWIDTH_TARGETVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_TARGETVERTEX", langID, "Line width of the target vertex"), lineWidthTargetVertex, true);
		lwTargetVertex.setMinimum(1);
		lwTargetVertex.setMaximum(5);
		plm.add(lwTargetVertex);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorTransitVertex = config.addColor(CFGKEY_COLOR_TRANSITVERTEX, plm.getColorProperty(CFGKEY_COLOR_TRANSITVERTEX).getValue());
		colorSourceVertex = config.addColor(CFGKEY_COLOR_SOURCEVERTEX, plm.getColorProperty(CFGKEY_COLOR_SOURCEVERTEX).getValue());
		colorTargetVertex = config.addColor(CFGKEY_COLOR_TARGETVERTEX, plm.getColorProperty(CFGKEY_COLOR_TARGETVERTEX).getValue());
		colorHighlightedVertices = config.addColor(CFGKEY_COLOR_HIGHLIGHTEDVERTICES, plm.getColorProperty(CFGKEY_COLOR_HIGHLIGHTEDVERTICES).getValue());
		colorHighlightedEdge = config.addColor(CFGKEY_COLOR_HIGHLIGHTEDEDGE, plm.getColorProperty(CFGKEY_COLOR_HIGHLIGHTEDEDGE).getValue());
		colorSmallerDist = config.addColor(CFGKEY_COLOR_SMALLERDIST, plm.getColorProperty(CFGKEY_COLOR_SMALLERDIST).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthTransitVertex = config.addInt(CFGKEY_LINEWIDTH_TRANSITVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_TRANSITVERTEX).getValue().intValue());
		lineWidthSourceVertex = config.addInt(CFGKEY_LINEWIDTH_SOURCEVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_SOURCEVERTEX).getValue().intValue());
		lineWidthTargetVertex = config.addInt(CFGKEY_LINEWIDTH_TARGETVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_TARGETVERTEX).getValue().intValue());
		
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
		if(!isValidGraph()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NOTVALID", langID, "The created graph is not valid!\nThe Floyd-Warshall algorithm can only be applied to directed graphs that have no circles of negative length and\nto undirected graphs that have no negative weights."), LanguageFile.getLabel(langFile, "MSG_INFO_NOTVALID_TITLE", langID, "Graph not valid"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			// during the algorithm the graph is not editable
			graphView.deselectAll();
			graphView.setEditable(false);
			
			// create labels for the matrices
			final Map<Integer, String> labels = new HashMap<Integer, String>();
			GraphView<Vertex, Edge>.VisualVertex vv;
			
			for(int i = 0; i < graphView.getVisualVertexCount(); i++) {
				vv = graphView.getVisualVertex(i);
				labels.put(vv.getVertex().getIndex(), vv.getVertex().getCaption());
			}
			
			matrixViewD.setRowLabels(labels);
			matrixViewD.setColumnLabels(labels);
			matrixViewP.setRowLabels(labels);
			matrixViewP.setColumnLabels(labels);
			
			// reset the execution table so that a new table can be visualized
			assistantTableView.reset();
			
			// create columns
			ExecutionTableColumn column = new ExecutionTableColumn("v<sub>s</sub>");
			column.setWidth(30);
			assistantTableView.add(column);
			column = new ExecutionTableColumn("v<sub>t</sub>");
			column.setWidth(30);
			assistantTableView.add(column);
			column = new ExecutionTableColumn("v<sub>z</sub>");
			column.setWidth(30);
			assistantTableView.add(column);
			column = new ExecutionTableColumn("d(v<sub>s</sub>,v<sub>t</sub>) + d(v<sub>t</sub>,v<sub>z</sub>) &lt; d(v<sub>s</sub>,v<sub>z</sub>)");
			column.setWidth(175);
			assistantTableView.add(column);
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_ASSISTANTTABLE_COLUMNCHANGE", langID, "Change?"));
			column.addMask(new Mask(COLUMN_CHANGE_TICK, Symbol.createLaTeXSymbol("\\checkmark", 12.0f)));
			assistantTableView.add(column);
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_ASSISTANTTABLE_COLUMNCHANGEDIST", langID, "Change in <i>D</i>"));
			column.setWidth(75);
			assistantTableView.add(column);
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_ASSISTANTTABLE_COLUMNCHANGEPRED", langID, "Change in <i>P</i>"));
			column.setWidth(75);
			assistantTableView.add(column);
			
			// create groups to separate the areas
			final ExecutionTableBorder border = new ExecutionTableBorder(2, Color.black);
			assistantTableView.addColumnGroup(new ExecutionTableGroup(border, 2, 1, false));
			assistantTableView.addColumnGroup(new ExecutionTableGroup(border, 4, 1, false));
			final int vertexGroupSize = (graphView.getVisualVertexCount() - 1) * (graphView.getVisualVertexCount() - 2);
			if(vertexGroupSize > 0)
				assistantTableView.addItemGroup(new ExecutionTableGroup(border, 0, vertexGroupSize, true));
			
			// if we start the algorithm in exercise mode then add a final exercise with random vertices
			if(rte.isExerciseModeEnabled()) {
				// get a random source and target vertex
				final Random rnd = new Random();
				final Set<Vertex> vertices = graphView.getGraph().getVertexSet();
				final Vertex rndSourceVertex = vertices.get(rnd.nextInt(vertices.size()));
				vertices.remove(rndSourceVertex);
				final Vertex rndTargetVertex = vertices.get(rnd.nextInt(vertices.size()));
				
				String finalExerciseText = LanguageFile.getLabel(langFile, "FINAL_EXERCISE", langID, "What is the shortest path from vertex &v_i& to vertex &v_j& and how long is this path?");
				finalExerciseText = finalExerciseText.replaceAll("&v_i&", rndSourceVertex.getCaption());
				finalExerciseText = finalExerciseText.replaceAll("&v_j&", rndTargetVertex.getCaption());
				algoText.setFinalExercise(new AlgorithmExercise<Object>(finalExerciseText, 3.0f) {
					
					@Override
					protected Object[] requestSolution() {
						final SolutionEntry<JTextField> entryPath = new SolutionEntry<JTextField>(LanguageFile.getLabel(langFile, "FINAL_EXERCISE_PATH", langID, "Path ="), new JTextField());
						final SolutionEntry<NumericTextField> entryLength = new SolutionEntry<NumericTextField>(LanguageFile.getLabel(langFile, "FINAL_EXERCISE_LENGTH", langID, "Length ="), new NumericTextField());
						
						if(!SolveExercisePane.showDialog(TripelAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryPath,  entryLength }, TripelAlgorithmPlugin.this.langFile, TripelAlgorithmPlugin.this.langID, LanguageFile.getLabel(TripelAlgorithmPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", TripelAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
							return null;
						
						final Walk<Vertex> w = GraphUtils.toWalk(entryPath.getComponent().getText(), TripelAlgorithmPlugin.this.graphView.getGraph());
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
						final Matrix<Float> D = state.getMatrix("D");
						final Matrix<String> P = state.getMatrix("P");
						
						if(w == null || length == null)
							return false;
						else if(length.floatValue() != D.get(rndSourceVertex.getIndex(), rndTargetVertex.getIndex()).floatValue())
							return false;
						
						final Graph<Vertex, Edge> graph = TripelAlgorithmPlugin.this.graphView.getGraph();
						final Walk<Vertex> idealWalk = new Walk<Vertex>(graph);
						Vertex v = rndTargetVertex;
						
						do {
							idealWalk.add(0, v);
							v = graph.getVertexByCaption(P.get(rndSourceVertex.getIndex(), v.getIndex()));
						} while(v != rndSourceVertex);
						
						idealWalk.add(0, rndSourceVertex);
						
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
		
		final int TAB_SIZE = 4;
		final AlgorithmText text = new AlgorithmText();
		
		// create paragraphs
		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_INITIALIZATION", langID, "1. Initialization:"), 1);
		final AlgorithmParagraph itParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ITERATION", langID, "2. Iteration:"), 2);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INITDANDP1", langID, "For all _latex{$v_i \\in V$}: set\n_latex{$d(v_i,v_i) := 0$}, _latex{$p(v_i,v_i) := v_i$}.\n"), 1);
		step.setExercise(new AlgorithmExercise<Matrix<?>>(LanguageFile.getLabel(langFile, "EXERCISE_SETP1", langID, "Initialize the distance matrix <i>D</i> and the predecessor matrix <i>P</i>."), 2.0f, new View[] { matrixViewD, matrixViewP }) {

			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				final int vertexCount = TripelAlgorithmPlugin.this.graphView.getVisualVertexCount();
				TripelAlgorithmPlugin.this.matrixViewD.setMatrix(new NumericMatrix<Float>(vertexCount, vertexCount));
				TripelAlgorithmPlugin.this.matrixViewD.setEditable(true);
				TripelAlgorithmPlugin.this.matrixViewP.setMatrix(new ObjectMatrix<String>(vertexCount, vertexCount));
				TripelAlgorithmPlugin.this.matrixViewP.setEditable(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				TripelAlgorithmPlugin.this.matrixViewD.setEditable(false);
				TripelAlgorithmPlugin.this.matrixViewP.setEditable(false);
			}
			
			@Override
			protected Matrix<?>[] requestSolution() {
				return new Matrix<?>[] { TripelAlgorithmPlugin.this.matrixViewD.getMatrix(), TripelAlgorithmPlugin.this.matrixViewP.getMatrix() };
			}
			
			@Override
			protected boolean examine(Matrix<?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "D", "P" }, results);
			}
		});
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_INITDANDP2", langID, "For all _latex{$v_i,v_j \\in V$}, _latex{$i \\neq j$}: set _latex{$p(v_i,v_j) = \\begin{cases} v_i, \\; if \\; (v_i,v_j) \\in E \\\\ 0, \\; else \\end{cases}$}\n_latex{$d(v_i,v_j) = \\begin{cases} c(v_i,v_j), \\; if \\; (v_i,v_j) \\in E \\\\ \\inf, \\; else \\end{cases}$}.\n\n"), 2);
		step.setExercise(new AlgorithmExercise<Matrix<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "What are <i>D</i> and <i>P</i> after this step (<i>use \"-\" as infinity</i>)?"), 5.0f, new View[] { matrixViewD, matrixViewP }) {

			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				final Matrix<Float> m = state.getMatrix("D");
				final Matrix<String> p = state.getMatrix("P");
				TripelAlgorithmPlugin.this.matrixViewD.setMatrix(m);
				TripelAlgorithmPlugin.this.matrixViewD.setEditable(true);
				TripelAlgorithmPlugin.this.matrixViewP.setMatrix(p);
				TripelAlgorithmPlugin.this.matrixViewP.setEditable(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				TripelAlgorithmPlugin.this.matrixViewD.setEditable(false);
				TripelAlgorithmPlugin.this.matrixViewP.setEditable(false);
			}
			
			@Override
			protected Matrix<?>[] requestSolution() {
				return new Matrix<?>[] { TripelAlgorithmPlugin.this.matrixViewD.getMatrix(), TripelAlgorithmPlugin.this.matrixViewP.getMatrix() };
			}
			
			@Override
			protected boolean examine(Matrix<?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "D", "P" }, results);
			}
		});
		
		// 2. iteration
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_FORALLTRANSITVERTICES", langID, "For all vertices _latex{$v_t \\in V$} (transit vertices)"), 3);
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_FORALLSOURCEVERTICES", langID, "For all vertices _latex{$v_s \\in V$}, _latex{$v_s \\neq v_t$} (source vertices)"), 4, TAB_SIZE * 1);
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_FORALLTARGETVERTICES", langID, "For all vertices _latex{$v_z \\in V$}, _latex{$v_z \neq v_z$}, _latex{$v_z \neq v_t$} (target vertices)"), 5, TAB_SIZE * 2);
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_UPDATEDANDP", langID, "If _latex{$d(v_s,v_t) + d(v_t,v_z) < d(v_s,v_z)$}\n"), 6, TAB_SIZE * 3);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP6", langID, "Is d(v<sub>s</sub>,v<sub>t</sub>) + d(v<sub>t</sub>,v<sub>z</sub>) < d(v<sub>s</sub>,v<sub>z</sub>)?"), 1.0f) {
			
			private final String labelYes = LanguageFile.getLabel(langFile, "EXERCISE_STEP6_YES", langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(langFile, "EXERCISE_STEP6_NO", langID, "No");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelYes);
				final JRadioButton rdobtn2 = new JRadioButton(labelNo);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(TripelAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, TripelAlgorithmPlugin.this.langFile, TripelAlgorithmPlugin.this.langID))
					return null;
				
				return new Boolean[] { (!rdobtn1.isSelected() && !rdobtn2.isSelected()) ? null : rdobtn1.isSelected() };
			}
			
			@Override
			protected String getResultAsString(Boolean result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return (result == Boolean.TRUE) ? labelYes : labelNo;
			}
			
			@Override
			protected boolean examine(Boolean[] results, AlgorithmState state) {
				final Graph<Vertex, Edge> graph = TripelAlgorithmPlugin.this.graphView.getGraph();
				final int v_transit = state.getInt("v_transit");
				final int v_source = state.getInt("v_source");
				final int v_target = state.getInt("v_target");
				final Matrix<Float> D = state.getMatrix("D");
				// get vertex objects
				final Vertex transit = graph.getVertexByID(v_transit);
				final Vertex source = graph.getVertexByID(v_source);
				final Vertex target = graph.getVertexByID(v_target);
				
				// smaller distance taking the transit vertex?
				return (results[0] != null && results[0] == (D.get(source.getIndex(), transit.getIndex()) + D.get(transit.getIndex(), target.getIndex()) < D.get(source.getIndex(), target.getIndex())));
			}
		});

		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_UPDATEDANDP", langID, "then set _latex{$d(v_s,v_z) = d(v_s,v_t) + d(v_t,v_z)$} and _latex{$p(v_s,v_z) = p(v_t,v_z)$}."), 7, TAB_SIZE * 3);
		step.setExercise(new AlgorithmExercise<Matrix<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP7", langID, "What are <i>D</i> and <i>P</i> after this step?"), 1.0f, new View[] { matrixViewD, matrixViewP }) {

			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				final Matrix<Float> m = state.getMatrix("D");
				final Matrix<String> p = state.getMatrix("P");
				TripelAlgorithmPlugin.this.matrixViewD.setMatrix(m);
				TripelAlgorithmPlugin.this.matrixViewD.setEditable(true);
				TripelAlgorithmPlugin.this.matrixViewP.setMatrix(p);
				TripelAlgorithmPlugin.this.matrixViewP.setEditable(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				TripelAlgorithmPlugin.this.matrixViewD.setEditable(false);
				TripelAlgorithmPlugin.this.matrixViewP.setEditable(false);
			}
			
			@Override
			protected Matrix<?>[] requestSolution() {
				return new Matrix<?>[] { TripelAlgorithmPlugin.this.matrixViewD.getMatrix(), TripelAlgorithmPlugin.this.matrixViewP.getMatrix() };
			}
			
			@Override
			protected boolean examine(Matrix<?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "D", "P" }, results);
			}
		});
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_TRANSITVERTEX", langID, "Current transit vertex v<sub>t</sub>"), LegendItem.createCircleIcon(colorTransitVertex, Color.black, lineWidthTransitVertex)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SOURCEVERTEX", langID, "Current source vertex v<sub>s</sub>"), LegendItem.createCircleIcon(colorSourceVertex, Color.black, lineWidthSourceVertex)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_TARGETVERTEX", langID, "Current target vertex v<sub>z</sub>"), LegendItem.createCircleIcon(colorTargetVertex, Color.black, lineWidthTargetVertex)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_HIGHLIGHTEDV_I", langID, "Vertex v<sub>i</sub> that is currently under investigation"), LegendItem.createCircleIcon(colorHighlightedVertices, Color.black, 2)));
		legendView.add(new LegendItem("item5", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_HIGHLIGHTEDV_J", langID, "Vertex v<sub>j</sub> that is currently under investigation"), LegendItem.createCircleIcon(colorHighlightedVertices, Color.black, 1)));
		legendView.add(new LegendItem("item6", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_HIGHLIGHTEDEDGE", langID, "Edge (v<sub>i</sub>, v<sub>j</sub>) that is currently under investigation"), LegendItem.createLineIcon(colorHighlightedEdge, 2)));
		legendView.add(new LegendItem("item7", assistantTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_ASSISTANTTABLE_SMALLERDIST", langID, "Their is a shorter path taking the transit vertex"), LegendItem.createRectangleIcon(colorSmallerDist, colorSmallerDist, 0)));
		legendView.add(new LegendItem("item8", assistantTableView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_ASSISTANTTABLE_CHANGE", langID, "Change in D or P"), LegendItem.createRectangleIcon(Color.white, colorModified, 2)));
		legendView.add(new LegendItem("item9", matrixViewD.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_MATRIXD_MODIFICATION", langID, "Modification of matrix D"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item10", matrixViewP.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_MATRIXP_MODIFICATION", langID, "Modification of matrix P"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
	}
	
	/**
	 * Indicates whether the graph the user has created is valid.
	 * 
	 * @return <code>true</code> if the graph is valid otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean isValidGraph() {
		final Graph<Vertex, Edge> graph = graphView.getGraph();
		
		if(graph.getType() == Type.DIRECTED) {
			final NumericMatrix<Float> d = new NumericMatrix<Float>(graph.getOrder(), graph.getOrder());
			final ObjectMatrix<Vertex> p = new ObjectMatrix<Vertex>(graph.getOrder(), graph.getOrder());
			GraphUtils.findShortestPaths(graph, d, p, true);
			
			// on the way from v_i to v_i is another predecessor than v_i then there is a cycle with negative length
			for(int i = 0; i < graph.getOrder(); i++)
				if(p.get(i, i) != graph.getVertex(i))
					return false;
		}
		else {
			// an undirected graph may not has negative weights
			for(int i = 0; i < graph.getSize(); i++)
				if(graph.getEdge(i).getWeight() < 0.0f)
					return false;
		}
		
		return true;
	}
	
	/**
	 * The runtime environment of the Tripel algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class TripelRTE extends AlgorithmRTE {
		
		/** the elements that can be used in the for loop of the transit vertices */
		private Set<Integer> transits;
		/** the elements that can be used in the for loop of the source vertices */
		private Set<Integer> sources;
		/** the elements that can be used in the for loop of the targets vertices */
		private Set<Integer> targets;
		/** the matrix D with the distances between the vertices */
		private Matrix<Float> D;
		/** the matrix P with the predecessor vertices */
		private Matrix<String> P;
		/** the current transit vertex */
		private int v_transit;
		/** the current source vertex */
		private int v_source;
		/** the current target vertex */
		private int v_target;
		
		/** the value of the else branch in P */
		private static final String P_UNDEFINED = "0";
		
		/**
		 * Creates a new runtime environment.
		 * 
		 * @since 1.0
		 */
		public TripelRTE() {
			super(TripelAlgorithmPlugin.this, TripelAlgorithmPlugin.this.algoText);
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = graphView.getGraph();
			GraphView<Vertex, Edge>.VisualEdge ve;
			final Set<Integer> V = graph.getVertexByIDSet();
			Vertex v_i;
			Vertex v_j;
			Edge e;
			GraphView<Vertex, Edge>.VisualVertex vv_i;
			GraphView<Vertex, Edge>.VisualVertex vv_j;
			Vertex transit;
			Vertex source;
			Vertex target;
			ExecutionTableItem item;
			float distST;
			float distTZ;
			float distSZ;
			float dist;
			int nextStep = -1;
			
			switch(stepID) {
				case 1:
					transits = V.clone();
					// initialize the matrices because they need the correct size of the number of vertices
					D = new NumericMatrix<Float>(V.size(), V.size());
					P = new ObjectMatrix<String>(V.size(), V.size());
					
					// initialize D and P for all v_i in V
					for(Integer i : V) {
						v_i = graph.getVertexByID(i.intValue());
						D.set(v_i.getIndex(), v_i.getIndex(), 0.0f);
						P.set(v_i.getIndex(), v_i.getIndex(), v_i.getCaption());
					}
					
					// visualize the matrices
					sleep(250);
					TripelAlgorithmPlugin.this.matrixViewD.setMatrix(D);
					sleep(500);
					TripelAlgorithmPlugin.this.matrixViewP.setMatrix(P);
					sleep(1000);
					
					nextStep = 2;
					break;
				case 2:
					String elemP;
					Float elemD;
					
					// for all v_i,v_j in V with v_i!=v_j initialize P and D
					for(Integer i : V) {
						for(Integer j : V) {
							if(i.intValue() == j.intValue())
								continue;
							
							// get vertex objects
							v_i = graph.getVertexByID(i.intValue());
							vv_i = TripelAlgorithmPlugin.this.graphView.getVisualVertex(v_i);
							v_j = graph.getVertexByID(j.intValue());
							vv_j = TripelAlgorithmPlugin.this.graphView.getVisualVertex(v_j);
							// get the edge between the vertices
							e = graph.getEdge(i.intValue(), j.intValue());
							ve = (e != null) ? TripelAlgorithmPlugin.this.graphView.getVisualEdge(e) : null;
							
							sleep(250);
							
							// highlight the current vertices
							vv_i.setBackground(TripelAlgorithmPlugin.this.colorHighlightedVertices);
							vv_i.setEdgeWidth(2);
							vv_j.setBackground(TripelAlgorithmPlugin.this.colorHighlightedVertices);
							TripelAlgorithmPlugin.this.graphView.repaint();
							sleep(250);
							
							// if (v_i,v_j) in E then P(v_i,v_j)=v_i and D(v_i,v_j)=c(v_i,v_j) else P(v_i,v_j)=- and D(v_i,v_j)=infinity
							if(e != null) {
								ve.setColor(TripelAlgorithmPlugin.this.colorHighlightedEdge);
								ve.setLineWidth(2);
								TripelAlgorithmPlugin.this.graphView.repaint();
								sleep(250);
								
								elemP = v_i.getCaption();
								elemD = e.getWeight();
							}
							else {
								elemP = P_UNDEFINED;
								elemD = Float.POSITIVE_INFINITY;
							}
							
							// update P and D
							P.set(v_i.getIndex(), v_j.getIndex(), elemP);
							D.set(v_i.getIndex(), v_j.getIndex(), elemD);
							
							// visualize P and D
							sleep(250);
							TripelAlgorithmPlugin.this.matrixViewP.setElementBackground(v_i.getIndex(), v_j.getIndex(), TripelAlgorithmPlugin.this.colorModified);
							TripelAlgorithmPlugin.this.matrixViewP.setMatrix(P);
							sleep(250);
							TripelAlgorithmPlugin.this.matrixViewD.setElementBackground(v_i.getIndex(), v_j.getIndex(), TripelAlgorithmPlugin.this.colorModified);
							TripelAlgorithmPlugin.this.matrixViewD.setMatrix(D);
							sleep(250);
							
							vv_i.setBackground(GraphView.DEF_VERTEXBACKGROUND);
							vv_i.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
							vv_j.setBackground(GraphView.DEF_VERTEXBACKGROUND);
							TripelAlgorithmPlugin.this.graphView.repaint();
							if(ve != null) {
								ve.setColor(GraphView.DEF_EDGECOLOR);
								ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
								TripelAlgorithmPlugin.this.graphView.repaint();
							}
							TripelAlgorithmPlugin.this.matrixViewP.setElementBackground(v_i.getIndex(), v_j.getIndex(), Color.white);
							TripelAlgorithmPlugin.this.matrixViewD.setElementBackground(v_i.getIndex(), v_j.getIndex(), Color.white);
						}
					}
					
					nextStep = 3;
					break;
				case 3:
					v_transit = 0;
					v_source = 0;
					v_target = 0;
					
					// initialize the set of elements for the next loop because this has to be done each time
					// a new transit vertex has been chosen
					sources = V.clone();
					
					// for all transit vertices
					v_transit = forEachGetNext(transits);
					
					// if their are no more transit vertices then the algorithm is finished
					if(v_transit < 1)
						nextStep = -1;
					else {
						sleep(250);
						visualizeVertices();
						sleep(500);
						
						nextStep = 4;
					}
					break;
				case 4:
					v_source = 0;
					v_target = 0;
					
					// initialize the set of elements for the next loop because this has to be done each time
					// a new source vertex has been chosen
					targets = V.clone();

					// for all source vertices that are unequal the transit
					v_source = forEachGetNext(sources, v_transit);
					
					// if their are no more source vertices then go a loop above
					if(v_source < 1)
						nextStep = 3;
					else {
						sleep(250);
						visualizeVertices();
						sleep(500);
						
						nextStep = 5;
					}
					break;
				case 5:
					v_target = 0;
					
					// for all target vertices that are unequal the transit and the source
					v_target = forEachGetNext(targets, v_transit, v_source);
					
					// if their are no more source vertices then go a loop above
					if(v_target < 1)
						nextStep = 4;
					else {
						sleep(250);
						visualizeVertices();
						sleep(250);

						// visualize the current set of vertices in the execution table
						transit = graph.getVertexByID(v_transit);
						source = graph.getVertexByID(v_source);
						target = graph.getVertexByID(v_target);
						TripelAlgorithmPlugin.this.assistantTableView.add(new ExecutionTableItem(new String[] { source.getCaption(), transit.getCaption(), target.getCaption() }));
						sleep(250);
						
						nextStep = 6;
					}
					break;
				case 6:
					// get last item to modify it with the current distance data
					item = TripelAlgorithmPlugin.this.assistantTableView.getLastItem();
					
					// get vertex objects
					transit = graph.getVertexByID(v_transit);
					source = graph.getVertexByID(v_source);
					target = graph.getVertexByID(v_target);
					
					// what are the distances between the vertices?
					distST = D.get(source.getIndex(), transit.getIndex());
					distTZ = D.get(transit.getIndex(), target.getIndex());
					distSZ = D.get(source.getIndex(), target.getIndex());
					dist = distST + distTZ;
					
					// highlight the distance cell in the table
					item.setCellObject(3, MathUtils.formatFloat(distST) + " + " + MathUtils.formatFloat(distTZ) + " < " + MathUtils.formatFloat(distSZ));
					sleep(500);
					
					// if d(v_s,v_t) + d(v_t,v_z) < d(v_s,v_z) then go to 7 (stepid)
					if(dist < distSZ) {
						// highlight the cell that shows that there is a smaller distance using the transit vertex
						item.setCellBackground(3, TripelAlgorithmPlugin.this.colorSmallerDist);
						sleep(1000);
						// remove the highlight
						item.setCellBackground(3, Color.white);
						
						nextStep = 7;
					}
					else {
						item.setCellObject(4, "-");
						item.setCellObject(5, "-");
						item.setCellObject(6, "-");
						
						sleep(500);
						
						nextStep = 5;
					}
					
					break;
				case 7:
					// d(v_s,v_z)=d(v_s,v_t) + d(v_t,v_z) and p(v_s,v_z)=p(v_t,v_z)
					
					// get last item to modify it with the current distance data
					item = TripelAlgorithmPlugin.this.assistantTableView.getLastItem();
					
					// get vertex objects
					transit = graph.getVertexByID(v_transit);
					source = graph.getVertexByID(v_source);
					target = graph.getVertexByID(v_target);
					
					// calculate the smaller distance
					dist = D.get(source.getIndex(), transit.getIndex()) + D.get(transit.getIndex(), target.getIndex());
					
					// get p(v_t,v_z)
					final String pred = P.get(transit.getIndex(), target.getIndex());
					
					// visualize that the distance is smaller
					item.setCellObject(4, COLUMN_CHANGE_TICK);
					sleep(500);
					
					// update changes in the assistant table
					item.setCellBorder(5, TripelAlgorithmPlugin.this.colorModified, 2);
					sleep(250);
					item.setCellObject(5, "d(" + source.getCaption() + "," + target.getCaption() + ")=" + MathUtils.formatFloat(dist));
					sleep(250);
					item.setCellBorder(5, (Color)null);
					sleep(250);
					item.setCellBorder(6, TripelAlgorithmPlugin.this.colorModified, 2);
					sleep(250);
					item.setCellObject(6, "p(" + source.getCaption() + "," + target.getCaption() + ")=" + pred);
					sleep(250);
					item.setCellBorder(6, (Color)null);
					sleep(500);
					
					// update D and P
					D.set(source.getIndex(), target.getIndex(), dist);
					P.set(source.getIndex(), target.getIndex(), pred);
					
					// visualize the update
					TripelAlgorithmPlugin.this.matrixViewD.setElementBackground(source.getIndex(), target.getIndex(), TripelAlgorithmPlugin.this.colorModified);
					sleep(250);
					TripelAlgorithmPlugin.this.matrixViewD.setMatrix(D);
					sleep(250);
					TripelAlgorithmPlugin.this.matrixViewD.setElementBackground(source.getIndex(), target.getIndex(), Color.white);
					
					TripelAlgorithmPlugin.this.matrixViewP.setElementBackground(source.getIndex(), target.getIndex(), TripelAlgorithmPlugin.this.colorModified);
					sleep(250);
					TripelAlgorithmPlugin.this.matrixViewP.setMatrix(P);
					sleep(250);
					TripelAlgorithmPlugin.this.matrixViewP.setElementBackground(source.getIndex(), target.getIndex(), Color.white);
					
					sleep(500);
					
					nextStep = 5;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addSet("transits", transits);
			state.addSet("sources", sources);
			state.addSet("targets", targets);
			state.addMatrix("D", D);
			state.addMatrix("P", P);
			state.addInt("v_transit", v_transit);
			state.addInt("v_source", v_source);
			state.addInt("v_target", v_target);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			transits = state.getSet("transits");
			sources = state.getSet("sources");
			targets = state.getSet("targets");
			D = state.getMatrix("D");
			P = state.getMatrix("P");
			v_transit = state.getInt("v_transit");
			v_source = state.getInt("v_source");
			v_target = state.getInt("v_target");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			transits = state.addSet("transits", new Set<Integer>());
			sources = state.addSet("sources", new Set<Integer>());
			targets = state.addSet("targets", new Set<Integer>());
			D = state.addMatrix("D", new NumericMatrix<Float>(1, 1));
			P = state.addMatrix("P", new ObjectMatrix<String>(1, 1));
			v_transit = state.addInt("v_transit", 0);
			v_source = state.addInt("v_source", 0);
			v_target = state.addInt("v_target", 0);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			if(stepID == 1 || stepID == 2 || stepID == 7) {
				// reset the matrices that were changed in step 1, 2 or 7
				TripelAlgorithmPlugin.this.matrixViewD.setMatrix(D);
				TripelAlgorithmPlugin.this.matrixViewP.setMatrix(P);
			}
			else if(stepID == 5 && nextStepID == 6) {
				// remove the last item that was created in step 5
				TripelAlgorithmPlugin.this.assistantTableView.remove(TripelAlgorithmPlugin.this.assistantTableView.getLastItem());
			}
			else if(stepID == 6 || stepID == 7) {
				// clear the fields in the table that were added in step 6 or 7
				final ExecutionTableItem item = TripelAlgorithmPlugin.this.assistantTableView.getLastItem();
				final int start = (stepID == 6) ? 3 : 4;
				for(int i = start; i <= 6; i++)
					item.setCellObject(i, null);
			}
			
			// update the visual appearance of the graph
			visualizeVertices();
		}
		
		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			// not necessary because we do not override getApplySolutionToAlgorithm() in the exercises of the algorithm
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { TripelAlgorithmPlugin.this.graphView, TripelAlgorithmPlugin.this.matrixViewD, TripelAlgorithmPlugin.this.matrixViewP, TripelAlgorithmPlugin.this.assistantTableView };
		}
		
		/**
		 * Visualizes the vertices in the graph.
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			GraphView<Vertex, Edge>.VisualVertex vv;
			
			// reset current appearance to refresh it
			TripelAlgorithmPlugin.this.graphView.resetVisualAppearance();
			
			// visualize the transit vertex if possible
			vv = TripelAlgorithmPlugin.this.graphView.getVisualVertexByID(v_transit);
			if(vv != null) {
				vv.setBackground(TripelAlgorithmPlugin.this.colorTransitVertex);
				vv.setEdgeWidth(TripelAlgorithmPlugin.this.lineWidthTransitVertex);
			}
			
			// visualize the source vertex if possible
			vv = TripelAlgorithmPlugin.this.graphView.getVisualVertexByID(v_source);
			if(vv != null) {
				vv.setBackground(TripelAlgorithmPlugin.this.colorSourceVertex);
				vv.setEdgeWidth(TripelAlgorithmPlugin.this.lineWidthSourceVertex);
			}
			
			// visualize the target vertex if possible
			vv = TripelAlgorithmPlugin.this.graphView.getVisualVertexByID(v_target);
			if(vv != null) {
				vv.setBackground(TripelAlgorithmPlugin.this.colorTargetVertex);
				vv.setEdgeWidth(TripelAlgorithmPlugin.this.lineWidthTargetVertex);
			}
			
			// show the visualization
			TripelAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Gets the next element in the set.
		 * 
		 * @param set the set
		 * @param without the elements that can not be used
		 * @return the next element
		 * @since 1.0
		 */
		private int forEachGetNext(final Set<Integer> set, int... without) {
			int next = 0;
			
			// remove the vertices that cannot be chosen
			if(without != null)
				for(int i : without)
					set.remove(i);
			
			// get next vertex
			if(set.size() > 0) {
				next = set.get(0);
				set.remove(next);
			}
			
			return next;
		}
		
	}

}
