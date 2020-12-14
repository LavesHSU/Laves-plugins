package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

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
import lavesdk.algorithm.plugin.extensions.CompleteGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.MatrixToGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultGraphView;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.LegendView;
import lavesdk.algorithm.plugin.views.TextAreaView;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.plugin.views.GraphView.SelectionType;
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
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.ElementParser;
import lavesdk.math.Set;
import lavesdk.math.Set.StringElementParser;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.Path;
import lavesdk.math.graph.PathByID;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.utils.GraphUtils;

/**
 * Plugin that visualizes and teaches users the nearest neighbor algorithm.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class NearestNeighborAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the view that displays the Hamiltonian cycle r */
	private TextAreaView cycleView;
	/** the view that displays the set V' */
	private TextAreaView setView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the nearest neighbor algorithm */
	private NearestNeighborRTE rte;
	/** toolbar extension to create graphs from adjacency matrices */
	private MatrixToGraphToolBarExtension<Vertex, Edge> matrixToGraph;
	/** toolbar extension to check whether a graph is complete or to create one */
	private CompleteGraphToolBarExtension<Vertex, Edge> completeExt;
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
	/** the view group for C and D,E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cde;
	/** the view group for A,B,C,D and E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcde;
	
	// modifiable visualization data
	/** color to visualize the vertices of set V' */
	private Color colorSetV_Apostrophe;
	/** color to visualize the starting vertex */
	private Color colorStartVertex;
	/** color to visualize the Hamiltonian cycle r */
	private Color colorCycleR;
	/** color to visualize the vertex v_akt */
	private Color colorV_Akt;
	/** color to visualize the vertex v' */
	private Color colorV_Apostrophe;
	/** color to visualize the edge with the current minimum weight */
	private Color colorCurrEdgeMinWeight;
	/** color to visualize modified objects */
	private Color colorModified;
	/** line with of the starting vertex */
	private int lineWidthStartVertex;
	/** line with of the edges of the Hamiltonian cycle r */
	private int lineWidthCycleR;
	/** line with of the vertex v_akt */
	private int lineWidthV_Akt;
	/** line with of the edge with the current minimum weight */
	private int lineWidthCurrEdgeMinWeight;
	
	/** configuration key for the {@link #creatorPrefsDirectedValue} */
	private static final String CFGKEY_CREATORPROP_DIRECTED = "creatorPropDirected";
	/** configuration key for the {@link #colorSetV_Apostrophe} */
	private static final String CFGKEY_COLOR_SETV_APOSTROPHE = "colorSetV_Apostrophe";
	/** configuration key for the {@link #colorStartVertex} */
	private static final String CFGKEY_COLOR_STARTVERTEX = "colorStartVertex";
	/** configuration key for the {@link #colorCycleR} */
	private static final String CFGKEY_COLOR_CYCLER = "colorCycleR";
	/** configuration key for the {@link #colorV_Akt} */
	private static final String CFGKEY_COLOR_V_AKT = "colorV_Akt";
	/** configuration key for the {@link #colorV_Apostrophe} */
	private static final String CFGKEY_COLOR_V_APOSTROPHE = "colorV_Apostrophe";
	/** configuration key for the {@link #colorCurrEdgeMinWeight} */
	private static final String CFGKEY_COLOR_CURREDGEMINWEIGHT = "colorCurrEdgeMinWeight";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #lineWidthStartVertex} */
	private static final String CFGKEY_LINEWIDTH_STARTVERTEX = "lineWidthStartVertex";
	/** configuration key for the {@link #lineWidthCycleR} */
	private static final String CFGKEY_LINEWIDTH_CYCLER = "lineWidthCycleR";
	/** configuration key for the {@link #lineWidthV_Akt} */
	private static final String CFGKEY_LINEWIDTH_V_AKT = "lineWidthV_Akt";
	/** configuration key for the {@link #lineWidthCurrEdgeMinWeight} */
	private static final String CFGKEY_LINEWIDTH_CURREDGEMINWEIGHT = "lineWidthCurrEdgeMinWeight";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langNearestNeighbor.txt"));
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
		this.cycleView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_CYCLE_TITLE", langID, "Hamiltonian Cycle r"), true, langFile, langID);
		this.setView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_SET_TITLE", langID, "Set V'"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new NearestNeighborRTE();
		this.matrixToGraph = new MatrixToGraphToolBarExtension<>(host, graphView, AllowedGraphType.BOTH, langFile, langID, true);
		this.completeExt = new CompleteGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.BOTH, langFile, langID, true);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, false);
		this.creatorPrefsDirected = LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED", langID, "directed");
		this.creatorPrefsUndirected = LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED", langID, "undirected");
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		cycleView.setAutoRepaint(true);
		setView.setAutoRepaint(true);
		
		// load the creator preference data from the configuration
		creatorPrefsDirectedValue = this.config.getBoolean(CFGKEY_CREATORPROP_DIRECTED, false);
		
		// load the visualization colors from the configuration of the plugin
		colorSetV_Apostrophe = this.config.getColor(CFGKEY_COLOR_SETV_APOSTROPHE, new Color(180, 210, 230));
		colorStartVertex = this.config.getColor(CFGKEY_COLOR_STARTVERTEX, new Color(130, 200, 255));
		colorCycleR = this.config.getColor(CFGKEY_COLOR_CYCLER, new Color(200, 145, 145));
		colorV_Akt = this.config.getColor(CFGKEY_COLOR_V_AKT, new Color(255, 220, 80));
		colorV_Apostrophe = this.config.getColor(CFGKEY_COLOR_V_APOSTROPHE, new Color(235, 190, 80));
		colorCurrEdgeMinWeight = this.config.getColor(CFGKEY_COLOR_CURREDGEMINWEIGHT, new Color(50, 110, 150));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		lineWidthStartVertex = this.config.getInt(CFGKEY_LINEWIDTH_STARTVERTEX, 2);
		lineWidthCycleR = this.config.getInt(CFGKEY_LINEWIDTH_CYCLER, 3);
		lineWidthV_Akt = this.config.getInt(CFGKEY_LINEWIDTH_V_AKT, 2);
		lineWidthCurrEdgeMinWeight = this.config.getInt(CFGKEY_LINEWIDTH_CURREDGEMINWEIGHT, 3);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		cycleView.loadConfiguration(config, "cycleView");
		setView.loadConfiguration(config, "setView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Nearest neighbor");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "A starting algorithm to find a Hamiltonian cycle that contains each vertex of a graph exactly once.");
	}
	
	@Override
	public String getType() {
		return LanguageFile.getLabel(langFile, "ALGO_TYPE", langID, "Heuristic");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A non-negative weighted graph K<sub>n</sub> with n > 2 and a starting vertex v<sub>s</sub>.");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Traveling salesman problem");
	}

	@Override
	public String getSubject() {
		return LanguageFile.getLabel(langFile, "ALGO_SUBJECT", langID, "Logistics");
	}
	
	@Override
	public String getInstructions() {
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extensions to check whether the created graph is complete, to create a complete graph by indicating the number of vertices, to<br>create a graph by use of an adjacency matrix or you can arrange the vertices of your created graph in a circle.<br><br><b>Starting the algorithm</b>:<br>Before you start the algorithm select a vertex v<sub>s</sub> the algorithm should begin with.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
	}

	@Override
	public String getVersion() {
		return "1.3";
	}

	@Override
	public LAVESDKV getUsedSDKVersion() {
		return new LAVESDKV(1, 3);
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
		completeExt.setAllowedGraphType(creatorPrefsDirectedValue ? AllowedGraphType.DIRECTED_ONLY : AllowedGraphType.UNDIRECTED_ONLY);
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|///////////////
		 * / /|/             /	A = algorithm text view
		 * /A/|/      C      /	B = legend view
		 * / /|/             /	C = graph view
		 * ///|///////////////	D = text area view (cycle view)
		 * ---|---------------	E = text area view (set view)
		 * ///|///////|///////
		 * /B/|/  D  /|/  E  /
		 * ///|///////|///////
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		de = new ViewGroup(ViewGroup.HORIZONTAL);
		cde = new ViewGroup(ViewGroup.VERTICAL);
		abcde = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// bottom right group D and E
		de.add(cycleView);
		de.add(setView);
		de.restoreWeights(config, "weights_de", new float[] { 0.6f, 0.4f });
		
		// right group C and (D,E)
		cde.add(graphView);
		cde.add(de);
		cde.restoreWeights(config, "weights_cde", new float[] { 0.7f, 0.3f });
		
		// group for (A,B) and (C,(D,E))
		abcde.add(ab);
		abcde.add(cde);
		abcde.restoreWeights(config, "weights_abcde", new float[] { 0.4f, 0.6f });
		
		container.setLayout(new BorderLayout());
		container.add(abcde, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		cycleView.saveConfiguration(config, "cycleView");
		setView.saveConfiguration(config, "setView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(de != null)
			de.storeWeights(config, "weights_de");
		if(cde != null)
			cde.storeWeights(config, "weights_cde");
		if(abcde != null)
			abcde.storeWeights(config, "weights_abcde");
		
		// reset view content where it is necessary
		graphView.reset();
		cycleView.reset();
		setView.reset();
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_STARTVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_STARTVERTEX", langID, "Background color of the starting vertex v<sub>s</sub>"), colorStartVertex));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETV_APOSTROPHE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SETV_APOSTROPHE", langID, "Background color of the vertices of set V'"), colorSetV_Apostrophe));
		plm.add(new ColorProperty(CFGKEY_COLOR_CYCLER, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CYCLER", langID, "Color of the Hamiltonian cycle r"), colorCycleR));
		plm.add(new ColorProperty(CFGKEY_COLOR_V_AKT, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_V_AKT", langID, "Background color of the vertex v<sub>akt</sub>"), colorV_Akt));
		plm.add(new ColorProperty(CFGKEY_COLOR_V_APOSTROPHE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_V_APOSTROPHE", langID, "Background color of the vertex v'"), colorV_Apostrophe));
		plm.add(new ColorProperty(CFGKEY_COLOR_CURREDGEMINWEIGHT, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURREDGEMINWEIGHT", langID, "Color of the edge with a currently minimum weight searching for a vertex v'"), colorCurrEdgeMinWeight));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwStartVertex = new NumericProperty(CFGKEY_LINEWIDTH_STARTVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_STARTVERTEX", langID, "Line with of the starting vertex v<sub>s</sub>"), lineWidthStartVertex, true);
		lwStartVertex.setMinimum(1);
		lwStartVertex.setMaximum(5);
		plm.add(lwStartVertex);
		final NumericProperty lwCycleR = new NumericProperty(CFGKEY_LINEWIDTH_CYCLER, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_CYCLER", langID, "Line with of the Hamiltonian cycle r"), lineWidthCycleR, true);
		lwCycleR.setMinimum(1);
		lwCycleR.setMaximum(5);
		plm.add(lwCycleR);
		final NumericProperty lwV_Akt = new NumericProperty(CFGKEY_LINEWIDTH_V_AKT, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_V_AKT", langID, "Line with of the vertex v<sub>akt</sub>"), lineWidthV_Akt, true);
		lwV_Akt.setMinimum(1);
		lwV_Akt.setMaximum(5);
		plm.add(lwV_Akt);
		final NumericProperty lwCurrEdgeMinDist = new NumericProperty(CFGKEY_LINEWIDTH_CURREDGEMINWEIGHT, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_CURREDGEMINDIST", langID, "Line with of the edge with the currently minimum weight"), lineWidthCurrEdgeMinWeight, true);
		lwCurrEdgeMinDist.setMinimum(1);
		lwCurrEdgeMinDist.setMaximum(5);
		plm.add(lwCurrEdgeMinDist);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorStartVertex = config.addColor(CFGKEY_COLOR_STARTVERTEX, plm.getColorProperty(CFGKEY_COLOR_STARTVERTEX).getValue());
		colorSetV_Apostrophe = config.addColor(CFGKEY_COLOR_SETV_APOSTROPHE, plm.getColorProperty(CFGKEY_COLOR_SETV_APOSTROPHE).getValue());
		colorCycleR = config.addColor(CFGKEY_COLOR_CYCLER, plm.getColorProperty(CFGKEY_COLOR_CYCLER).getValue());
		colorV_Akt = config.addColor(CFGKEY_COLOR_V_AKT, plm.getColorProperty(CFGKEY_COLOR_V_AKT).getValue());
		colorV_Apostrophe = config.addColor(CFGKEY_COLOR_V_APOSTROPHE, plm.getColorProperty(CFGKEY_COLOR_V_APOSTROPHE).getValue());
		colorCurrEdgeMinWeight = config.addColor(CFGKEY_COLOR_CURREDGEMINWEIGHT, plm.getColorProperty(CFGKEY_COLOR_CURREDGEMINWEIGHT).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthStartVertex = config.addInt(CFGKEY_LINEWIDTH_STARTVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_STARTVERTEX).getValue().intValue());
		lineWidthCycleR = config.addInt(CFGKEY_LINEWIDTH_CYCLER, plm.getNumericProperty(CFGKEY_LINEWIDTH_CYCLER).getValue().intValue());
		lineWidthV_Akt = config.addInt(CFGKEY_LINEWIDTH_V_AKT, plm.getNumericProperty(CFGKEY_LINEWIDTH_V_AKT).getValue().intValue());
		lineWidthCurrEdgeMinWeight = config.addInt(CFGKEY_LINEWIDTH_CURREDGEMINWEIGHT, plm.getNumericProperty(CFGKEY_LINEWIDTH_CURREDGEMINWEIGHT).getValue().intValue());
		
		// recreate the legend
		createLegend();
	}
	
	@Override
	public ToolBarExtension[] getToolBarExtensions() {
		return new ToolBarExtension[] { matrixToGraph, completeExt, circleLayoutExt };
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
		// check the assumptions
		if(graphView.getGraph().getOrder() <= 2) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_INSUFFICIENTVERTEXCOUNT", langID, "The created graph does not comply with the assumptions!\nThe vertex count is insufficient."), LanguageFile.getLabel(langFile, "MSG_INFO_INSUFFICIENTVERTEXCOUNT_TITLE", langID, "Invalid graph"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(graphView.getSelectedVertexCount() != 1) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTVERTEX", langID, "Please select the starting vertex in the graph!"), LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTVERTEX_TITLE", langID, "Select starting vertex"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(containsGraphNegativeWeights()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS", langID, "The created graph contains edges with a negative weight!\nThe Nearest neighbor algorithm can only be applied to non-negative weighted graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS_TITLE", langID, "Negative weights"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(!GraphUtils.isComplete(graphView.getGraph())) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NOTCOMPLETE", langID, "The created graph is not complete!\nThe Nearest neighbor algorithm can only be applied to complete graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NOTCOMPLETE_TITLE", langID, "No complete graph"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			// get the starting vertex
			final Vertex startVertex = graphView.getSelectedVertex(0).getVertex();
			
			graphView.deselectAll();
			graphView.setEditable(false);
			
			// set the start vertex (after disable the edit mode) because the start mode should be visualized
			rte.setStartVertex(startVertex);
			
			// reset the views
			cycleView.reset();
			setView.reset();
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
		
		final AlgorithmExercise<String> step1_5_8 = new AlgorithmExercise<String>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1_5_8", langID, "What is the current Hamiltonian cycle <i>r</i>?"), 1.0f) {
			
			@Override
			protected String[] requestSolution() {
				final SolutionEntry<JTextField> entry = new SolutionEntry<JTextField>("r=", new JTextField());
				
				if(!SolveExercisePane.showDialog(NearestNeighborAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entry }, NearestNeighborAlgorithmPlugin.this.langFile, NearestNeighborAlgorithmPlugin.this.langID, LanguageFile.getLabel(NearestNeighborAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", NearestNeighborAlgorithmPlugin.this.langID, "Use a comma as the delimiter!<br>Enter the Hamiltonian cycle in the following form: v<sub>s</sub>, v<sub>i</sub>, v<sub>j</sub>, ...")))
					return null;
				
				final Path<Vertex> p = GraphUtils.toPath(entry.getComponent().getText(), NearestNeighborAlgorithmPlugin.this.graphView.getGraph());
				
				if(p == null) {
					NearestNeighborAlgorithmPlugin.this.host.showMessage(NearestNeighborAlgorithmPlugin.this, LanguageFile.getLabel(NearestNeighborAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT", NearestNeighborAlgorithmPlugin.this.langID, "Your input is incorrect!\nPlease enter the Hamiltonian cycle in the specified form and only use vertex captions that are existing."), LanguageFile.getLabel(NearestNeighborAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT_TITLE", NearestNeighborAlgorithmPlugin.this.langID, "Invalid input"), MessageIcon.INFO);
					return null;
				}
				
				return new String[] { entry.getComponent().getText() };
			}
			
			@Override
			protected String getResultAsString(String result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					if(result.startsWith("(") && result.endsWith(")"))
						return "r=" + result;
					else
						return "r=(" + result + ")";
				}
			}
			
			@Override
			protected boolean examine(String[] results, AlgorithmState state) {
				final PathByID<Vertex> r = state.getPath("r", NearestNeighborAlgorithmPlugin.this.graphView.getGraph());
				final Path<Vertex> p = GraphUtils.toPath(results[0], NearestNeighborAlgorithmPlugin.this.graphView.getGraph());
				
				return r.equals(p.cast());
			}
		};
		
		final AlgorithmExercise<Set<?>> step2_6 = new AlgorithmExercise<Set<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2_6", langID, "What is <i>V'</i>?"), 1.0f) {
			
			@Override
			protected Set<?>[] requestSolution() {
				final SolutionEntry<JTextField> entryV_Apo = new SolutionEntry<JTextField>("V'=", new JTextField());
				
				if(!SolveExercisePane.showDialog(NearestNeighborAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryV_Apo }, NearestNeighborAlgorithmPlugin.this.langFile, NearestNeighborAlgorithmPlugin.this.langID, LanguageFile.getLabel(NearestNeighborAlgorithmPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", NearestNeighborAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				
				final ElementParser<String> parser = new StringElementParser();
				final Set<String> V_Apo = Set.parse(entryV_Apo.getComponent().getText(), parser);
				
				return new Set<?>[] { V_Apo };
			}
			
			@Override
			protected String getResultAsString(Set<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return "V'=" + super.getResultAsString(result, index);
			}
			
			@Override
			protected boolean examine(Set<?>[] results, AlgorithmState state) {
				// convert the input set to a set with the identifiers of the vertices to use auto examination
				final Set<Integer> V_Apo = NearestNeighborAlgorithmPlugin.this.toIDs(results[0]);
				return doAutoExamine(state, new String[] { "V_Apo" }, new Set<?>[] { V_Apo });
			}
		};
		
		// create paragraphs
		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_INITIALIZATION", langID, "1. Initialization:"), 1);
		final AlgorithmParagraph itParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ITERATION", langID, "2. Iteration:"), 2);
		final AlgorithmParagraph expParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_EXPANSION", langID, "3. Expansion:"), 3);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOP", langID, "4. Stop:"), 4);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INITR", langID, "Let _latex{$r := (v_s)$} and\n"), 1);
		step.setExercise(step1_5_8);
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_INITV_APOSTROPHE", langID, "_latex{$V' = V \\setminus \\{v_s\\}$}.\n\n"), 2);
		step.setExercise(step2_6);
		
		// 2. iteration
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_ITERATION", langID, "Let _latex{$v_{akt}$} be the last vertex in _latex{$r$} "), 3);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Select <i>v<sub>akt</sub></i> in the graph."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				NearestNeighborAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				NearestNeighborAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				NearestNeighborAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				NearestNeighborAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				NearestNeighborAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(NearestNeighborAlgorithmPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { NearestNeighborAlgorithmPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return NearestNeighborAlgorithmPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "v_akt" }, results);
			}
		});
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_ITERATION", langID, "and _latex{$v' \\in \\; \\underset{v \\in V'}{argmin} \\; c(v_{akt},v)$}.\n\n"), 4);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "Select <i>v'</i> in the graph."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				NearestNeighborAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				NearestNeighborAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				NearestNeighborAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				NearestNeighborAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				NearestNeighborAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(NearestNeighborAlgorithmPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { NearestNeighborAlgorithmPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return NearestNeighborAlgorithmPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Integer[] solutions) {
				state.addInt("v_Apo", solutions[0]);
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				final Graph<Vertex, Edge> graph = NearestNeighborAlgorithmPlugin.this.graphView.getGraph();
				final Vertex vertex_akt = graph.getVertexByID(state.getInt("v_akt"));
				final Vertex vertex_apo = graph.getVertexByID(results[0]);
				final Set<Integer> V_Apo = state.getSet("V_Apo");
				float minWeight = Float.MAX_VALUE;
				Edge e;
				Vertex v_apostrophe;
				
				// find the smallest weight
				for(int i = 0; i < vertex_akt.getOutgoingEdgeCount(); i++) {
					e = vertex_akt.getOutgoingEdge(i);
					v_apostrophe = e.getSuccessor(vertex_akt);
					if(V_Apo.contains(v_apostrophe.getID()) && e.getWeight() < minWeight)
						minWeight = e.getWeight();
				}
				
				// and this has to be the same as the one between v_akt und v_Apo and furthermore V_Apo must contain v_Apo
				e = graph.getEdge(vertex_akt, vertex_apo);
				return (e != null) && V_Apo.contains(vertex_apo.getID()) && e.getWeight() == minWeight;
			}
		});

		// 3. expansion
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_EXPANSIONR", langID, "Add _latex{$v'$} to _latex{$r$}.\n"), 5);
		step.setExercise(step1_5_8);
		
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_EXPANSIONV_APOSTROPHE", langID, "Set _latex{$V' = V' \\setminus \\{v'\\}$}.\n"), 6);
		step.setExercise(step2_6);
		
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_EXPANSION", langID, "If _latex{$V' \\neq \\emptyset$} then go to 2.\n\n"), 7);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP7", langID, "Into what step will the algorithm pass?"), 1.0f) {
			
			private final String labelIteration = LanguageFile.getLabel(NearestNeighborAlgorithmPlugin.this.langFile, "EXERCISE_STEP7_ITERATION", NearestNeighborAlgorithmPlugin.this.langID, "2. Iteration");
			private final String labelStop = LanguageFile.getLabel(NearestNeighborAlgorithmPlugin.this.langFile, "EXERCISE_STEP7_STOP", NearestNeighborAlgorithmPlugin.this.langID, "4. Stop");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelIteration);
				final JRadioButton rdobtn2 = new JRadioButton(labelStop);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryIt = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryStop = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(NearestNeighborAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryIt,  entryStop }, NearestNeighborAlgorithmPlugin.this.langFile, NearestNeighborAlgorithmPlugin.this.langID))
					return null;
				
				return new Boolean[] { rdobtn1.isSelected() };
			}
			
			@Override
			protected String getResultAsString(Boolean result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return (result == Boolean.TRUE) ? labelIteration : labelStop;
			}
			
			@Override
			protected boolean examine(Boolean[] results, AlgorithmState state) {
				final Set<Integer> V_Apo = state.getSet("V_Apo");
				
				return results[0] == !V_Apo.isEmpty();
			}
		});
		
		// 4. stop
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP8_STOP", langID, "Add _latex{$v_s$} to _latex{$r$} so that a cycle develops."), 8);
		step.setExercise(step1_5_8);
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_STARTVERTEX", langID, "The starting vertex v<sub>s</sub>"), LegendItem.createCircleIcon(colorStartVertex, Color.black, lineWidthStartVertex)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SETV_APOSTROPHE", langID, "The vertices of the set V'"), LegendItem.createCircleIcon(colorSetV_Apostrophe, Color.black, 1)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CYCLER", langID, "The Hamiltonian cycle r"), LegendItem.createLineIcon(colorCycleR, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_V_AKT", langID, "The vertex v<sub>akt</sub>"), LegendItem.createCircleIcon(colorV_Akt, Color.black, lineWidthV_Akt)));
		legendView.add(new LegendItem("item5", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_V_APOSTROPHE", langID, "The vertex v'"), LegendItem.createCircleIcon(colorV_Apostrophe, Color.black, 1)));
		legendView.add(new LegendItem("item6", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURREDGEMINWEIGHT", langID, "The current edge with the minimum weight searching for a vertex v'"), LegendItem.createLineIcon(colorCurrEdgeMinWeight, lineWidthCurrEdgeMinWeight, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item7", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_EDGESMINWEIGHT", langID, "The edges that are investigated searching for a vertex v'"), LegendItem.createLineIcon(colorCurrEdgeMinWeight, 1, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item8", cycleView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_CYCLE_MODIFICATION", langID, "The Hamiltonian cycle r becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item9", setView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_SET_MODIFICATION", langID, "The set V' becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
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
	 * The runtime environment of the Nearest neighbor algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class NearestNeighborRTE extends AlgorithmRTE {
		
		/** the starting vertex */
		private Vertex v_s;
		/** the Hamiltonian cycle r */
		private Path<Vertex> r;
		/** the set of the (unvisited) vertices */
		private Set<Integer> V_Apo;
		/** the vertex v_akt */
		private int v_akt;
		/** the vertex v' */
		private int v_Apo;
		/** the user's choice of {@link #v_Apo} */
		private int userChoice_v_Apo;

		/**
		 * Creates the runtime environment.
		 * 
		 * @since 1.0
		 */
		public NearestNeighborRTE() {
			super(NearestNeighborAlgorithmPlugin.this, NearestNeighborAlgorithmPlugin.this.algoText);
			
			v_s = null;
			userChoice_v_Apo = 0;
		}
		
		/**
		 * Sets the starting vertex of the algorithm and visualizes the vertex.
		 * 
		 * @param v the vertex
		 * @since 1.0
		 */
		public void setStartVertex(final Vertex v) {
			v_s = v;
			visualizeVertices();
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = NearestNeighborAlgorithmPlugin.this.graphView.getGraph();
			int nextStep = -1;
			
			switch(stepID) {
				case 1:
					// let r := (v_s)
					
					r = new Path<Vertex>(graph);
					r.add(v_s);
					
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.cycleView.setBackground(NearestNeighborAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeCycleAsText();
					visualizeCycle();
					visualizeVertices();
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.cycleView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 2;
					break;
				case 2:
					// let V' = V \ {v_s}
					
					V_Apo = graph.getVertexByIDSet();
					V_Apo.remove(v_s.getID());
					
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.setView.setBackground(NearestNeighborAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeSetAsText();
					visualizeVertices();
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.setView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 3;
					break;
				case 3:
					// let v_akt be the last vertex in r
					
					v_akt = r.get(r.length()).getID();
					
					sleep(500);
					visualizeVertices();
					sleep(750);
					
					nextStep = 4;
					break;
				case 4:
					// and let v' in V' be the neighbor of v_akt with the lowest weight

					Vertex vertex_akt = graph.getVertexByID(v_akt);
					Edge e;
					Edge minWeightEdge = null;
					GraphView<Vertex, Edge>.VisualEdge ve = null;
					
					// if the user has solved the corresponding exercise correctly then take this choice
					if(userChoice_v_Apo > 0)
						v_Apo = userChoice_v_Apo;
					else {
						v_Apo = 0;
						
						float minWeight = Float.MAX_VALUE;
						Vertex v_apostrophe;
						
						for(int i = 0; i < vertex_akt.getOutgoingEdgeCount(); i++) {
							e = vertex_akt.getOutgoingEdge(i);
							v_apostrophe = e.getSuccessor(vertex_akt);
							
							if(V_Apo.contains(v_apostrophe.getID())) {
								// highlight all edges that are under investigation
								ve = NearestNeighborAlgorithmPlugin.this.graphView.getVisualEdge(e);
								
								// highlight the current edge
								ve.setColor(NearestNeighborAlgorithmPlugin.this.colorCurrEdgeMinWeight);
								ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
								
								if(e.getWeight() < minWeight) {
									minWeight = e.getWeight();
									minWeightEdge = e;
									v_Apo = v_apostrophe.getID();
								}
							}
						}
					}
					
					// clear the user's choice
					userChoice_v_Apo = 0;
					
					// repaint the graph to make the highlighted edges visible
					NearestNeighborAlgorithmPlugin.this.graphView.repaint();
					
					sleep(500);
					
					ve = (minWeightEdge != null) ? NearestNeighborAlgorithmPlugin.this.graphView.getVisualEdge(minWeightEdge) : null;
					if(ve != null) {
						// highlight the edge with a minimum weight
						ve.setColor(NearestNeighborAlgorithmPlugin.this.colorCurrEdgeMinWeight);
						ve.setLineWidth(NearestNeighborAlgorithmPlugin.this.lineWidthCurrEdgeMinWeight);
						NearestNeighborAlgorithmPlugin.this.graphView.repaint();
						
						sleep(1000);
					}
					
					// remove the highlight from all outgoing edges by repaint the cycle with affects all edges
					visualizeCycle();
					
					visualizeVertices();
					sleep(1000);
					
					if(v_Apo < 1)
						nextStep = -1;
					else
						nextStep = 5;
					break;
				case 5:
					// add v' to r
					
					r.add(graph.getVertexByID(v_Apo));
					
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.cycleView.setBackground(NearestNeighborAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeCycleAsText();
					visualizeCycle();
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.cycleView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 6;
					break;
				case 6:
					// and set V' = V' \ {v'}
					
					V_Apo.remove(v_Apo);
					
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.setView.setBackground(NearestNeighborAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeSetAsText();
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.setView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 7;
					break;
				case 7:
					// if V' is not empty then go to step 2 (stepid=3)
					
					sleep(750);
					
					if(!V_Apo.isEmpty())
						nextStep = 3;
					else
						nextStep = 8;
					break;
				case 8:
					// add v_s to r so that a cycle develops
					
					r.add(v_s);
					
					// clear the helper vertices to visualize the final presentation
					v_akt = 0;
					v_Apo = 0;
					
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.cycleView.setBackground(NearestNeighborAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeCycleAsText();
					visualizeCycle();
					visualizeVertices();
					sleep(250);
					NearestNeighborAlgorithmPlugin.this.cycleView.setBackground(Color.white);
					sleep(250);
					
					nextStep = -1;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addPath("r", (r != null) ? r.cast() : null);
			state.addSet("V_Apo", V_Apo);
			state.addInt("v_akt", v_akt);
			state.addInt("v_Apo", v_Apo);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			final PathByID<Vertex> tmpR = state.getPath("r", graphView.getGraph());
			r = (tmpR != null) ? tmpR.cast() : null;
			V_Apo = state.getSet("V_Apo");
			v_akt = state.getInt("v_akt");
			v_Apo = state.getInt("v_Apo");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			final PathByID<Vertex> tmpR = state.addPath("r", null);
			r = (tmpR != null) ? tmpR.cast() : null;
			V_Apo = state.addSet("V_Apo", new Set<Integer>());
			v_akt = state.addInt("v_akt", 0);
			v_Apo = state.addInt("v_Apo", 0);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			switch(stepID) {
				case 1:
				case 8:
					visualizeCycle();
					visualizeCycleAsText();
					visualizeVertices();
					break;
				case 2:
					visualizeVertices();
					visualizeSetAsText();
					break;
				case 3:
				case 4:
					visualizeVertices();
					break;
				case 5:
					visualizeCycle();
					visualizeCycleAsText();
					break;
				case 6:
					visualizeSetAsText();
					break;
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			if(stepID == 4)
				userChoice_v_Apo = state.getInt("v_Apo", 0);
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { NearestNeighborAlgorithmPlugin.this.graphView, NearestNeighborAlgorithmPlugin.this.cycleView, NearestNeighborAlgorithmPlugin.this.setView };
		}
		
		/**
		 * Visualizes the vertices of the graph using the information of {@link #v_s}, {@link #v_akt}, {@link #v_Apo}
		 * and {@link #V_Apo}.
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			GraphView<Vertex, Edge>.VisualVertex vv;
			
			for(int i = 0; i < NearestNeighborAlgorithmPlugin.this.graphView.getVisualVertexCount(); i++) {
				vv = NearestNeighborAlgorithmPlugin.this.graphView.getVisualVertex(i);
				
				if(vv.getVertex().getID() == v_akt) {
					vv.setBackground(NearestNeighborAlgorithmPlugin.this.colorV_Akt);
					vv.setEdgeWidth(NearestNeighborAlgorithmPlugin.this.lineWidthV_Akt);
				}
				else if(vv.getVertex().getID() == v_s.getID()) {
					vv.setBackground(NearestNeighborAlgorithmPlugin.this.colorStartVertex);
					vv.setEdgeWidth(NearestNeighborAlgorithmPlugin.this.lineWidthStartVertex);
				}
				else if(vv.getVertex().getID() == v_Apo) {
					vv.setBackground(NearestNeighborAlgorithmPlugin.this.colorV_Apostrophe);
					vv.setEdgeWidth(1);
				}
				else if(V_Apo.contains(vv.getVertex().getID())) {
					vv.setBackground(NearestNeighborAlgorithmPlugin.this.colorSetV_Apostrophe);
					vv.setEdgeWidth(1);
				}
				else {
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
					vv.setEdgeWidth(1);
				}
			}
			
			// show the visualization
			NearestNeighborAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the cycle r in the graph.
		 * 
		 * @since 1.0
		 */
		private void visualizeCycle() {
			if(r == null)
				return;
			
			GraphView<Vertex, Edge>.VisualEdge ve;
			
			for(int i = 0; i < NearestNeighborAlgorithmPlugin.this.graphView.getVisualEdgeCount(); i++) {
				ve = NearestNeighborAlgorithmPlugin.this.graphView.getVisualEdge(i);
				
				// visualize edges of the Hamiltonian cycle r
				if(r.contains(ve.getEdge())) {
					ve.setColor(NearestNeighborAlgorithmPlugin.this.colorCycleR);
					ve.setLineWidth(NearestNeighborAlgorithmPlugin.this.lineWidthCycleR);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			// show the visualization
			NearestNeighborAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the Hamiltonian cycle r in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeCycleAsText() {
			NearestNeighborAlgorithmPlugin.this.cycleView.setText((r != null) ? "r=" + r.toString() : "");
		}
		
		/**
		 * Visualizes the set V' in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeSetAsText() {
			NearestNeighborAlgorithmPlugin.this.setView.setText((V_Apo != null) ? "V'=" + toCaptions(V_Apo) : "");
		}
		
		/**
		 * Converts the given set of vertex identifiers to a set of related vertex captions.
		 * 
		 * @param set the set of vertex identifiers
		 * @return the converted set
		 * @since 1.0
		 */
		private Set<String> toCaptions(final Set<Integer> set) {
			final Graph<Vertex, Edge> graph = NearestNeighborAlgorithmPlugin.this.graphView.getGraph();
			final Set<String> res = new Set<String>(set.size());
			
			for(Integer id : set)
				res.add(graph.getVertexByID(id).getCaption());
			
			return res;
		}
		
	}

}
