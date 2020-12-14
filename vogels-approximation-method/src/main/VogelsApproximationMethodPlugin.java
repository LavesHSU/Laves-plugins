package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import lavesdk.algorithm.plugin.extensions.BipartiteLayoutToolBarExtension;
import lavesdk.algorithm.plugin.extensions.CircleLayoutToolBarExtension;
import lavesdk.algorithm.plugin.extensions.CompleteBipartiteGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.CompleteGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.MatrixToGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultGraphView;
import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.LegendView;
import lavesdk.algorithm.plugin.views.MatrixView;
import lavesdk.algorithm.plugin.views.TextAreaView;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.plugin.views.GraphView.SelectionType;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.algorithm.text.Annotation;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.dialogs.enums.AllowedGraphType;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.ExecutionTableColumn;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.Mask;
import lavesdk.gui.widgets.MatrixEditor;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.ElementParser;
import lavesdk.math.Set;
import lavesdk.math.Set.StringElementParser;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.matching.Matching;
import lavesdk.math.graph.matching.MatchingByID;
import lavesdk.utils.GraphUtils;
import lavesdk.utils.MathUtils;

/**
 * Plugin that visualizes and teaches users the vogels approximation method to find a perfect matching of low weight.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class VogelsApproximationMethodPlugin implements AlgorithmPlugin {
	
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
	/** the view that displays the set L */
	private TextAreaView setView;
	/** the view that displays the matching */
	private TextAreaView matchingView;
	/** the view that displays the regret */
	private ExecutionTableView regretView;
	/** the view that displays the adjacency matrix as a helper */
	private MatrixView<Float> adjacencyMatrixView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the method */
	private VogelsApproximationRTE rte;
	/** toolbar extension to check whether a graph is complete or to create one */
	private CompleteGraphToolBarExtension<Vertex, Edge> completeExt;
	/** toolbar extension to layout a graph in a circle */
	private CircleLayoutToolBarExtension<Vertex, Edge> circleLayoutExt;
	/** toolbar extension to check whether a graph is complete bipartite or to create one */
	private CompleteBipartiteGraphToolBarExtension<Vertex, Edge> completeBipartiteExt;
	/** toolbar extension to layout a graph bipartite */
	private BipartiteLayoutToolBarExtension<Vertex, Edge> bipartiteLayoutExt;
	/** toolbar extension to create graphs from adjacency matrices */
	private MatrixToGraphToolBarExtension<Vertex, Edge> matrixToGraph;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for D,E,F and G (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup defg;
	/** the view group for C and (D,E,F,G) (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cdefg;
	/** the view group for A,B,C,D,E,F and G (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcdefg;

	// modifiable visualization data
	/** color to visualize modifications */
	private Color colorModified;
	/** color to visualize matched edges */
	private Color colorMatchedEdges;
	/** color to visualize the current vertices v and v1 */
	private Color colorCurrVertices;
	/** color to visualize the current edge */
	private Color colorCurrEdge;
	/** color to visualize the vertices of set L */
	private Color colorSetL;
	/** color to visualize the minimum weights of the regret of a vertex */
	private Color colorMinWeights;
	/** color to visualize the largest regret */
	private Color colorLargestRegret;
	/** line width of matched edges */
	private int lineWidthMatchedEdges;
	/** line width of the current edge */
	private int lineWidthCurrEdge;
	
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #colorMatchedEdges} */
	private static final String CFGKEY_COLOR_MATCHEDEDGES = "colorMatchedEdges";
	/** configuration key for the {@link #colorCurrVertices} */
	private static final String CFGKEY_COLOR_CURRVERTICES= "colorCurrVertices";
	/** configuration key for the {@link #colorCurrEdge} */
	private static final String CFGKEY_COLOR_CURREDGE = "colorCurrEdge";
	/** configuration key for the {@link #colorSetL} */
	private static final String CFGKEY_COLOR_SETL = "colorSetL";
	/** configuration key for the {@link #colorMinWeights} */
	private static final String CFGKEY_COLOR_MINWEIGHTS = "colorMinWeights";
	/** configuration key for the {@link #colorLargestRegret} */
	private static final String CFGKEY_COLOR_LARGESTREGRET = "colorLargestRegret";
	/** configuration key for the {@link #lineWidthMatchedEdges} */
	private static final String CFGKEY_LINEWIDTH_MATCHEDEDGES = "lineWidthMatchedEdges";
	/** configuration key for the {@link #lineWidthCurrEdge} */
	private static final String CFGKEY_LINEWIDTH_CURREDGE = "lineWidthCurrEdge";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langVogelsApproximation.txt"));
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
		this.setView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_SET_TITLE", langID, "Set L"), true, langFile, langID);
		this.matchingView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_MATCHING_TITLE", langID, "Matching M"), true, langFile, langID);
		this.regretView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_REGRET_TITLE", langID, "Regret"), true, langFile, langID);
		this.adjacencyMatrixView = new MatrixView<Float>(LanguageFile.getLabel(langFile, "VIEW_ADJACENCYMATRIX_TITLE", langID, "Adjacency Matrix"), new MatrixEditor.FloatElementFormat(), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new VogelsApproximationRTE();
		this.completeExt = new CompleteGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, false);
		this.completeBipartiteExt = new CompleteBipartiteGraphToolBarExtension<Vertex, Edge>(host, graphView, langFile, langID, true);
		this.bipartiteLayoutExt = new BipartiteLayoutToolBarExtension<Vertex, Edge>(graphView, true, langFile, langID, false);
		this.matrixToGraph = new MatrixToGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		setView.setAutoRepaint(true);
		matchingView.setAutoRepaint(true);
		regretView.setAutoRepaint(true);
		adjacencyMatrixView.setAutoRepaint(true);
		
		regretView.setAutoResizeColumns(false);
		
		// all values that are zero should be displayed as a "-"
		adjacencyMatrixView.addMask(new Mask(0.0f, "-"));
		
		// load the visualization colors from the configuration of the plugin
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		colorMatchedEdges = this.config.getColor(CFGKEY_COLOR_MATCHEDEDGES, Color.black);
		colorCurrVertices = this.config.getColor(CFGKEY_COLOR_CURRVERTICES, new Color(255, 220, 80));
		colorCurrEdge = this.config.getColor(CFGKEY_COLOR_CURREDGE, new Color(220, 105, 105));
		colorSetL = this.config.getColor(CFGKEY_COLOR_SETL, new Color(180, 210, 230));
		colorMinWeights = this.config.getColor(CFGKEY_COLOR_MINWEIGHTS, new Color(120, 210, 80));
		colorLargestRegret = this.config.getColor(CFGKEY_COLOR_LARGESTREGRET, new Color(255, 220, 80));
		lineWidthMatchedEdges = this.config.getInt(CFGKEY_LINEWIDTH_MATCHEDEDGES, 3);
		lineWidthCurrEdge = this.config.getInt(CFGKEY_LINEWIDTH_CURREDGE, 2);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		setView.loadConfiguration(config, "setView");
		matchingView.loadConfiguration(config, "matchingView");
		regretView.loadConfiguration(config, "regretView");
		adjacencyMatrixView.loadConfiguration(config, "adjacencyMatrixView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Greedy algorithm");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds a perfect matching <i>M</i> with a low weight of the edges.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A weighted complete graph K<sub>n</sub> with <i>n mod 2 = 0</i> (even number of vertices) or a weighted complete bipartite graph K<sub>n/2,n/2</sub>, n = |V|.");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Matching problem");
	}

	@Override
	public String getSubject() {
		return LanguageFile.getLabel(langFile, "ALGO_SUBJECT", langID, "Logistics");
	}

	@Override
	public String getInstructions() {
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extensions to check whether the created graph is complete or complete bipartite, to create a complete graph or a complete bipartite graph<br>by indicating the number of vertices, to create a graph by use of an adjacency matrix or you can arrange the vertices of your created graph<br>in a predefined layout.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
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
	public boolean hasExerciseMode() {
		return true;
	}

	@Override
	public Configuration getConfiguration() {
		return config;
	}

	@Override
	public boolean hasCreatorPreferences() {
		return false;
	}

	@Override
	public void loadCreatorPreferences(PropertiesListModel plm) {
	}

	@Override
	public void onCreate(ViewContainer container, PropertiesListModel creatorProperties) {
		// change the graph in the view
		graphView.setGraph(new SimpleGraph<Vertex, Edge>(false));
		graphView.repaint();
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|///////////////
		 * / /|/             /	A = algorithm text view
		 * /A/|/      C      /	B = legend view
		 * / /|/             /	C = graph view
		 * ///|///////////////	D = text area view (set view)
		 * ---|---------------	E = text area view (matching view)
		 * ///|///|///|///|///	F = matrix view
		 * /B/|/D/|/E/|/F/|/G/	G = execution table view (regret view)
		 * ///|///|///|///|///
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		defg = new ViewGroup(ViewGroup.HORIZONTAL);
		cdefg = new ViewGroup(ViewGroup.VERTICAL);
		abcdefg = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// bottom right group D, E, F and G
		defg.add(setView);
		defg.add(matchingView);
		defg.add(adjacencyMatrixView);
		defg.add(regretView);
		defg.restoreWeights(config, "weights_defg", new float[] { 0.25f, 0.25f, 0.25f, 0.25f });
		
		// right group for C and (D,E,F,G)
		cdefg.add(graphView);
		cdefg.add(defg);
		cdefg.restoreWeights(config, "weights_cdefg", new float[] { 0.6f, 0.4f });
		
		// group for (A,B) and (C,(D,E,F,G))
		abcdefg.add(ab);
		abcdefg.add(cdefg);
		abcdefg.restoreWeights(config, "weights_abcdefg", new float[] { 0.4f, 0.6f });
		
		container.setLayout(new BorderLayout());
		container.add(abcdefg, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		setView.saveConfiguration(config, "setView");
		matchingView.saveConfiguration(config, "matchingView");
		adjacencyMatrixView.saveConfiguration(config, "adjacencyMatrixView");
		regretView.saveConfiguration(config, "regretView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(defg != null)
			defg.storeWeights(config, "weights_defg");
		if(cdefg != null)
			cdefg.storeWeights(config, "weights_cdefg");
		if(abcdefg != null)
			abcdefg.storeWeights(config, "weights_abcdefg");
		
		// reset view content where it is necessary
		graphView.reset();
		setView.reset();
		matchingView.reset();
		adjacencyMatrixView.reset();
		regretView.reset();
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_MATCHEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MATCHEDEDGES", langID, "Color of the matching edges"), colorMatchedEdges));
		plm.add(new ColorProperty(CFGKEY_COLOR_CURRVERTICES, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURRVERTICES", langID, "Background color of the current vertices v and v<sub>1</sub>(v)"), colorCurrVertices));
		plm.add(new ColorProperty(CFGKEY_COLOR_CURREDGE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURREDGE", langID, "Color of the edge (v, v<sub>1</sub>(v))"), colorCurrEdge));
		plm.add(new ColorProperty(CFGKEY_COLOR_SETL, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SETL", langID, "Background color of the vertices of set L"), colorSetL));
		plm.add(new ColorProperty(CFGKEY_COLOR_MINWEIGHTS, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MINWEIGHTS", langID, "Background color of the elements with the first and the second smallest weight of the edges (v, v<sub>1</sub>(v)) and (v, v<sub>2</sub>(v)) required to calculate the regret"), colorSetL));
		plm.add(new ColorProperty(CFGKEY_COLOR_LARGESTREGRET, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_LARGESTREGRET", langID, "Background color of the item with a largest regret"), colorSetL));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications of objects"), colorModified));
		
		final NumericProperty lwMatchedEdges = new NumericProperty(CFGKEY_LINEWIDTH_MATCHEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIE_LINEWIDTH_MATCHEDEDGES", langID, "Line width of the matching edges"), lineWidthMatchedEdges, true);
		lwMatchedEdges.setMinimum(1);
		lwMatchedEdges.setMaximum(5);
		plm.add(lwMatchedEdges);
		final NumericProperty lwCurrEdge = new NumericProperty(CFGKEY_LINEWIDTH_CURREDGE, LanguageFile.getLabel(langFile, "CUSTOMIE_LINEWIDTH_CURREDGE", langID, "Line width of the edge (v, v<sub>1</sub>(v))"), lineWidthCurrEdge, true);
		lwCurrEdge.setMinimum(1);
		lwCurrEdge.setMaximum(5);
		plm.add(lwCurrEdge);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorMatchedEdges = config.addColor(CFGKEY_COLOR_MATCHEDEDGES, plm.getColorProperty(CFGKEY_COLOR_MATCHEDEDGES).getValue());
		colorCurrVertices = config.addColor(CFGKEY_COLOR_CURRVERTICES, plm.getColorProperty(CFGKEY_COLOR_CURRVERTICES).getValue());
		colorCurrEdge = config.addColor(CFGKEY_COLOR_CURREDGE, plm.getColorProperty(CFGKEY_COLOR_CURREDGE).getValue());
		colorSetL = config.addColor(CFGKEY_COLOR_SETL, plm.getColorProperty(CFGKEY_COLOR_SETL).getValue());
		colorSetL = config.addColor(CFGKEY_COLOR_MINWEIGHTS, plm.getColorProperty(CFGKEY_COLOR_MINWEIGHTS).getValue());
		colorSetL = config.addColor(CFGKEY_COLOR_LARGESTREGRET, plm.getColorProperty(CFGKEY_COLOR_LARGESTREGRET).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthMatchedEdges = config.addInt(CFGKEY_LINEWIDTH_MATCHEDEDGES, plm.getNumericProperty(CFGKEY_LINEWIDTH_MATCHEDEDGES).getValue().intValue());
		lineWidthCurrEdge = config.addInt(CFGKEY_LINEWIDTH_CURREDGE, plm.getNumericProperty(CFGKEY_LINEWIDTH_CURREDGE).getValue().intValue());
		
		// recreate the legend
		createLegend();
	}

	@Override
	public ToolBarExtension[] getToolBarExtensions() {
		return new ToolBarExtension[] { completeExt, circleLayoutExt, completeBipartiteExt, bipartiteLayoutExt, matrixToGraph };
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
		// check whether the graph fulfills the assumptions of the algorithm
		if(!areAssumptionsFulfilled()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_GRAPHNOTPERMISSIBLE", langID, "The created graph is not permissible!\nThe graph has to fulfill the assumptions (see information bar)."), LanguageFile.getLabel(langFile, "MSG_INFO_GRAPHNOTPERMISSIBLE_TITLE", langID, "Impermissible graph"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(containsGraphZeroWeights()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_GRAPHZEROWEIGHTS", langID, "The created graph contains edges with a zero weight!\nPlease specify a valid weight for all edges of the graph."), LanguageFile.getLabel(langFile, "MSG_INFO_GRAPHZEROWEIGHTS_TITLE", langID, "Invalid weight"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			graphView.setEditable(false);
			graphView.deselectAll();
			
			// reset the views
			regretView.reset();
			adjacencyMatrixView.reset();
			setView.reset();
			matchingView.reset();
			
			// initialize the adjacency matrix view by the runtime environment
			rte.initAdjacencyMatrixAndRegretDisplay();
			
			// manual repaint because of a display error with the strikeouts that are still painted when the visualization is started
			adjacencyMatrixView.repaint();
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
		
		final AlgorithmExercise<Set<?>> step1_8 = new AlgorithmExercise<Set<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1_8", langID, "What is the set <i>L</i>?"), 1.0f) {
			
			@Override
			protected Set<?>[] requestSolution() {
				final SolutionEntry<JTextField> entryL = new SolutionEntry<JTextField>("L=", new JTextField());
				
				if(!SolveExercisePane.showDialog(VogelsApproximationMethodPlugin.this.host, this, new SolutionEntry<?>[] { entryL }, VogelsApproximationMethodPlugin.this.langFile, VogelsApproximationMethodPlugin.this.langID, LanguageFile.getLabel(VogelsApproximationMethodPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", VogelsApproximationMethodPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				
				final ElementParser<String> parser = new StringElementParser();
				final Set<String> L = Set.parse(entryL.getComponent().getText(), parser);
				
				return new Set<?>[] { L };
			}
			
			@Override
			protected String getResultAsString(Set<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return "L=" + super.getResultAsString(result, index);
			}
			
			@Override
			protected boolean examine(Set<?>[] results, AlgorithmState state) {
				// convert the input set to a set with the identifiers of the vertices to use auto examination
				final Set<Integer> L = VogelsApproximationMethodPlugin.this.toIDs(results[0]);
				return doAutoExamine(state, new String[] { "L" }, new Set<?>[] { L });
			}
		};
		
		final AlgorithmExercise<Matching<?>> step3_7 = new AlgorithmExercise<Matching<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3_7", langID, "What is <i>M</i> after this step (<i>select all matched edges in the graph</i>)?"), 1.0f, graphView) {

			@Override
			public boolean hasInputHint() {
				return true;
			}
			
			@Override
			public Annotation getInputHintMessage(LanguageFile langFile, String langID) {
				return new Annotation(LanguageFile.getLabel(VogelsApproximationMethodPlugin.this.langFile, "EXERCISE_STEP3_7_INPUTHINT", langID, "<b>Select matched edges</b>:<br>Select the matched edges in the graph by using the mouse and pressing the <b>Ctrl</b>-key on your keyboard.<br>Afterwards click on the \"Solve Exercise\"-button of the task."));
			}
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				VogelsApproximationMethodPlugin.this.graphView.setSelectionType(SelectionType.EDGES_ONLY);
				VogelsApproximationMethodPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				VogelsApproximationMethodPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				VogelsApproximationMethodPlugin.this.graphView.setShowCursorToolAlways(false);
				VogelsApproximationMethodPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Matching<?>[] requestSolution() {
				// if their are no edges selected then break up
				if(VogelsApproximationMethodPlugin.this.graphView.getSelectedEdgeCount() == 0)
					return null;
				
				Matching<Edge> m = new Matching<Edge>(VogelsApproximationMethodPlugin.this.graphView.getGraph());
				
				try {
					// add the selected edges to the matching
					for(int i = 0; i < VogelsApproximationMethodPlugin.this.graphView.getSelectedEdgeCount(); i++)
						m.add(VogelsApproximationMethodPlugin.this.graphView.getSelectedEdge(i).getEdge());
				}
				catch(IllegalArgumentException e) {
					m = null;
				}
				
				// if the user selects an invalid matching then return an empty solution array so that the examination fails
				if(m != null)
					return new Matching<?>[] { m };
				else
					return new Matching<?>[] {};
			}
			
			@Override
			protected String getResultAsString(Matching<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return "M=" + super.getResultAsString(result, index);
			}
			
			@Override
			protected boolean examine(Matching<?>[] results, AlgorithmState state) {
				final MatchingByID<Edge> m = state.getMatching("M", VogelsApproximationMethodPlugin.this.graphView.getGraph());
				return results[0].cast().equals(m);
			}
		};
		
		// create paragraphs
		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_INITIALIZATION", langID, "1. Initialization:"), 1);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOPCRITERION", langID, "2. Stop criterion:"), 2);
		final AlgorithmParagraph regretParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_REGRET", langID, "3. Regret:"), 3);
		final AlgorithmParagraph expParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_MATCHINGEXPANSION", langID, "4. Matching expansion:"), 4);
		final AlgorithmParagraph updateParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_UPDATEL", langID, "5. Update L:"), 5);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INIT", langID, "Let _latex{$M := \\emptyset$}.\nLet _latex{$L := V$} be the set of all vertices of the graph.\n\n"), 1);
		step.setExercise(step1_8);
		
		// 2. stop criterion
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_STOP", langID, "If _latex{$|L| = 2$} "), 2);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Is |L| = 2?"), 1.0f) {
			
			private final String labelYes = LanguageFile.getLabel(langFile, "EXERCISE_STEP2_YES", langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(langFile, "EXERCISE_STEP2_NO", langID, "No");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelYes);
				final JRadioButton rdobtn2 = new JRadioButton(labelNo);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(VogelsApproximationMethodPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, VogelsApproximationMethodPlugin.this.langFile, VogelsApproximationMethodPlugin.this.langID))
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
				final Set<Integer> L = state.getSet("L");
				
				return (results[0] != null && results[0] == (L.size() == 2));
			}
		});
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_ADDLASTEDGE", langID, "then add the edge between the remaining vertices to the set _latex{$M$} and stop.\n"), 3);
		step.setExercise(step3_7);
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_STOP", langID, "Otherwise go to step 3.\n\n"), 4);
		
		// 3. regret
		step = new AlgorithmStep(regretParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_REGRET", langID, "For each _latex{$v \\in L$} determine the regret as follows: Let _latex{$v_1(v) \\in argmin\\{c(v,v') | v' \\in L\\}$} and _latex{$v_2(v) \\in argmin\\{c(v,v') | v' \\in L \\setminus \\{v_1(v)\\}\\}$}. _latex{$regret(v) := c(v,v_2(v)) - c(v,v_1(v))$}.\n\n"), 5);
		step.setExercise(new AlgorithmExercise<Map<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP5", langID, "Determine the regrets."), 1.0f, regretView) {

			@Override
			public boolean hasInputHint() {
				return true;
			}
			
			@Override
			public Annotation getInputHintMessage(LanguageFile langFile, String langID) {
				return new Annotation(LanguageFile.getLabel(VogelsApproximationMethodPlugin.this.langFile, "EXERCISE_STEP5_INPUTHINT", langID, "<b>Input of regrets</b>:<br>Enter the regrets in the last column of the regret table. If their is no regret for a vertex to be calculated<br>(because the vertex is not in <i>L</i>) then leave the field blank.<br>After entering the regrets click on the \"Solve Exercise\"-button of the task."));
			}
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				// the items are already editable (see initAdjacencyMatrixAndRegretDisplay() in the rte)
				// add a new column for regret input
				final ExecutionTableColumn c = new ExecutionTableColumn("");
				c.setWidth(30);
				c.setEditable(true);
				VogelsApproximationMethodPlugin.this.regretView.add(c);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				// remove the added column
				VogelsApproximationMethodPlugin.this.regretView.remove(VogelsApproximationMethodPlugin.this.regretView.getLastColumn());
			}
			
			@Override
			protected Map<?, ?>[] requestSolution() {
				final Map<Integer, RegretEntry> regret = new HashMap<Integer, RegretEntry>();
				final ExecutionTableColumn c = VogelsApproximationMethodPlugin.this.regretView.getLastColumn();
				ExecutionTableItem item;
				Number r;
				
				for(int i = 0; i < VogelsApproximationMethodPlugin.this.regretView.getItemCount(); i++) {
					item = VogelsApproximationMethodPlugin.this.regretView.getItem(i);
					r = (Number)item.getCellObject(c.getIndex());
					if(r != null)
						regret.put(item.getID(), new RegretEntry(item.getID(), 0, 0, r.floatValue()));
				}
				
				return new Map<?, ?>[] { regret };
			}
			
			@Override
			protected String getResultAsString(Map<?, ?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					@SuppressWarnings("unchecked")
					final Map<Integer, RegretEntry> regret = (Map<Integer, RegretEntry>)result;
					final StringBuilder s = new StringBuilder();
					final Iterator<Integer> it = regret.keySet().iterator();
					final Graph<Vertex, Edge> graph = VogelsApproximationMethodPlugin.this.graphView.getGraph();
					Vertex v;
					boolean delimiter = false;
					
					while(it.hasNext()) {
						v = graph.getVertexByID(it.next());
						if(delimiter)
							s.append(", ");
						
						s.append(v.getCaption() + " = " + MathUtils.formatFloat(regret.get(v.getID()).regret));
						delimiter = true;
					}
					
					return s.toString();
				}
			}
			
			@Override
			protected boolean examine(Map<?, ?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "regret" }, results);
			}
		});

		// 4. matching expansion
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_MATCHINGEXPANSION", langID, "Let _latex{$v \\in L$} be any vertex with a largest regret.\n"), 6);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP6", langID, "Select a vertex <i>v</i> with a largest regret in the graph."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				VogelsApproximationMethodPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				VogelsApproximationMethodPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				VogelsApproximationMethodPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				VogelsApproximationMethodPlugin.this.graphView.setShowCursorToolAlways(false);
				VogelsApproximationMethodPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(VogelsApproximationMethodPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { VogelsApproximationMethodPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return VogelsApproximationMethodPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Integer[] solutions) {
				state.addInt("v", solutions[0]);
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				final Map<Integer, RegretEntry> regret = state.getMap("regret");
				final Iterator<Integer> it = regret.keySet().iterator();
				float maxRegret = Float.MIN_VALUE;
				float currRegret;
				
				// find the largest regret
				while(it.hasNext()) {
					currRegret = regret.get(it.next()).regret;
					if(currRegret > maxRegret)
						maxRegret = currRegret;
				}
				
				return regret.get(results[0]).regret == maxRegret;
			}
		});

		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_MATCHINGEXPANSION", langID, "Add _latex{$(v,v_1(v))$} to the matching _latex{$M$}.\n\n"), 7);
		step.setExercise(step3_7);
		
		// 5. update L
		step = new AlgorithmStep(updateParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP8_UPDATEL", langID, "Set _latex{$L := L \\setminus \\{v, v_1(v)\\}$} and go to step 2."), 8);
		step.setExercise(step1_8);
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_MATCHEDEDGES", langID, "The matched edges of matching M"), LegendItem.createLineIcon(colorMatchedEdges, lineWidthMatchedEdges, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SETL", langID, "Vertices of the set L"), LegendItem.createCircleIcon(colorSetL, Color.black, 1)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURRVERTEX", langID, "The current vertex v"), LegendItem.createCircleIcon(colorCurrVertices, Color.black, 2)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURRVERTEX1", langID, "The current vertex v<sub>1</sub>(v)"), LegendItem.createCircleIcon(colorCurrVertices, Color.black, 1)));
		legendView.add(new LegendItem("item5", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURREDGE", langID, "The current edge (v, v<sub>1</sub>(v))"), LegendItem.createLineIcon(colorCurrEdge, lineWidthCurrEdge, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item6", setView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_SET_MODIFICATION", langID, "The set L becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item7", matchingView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_MATCHING_MODIFICATION", langID, "The matching M becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item8", adjacencyMatrixView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_ADJACENCYMATRIX_MINWEIGHTS", langID, "The first and the second smallest weight in a row resulting in the regret of the related vertex"), LegendItem.createRectangleIcon(colorMinWeights, colorMinWeights, 0)));
		legendView.add(new LegendItem("item9", adjacencyMatrixView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_ADJACENCYMATRIX_CURREDGE", langID, "The current edge (v, v<sub>1</sub>(v))"), LegendItem.createRectangleIcon(colorCurrEdge, colorCurrEdge, 0)));
		legendView.add(new LegendItem("item10", adjacencyMatrixView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_ADJACENCYMATRIX_STRIKEOUT", langID, "Striked off vertices its edge was added to the matching M"), LegendItem.createLineIcon(Color.black, 1)));
		legendView.add(new LegendItem("item11", regretView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_REGRET_DESC", langID, "the difference between the best and the second best edge concerning the weight")));
		legendView.add(new LegendItem("item12", regretView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_REGRET_LARGEST", langID, "A largest regret"), LegendItem.createRectangleIcon(colorLargestRegret, colorLargestRegret, 0)));
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
	 * Indicates whether the graph the user has created is complete with <code>n mod 2 = 0</code> or whether the graph is
	 * complete bipartite.
	 * 
	 * @return <code>true</code> if the graph fulfills the assumptions otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean areAssumptionsFulfilled() {
		final Graph<Vertex, Edge> graph = graphView.getGraph();
		final List<List<Vertex>> subsets = GraphUtils.getBipartiteVertexSets(graph);
		
		// Kn or Kn/2,n/2 with n mod 2 = 0
		if(graph.getOrder() % 2 != 0)
			return false;
		else
			return (GraphUtils.isComplete(graph)) || (GraphUtils.isCompleteBipartite(graph) && subsets.get(0).size() == graph.getOrder() / 2 && subsets.get(0).size() == subsets.get(1).size());
	}
	
	/**
	 * Indicates whether the graph the user has created contains invalid weights (meaning zero weights).
	 * 
	 * @return <code>true</code> if their is an edge with a weight of zero otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean containsGraphZeroWeights() {
		final Graph<Vertex, Edge> graph = graphView.getGraph();
		
		for(int i = 0; i < graph.getSize(); i++)
			if(graph.getEdge(i).getWeight() == 0.0f)
				return true;
		
		return false;
	}
	
	/**
	 * The runtime environment of the Vogels Approximation Method.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class VogelsApproximationRTE extends AlgorithmRTE {
		
		/** the matching */
		private Matching<Edge> M;
		/** the set of all vertices */
		private Set<Integer> L;
		/** the regret of the vertices with key=vertex id, value=regret entry */
		private Map<Integer, RegretEntry> regret;
		/** the current vertex v */
		private int v;
		/** the current vertex v1 */
		private int v1;
		/** the user's choice of {@link #v} */
		private int userChoiceV;
		/** the random strikeout colors */
		private List<Color> strikeoutColors;
		/** the index of the next strikeout color */
		private int nextStrikeoutColorIndex;
		
		/**
		 * Creates a new runtime environment.
		 * 
		 * @since 1.0
		 */
		public VogelsApproximationRTE() {
			super(VogelsApproximationMethodPlugin.this, VogelsApproximationMethodPlugin.this.algoText);
			
			userChoiceV = 0;
			
			// create a fixed list of pseudo random colors that is used as a cycle
			// (this brings the advantage that the colors do not change every time and that they are more different)
			strikeoutColors = new ArrayList<Color>(15);
			strikeoutColors.add(Color.black);
			strikeoutColors.add(Color.red);
			strikeoutColors.add(Color.blue);
			strikeoutColors.add(Color.green);
			strikeoutColors.add(Color.gray);
			strikeoutColors.add(Color.yellow);
			strikeoutColors.add(Color.magenta);
			strikeoutColors.add(Color.lightGray);
			strikeoutColors.add(Color.cyan);
			strikeoutColors.add(Color.orange);
		}
		
		/**
		 * Initializes the adjacency matrix in the matrix view, adjusts the heights (row and column header) in the
		 * regret view and adds the items representing the rows of the matrix and generates random strikeout colors.
		 * 
		 * @since 1.0
		 */
		public void initAdjacencyMatrixAndRegretDisplay() {
			final Graph<Vertex, Edge> graph = VogelsApproximationMethodPlugin.this.graphView.getGraph();
			final Map<Integer, String> labels = new HashMap<Integer, String>();
			ExecutionTableItem item;
			
			// create the vertex labels
			for(int i = 0; i < graph.getOrder(); i++)
				labels.put(i, graph.getVertex(i).getCaption());
			
			// initialize the adjacency matrix view and set the vertex labels
			VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setMatrix(GraphUtils.createAdjacencyMatrix(graph));
			VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setColumnLabels(labels);
			VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setRowLabels(labels);
			VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setPaintLabels(true);
			
			// adjust the size of the regret view components to the adjacency matrix
			VogelsApproximationMethodPlugin.this.regretView.setColumnHeaderHeight(adjacencyMatrixView.getRowHeight() + 4);	// include +4 as the matrix padding
			VogelsApproximationMethodPlugin.this.regretView.setItemHeight(adjacencyMatrixView.getRowHeight());
			
			// add the rows of the regret table with the id of the related vertices
			for(int i = 0; i < graph.getOrder(); i++) {
				item = new ExecutionTableItem(graph.getVertex(i).getID());
				// every item is editable by default so this must not be set in the exercise to determine the regrets
				item.setEditable(true);
				item.setDefaultInputParser(new ExecutionTableItem.NumericInputParser());
				VogelsApproximationMethodPlugin.this.regretView.add(item);
			}
			
			// reset the strikeout color index
			nextStrikeoutColorIndex = 0;
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = VogelsApproximationMethodPlugin.this.graphView.getGraph();
			int nextStep = -1;
			GraphView<Vertex, Edge>.VisualVertex vv;
			GraphView<Vertex, Edge>.VisualVertex vv1;
			GraphView<Vertex, Edge>.VisualEdge ve;
			Vertex vertex;
			Vertex vertex1;
			Vertex vertex2;
			Edge e;
			ExecutionTableItem item;
			
			switch(stepID) {
				case 1:
					// let M be a zero matching and L be the set of all vertices
					
					M = new Matching<Edge>(graph);
					
					sleep(250);
					VogelsApproximationMethodPlugin.this.matchingView.setBackground(VogelsApproximationMethodPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					visualizeMatching();
					sleep(250);
					VogelsApproximationMethodPlugin.this.matchingView.setBackground(Color.white);
					sleep(250);
					
					L.clear();
					for(int i = 0; i < graph.getOrder(); i++)
						L.add(graph.getVertex(i).getID());
					
					VogelsApproximationMethodPlugin.this.setView.setBackground(VogelsApproximationMethodPlugin.this.colorModified);
					sleep(250);
					visualizeVertices();
					visualizeSetAsText();
					sleep(250);
					VogelsApproximationMethodPlugin.this.setView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 2;
					break;
				case 2:
					
					// if |L| = 2 then go to 7 otherwise go to 8
					
					if(L.size() == 2)
						nextStep = 3;
					else
						nextStep = 4;
					
					break;
				case 3:
					// add the remaining edge between the last two vertices to matching M and stop
					
					e = graph.getEdge(L.get(0), L.get(1));
					M.add(e);
					
					sleep(250);
					
					// highlight the last edge
					ve = VogelsApproximationMethodPlugin.this.graphView.getVisualEdge(e);
					ve.setColor(VogelsApproximationMethodPlugin.this.colorCurrEdge);
					ve.setLineWidth(VogelsApproximationMethodPlugin.this.lineWidthCurrEdge);
					VogelsApproximationMethodPlugin.this.graphView.repaint();
					
					sleep(500);
					
					// visualize the new matching
					VogelsApproximationMethodPlugin.this.matchingView.setBackground(VogelsApproximationMethodPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					visualizeMatching();
					sleep(250);
					VogelsApproximationMethodPlugin.this.matchingView.setBackground(Color.white);
					sleep(250);
					
					// and visualize the vertices so that no vertex is colored anymore
					L.clear();
					visualizeVertices();
					
					sleep(500);
					
					nextStep = -1;
					break;
				case 4:
					// otherwise go to 3 (stepID=5).
					
					sleep(1000);
					
					nextStep = 5;
					break;
				case 5:
					Vertex vertex1_tmp;
					Vertex vertex2_tmp;
					float minWeightV_V2;
					float minWeightV_V1;
				
					// determine the regret for each vertex in L
					
					// add a new column for the next regret
					final ExecutionTableColumn column = new ExecutionTableColumn("");
					column.setWidth(30);
					VogelsApproximationMethodPlugin.this.regretView.add(column);
					
					regret.clear();
					for(int i = 0; i < L.size(); i++) {
						vertex = graph.getVertexByID(L.get(i));
						vertex1 = null;
						minWeightV_V1 = Float.MAX_VALUE;
						
						// find v1 in argmin{c(v,v')|v' in L}
						for(int j = 0; j < L.size(); j++) {
							vertex1_tmp = graph.getVertexByID(L.get(j));
							e = graph.getEdge(vertex, vertex1_tmp);
							
							if(e != null && e.getWeight() < minWeightV_V1) {
								minWeightV_V1 = e.getWeight();
								vertex1 = vertex1_tmp;
							}
						}
						
						sleep(250);
						
						// highlight v1 in the matrix
						VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setElementBackground(vertex.getIndex(), vertex1.getIndex(), VogelsApproximationMethodPlugin.this.colorMinWeights);
						sleep(500);
						
						vertex2 = null;
						minWeightV_V2 = Float.MAX_VALUE;
						
						// find v2 in argmin{c(v,v')|v' in L\{v1}}
						for(int j = 0; j < L.size(); j++) {
							vertex2_tmp = graph.getVertexByID(L.get(j));
							if(vertex2_tmp == vertex1)
								continue;
							
							e = graph.getEdge(vertex, vertex2_tmp);
							
							if(e != null && e.getWeight() < minWeightV_V2) {
								minWeightV_V2 = e.getWeight();
								vertex2 = vertex2_tmp;
							}
						}
						
						// highlight v1 in the matrix
						VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setElementBackground(vertex.getIndex(), vertex2.getIndex(), VogelsApproximationMethodPlugin.this.colorMinWeights);
						sleep(500);
						
						// calculate regret
						final RegretEntry regretEntry = new RegretEntry(vertex.getID(), vertex1.getID(), vertex2.getID(), minWeightV_V2 - minWeightV_V1);
						regret.put(vertex.getID(), regretEntry);
						
						// show the regret in the table (the index of the vertex and the item are compliant)
						item = VogelsApproximationMethodPlugin.this.regretView.getItem(vertex.getIndex());
						item.setCellObject(VogelsApproximationMethodPlugin.this.regretView.getLastColumn().getIndex(), regretEntry.regret);
						
						sleep(500);
						
						// remove the highlight
						VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setElementBackground(vertex.getIndex(), vertex1.getIndex(), Color.white);
						VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setElementBackground(vertex.getIndex(), vertex2.getIndex(), Color.white);
					}
					
					nextStep = 6;
					break;
				case 6:
					// find v in L with a largest regret
					
					final Iterator<Integer> it = regret.keySet().iterator();
					int currV;
					RegretEntry currRegret;
					float largestRegret = Float.MIN_VALUE;
					
					// if user has made a choice in the related exercise then apply the solution
					if(userChoiceV > 0) {
						v = userChoiceV;
						v1 = regret.get(v).v1;
					}
					else {
						// otherwise find a vertex with a largest regret
						v = 0;
						v1 = 0;
						while(it.hasNext()) {
							currV = it.next();
							currRegret = regret.get(currV);
							if(currRegret.regret > largestRegret) {
								largestRegret = currRegret.regret;
								v = currV;
								v1 = currRegret.v1;
							}
						}
					}
					
					// clear the user choice
					userChoiceV = 0;
					
					if(v < 1)
						nextStep = -1;
					else {
						sleep(500);
						
						// highlight v in the regret table
						item = VogelsApproximationMethodPlugin.this.regretView.getItemByID(v);
						item.setCellBackground(VogelsApproximationMethodPlugin.this.regretView.getLastColumn().getIndex(), VogelsApproximationMethodPlugin.this.colorLargestRegret);
						
						sleep(500);
						
						// highlight the vertices because v and v1 have changed
						visualizeVertices();
						// and remove highlight from the table
						item.setCellBackground(VogelsApproximationMethodPlugin.this.regretView.getLastColumn().getIndex(), Color.white);
						
						sleep(500);
						
						nextStep = 7;
					}
					break;
				case 7:
					// add (v,v1) to the matching M
					
					e = graph.getEdge(v, v1);
					M.add(e);
					
					sleep(250);
					
					vertex1 = graph.getVertexByID(v);
					vertex2 = graph.getVertexByID(v1);
					
					// first: highlight the current edge in the matrix view
					VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setElementBackground(vertex1.getIndex(), vertex2.getIndex(), VogelsApproximationMethodPlugin.this.colorCurrEdge);
					sleep(250);
					
					// second: highlight the current edge and the related vertices v and v1 in the graph view
					ve = VogelsApproximationMethodPlugin.this.graphView.getVisualEdge(e);
					ve.setColor(VogelsApproximationMethodPlugin.this.colorCurrEdge);
					ve.setLineWidth(VogelsApproximationMethodPlugin.this.lineWidthCurrEdge);
					vv = VogelsApproximationMethodPlugin.this.graphView.getVisualVertexByID(v);
					vv1 = VogelsApproximationMethodPlugin.this.graphView.getVisualVertexByID(v1);
					vv.setForeground(VogelsApproximationMethodPlugin.this.colorCurrEdge);
					vv1.setForeground(VogelsApproximationMethodPlugin.this.colorCurrEdge);
					VogelsApproximationMethodPlugin.this.graphView.repaint();
					
					sleep(750);
					
					// remove the highlight in the matrix
					VogelsApproximationMethodPlugin.this.adjacencyMatrixView.setElementBackground(vertex1.getIndex(), vertex2.getIndex(), Color.white);
					
					// remove highlighting from the vertices (the edge is overridden in visualizeMatching())
					// (do not repaint because visualizeMatching() is invoked later)
					vv.setForeground(GraphView.DEF_VERTEXFOREGROUND);
					vv1.setForeground(GraphView.DEF_VERTEXFOREGROUND);
					
					// visualize the new matching
					VogelsApproximationMethodPlugin.this.matchingView.setBackground(VogelsApproximationMethodPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					visualizeMatching();
					sleep(250);
					VogelsApproximationMethodPlugin.this.matchingView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 8;
					break;
				case 8:
					// put L = L \ {v,v1} and go to 2. (stepid=2)
					
					L.remove(v);
					L.remove(v1);
					e = graph.getEdge(v, v1);
					
					final int i = Math.min(e.getPredecessor().getIndex(), e.getSuccessor().getIndex());
					final int j = Math.max(e.getPredecessor().getIndex(), e.getSuccessor().getIndex());
					final Color c = getNextStrikeoutColor();
					final MatrixEditor.Strikeout s1 = new MatrixEditor.Strikeout(i, c, 2);
					final MatrixEditor.Strikeout s2 = new MatrixEditor.Strikeout(j, c, 2);
					
					sleep(250);
					// strikeout the edge
					VogelsApproximationMethodPlugin.this.adjacencyMatrixView.addRowStrikeout(s1);
					VogelsApproximationMethodPlugin.this.adjacencyMatrixView.addRowStrikeout(s2);
					VogelsApproximationMethodPlugin.this.adjacencyMatrixView.addColumnStrikeout(s1);
					VogelsApproximationMethodPlugin.this.adjacencyMatrixView.addColumnStrikeout(s2);
					
					sleep(500);
					
					VogelsApproximationMethodPlugin.this.setView.setBackground(VogelsApproximationMethodPlugin.this.colorModified);
					sleep(250);
					visualizeVertices();
					visualizeSetAsText();
					sleep(250);
					VogelsApproximationMethodPlugin.this.setView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 2;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addMatching("M", (M != null) ? M.cast() : null);
			state.addSet("L", L);
			state.addMap("regret", regret);
			state.addInt("v", v);
			state.addInt("v1", v1);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			final MatchingByID<Edge> m = state.getMatching("M", VogelsApproximationMethodPlugin.this.graphView.getGraph());
			M = (m != null) ? m.cast() : null;
			L = state.getSet("L");
			regret = state.getMap("regret");
			v = state.getInt("v");
			v1 = state.getInt("v1");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			state.addMatching("M", null);
			L = state.addSet("L", new Set<Integer>());
			regret = state.addMap("regret", new HashMap<Integer, RegretEntry>());
			v = state.addInt("v", 0);
			v1 = state.addInt("v1", 0);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			if(stepID == 1 || stepID == 3) {
				visualizeVertices();
				visualizeSetAsText();
				visualizeMatching();
				visualizeMatchingAsText();
			}
			
			if(stepID == 5)
				VogelsApproximationMethodPlugin.this.regretView.remove(VogelsApproximationMethodPlugin.this.regretView.getLastColumn());
			
			if(stepID == 6)
				visualizeVertices();
			
			if(stepID == 7) {
				visualizeMatching();
				visualizeMatchingAsText();
			}
			
			if(stepID == 8) {
				visualizeVertices();
				visualizeSetAsText();
				VogelsApproximationMethodPlugin.this.adjacencyMatrixView.removeLastColumnStrikeout();
				VogelsApproximationMethodPlugin.this.adjacencyMatrixView.removeLastColumnStrikeout();
				VogelsApproximationMethodPlugin.this.adjacencyMatrixView.removeLastRowStrikeout();
				VogelsApproximationMethodPlugin.this.adjacencyMatrixView.removeLastRowStrikeout();
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			if(stepID == 6)
				userChoiceV = state.getInt("v");
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { VogelsApproximationMethodPlugin.this.graphView, VogelsApproximationMethodPlugin.this.setView, VogelsApproximationMethodPlugin.this.matchingView, VogelsApproximationMethodPlugin.this.adjacencyMatrixView, VogelsApproximationMethodPlugin.this.regretView };
		}
		
		/**
		 * Visualizes the vertices of the set L. All other vertices are displayed in the default background color.
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			GraphView<Vertex, Edge>.VisualVertex vv;
			
			for(int i = 0; i < VogelsApproximationMethodPlugin.this.graphView.getVisualVertexCount(); i++) {
				vv = VogelsApproximationMethodPlugin.this.graphView.getVisualVertex(i);
				
				if(L.contains(vv.getVertex().getID())) {
					vv.setBackground((vv.getVertex().getID() == v || vv.getVertex().getID() == v1) ? VogelsApproximationMethodPlugin.this.colorCurrVertices : VogelsApproximationMethodPlugin.this.colorSetL);
					vv.setEdgeWidth((vv.getVertex().getID() == v) ?  2 : 1);
				}
				else {
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
					vv.setEdgeWidth(1);
				}
			}
			
			// show the visualization
			VogelsApproximationMethodPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the matching in the graph.
		 * <br><br>
		 * Therefore all edges of the graph are visited and colored. So be careful when you invoke this method because all the other
		 * visualization of edges is overridden.
		 * 
		 * @since 1.0
		 */
		private void visualizeMatching() {
			if(M == null)
				return;
			
			GraphView<Vertex, Edge>.VisualEdge ve;
			
			for(int i = 0; i < VogelsApproximationMethodPlugin.this.graphView.getVisualEdgeCount(); i++) {
				ve = VogelsApproximationMethodPlugin.this.graphView.getVisualEdge(i);
				
				// visualize matched and unmatched edges
				if(M.contains(ve.getEdge())) {
					ve.setColor(VogelsApproximationMethodPlugin.this.colorMatchedEdges);
					ve.setLineWidth(VogelsApproximationMethodPlugin.this.lineWidthMatchedEdges);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			// show the visualization
			VogelsApproximationMethodPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the matching M in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeMatchingAsText() {
			VogelsApproximationMethodPlugin.this.matchingView.setText((M != null) ? "M=" + M.toString() : "");
		}
		
		/**
		 * Visualizes the set L in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeSetAsText() {
			VogelsApproximationMethodPlugin.this.setView.setText("L=" + toCaptions(L));
		}
		
		/**
		 * Converts the given set of vertex identifiers to a set of related vertex captions.
		 * 
		 * @param set the set of vertex identifiers
		 * @return the converted set
		 * @since 1.0
		 */
		private Set<String> toCaptions(final Set<Integer> set) {
			final Graph<Vertex, Edge> graph = VogelsApproximationMethodPlugin.this.graphView.getGraph();
			final Set<String> res = new Set<String>(set.size());
			
			for(Integer id : set)
				res.add(graph.getVertexByID(id).getCaption());
			
			return res;
		}
		
		/**
		 * Gets a strikeout color.
		 * 
		 * @param index the index (can be greater than the size of the color list because it is used as a cycle)
		 * @return the color
		 * @since 1.0
		 */
		private Color getNextStrikeoutColor() {
			return strikeoutColors.get(nextStrikeoutColorIndex++ % strikeoutColors.size());
		}
		
	}

}
