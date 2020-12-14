package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.SortOrder;
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
import lavesdk.gui.widgets.ListProperty;
import lavesdk.gui.widgets.MatrixEditor;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.matching.Matching;
import lavesdk.math.graph.matching.MatchingByID;
import lavesdk.utils.GraphUtils;

/**
 * Plugin that visualizes and teaches users the greedy algorithm to find a perfect matching of low weight.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class GreedyAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the view that displays the list L */
	private ExecutionTableView listView;
	/** the view that displays the adjacency matrix as a helper */
	private MatrixView<Float> adjacencyMatrixView;
	/** the view that displays the matching */
	private TextAreaView matchingView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the algorithm */
	private GreedyRTE rte;
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
	/** the string describing the display mode creator preference */
	private String creatorPrefsDispMode;
	/** display mode without adjacency matrix */
	private String dispModeWithoutAM;
	/** display mode with adjacency matrix */
	private String dispModeWithAM;
	/** the value of the display mode creator preference */
	private String creatorPrefsDispModeValue;
	/** flag that indicates whether the adjacency matrix is currently displayed or not */
	private boolean adjacencyMatrixEnabled;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for C and D (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cd;
	/** the view group for E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ef;
	/** the view group for A,B,C,D,E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcdef;

	// modifiable visualization data
	/** color to visualize modifications */
	private Color colorModified;
	/** color to visualize matched edges */
	private Color colorMatchedEdges;
	/** color to visualize the current edge */
	private Color colorCurrEdge;
	/** color to visualize an edge that is removed from L */
	private Color colorEdgeToRemove;
	/** line width of matched edges */
	private int lineWidthMatchedEdges;
	/** line width of the current edge */
	private int lineWidthCurrEdge;
	/** line width of the edge that is removed from L */
	private int lineWidthEdgeToRemove;
	
	/** configuration key for the {@link #creatorPrefsDispModeValue} */
	private static final String CFGKEY_CREATORPROP_DISPMODE = "creatorPrefsDispModeValue";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #colorMatchedEdges} */
	private static final String CFGKEY_COLOR_MATCHEDEDGES = "colorMatchedEdges";
	/** configuration key for the {@link #colorCurrEdge} */
	private static final String CFGKEY_COLOR_CURREDGE = "colorCurrEdge";
	/** configuration key for the {@link #colorEdgeToRemove} */
	private static final String CFGKEY_COLOR_EDGETOREMOVE = "colorEdgeToRemove";
	/** configuration key for the {@link #lineWidthMatchedEdges} */
	private static final String CFGKEY_LINEWIDTH_MATCHEDEDGES = "lineWidthMatchedEdges";
	/** configuration key for the {@link #lineWidthCurrEdge} */
	private static final String CFGKEY_LINEWIDTH_CURREDGE = "lineWidthCurrEdge";
	/** configuration key for the {@link #lineWidthEdgeToRemove} */
	private static final String CFGKEY_LINEWIDTH_EDGETOREMOVE = "lineWidthEdgeToRemove";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langGreedy.txt"));
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
		this.listView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_LIST_TITLE", langID, "List L"), true, langFile, langID);
		this.adjacencyMatrixView = new MatrixView<Float>(LanguageFile.getLabel(langFile, "VIEW_ADJACENCYMATRIX_TITLE", langID, "Adjacency Matrix"), new MatrixEditor.FloatElementFormat(), true, langFile, langID);
		this.matchingView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_MATCHING_TITLE", langID, "Matching M"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new GreedyRTE();
		this.completeExt = new CompleteGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, false);
		this.completeBipartiteExt = new CompleteBipartiteGraphToolBarExtension<Vertex, Edge>(host, graphView, langFile, langID, true);
		this.bipartiteLayoutExt = new BipartiteLayoutToolBarExtension<Vertex, Edge>(graphView, true, langFile, langID, false);
		this.matrixToGraph = new MatrixToGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.creatorPrefsDispMode = LanguageFile.getLabel(langFile, "CREATORPREFS_DISPLAYMODE", langID, "Display Mode");
		this.dispModeWithoutAM = LanguageFile.getLabel(langFile, "CREATORPREFS_DISPLAYMODE_WOAM", langID, "Without Adjacency Matrix");
		this.dispModeWithAM = LanguageFile.getLabel(langFile, "CREATORPREFS_DISPLAYMODE_WAM", langID, "With Adjacency Matrix");
		this.adjacencyMatrixEnabled = false;
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		listView.setAutoRepaint(true);
		adjacencyMatrixView.setAutoRepaint(true);
		matchingView.setAutoRepaint(true);
		
		// load the creator preference data from the configuration
		creatorPrefsDispModeValue = this.config.getString(CFGKEY_CREATORPROP_DISPMODE, dispModeWithoutAM);
		
		// load the visualization colors from the configuration of the plugin
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		colorMatchedEdges = this.config.getColor(CFGKEY_COLOR_MATCHEDEDGES, Color.black);
		colorCurrEdge = this.config.getColor(CFGKEY_COLOR_CURREDGE, new Color(105, 140, 75));
		colorEdgeToRemove = this.config.getColor(CFGKEY_COLOR_EDGETOREMOVE, new Color(215, 75, 75));
		lineWidthMatchedEdges = this.config.getInt(CFGKEY_LINEWIDTH_MATCHEDEDGES, 3);
		lineWidthCurrEdge = this.config.getInt(CFGKEY_LINEWIDTH_CURREDGE, 2);
		lineWidthEdgeToRemove = this.config.getInt(CFGKEY_LINEWIDTH_EDGETOREMOVE, 2);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		listView.loadConfiguration(config, "listView");
		adjacencyMatrixView.loadConfiguration(config, "adjacencyMatrixView");
		matchingView.loadConfiguration(config, "matchingView");
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
		return true;
	}

	@Override
	public void loadCreatorPreferences(PropertiesListModel plm) {
		plm.add(new ListProperty(creatorPrefsDispMode, LanguageFile.getLabel(langFile, "CREATORPREFS_DISPLAYMODE_DESC", langID, "Choose the display mode of the algorithm"), creatorPrefsDispModeValue, new String[] { dispModeWithoutAM, dispModeWithAM }));
	}

	@Override
	public void onCreate(ViewContainer container, PropertiesListModel creatorProperties) {
		creatorPrefsDispModeValue = (creatorProperties != null) ? creatorProperties.getListProperty(creatorPrefsDispMode).getValue() : dispModeWithoutAM;
		adjacencyMatrixEnabled = creatorPrefsDispModeValue.equals(dispModeWithAM);
		
		// update the configuration
		config.addString(CFGKEY_CREATORPROP_DISPMODE, creatorPrefsDispModeValue);
		
		// change the graph in the view
		graphView.setGraph(new SimpleGraph<Vertex, Edge>(false));
		graphView.repaint();
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|/////|///
		 * /A/|/ C /|/E/	A = algorithm text view
		 * ///|/////|///	B = legend view
		 * ---|-----|---	C = graph view
		 * ///|/////|///	D = matrix view (adjacency matrix helper) (optional)
		 * /B/|/ D /|/F/	E = list view
		 * ///|/////|///	F = text area view (matching view)
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		cd = new ViewGroup(ViewGroup.VERTICAL);
		ef = new ViewGroup(ViewGroup.VERTICAL);
		abcdef = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// middle group for C and D
		cd.add(graphView);
		if(adjacencyMatrixEnabled) {
			cd.add(adjacencyMatrixView);
			cd.restoreWeights(config, "weights_cd", new float[] { 0.7f, 0.3f });
		}
		else
			cd.restoreWeights(config, "weights_c", new float[] { 1.0f });
		
		// right group for E and F
		ef.add(listView);
		ef.add(matchingView);
		ef.restoreWeights(config, "weights_ef", new float[] { 0.8f, 0.2f });
		
		// group for (A,B), (C,D) and (E,F)
		abcdef.add(ab);
		abcdef.add(cd);
		abcdef.add(ef);
		abcdef.restoreWeights(config, "weights_abcdef", new float[] { 0.4f, 0.4f, 0.2f });
		
		container.setLayout(new BorderLayout());
		container.add(abcdef, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		listView.saveConfiguration(config, "listView");
		adjacencyMatrixView.saveConfiguration(config, "adjacencyMatrixView");
		matchingView.saveConfiguration(config, "matchingView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(cd != null) {
			if(adjacencyMatrixEnabled)
				cd.storeWeights(config, "weights_cd");
			else
				cd.storeWeights(config, "weights_c");
		}
		if(ef != null)
			ef.storeWeights(config, "weights_ef");
		if(abcdef != null)
			abcdef.storeWeights(config, "weights_abcdef");
		
		// reset view content where it is necessary
		graphView.reset();
		listView.reset();
		adjacencyMatrixView.reset();
		matchingView.reset();
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
		plm.add(new ColorProperty(CFGKEY_COLOR_CURREDGE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURREDGE", langID, "Color of the current edge (v<sub>i</sub>, v<sub>j</sub>)"), colorCurrEdge));
		plm.add(new ColorProperty(CFGKEY_COLOR_EDGETOREMOVE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_EDGETOREMOVE", langID, "Color of the edge that has to be removed from L"), colorEdgeToRemove));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwMatchedEdges = new NumericProperty(CFGKEY_LINEWIDTH_MATCHEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIE_LINEWIDTH_MATCHEDEDGES", langID, "Line width of matching edges"), lineWidthMatchedEdges, true);
		lwMatchedEdges.setMinimum(1);
		lwMatchedEdges.setMaximum(5);
		plm.add(lwMatchedEdges);
		final NumericProperty lwCurrEdge = new NumericProperty(CFGKEY_LINEWIDTH_CURREDGE, LanguageFile.getLabel(langFile, "CUSTOMIE_LINEWIDTH_CURREDGE", langID, "Line width of the current edge (v<sub>i</sub>, v<sub>j</sub>)"), lineWidthCurrEdge, true);
		lwCurrEdge.setMinimum(1);
		lwCurrEdge.setMaximum(5);
		plm.add(lwCurrEdge);
		final NumericProperty lwEdgeToRemove = new NumericProperty(CFGKEY_LINEWIDTH_EDGETOREMOVE, LanguageFile.getLabel(langFile, "CUSTOMIE_LINEWIDTH_EDGETOREMOVE", langID, "Line width of the edge that has to be removed from L"), lineWidthEdgeToRemove, true);
		lwEdgeToRemove.setMinimum(1);
		lwEdgeToRemove.setMaximum(5);
		plm.add(lwEdgeToRemove);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorMatchedEdges = config.addColor(CFGKEY_COLOR_MATCHEDEDGES, plm.getColorProperty(CFGKEY_COLOR_MATCHEDEDGES).getValue());
		colorCurrEdge = config.addColor(CFGKEY_COLOR_CURREDGE, plm.getColorProperty(CFGKEY_COLOR_CURREDGE).getValue());
		colorEdgeToRemove = config.addColor(CFGKEY_COLOR_EDGETOREMOVE, plm.getColorProperty(CFGKEY_COLOR_EDGETOREMOVE).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthMatchedEdges = config.addInt(CFGKEY_LINEWIDTH_MATCHEDEDGES, plm.getNumericProperty(CFGKEY_LINEWIDTH_MATCHEDEDGES).getValue().intValue());
		lineWidthCurrEdge = config.addInt(CFGKEY_LINEWIDTH_CURREDGE, plm.getNumericProperty(CFGKEY_LINEWIDTH_CURREDGE).getValue().intValue());
		lineWidthEdgeToRemove = config.addInt(CFGKEY_LINEWIDTH_EDGETOREMOVE, plm.getNumericProperty(CFGKEY_LINEWIDTH_EDGETOREMOVE).getValue().intValue());
		
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
		
		if(e.doit) {
			graphView.setEditable(false);
			graphView.deselectAll();
			
			listView.reset();
			// create the two columns of the list displaying the edges and their weights
			listView.add(new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_LIST_COLUMNEDGE", langID, "Edge")));
			listView.add(new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_LIST_COLUMNWEIGHT", langID, "Weight")));
			
			matchingView.reset();
			
			adjacencyMatrixView.reset();
			rte.initAMDisplay();
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
		
		// create paragraphs
		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_INITIALIZATION", langID, "1. Initialization:"), 1);
		final AlgorithmParagraph expParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_MATCHINGEXPANSION", langID, "2. Matching expansion:"), 2);
		final AlgorithmParagraph updateParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_UPDATEL", langID, "3. Update L:"), 3);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOPCRITERION", langID, "4. Stop criterion:"), 4);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INIT", langID, "Let _latex{$M := \\emptyset$}.\nLet _latex{$L := E$} be the list of all edges of the graph.\n"), 1);
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_SORT", langID, "Sort _latex{$L$} non-decreasingly by the weight of the edges.\n\n"), 2);
		step.setExercise(new AlgorithmExercise<List<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Sort <i>L</i> (<i>use the arrows in the list to change the positions of the edges</i>)."), 2.0f, listView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				GreedyAlgorithmPlugin.this.listView.setSortable(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				GreedyAlgorithmPlugin.this.listView.setSortable(false);
			}
			
			@Override
			protected List<?>[] requestSolution() {
				final List<Integer> l = new ArrayList<Integer>();
				
				for(int i = 0; i < GreedyAlgorithmPlugin.this.listView.getItemCount(); i++)
					l.add(GreedyAlgorithmPlugin.this.listView.getItem(i).getID());
				
				return new List<?>[] { l };
			}
			
			@Override
			protected String getResultAsString(List<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					@SuppressWarnings("unchecked")
					final List<Integer> L = (List<Integer>)result;
					return "L=" + getEdgeListAsString(L);
				}
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, List<?>[] solutions) {
				@SuppressWarnings("unchecked")
				final List<Integer> L = (List<Integer>)solutions[0];
				state.addList("L", L);
			}
			
			@Override
			protected boolean examine(List<?>[] results, AlgorithmState state) {
				@SuppressWarnings("unchecked")
				final List<Integer> L = (List<Integer>)results[0];
				final Graph<Vertex, Edge> graph = GreedyAlgorithmPlugin.this.graphView.getGraph();
				float lastWeight = Float.MIN_VALUE;
				Edge e;
				
				for(int i = 0; i < L.size(); i++) {
					e = graph.getEdgeByID(L.get(i));
					// if the list is not sorted ascending then the exercise is failed
					if(e.getWeight() < lastWeight)
						return false;
					
					lastWeight = e.getWeight();
				}
				
				return true;
			}
		});
		
		// 2. matching expansion
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_EXPANSION1", langID, "Let _latex{$(v_i,v_j)$} be the first edge in _latex{$L$}. "), 3);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Select the edge (v<sub>i</sub>, v<sub>j</sub>) in the graph."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				GreedyAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.EDGES_ONLY);
				GreedyAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				GreedyAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				GreedyAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				GreedyAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(GreedyAlgorithmPlugin.this.graphView.getSelectedEdgeCount() != 1)
					return null;
				else
					return new Integer[] { GreedyAlgorithmPlugin.this.graphView.getSelectedEdge(0).getEdge().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					final Edge e = GreedyAlgorithmPlugin.this.graphView.getVisualEdgeByID(result.intValue()).getEdge();
					return "(" + e.getPredecessor() + ", " + e.getSuccessor() + ")";
				}
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Integer[] solutions) {
				state.addInt("firstEdge", solutions[0]);
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				final List<Integer> L = state.getList("L");
				return (L.size() > 0) ? L.get(0).equals(results[0]) : false;
			}
		});
		
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_EXPANSION2", langID, "Add _latex{$(v_i,v_j)$} to the matching _latex{$M$}.\n\n"), 4);
		step.setExercise(new AlgorithmExercise<Matching<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "What is <i>M</i> after this step (<i>select all matched edges in the graph</i>)?"), 1.0f, graphView) {
			
			@Override
			public boolean hasInputHint() {
				return true;
			}
			
			@Override
			public Annotation getInputHintMessage(LanguageFile langFile, String langID) {
				return new Annotation(LanguageFile.getLabel(GreedyAlgorithmPlugin.this.langFile, "EXERCISE_STEP4_INPUTHINT", langID, "<b>Select matched edges</b>:<br>Select the matched edges in the graph by using the mouse and pressing the <b>Ctrl</b>-key on your keyboard.<br>Afterwards click on the \"Solve Exercise\"-button of the task."));
			}
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				GreedyAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.EDGES_ONLY);
				GreedyAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				GreedyAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				GreedyAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				GreedyAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Matching<?>[] requestSolution() {
				// if their are no edges selected then break up
				if(GreedyAlgorithmPlugin.this.graphView.getSelectedEdgeCount() == 0)
					return null;
				
				Matching<Edge> m = new Matching<Edge>(GreedyAlgorithmPlugin.this.graphView.getGraph());
				
				try {
					// add the selected edges to the matching
					for(int i = 0; i < GreedyAlgorithmPlugin.this.graphView.getSelectedEdgeCount(); i++)
						m.add(GreedyAlgorithmPlugin.this.graphView.getSelectedEdge(i).getEdge());
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
				final MatchingByID<Edge> m = state.getMatching("M", GreedyAlgorithmPlugin.this.graphView.getGraph());
				return results[0].cast().equals(m);
			}
		});
		
		// 3. update L
		step = new AlgorithmStep(updateParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_UPDATEL", langID, "Delete all edges from _latex{$L$} which have _latex{$v_i$} or _latex{$v_j$} as endpoint.\n\n"), 5);
		step.setExercise(new AlgorithmExercise<List<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP5", langID, "Which edges will be removed from <i>L</i>?"), 2.0f) {
			
			private final String remove = LanguageFile.getLabel(GreedyAlgorithmPlugin.this.langFile, "EXERCISE_STEP5_REMOVE", GreedyAlgorithmPlugin.this.langID, "remove?");
			private List<Integer> L;
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				L = state.getList("L");
			}
			
			@SuppressWarnings("unchecked")
			@Override
			protected List<?>[] requestSolution() {
				final Graph<Vertex, Edge> graph = GreedyAlgorithmPlugin.this.graphView.getGraph();
				final SolutionEntry<?>[] entries = new SolutionEntry<?>[L.size()];
				SolutionEntry<JCheckBox> entry;
				Edge e;
				
				// create a solution entry with a checkbox for each edge of the list
				for(int i = 0; i < L.size(); i++) {
					e = graph.getEdgeByID(L.get(i));
					entries[i] = new SolutionEntry<JCheckBox>("(" + e.getPredecessor() + ", " + e.getSuccessor() + ")", new JCheckBox(remove));
				}
				
				if(!SolveExercisePane.showDialog(GreedyAlgorithmPlugin.this.host, this, entries, GreedyAlgorithmPlugin.this.langFile, GreedyAlgorithmPlugin.this.langID))
					return null;
				
				// remove all edges from the list that are selected to be removed
				for(int i = L.size() - 1; i >= 0; i--) {
					entry = (SolutionEntry<JCheckBox>)entries[i];
					if(entry.getComponent().isSelected())
						L.remove(i);
				}
				
				return new List<?>[] { L };
			}
			
			@Override
			protected String getResultAsString(List<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					@SuppressWarnings("unchecked")
					final List<Integer> L = (List<Integer>)result;
					return "L=" + getEdgeListAsString(L);
				}
			}
			
			@Override
			protected boolean examine(List<?>[] results, AlgorithmState state) {
				return doAutoExamine(state, new String[] { "L" }, results);
			}
		});
		
		// 4. stop criterion
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_STOP", langID, "If _latex{$L = \\emptyset$} then stop. Otherwise go to step 2."), 6);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP6", langID, "Will the algorithm stop?"), 1.0f) {
			
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
				
				if(!SolveExercisePane.showDialog(GreedyAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, GreedyAlgorithmPlugin.this.langFile, GreedyAlgorithmPlugin.this.langID))
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
				final List<Integer> L = state.getList("L");
				
				return (results[0] != null && results[0] == L.isEmpty());
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
		final String amvAddition = " (" + LanguageFile.getLabel(langFile, "LEGEND_ADJACENCYMATRIX_OPTIONAL", langID, "optional") + ")";
		
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_MATCHEDEDGES", langID, "The matched edges of matching M"), LegendItem.createLineIcon(colorMatchedEdges, lineWidthMatchedEdges, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURREDGE", langID, "The current edge (v<sub>i</sub>, v<sub>j</sub>)"), LegendItem.createLineIcon(colorCurrEdge, lineWidthCurrEdge, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_V_I_V_J", langID, "The vertices v<sub>i</sub> and v<sub>j</sub> of the current matched edge"), LegendItem.createCircleIcon(Color.white, colorCurrEdge, 1)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_REMOVABLEEDGE", langID, "Edge that shares an endpoint with the current edge (v<sub>i</sub>, v<sub>j</sub>) and has to be removed from L"), LegendItem.createLineIcon(colorEdgeToRemove, 2, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item5", matchingView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_MATCHING_MODIFICATION", langID, "The matching M becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item6", listView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_LIST_CURREDGE", langID, "The current edge (v<sub>i</sub>, v<sub>j</sub>)"), LegendItem.createRectangleIcon(colorCurrEdge, colorCurrEdge, 0)));
		legendView.add(new LegendItem("item7", listView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_LIST_REMOVABLEEDGE", langID, "Edge that shares an endpoint with the current edge (v<sub>i</sub>, v<sub>j</sub>) and has to be removed from L"), LegendItem.createRectangleIcon(colorEdgeToRemove, colorEdgeToRemove, 0)));
		legendView.add(new LegendItem("item8", adjacencyMatrixView.getTitle() + amvAddition, LanguageFile.getLabel(langFile, "LEGEND_ADJACENCYMATRIX_CURREDGE", langID, "The current edge (v<sub>i</sub>, v<sub>j</sub>)"), LegendItem.createRectangleIcon(colorCurrEdge, colorCurrEdge, 0)));
		legendView.add(new LegendItem("item9", adjacencyMatrixView.getTitle() + amvAddition, LanguageFile.getLabel(langFile, "LEGEND_ADJACENCYMATRIX_STRIKEOUT", langID, "Striked off vertices its edge was added to the matching M"), LegendItem.createLineIcon(Color.black, 1)));
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
	 * Gets the given list as a string of edges.
	 * 
	 * @param l the list with the identifiers of the edges
	 * @return the string representation of the list
	 * @since 1.0
	 */
	private String getEdgeListAsString(final List<Integer> l) {
		final Graph<Vertex, Edge> graph = GreedyAlgorithmPlugin.this.graphView.getGraph();
		final StringBuilder s = new StringBuilder();
		boolean delimiter = false;
		Edge e;
		
		s.append("[");
		for(int i = 0; i < l.size(); i++) {
			e = graph.getEdgeByID(l.get(i));
			if(delimiter)
				s.append(",");
			s.append("(" + e.getPredecessor() + ", " + e.getSuccessor() + ")");
			delimiter = true;
		}
		s.append("]");
		
		return s.toString();
	}
	
	/**
	 * The runtime environment of the Greedy algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class GreedyRTE extends AlgorithmRTE {
		
		/** the list of all edges */
		private List<Integer> L;
		/** the matching (is <code>null</code> when the algorithm starts) */
		private Matching<Edge> M;
		/** the id of the current first edge (v_i,v_j) of L */
		private int firstEdge;
		/** the sorted list of all edges by the user */
		private List<Integer> userSortL;
		/** the first edge the user has chosen */
		private int userChoiceFirstEdge;
		/** the random strikeout colors */
		private List<Color> strikeoutColors;
		/** the index of the next strikeout color */
		private int nextStrikeoutColorIndex;
		
		/**
		 * Creates a new runtime environment.
		 * 
		 * @since 1.0
		 */
		public GreedyRTE() {
			super(GreedyAlgorithmPlugin.this, GreedyAlgorithmPlugin.this.algoText);
			
			userSortL = null;
			userChoiceFirstEdge = 0;
			
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
		 * Initializes the adjacency matrix and random strikeout colors but only if the display mode is
		 * with adjacency matrix view.
		 * 
		 * @since 1.0
		 */
		public void initAMDisplay() {
			if(!GreedyAlgorithmPlugin.this.adjacencyMatrixEnabled)
				return;
			
			final Graph<Vertex, Edge> graph = GreedyAlgorithmPlugin.this.graphView.getGraph();
			final Map<Integer, String> labels = new HashMap<Integer, String>();
			
			// create the vertex labels
			for(int i = 0; i < graph.getOrder(); i++)
				labels.put(i, graph.getVertex(i).getCaption());
			
			// initialize the adjacency matrix view and set the vertex labels
			GreedyAlgorithmPlugin.this.adjacencyMatrixView.setMatrix(GraphUtils.createAdjacencyMatrix(graph, true));
			GreedyAlgorithmPlugin.this.adjacencyMatrixView.setColumnLabels(labels);
			GreedyAlgorithmPlugin.this.adjacencyMatrixView.setRowLabels(labels);
			GreedyAlgorithmPlugin.this.adjacencyMatrixView.setPaintLabels(true);
			
			// reset the strikeout color index
			nextStrikeoutColorIndex = 0;
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = GreedyAlgorithmPlugin.this.graphView.getGraph();
			ExecutionTableItem item;
			GraphView<Vertex, Edge>.VisualEdge ve;
			GraphView<Vertex, Edge>.VisualVertex vv_i;
			GraphView<Vertex, Edge>.VisualVertex vv_j;
			Edge e;
			int nextStep = -1;
			
			switch(stepID) {
				case 1:
					// let M be a zero matching and L be the list of all edges
					
					M = new Matching<Edge>(graph);
					
					sleep(250);
					GreedyAlgorithmPlugin.this.matchingView.setBackground(GreedyAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					visualizeMatching();
					sleep(250);
					GreedyAlgorithmPlugin.this.matchingView.setBackground(Color.white);
					sleep(250);
					
					L.clear();
					for(Edge edge : graph.getEdgeSet()) {
						L.add(edge.getID());
						// visualize the edge in the list
						GreedyAlgorithmPlugin.this.listView.add(new ExecutionTableItem(new Object[] { "(" + edge.getPredecessor() + ", " + edge.getSuccessor() + ")", edge.getWeight() }, edge.getID()));
						sleep(250);
					}
				
					nextStep = 2;
					break;
				case 2:
					// sort L non-descending by the weight of the edges
					
					sleep(1000);
					
					// if the user solves the related exercise of step 2 correct then adopt the sorted list
					if(userSortL != null)
						L = userSortL;
					else {
						GreedyAlgorithmPlugin.this.listView.sortItems(1, SortOrder.ASCENDING);
						// adopt the sort order of the list
						L.clear();
						for(int i = 0; i < GreedyAlgorithmPlugin.this.listView.getItemCount(); i++)
							L.add(GreedyAlgorithmPlugin.this.listView.getItem(i).getID());
						
						sleep(1500);
					}
					
					// clear the user sort
					userSortL = null;
					
					nextStep = 3;
					break;
				case 3:
					//  let (v_i,v_j) be the first edge in L
					firstEdge = (L.size() > 0) ? L.get(0) : 0;
					
					// adopt the user's choice of the first edge in the related exercise of step 3 if one is given
					if(userChoiceFirstEdge > 0)
						firstEdge = userChoiceFirstEdge;
					
					// clear the user's choice of the first edge
					userChoiceFirstEdge = 0;
					
					if(firstEdge < 1)
						nextStep = -1;
					else {
						ve = GreedyAlgorithmPlugin.this.graphView.getVisualEdgeByID(firstEdge);
						item = GreedyAlgorithmPlugin.this.listView.getVisibleRow(0);
						
						sleep(500);
						
						// highlight the first edge in the list and the graph view (remember that the graph view is not auto repaintable!)
						item.setBackground(GreedyAlgorithmPlugin.this.colorCurrEdge);
						ve.setColor(GreedyAlgorithmPlugin.this.colorCurrEdge);
						ve.setLineWidth(GreedyAlgorithmPlugin.this.lineWidthCurrEdge);
						GreedyAlgorithmPlugin.this.graphView.repaint();
						
						sleep(750);
						
						// remove the highlight of the edge item but not from the edge in the graph so that it is
						// highlighted up to the next step
						item.setBackground(Color.white);
						
						nextStep = 4;
					}
					
					break;
				case 4:
					// add (v_i,v_j) to M
					
					e = graph.getEdgeByID(firstEdge);
					M.add(e);
					
					sleep(250);
					visualizeMatching();
					sleep(250);
					GreedyAlgorithmPlugin.this.matchingView.setBackground(GreedyAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					sleep(250);
					GreedyAlgorithmPlugin.this.matchingView.setBackground(Color.white);
					sleep(500);
					
					nextStep = 5;
					break;
				case 5:
					// remove all edges from L with v_i or v_j as endpoint
					
					e = graph.getEdgeByID(firstEdge);
					
					// highlight v_i and v_j and their edge
					ve = GreedyAlgorithmPlugin.this.graphView.getVisualEdgeByID(firstEdge);
					vv_i = ve.getPredecessor();
					vv_j = ve.getSuccessor();
					ve.setColor(GreedyAlgorithmPlugin.this.colorCurrEdge);
					ve.setLineWidth(GreedyAlgorithmPlugin.this.lineWidthCurrEdge);
					vv_i.setForeground(GreedyAlgorithmPlugin.this.colorCurrEdge);
					vv_j.setForeground(GreedyAlgorithmPlugin.this.colorCurrEdge);
					GreedyAlgorithmPlugin.this.graphView.repaint();
					
					sleep(500);
					
					//for(int i = L.size() - 1; i >= 0; i--) {
					for(int i = 0; i < L.size(); i++) {
						final Edge edge = graph.getEdgeByID(L.get(i));
						// does this edge has a shared endpoint with e?
						if(edge == e || edge.getPredecessor() == e.getPredecessor() || edge.getSuccessor() == e.getSuccessor() || edge.getPredecessor() == e.getSuccessor() || edge.getSuccessor() == e.getPredecessor()) {
							// remove edge because one of their endpoints matches v_i or v_j
							L.remove(i);
							i--;
							
							final GraphView<Vertex, Edge>.VisualEdge vedge = GreedyAlgorithmPlugin.this.graphView.getVisualEdge(edge);
							item = GreedyAlgorithmPlugin.this.listView.getVisibleRowByID(edge.getID());
							
							// highlight the edge in the graph
							vedge.setColor(GreedyAlgorithmPlugin.this.colorEdgeToRemove);
							vedge.setLineWidth(GreedyAlgorithmPlugin.this.lineWidthEdgeToRemove);
							GreedyAlgorithmPlugin.this.graphView.repaint();
							sleep(500);
							// and afterwards in the list
							item.setBackground(GreedyAlgorithmPlugin.this.colorEdgeToRemove);
							
							sleep(750);
							
							// deactivate the edge in the list and remove the highlight
							item.setVisible(false);
							item.setBackground(Color.white);
							vedge.setColor((edge == e) ? GreedyAlgorithmPlugin.this.colorCurrEdge : GraphView.DEF_EDGECOLOR);
							vedge.setLineWidth((edge == e) ? GreedyAlgorithmPlugin.this.lineWidthCurrEdge : GraphView.DEF_EDGELINEWIDTH);
							GreedyAlgorithmPlugin.this.graphView.repaint();
							
							sleep(500);
						}
					}
					
					// strikeout the edges from the adjacency matrix if necessary
					if(GreedyAlgorithmPlugin.this.adjacencyMatrixEnabled) {
						final int i = Math.min(ve.getEdge().getPredecessor().getIndex(), ve.getEdge().getSuccessor().getIndex());
						final int j = Math.max(ve.getEdge().getPredecessor().getIndex(), ve.getEdge().getSuccessor().getIndex());
						final Color c = getNextStrikeoutColor();
						final MatrixEditor.Strikeout s1 = new MatrixEditor.Strikeout(i, c, 2);
						final MatrixEditor.Strikeout s2 = new MatrixEditor.Strikeout(j, c, 2);
						
						// highlight the weight in the matrix
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.setElementBackground(i, j, GreedyAlgorithmPlugin.this.colorCurrEdge);
						sleep(750);
						// strikeout the edge
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.addRowStrikeout(s1);
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.addRowStrikeout(s2);
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.addColumnStrikeout(s1);
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.addColumnStrikeout(s2);
						sleep(750);
						// remove the highlight from the matrix
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.setElementBackground(i, j, Color.white);
					}
					
					// remove the highlight from v_i and v_j (repaint is done in visualizeMatching())
					vv_i.setForeground(GraphView.DEF_VERTEXFOREGROUND);
					vv_j.setForeground(GraphView.DEF_VERTEXFOREGROUND);
					// finally visualize the matching once more
					visualizeMatching();
					
					nextStep = 6;
					break;
				case 6:
					// if L is empty then stop otherwise go to 2.
					
					sleep(1000);
					
					if(L.isEmpty())
						nextStep = -1;
					else
						nextStep = 3;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addList("L", L);
			state.addMatching("M", (M != null) ? M.cast() : null);
			state.addInt("firstEdge", firstEdge);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			L = state.getList("L");
			final MatchingByID<Edge> m = state.getMatching("M", GreedyAlgorithmPlugin.this.graphView.getGraph());
			M = (m != null) ? m.cast() : null;
			firstEdge = state.getInt("firstEdge");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			L = state.addList("L", new ArrayList<Integer>());
			state.addMatching("M", null);
			firstEdge = state.addInt("firstEdge", 0);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			visualizeMatching();
			visualizeMatchingAsText();
			
			switch(stepID) {
				case 1:
					// if setp 1 is rolled back remove all items that were added in this step
					GreedyAlgorithmPlugin.this.listView.removeAllItems();
					break;
				case 2:
					// if the sort step is rolled back make the list view unsorted
					GreedyAlgorithmPlugin.this.listView.sortItems(1, SortOrder.UNSORTED);
					break;
				case 5:
					// step 5 removes edges from L meaning items are set to invisible
					// roll back this step means that all items are set to visible that are currently in L
					ExecutionTableItem item;
					for(int i = 0; i < GreedyAlgorithmPlugin.this.listView.getItemCount(); i++) {
						item = GreedyAlgorithmPlugin.this.listView.getItem(i);
						item.setVisible(L.contains(item.getID()));
					}
					
					// remove the strikeouts if necessary
					if(GreedyAlgorithmPlugin.this.adjacencyMatrixEnabled) {
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.removeLastColumnStrikeout();
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.removeLastColumnStrikeout();
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.removeLastRowStrikeout();
						GreedyAlgorithmPlugin.this.adjacencyMatrixView.removeLastRowStrikeout();
					}
					break;
					
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			if(stepID == 2)
				userSortL = state.getList("L");
			else if(stepID == 3)
				userChoiceFirstEdge = state.getInt("firstEdge");
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { GreedyAlgorithmPlugin.this.graphView, GreedyAlgorithmPlugin.this.adjacencyMatrixView, GreedyAlgorithmPlugin.this.listView, GreedyAlgorithmPlugin.this.matchingView };
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
			
			for(int i = 0; i < GreedyAlgorithmPlugin.this.graphView.getVisualEdgeCount(); i++) {
				ve = GreedyAlgorithmPlugin.this.graphView.getVisualEdge(i);
				
				// visualize matched and unmatched edges
				if(M.contains(ve.getEdge())) {
					ve.setColor(GreedyAlgorithmPlugin.this.colorMatchedEdges);
					ve.setLineWidth(GreedyAlgorithmPlugin.this.lineWidthMatchedEdges);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			// show the visualization
			GreedyAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the matching M in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeMatchingAsText() {
			GreedyAlgorithmPlugin.this.matchingView.setText((M != null) ? "M=" + M.toString() : "");
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
