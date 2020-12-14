package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
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
import lavesdk.algorithm.plugin.extensions.CircleLayoutToolBarExtension;
import lavesdk.algorithm.plugin.extensions.CompleteGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.MatrixToGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultTransferProtocol;
import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.GraphScene;
import lavesdk.algorithm.plugin.views.GraphTransferProtocol;
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
import lavesdk.algorithm.text.Annotation;
import lavesdk.algorithm.text.AnnotationImagesList;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.InputDialog;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.dialogs.enums.AllowedGraphType;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.ExecutionTableColumn;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Walk;
import lavesdk.math.graph.WalkByID;
import lavesdk.math.graph.enums.Type;
import lavesdk.utils.GraphUtils;
import lavesdk.utils.MathUtils;

/**
 * Plugin that visualizes and teaches users the Savings algorithm.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class SavingsAlgorithmPlugin implements AlgorithmPlugin {
	
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
	private SavingsGraphView graphView;
	/** the view that displays the algorithm text */
	private AlgorithmTextView algoTextView;
	/** the view that displays the cycle r */
	private TextAreaView cycleRView;
	/** the view that displays the cycle r' */
	private TextAreaView cycleR_ApoView;
	/** the view that displays the list */
	private ExecutionTableView listView;
	/** the view that displays the savings */
	private ExecutionTableView savingsView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the savings algorithm */
	private SavingsRTE rte;
	/** the graph the user has created */
	private Graph<WeightedVertex, Edge> userGraph;
	/** the transfer protocol of the user graph */
	private GraphTransferProtocol<WeightedVertex, Edge> userGTP;
	/** toolbar extension to create graphs from adjacency matrices */
	private MatrixToGraphToolBarExtension<WeightedVertex, Edge> matrixToGraph;
	/** toolbar extension to check whether a graph is complete or to create one */
	private CompleteGraphToolBarExtension<WeightedVertex, Edge> completeExt;
	/** toolbar extension to layout a graph in a circle */
	private CircleLayoutToolBarExtension<WeightedVertex, Edge> circleLayoutExt;
	/** the images list of the annotations */
	private AnnotationImagesList annotationImgList;
	/** the add icon resource */
	private Icon addIconRes;
	/** the remove icon resource */
	private Icon removeIconRes;
	/** the import icon resource */
	private Icon importIconRes;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for C and D (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cd;
	/** the view group for E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ef;
	/** the view group for A,B,C,D,E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcdef;
	
	// modifiable visualization data
	/** color to visualize the starting vertex */
	private Color colorStartVertex;
	/** color to visualize the cycle r */
	private Color colorCycleR;
	/** color to visualize a positive savings value */
	private Color colorPositiveSavingsValue;
	/** color to visualize a non-positive savings value */
	private Color colorNonPositiveSavingsValue;
	/** color to visualize the used edges of the undirected graph */
	private Color colorUsedEdges;
	/** color to visualize the unused edges of the undirected graph */
	private Color colorUnusedEdges;
	/** color to visualize the current vertex pair */
	private Color colorVertexPair;
	/** color to visualize the edges that are removed from r */
	private Color colorEdgesToRemove;
	/** color to visualize an edge that is added to r */
	private Color colorEdgeToAdd;
	/** color to visualize modified objects */
	private Color colorModified;
	/** line with of the starting vertex */
	private int lineWidthStartVertex;
	/** line with of the edges of the cycle r */
	private int lineWidthCycleR;
	/** line with of an edge that is added to r */
	private int lineWidthEdgeToAdd;
	
	/** configuration key for the {@link #colorStartVertex} */
	private static final String CFGKEY_COLOR_STARTVERTEX = "colorStartVertex";
	/** configuration key for the {@link #colorCycleR} */
	private static final String CFGKEY_COLOR_CYCLER = "colorCycleR";
	/** configuration key for the {@link #colorPositiveSavingsValue} */
	private static final String CFGKEY_COLOR_POSITIVESAVINGSVALUE = "colorPositiveSavingsValue";
	/** configuration key for the {@link #colorNonPositiveSavingsValue} */
	private static final String CFGKEY_COLOR_NONPOSITIVESAVINGSVALUE = "colorNonPositiveSavingsValue";
	/** configuration key for the {@link #colorUsedEdges} */
	private static final String CFGKEY_COLOR_USEDEDGES = "colorUsedEdges";
	/** configuration key for the {@link #colorUnusedEdges} */
	private static final String CFGKEY_COLOR_UNUSEDEDGES = "colorUnusedEdges";
	/** configuration key for the {@link #colorVertexPair} */
	private static final String CFGKEY_COLOR_VERTEXPAIR = "colorVertexPair";
	/** configuration key for the {@link #colorEdgesToRemove} */
	private static final String CFGKEY_COLOR_EDGESTOREMOVE = "colorEdgesToRemove";
	/** configuration key for the {@link #colorEdgeToAdd} */
	private static final String CFGKEY_COLOR_EDGETOADD = "colorEdgeToAdd";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #lineWidthStartVertex} */
	private static final String CFGKEY_LINEWIDTH_STARTVERTEX = "lineWidthStartVertex";
	/** configuration key for the {@link #lineWidthCycleR} */
	private static final String CFGKEY_LINEWIDTH_CYCLER = "lineWidthCycleR";
	/** configuration key for the {@link #lineWidthEdgeToAdd} */
	private static final String CFGKEY_LINEWIDTH_EDGETOADD = "lineWidthEdgeToAdd";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langSavings.txt"));
			// include the language file of the host to only use one language file
			this.langFile.include(host.getLanguageFile());
		} catch (IOException e) {
			this.langFile = null;
		}
		this.langID = host.getLanguageID();
		
		// load the images
		annotationImgList = new AnnotationImagesList();
		annotationImgList.add("case1", resLoader.getResource("main/resources/case1.png"));
		annotationImgList.add("case2", resLoader.getResource("main/resources/case2.png"));
		annotationImgList.add("case3", resLoader.getResource("main/resources/case3.png"));
		annotationImgList.add("case4", resLoader.getResource("main/resources/case4.png"));
		
		// create plugin
		this.host = host;
		this.config = (config != null) ? config : new Configuration();
		this.vgfFileFilter = new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf");
		this.pngFileFilter = new FileNameExtensionFilter("Portable Network Graphic (*.png)", "png");
		this.graphView = new SavingsGraphView(LanguageFile.getLabel(langFile, "VIEW_GRAPH_TITLE", langID, "Graph"), langFile, langID);
		this.cycleRView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_CYCLER_TITLE", langID, "Cycle r"), true, langFile, langID);
		this.cycleR_ApoView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_CYCLER_APOSTROPHE_TITLE", langID, "Cycle r'"), false, langFile, langID);
		this.listView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_LIST_TITLE", langID, "List"), true, langFile, langID);
		this.savingsView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_SAVINGS_TITLE", langID, "Savings"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new SavingsRTE();
		this.userGraph = null;
		this.matrixToGraph = new MatrixToGraphToolBarExtension<WeightedVertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.completeExt = new CompleteGraphToolBarExtension<WeightedVertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<WeightedVertex, Edge>(graphView, langFile, langID, false);
		this.addIconRes = resLoader.getResourceAsIcon("main/resources/plus.png");
		this.removeIconRes = resLoader.getResourceAsIcon("main/resources/minus.png");
		this.importIconRes = resLoader.getResourceAsIcon("main/resources/table-import.png");
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		cycleRView.setAutoRepaint(true);
		cycleR_ApoView.setAutoRepaint(true);
		listView.setAutoRepaint(true);
		savingsView.setAutoRepaint(true);
		
		// the view of cycle r' is displayed by the rte
		cycleR_ApoView.setVisible(false);
		
		// set a smaller offset distance so that directed edges are closer
		graphView.setEdgeOffsetDistance(16);
		
		// load the visualization colors from the configuration of the plugin
		colorStartVertex = this.config.getColor(CFGKEY_COLOR_STARTVERTEX, new Color(130, 200, 255));
		colorCycleR = this.config.getColor(CFGKEY_COLOR_CYCLER, new Color(200, 145, 145));
		colorPositiveSavingsValue = this.config.getColor(CFGKEY_COLOR_POSITIVESAVINGSVALUE, new Color(120, 210, 80));
		colorNonPositiveSavingsValue = this.config.getColor(CFGKEY_COLOR_NONPOSITIVESAVINGSVALUE, new Color(215, 75, 75));
		colorUsedEdges = this.config.getColor(CFGKEY_COLOR_USEDEDGES, new Color(155, 155, 155));
		colorUnusedEdges = this.config.getColor(CFGKEY_COLOR_UNUSEDEDGES, new Color(195, 195, 195));
		colorVertexPair = this.config.getColor(CFGKEY_COLOR_VERTEXPAIR, new Color(255, 220, 80));
		colorEdgesToRemove = this.config.getColor(CFGKEY_COLOR_EDGESTOREMOVE, new Color(255, 220, 80));
		colorEdgeToAdd = this.config.getColor(CFGKEY_COLOR_EDGETOADD, new Color(105, 150, 180));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		lineWidthStartVertex = this.config.getInt(CFGKEY_LINEWIDTH_STARTVERTEX, 2);
		lineWidthCycleR = this.config.getInt(CFGKEY_LINEWIDTH_CYCLER, 2);
		lineWidthEdgeToAdd = this.config.getInt(CFGKEY_LINEWIDTH_EDGETOADD, 2);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		cycleRView.loadConfiguration(config, "cycleRView");
		cycleR_ApoView.loadConfiguration(config, "cycleR_ApoView");
		listView.loadConfiguration(config, "listView");
		savingsView.loadConfiguration(config, "savingsView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Savings algorithm");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds a cycle of little length which contains all vertices of the graph and which can be separated into disjoint cycles, each of which fulfill the delivery constraint.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "An edge- and vertex weighted (c(e), b(v), each non-negative), undirected graph K<sub>n</sub>, a starting vertex v<sub>s</sub> and a delivery capacity b<sub>max</sub> ≥ max{b(v)}.");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Vehicle routing problem");
	}

	@Override
	public String getSubject() {
		return LanguageFile.getLabel(langFile, "ALGO_SUBJECT", langID, "Logistics");
	}
	
	@Override
	public String getInstructions() {
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extensions to check whether the created graph is complete, to create a complete graph by indicating the number of vertices, to<br>create a graph by use of an adjacency matrix or you can arrange the vertices of your created graph in a circle.<br><br><b>Starting the algorithm</b>:<br>Before you start the algorithm select a vertex v<sub>s</sub> the algorithm should begin with and afterwards enter the delivery capacity b<sub>max</sub> in the following dialog.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
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
		return false;
	}

	@Override
	public void loadCreatorPreferences(PropertiesListModel plm) {
	}

	@Override
	public void onCreate(ViewContainer container, PropertiesListModel creatorProperties) {
		// change the graph in the view
		graphView.setGraph(new SimpleGraph<WeightedVertex, Edge>(false));
		graphView.repaint();
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|///////////|///
		 * / /|/         /|/ /	A = algorithm text view
		 * /A/|/    C    /|/E/	B = legend view
		 * / /|/         /|///	C = graph view
		 * ///|///////////|---	D = text area view (cycle view)
		 * ---|-----------|///	E = execution table view (list view)
		 * ///|///////////|/ /	F = execution table view (savings view)
		 * /B/|/  D (H)  /|/F/  H = text area view (cycle W' view) -> its visibility is controlled by the runtime environment
		 * ///|///////////|///
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		cd = new ViewGroup(ViewGroup.VERTICAL);
		ef = new ViewGroup(ViewGroup.VERTICAL);
		abcdef = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// middle group C and D
		cd.add(graphView);
		cd.add(cycleRView);
		cd.add(cycleR_ApoView);
		cd.restoreWeights(config, "weights_cd", new float[] { 0.6f, 0.2f, 0.2f });
		
		// middle group E and F
		ef.add(listView);
		ef.add(savingsView);
		ef.restoreWeights(config, "weights_ef", new float[] { 0.5f, 0.5f });
		
		// group for (A,B), (C,D) and (E,F)
		abcdef.add(ab);
		abcdef.add(cd);
		abcdef.add(ef);
		abcdef.restoreWeights(config, "weights_abcdef", new float[] { 0.4f, 0.45f, 0.15f });
		
		container.setLayout(new BorderLayout());
		container.add(abcdef, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		cycleRView.saveConfiguration(config, "cycleView");
		cycleR_ApoView.saveConfiguration(config, "cycleR_ApoView");
		listView.saveConfiguration(config, "listView");
		savingsView.saveConfiguration(config, "savingsView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(cd != null)
			cd.storeWeights(config, "weights_cd");
		if(ef != null)
			ef.storeWeights(config, "weights_ef");
		if(abcdef != null)
			abcdef.storeWeights(config, "weights_abcdef");
		
		// reset view content where it is necessary
		graphView.reset();
		cycleRView.reset();
		cycleR_ApoView.reset();
		listView.reset();
		savingsView.reset();
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
		plm.add(new ColorProperty(CFGKEY_COLOR_CYCLER, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CYCLER", langID, "Color of the cycle r"), colorCycleR));
		plm.add(new ColorProperty(CFGKEY_COLOR_POSITIVESAVINGSVALUE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_POSITIVESAVINGSVALUE", langID, "Background color of an item with a positive savings value"), colorPositiveSavingsValue));
		plm.add(new ColorProperty(CFGKEY_COLOR_NONPOSITIVESAVINGSVALUE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_NONPOSITIVESAVINGSVALUE", langID, "Background color of an item with a non-positive savings value"), colorNonPositiveSavingsValue));
		plm.add(new ColorProperty(CFGKEY_COLOR_USEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_USEDEDGES", langID, "Color of the edges of the underlying undirected input graph that are contained in the cycle r"), colorUsedEdges));
		plm.add(new ColorProperty(CFGKEY_COLOR_UNUSEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_UNUSEDEDGES", langID, "Color of the edges of the underlying undirected input graph that are not contained in the cycle r"), colorUnusedEdges));
		plm.add(new ColorProperty(CFGKEY_COLOR_VERTEXPAIR, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_VERTEXPAIR", langID, "Background color of the current vertex pair v<sub>i</sub>, v<sub>j</sub>"), colorVertexPair));
		plm.add(new ColorProperty(CFGKEY_COLOR_EDGESTOREMOVE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_EDGESTOREMOVE", langID, "Color of the edges of cycle r that are substituted"), colorEdgesToRemove));
		plm.add(new ColorProperty(CFGKEY_COLOR_EDGETOADD, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_EDGETOADD", langID, "Color of the surrogate edge (v<sub>i</sub>, v<sub>j</sub>)"), colorEdgeToAdd));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwStartVertex = new NumericProperty(CFGKEY_LINEWIDTH_STARTVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_STARTVERTEX", langID, "Line width of the starting vertex v<sub>s</sub>"), lineWidthStartVertex, true);
		lwStartVertex.setMinimum(1);
		lwStartVertex.setMaximum(5);
		plm.add(lwStartVertex);
		final NumericProperty lwCycleR = new NumericProperty(CFGKEY_LINEWIDTH_CYCLER, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_CYCLER", langID, "Line with of the cycle r"), lineWidthCycleR, true);
		lwCycleR.setMinimum(1);
		lwCycleR.setMaximum(5);
		plm.add(lwCycleR);
		final NumericProperty lwEdgeToAdd = new NumericProperty(CFGKEY_LINEWIDTH_EDGETOADD, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_EDGETOADD", langID, "Line with of the surrogate edge"), lineWidthEdgeToAdd, true);
		lwEdgeToAdd.setMinimum(1);
		lwEdgeToAdd.setMaximum(5);
		plm.add(lwEdgeToAdd);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorStartVertex = config.addColor(CFGKEY_COLOR_STARTVERTEX, plm.getColorProperty(CFGKEY_COLOR_STARTVERTEX).getValue());
		colorCycleR = config.addColor(CFGKEY_COLOR_CYCLER, plm.getColorProperty(CFGKEY_COLOR_CYCLER).getValue());
		colorPositiveSavingsValue = config.addColor(CFGKEY_COLOR_POSITIVESAVINGSVALUE, plm.getColorProperty(CFGKEY_COLOR_POSITIVESAVINGSVALUE).getValue());
		colorNonPositiveSavingsValue = config.addColor(CFGKEY_COLOR_NONPOSITIVESAVINGSVALUE, plm.getColorProperty(CFGKEY_COLOR_NONPOSITIVESAVINGSVALUE).getValue());
		colorUsedEdges = config.addColor(CFGKEY_COLOR_USEDEDGES, plm.getColorProperty(CFGKEY_COLOR_USEDEDGES).getValue());
		colorUnusedEdges = config.addColor(CFGKEY_COLOR_UNUSEDEDGES, plm.getColorProperty(CFGKEY_COLOR_UNUSEDEDGES).getValue());
		colorVertexPair = config.addColor(CFGKEY_COLOR_VERTEXPAIR, plm.getColorProperty(CFGKEY_COLOR_VERTEXPAIR).getValue());
		colorEdgesToRemove = config.addColor(CFGKEY_COLOR_EDGESTOREMOVE, plm.getColorProperty(CFGKEY_COLOR_EDGESTOREMOVE).getValue());
		colorEdgeToAdd = config.addColor(CFGKEY_COLOR_EDGETOADD, plm.getColorProperty(CFGKEY_COLOR_EDGETOADD).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthStartVertex = config.addInt(CFGKEY_LINEWIDTH_STARTVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_STARTVERTEX).getValue().intValue());
		lineWidthCycleR = config.addInt(CFGKEY_LINEWIDTH_CYCLER, plm.getNumericProperty(CFGKEY_LINEWIDTH_CYCLER).getValue().intValue());
		lineWidthEdgeToAdd = config.addInt(CFGKEY_LINEWIDTH_EDGETOADD, plm.getNumericProperty(CFGKEY_LINEWIDTH_EDGETOADD).getValue().intValue());
		
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
		// the algorithm needs a starting vertex as input and the graph may not have negative weights
		if(graphView.getSelectedVertexCount() != 1) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTVERTEX", langID, "Please select the starting vertex in the graph!"), LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTVERTEX_TITLE", langID, "Select starting vertex"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(!GraphUtils.isComplete(graphView.getGraph())) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NOTCOMPLETE", langID, "The created graph is not complete!\nThe Savings algorithm can only be applied to complete graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NOTCOMPLETE_TITLE", langID, "No complete graph"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(containsNegativeWeights()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS", langID, "The created graph contains negative weights!\nThe Savings algorithm can only be applied to non-negative weighted graphs (see assumptions)."), LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS_TITLE", langID, "Negative weights"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			Number b_max = null;
			
			// let the user input the delivery capacity
			final InputDialog inputDlg = new InputDialog(host, LanguageFile.getLabel(langFile, "DELIVERYCAPACITYDLG_TITLE", langID, "Delivery Capacity"), LanguageFile.getLabel(langFile, "DELIVERYCAPACITYDLG_DESC", langID, "Enter the delivery capacity b<sub>max</sub> ≥ max{b(v)}."), "b<sub>max</sub> = ", langFile, langID);
			inputDlg.setVisible(true);
			
			if(!inputDlg.isCanceled()) {
				try {
					b_max = NumberFormat.getInstance().parse(inputDlg.getInput());
					
					// check if the entered b_max is smaller than one of the vertex weights (in case of that the entered value is invalid)
					for(int i = 0; i < graphView.getGraph().getOrder(); i++) {
						if(b_max.floatValue() < graphView.getGraph().getVertex(i).getWeight()) {
							e.doit = false;
							break;
						}
					}
				} catch (ParseException e1) {
					e.doit = false;
				}
			}
			else
				e.doit = false;
			
			// invalid input? then pop-up a message and quit
			if(!e.doit) {
				host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDDELIVERYCAPACITY", langID, "Your input is incorrect!\nThe delivery capacity has to be a number and must be greater or equal the maximum vertex weight in the graph."), LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDDELIVERYCAPACITY_TITLE", langID, "Invalid input"), MessageIcon.INFO);
				return;
			}
			
			final String selVertexCaption = graphView.getSelectedVertex(0).getVertex().getCaption();
			graphView.setEditable(false);
			
			// store the graph the user has created
			userGraph = graphView.getGraph();
			userGTP = new DefaultTransferProtocol<WeightedVertex, Edge>(graphView, false);
			userGTP.prepare();
			// create a mixed graph that can visualize directed and undirected edges
			graphView.setGraph(new Graph<WeightedVertex, Edge>(Type.MIXED));
			graphView.transferGraph(userGTP);
			
			// select the old vertex
			graphView.selectVertex(graphView.getVisualVertexByCaption(selVertexCaption), false);
			
			// get the starting vertex but after the graph is transfered (because the vertex object has changed)
			final WeightedVertex startVertex = graphView.getSelectedVertex(0).getVertex();
			graphView.deselectAll();
			
			// set the start vertex but only after the edit mode is disabled because the start vertex should be visualized)
			rte.setStartVertex(startVertex);
			rte.setDeliveryCapacity(b_max.floatValue());
			
			// add a custom object that shows the delivery capacity
			graphView.addVisualObject(new DeliveryCapacityDisplay(b_max.floatValue()));
			graphView.repaint();

			// reset the table views
			listView.reset();
			savingsView.reset();
			cycleRView.reset();
			cycleR_ApoView.reset();
			
			// the r' view is not visible yet
			cycleR_ApoView.setVisible(false);
			
			// create the columns of the tables
			ExecutionTableColumn column = new ExecutionTableColumn("v<sub>i</sub>");
			column.setWidth(30);
			listView.add(column);
			column = new ExecutionTableColumn("v<sub>j</sub>");
			column.setWidth(30);
			listView.add(column);
			listView.add(new ExecutionTableColumn("sav(v<sub>i</sub>,v<sub>j</sub>)"));
			column = new ExecutionTableColumn("v<sub>i</sub>");
			column.setWidth(30);
			savingsView.add(column);
			column = new ExecutionTableColumn("v<sub>j</sub>");
			column.setWidth(30);
			savingsView.add(column);
			savingsView.add(new ExecutionTableColumn("sav(v<sub>i</sub>,v<sub>j</sub>)"));
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
		// restore the user graph because it was transferred in a mixed graph
		graphView.setGraph(userGraph);
		graphView.transferGraph(userGTP);
		graphView.setEditable(true);
		graphView.removeAllVisualObjects();
		graphView.repaint();
		
		userGraph = null;
		
		cycleR_ApoView.setVisible(false);
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
		final AlgorithmParagraph itParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ITERATION", langID, "2. Iteration:"), 2);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INIT", langID, "Set _latex{$r := (v_s,v_1,v_s,v_2,v_s,...)$} with _latex{$v_i \\in V$}, _latex{$i = 1, 2, ..., n - 1$} (Oscillation Tours).\n"), 1);
		step.setExercise(new AlgorithmExercise<String>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1", langID, "Specify an Oscillation Tour."), 1.0f) {
			
			@Override
			protected String[] requestSolution() {
				final SolutionEntry<JTextField> entry = new SolutionEntry<JTextField>("r=", new JTextField());
				
				if(!SolveExercisePane.showDialog(SavingsAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entry }, SavingsAlgorithmPlugin.this.langFile, SavingsAlgorithmPlugin.this.langID, LanguageFile.getLabel(SavingsAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", SavingsAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				
				final Walk<WeightedVertex> w = GraphUtils.toWalk(entry.getComponent().getText(), SavingsAlgorithmPlugin.this.graphView.getGraph());
				
				if(w == null) {
					SavingsAlgorithmPlugin.this.host.showMessage(SavingsAlgorithmPlugin.this, LanguageFile.getLabel(SavingsAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT", SavingsAlgorithmPlugin.this.langID, "Your input is incorrect!\nPlease enter the cycle in the specified form and only use vertex captions that are existing."), LanguageFile.getLabel(SavingsAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT_TITLE", SavingsAlgorithmPlugin.this.langID, "Invalid input"), MessageIcon.INFO);
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
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, String[] solutions) {
				state.addWalk("r", GraphUtils.toWalk(solutions[0], SavingsAlgorithmPlugin.this.graphView.getGraph()).cast());
			}
			
			@Override
			protected boolean examine(String[] results, AlgorithmState state) {
				final Walk<WeightedVertex> r = GraphUtils.toWalk(results[0], SavingsAlgorithmPlugin.this.graphView.getGraph());
				final int v_s = SavingsAlgorithmPlugin.this.rte.getStartVertex().getID();
				final boolean[] visited = new boolean[SavingsAlgorithmPlugin.this.graphView.getGraph().getOrder()];
				boolean startVertex = true;
				WeightedVertex v;
				
				// the Oscillation Tour has to be a cycle
				if(!r.isClosed())
					return false;
				
				// check whether the specified cycle is a valid Oscillation Tour
				for(int i = 0; i <= r.length(); i++) {
					v = r.get(i);
					
					if(startVertex && v.getID() != v_s)
						return false;
					else if(!startVertex && v.getID() == v_s)
						return false;
					else if(!startVertex && visited[v.getIndex()])
						return false;
					
					visited[v.getIndex()] = true;
					startVertex = !startVertex;
				}
				
				// if their is a vertex that was not visited then it is no valid Oscillation Tour
				for(boolean b : visited)
					if(!b)
						return false;
				
				return true;
			}
			
		});
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_INIT", langID, "Calculate the savings for each pair of vertices _latex{$v_i,v_j \\in V \\setminus \\{v_s\\}$}:  _latex{$sav(v_i,v_j) := c(v_s,v_i) + c(v_s,v_j) - c(v_i,v_j)$}.\n"), 2);
		step.setExercise(new AlgorithmExercise<List<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Calculate the savings (<i>use the buttons in the header bar of the savings view to add or remove items</i>)."), 2.0f, savingsView) {
			
			private SavingsViewExerciseExt ext;
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				// create the extension
				ext = new SavingsViewExerciseExt(SavingsAlgorithmPlugin.this.savingsView, SavingsAlgorithmPlugin.this.addIconRes, SavingsAlgorithmPlugin.this.removeIconRes, SavingsAlgorithmPlugin.this.langFile, SavingsAlgorithmPlugin.this.langID);
				// add the header bar extension so that the user can add or remove items
				ext.apply();
				
				// enable the edit mode for all columns
				for(int i = 0; i < SavingsAlgorithmPlugin.this.savingsView.getColumnCount(); i++)
					SavingsAlgorithmPlugin.this.savingsView.getColumn(i).setEditable(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				// remove the extension
				ext.remove();
				ext = null;
				
				// deactivate the edit mode
				for(int i = 0; i < SavingsAlgorithmPlugin.this.savingsView.getColumnCount(); i++)
					SavingsAlgorithmPlugin.this.savingsView.getColumn(i).setEditable(true);
				
				// if the user gives up then remove the added items of the user because the solution cannot be used
				if(omitted)
					SavingsAlgorithmPlugin.this.savingsView.removeAllItems();
				else {
					// otherwise turn off the edit mode of the items
					for(int i = 0; i < SavingsAlgorithmPlugin.this.savingsView.getItemCount(); i++)
						SavingsAlgorithmPlugin.this.savingsView.getItem(i).setEditable(false);
				}
			}
			
			@Override
			protected List<?>[] requestSolution() {
				final Graph<WeightedVertex, Edge> graph = SavingsAlgorithmPlugin.this.graphView.getGraph();
				final List<VertexPair> savings = new ArrayList<VertexPair>();
				ExecutionTableItem item;
				WeightedVertex v_i;
				WeightedVertex v_j;
				Number sav;
				VertexPair vp;
				
				for(int i = 0; i < SavingsAlgorithmPlugin.this.savingsView.getItemCount(); i++) {
					item = SavingsAlgorithmPlugin.this.savingsView.getItem(i);
					
					v_i = graph.getVertexByCaption(item.getCellObject(0).toString());
					v_j = graph.getVertexByCaption(item.getCellObject(1).toString());
					sav = (Number)item.getCellObject(2);
					
					vp = new VertexPair((v_i != null) ? v_i.getID() : -1, (v_j != null) ? v_j.getID() : -1, (sav != null) ? sav.floatValue() : 0.0f);
					savings.add(vp);
					
					item.setUserData(vp);
				}
				
				return new List<?>[] { savings };
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, List<?>[] solutions) {
				@SuppressWarnings("unchecked")
				final List<VertexPair> savings = (List<VertexPair>)solutions[0];
				state.addList("savings", savings);
			}
			
			@Override
			protected String getResultAsString(List<?> result, int index) {
				@SuppressWarnings("unchecked")
				final List<VertexPair> list = (List<VertexPair>)result;
				final Graph<WeightedVertex, Edge> graph = SavingsAlgorithmPlugin.this.graphView.getGraph();
				
				if(list == null)
					return super.getResultAsString(result, index);
				else {
					final StringBuilder s = new StringBuilder();
					boolean delimiter = false;
					
					s.append("[");
					for(VertexPair vp : list) {
						if(delimiter)
							s.append(", ");
						
						s.append("(" + graph.getVertexByID(vp.v_i).getCaption() + ", " + graph.getVertexByID(vp.v_j).getCaption() + ") " + MathUtils.formatFloat(vp.savings));
						delimiter = true;
					}
					s.append("]");
					
					return s.toString();
				}
			}
			
			@Override
			protected boolean examine(List<?>[] results, AlgorithmState state) {
				@SuppressWarnings("unchecked")
				final List<VertexPair> result = (List<VertexPair>)results[0];
				final Graph<WeightedVertex, Edge> graph = SavingsAlgorithmPlugin.this.graphView.getGraph();
				final WeightedVertex v_s = SavingsAlgorithmPlugin.this.rte.getStartVertex();
				final List<VertexPair> savings = new ArrayList<VertexPair>();
				WeightedVertex v_i;
				WeightedVertex v_j;
				Edge eV_sV_i;
				Edge eV_sV_j;
				Edge eV_iV_j;
				
				for(int i = 0; i < graph.getOrder(); i++) {
					for(int j = i; j < graph.getOrder(); j++) {
						if(i == j || i == v_s.getIndex() || j == v_s.getIndex())
							continue;
						v_i = graph.getVertex(i);
						v_j = graph.getVertex(j);
						eV_sV_i = graph.getEdge(v_s, v_i);
						eV_sV_j = graph.getEdge(v_s, v_j);
						eV_iV_j = graph.getEdge(v_i, v_j);
						
						savings.add(new VertexPair(v_i.getID(), v_j.getID(), eV_sV_i.getWeight() + eV_sV_j.getWeight() - eV_iV_j.getWeight()));
					}
				}
				
				return result.size() == savings.size() && result.containsAll(savings);
			}
		});
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_INIT", langID, "List all pairs of vertices with positive savings value in non-increasing order according to their savings value.\n\n"), 3);
		step.setExercise(new AlgorithmExercise<List<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "List all pairs of vertices with positive savings value and sort them (<i>select the items in the savings view and use the button in the header bar of the list view to import the selected items</i>)."), 2.0f, new View[] { listView, savingsView }) {
			
			private ListViewExerciseExt ext;
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				// create the extension
				ext = new ListViewExerciseExt(SavingsAlgorithmPlugin.this.listView, SavingsAlgorithmPlugin.this.savingsView, SavingsAlgorithmPlugin.this.importIconRes, SavingsAlgorithmPlugin.this.langFile, SavingsAlgorithmPlugin.this.langID);
				// add the header bar extension so that the user can add or remove items
				ext.apply();
				
				// make the table sortable by user
				SavingsAlgorithmPlugin.this.listView.setSortable(true);
				// enable multi select in the savings view
				SavingsAlgorithmPlugin.this.savingsView.setSelectionType(lavesdk.gui.widgets.enums.SelectionType.ROWS);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				// remove the extension
				ext.remove();
				ext = null;
				
				// deactive the sort mode
				SavingsAlgorithmPlugin.this.listView.setSortable(false);
				// disable multi select in the savings view
				SavingsAlgorithmPlugin.this.savingsView.setSelectionType(lavesdk.gui.widgets.enums.SelectionType.NONE);
				
				// deactivate the edit mode
				for(int i = 0; i < SavingsAlgorithmPlugin.this.listView.getColumnCount(); i++)
					SavingsAlgorithmPlugin.this.listView.getColumn(i).setEditable(true);
				
				// if the user gives up then remove the added items of the user because the solution cannot be used
				if(omitted)
					SavingsAlgorithmPlugin.this.listView.removeAllItems();
			}
			
			@Override
			protected List<?>[] requestSolution() {
				final List<VertexPair> list = new ArrayList<VertexPair>();
				
				for(int i = 0; i < SavingsAlgorithmPlugin.this.listView.getItemCount(); i++)
					list.add((VertexPair)SavingsAlgorithmPlugin.this.listView.getItem(i).getUserData());
				
				return new List<?>[] { list };
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, List<?>[] solutions) {
				@SuppressWarnings("unchecked")
				final List<VertexPair> list = (List<VertexPair>)solutions[0];
				state.addList("list", list);
			}
			
			@Override
			protected String getResultAsString(List<?> result, int index) {
				@SuppressWarnings("unchecked")
				final List<VertexPair> list = (List<VertexPair>)result;
				final Graph<WeightedVertex, Edge> graph = SavingsAlgorithmPlugin.this.graphView.getGraph();
				
				if(list == null)
					return super.getResultAsString(result, index);
				else {
					final StringBuilder s = new StringBuilder();
					boolean delimiter = false;
					
					s.append("[");
					for(VertexPair vp : list) {
						if(delimiter)
							s.append(", ");
						
						s.append("(" + graph.getVertexByID(vp.v_i).getCaption() + ", " + graph.getVertexByID(vp.v_j).getCaption() + ") " + MathUtils.formatFloat(vp.savings));
						delimiter = true;
					}
					s.append("]");
					
					return s.toString();
				}
			}
			
			@Override
			protected boolean examine(List<?>[] results, AlgorithmState state) {
				final List<VertexPair> savings = state.getList("savings");
				@SuppressWarnings("unchecked")
				final List<VertexPair> list = (List<VertexPair>)results[0];
				VertexPair lastVP = null;
				VertexPair currVP;
				int posSavValuesCount = 0;
				
				for(VertexPair vp : savings)
					if(vp.savings > 0.0f)
						posSavValuesCount++;
				
				if(list.size() != posSavValuesCount)
					return false;
				
				// check whether the user has sorted correct
				for(int i = 0; i < list.size(); i++) {
					currVP = list.get(i);
					
					if(lastVP != null && currVP.savings > lastVP.savings)
						return false;
					
					lastVP = currVP;
				}
				
				return true;
			}
		});

		// 2. iteration
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_IT", langID, "For each vertex pair _latex{$v_i,v_j$} from the list:"), 4);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "Select the current vertex pair in the graph."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				SavingsAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				SavingsAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				SavingsAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				SavingsAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				SavingsAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(SavingsAlgorithmPlugin.this.graphView.getSelectedVertexCount() != 2)
					return null;
				else
					return new Integer[] { SavingsAlgorithmPlugin.this.graphView.getSelectedVertex(0).getVertex().getID(), SavingsAlgorithmPlugin.this.graphView.getSelectedVertex(1).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return SavingsAlgorithmPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				final VertexPair vp = state.getObject("currVertexPair");
				return (vp.v_i == results[0] && vp.v_j == results[1]) || (vp.v_i == results[1] && vp.v_j == results[0]);
			}
		});
		
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_IT", langID, "If the edges _latex{$(v_s,v_i)$} (_latex{$(v_i,v_s)$}, resp.) and _latex{$(v_s,v_j)$} (_latex{$(v_j,v_s)$}, resp.) are still contained in _latex{$r$} and if _latex{$v_i$} and _latex{$v_j$} are elements in different tours, merge the corresponding disjoint cycles within _latex{$r$} in such a way to a new tour _latex{$r'$} that both mentioned edges are being substituted by _latex{$(v_i,v_j)$}. "), 5, 4);
		step.setAnnotation(new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_ANNOTATION", langID, "<b>Cycle creation</b><br>Cases that need to be checked:<br><table border=\"0\"><tr><td valign=\"top\"></td><td valign=\"top\"></td><td valign=\"top\"><b>Resolve to</b></td></tr><tr><td valign=\"top\"><b>1. (v<sub>i</sub>, v<sub>s</sub>) and (v<sub>s</sub>, v<sub>j</sub>)</b></td><td valign=\"top\"><img src=\"case1\"></td><td valign=\"top\">Merge the pitch cycle of v<sub>i</sub> with the pitch cycle of v<sub>j</sub><br><b>Example</b>: v<sub>i</sub> = 1, v<sub>j</sub> = 3, (s, 2, 1, s) and (s, 3, 4, s)<br>Result: (s, 2, 1, 3, 4, s)</td></tr><tr><td valign=\"top\"><b>2. (v<sub>s</sub>, v<sub>i</sub>) and (v<sub>j</sub>, v<sub>s</sub>)</b></td><td valign=\"top\"><img src=\"case2\"></td><td valign=\"top\">Merge the pitch cycle of v<sub>j</sub> with the pitch cycle of v<sub>i</sub><br><b>Example</b>: v<sub>i</sub> = 1, v<sub>j</sub> = 3, (s, 1, 2, s) and (s, 4, 3, s)<br>Result: (s, 4, 3, 1, 2, s)</td></tr><tr><td valign=\"top\"><b>3. (v<sub>i</sub>, v<sub>s</sub>) and (v<sub>j</sub>, v<sub>s</sub>)</b></td><td valign=\"top\"><img src=\"case3\"></td><td valign=\"top\">Merge the pitch cycle of v<sub>i</sub> with the reversed pitch cycle of v<sub>j</sub><br><b>Example</b>: v<sub>i</sub> = 1, v<sub>j</sub> = 3, (s, 2, 1, s) and (s, 4, 3, s)<br>Reverse: (s, 4, 3, s) to (s, 3, 4, s)<br>Result: (s, 2, 1, 3, 4, s)</td></tr><tr><td valign=\"top\"><b>4. (v<sub>s</sub>, v<sub>i</sub>) and (v<sub>s</sub>, v<sub>j</sub>)</b></td><td valign=\"top\"><img src=\"case4\"></td><td valign=\"top\">Reverse the pitch cycle of v<sub>i</sub> and merge it with the pitch cycle of <sub>j</sub><br><b>Example</b>: v<sub>i</sub> = 1, v<sub>j</sub> = 3, (s, 1, 2, s) and (s, 3, 4, s)<br>Reverse: (s, 1, 2, s) to (s, 2, 1, s)<br>Result: (s, 2, 1, 3, 4, s)</td></tr></table>"), annotationImgList));
		step.setExercise(new AlgorithmExercise<Walk<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP5", langID, "Is it possible to create a new tour <i>r'</i> and if so, what is it?"), 2.0f) {

			private final String labelYes = LanguageFile.getLabel(langFile, "EXERCISE_STEP5_6_YES", langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(langFile, "EXERCISE_STEP5_6_NO", langID, "No");
			
			@Override
			protected Walk<?>[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelNo);
				final JRadioButton rdobtn2 = new JRadioButton(labelYes);
				final JTextField txtCycle = new JTextField();
				final ActionListener al = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if(e.getSource() == rdobtn1)
							txtCycle.setEnabled(false);
						else if(e.getSource() == rdobtn2)
							txtCycle.setEnabled(true);
					}
				};
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				txtCycle.setEnabled(false);
				
				rdobtn1.addActionListener(al);
				rdobtn2.addActionListener(al);
				
				final SolutionEntry<JRadioButton> entryNotPossible = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryPossible = new SolutionEntry<JRadioButton>("", rdobtn2);
				final SolutionEntry<JTextField> entryCycle = new SolutionEntry<JTextField>("r'=", txtCycle);
				
				if(!SolveExercisePane.showDialog(SavingsAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryNotPossible,  entryPossible, entryCycle }, SavingsAlgorithmPlugin.this.langFile, SavingsAlgorithmPlugin.this.langID, LanguageFile.getLabel(SavingsAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", SavingsAlgorithmPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				Walk<WeightedVertex> w = null;
				
				if(rdobtn2.isSelected()) {
					w = GraphUtils.toWalk(entryCycle.getComponent().getText(), SavingsAlgorithmPlugin.this.graphView.getGraph());
					
					if(w == null) {
						SavingsAlgorithmPlugin.this.host.showMessage(SavingsAlgorithmPlugin.this, LanguageFile.getLabel(SavingsAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT", SavingsAlgorithmPlugin.this.langID, "Your input is incorrect!\nPlease enter the cycle in the specified form and only use vertex captions that are existing."), LanguageFile.getLabel(SavingsAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT_TITLE", SavingsAlgorithmPlugin.this.langID, "Invalid input"), MessageIcon.INFO);
						return null;
					}
				}
				
				return new Walk<?>[] { w };
			}
			
			@Override
			protected String getResultAsString(Walk<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return "r'=" + result.toString();
			}
			
			@Override
			protected boolean examine(Walk<?>[] results, AlgorithmState state) {
				final WalkByID<WeightedVertex> r_ApoTmp = state.getWalk("r_Apo", SavingsAlgorithmPlugin.this.graphView.getGraph());
				final Walk<WeightedVertex> r_Apo = (r_ApoTmp != null) ? r_ApoTmp.cast() : null;
				return (r_Apo == null && results[0] == null) || (results[0] != null && r_Apo.equals(results[0]));
			}
		});

		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_IT", langID, "If the resulting tour _latex{$r'$} fulfills the capacity constraints, set _latex{$r := r'$}."), 6, 4);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP6", langID, "Does <i>r'</i> fulfill the delivery constraint?"), 1.0f) {
			
			private final String labelYes = LanguageFile.getLabel(langFile, "EXERCISE_STEP5_6_YES", langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(langFile, "EXERCISE_STEP5_6_NO", langID, "No");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelYes);
				final JRadioButton rdobtn2 = new JRadioButton(labelNo);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(SavingsAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, SavingsAlgorithmPlugin.this.langFile, SavingsAlgorithmPlugin.this.langID))
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
				return (results[0] != null && results[0].equals(SavingsAlgorithmPlugin.this.rte.checkDeliveryConstraint()));
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
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_STARTVERTEX", langID, "The starting vertex v<sub>s</sub>"), LegendItem.createCircleIcon(colorStartVertex, Color.black, lineWidthStartVertex)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CYCLER_DIRECTED", langID, "The cycle r, whereby the directed edges show the running direction of the walk"), LegendItem.createLineIcon(colorCycleR, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CYCLER_UNDIRECTED", langID, "The undirected edges of the cycle r"), LegendItem.createLineIcon(colorUsedEdges, 1)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_UNUSEDEDGES", langID, "The currently unused edges of the graph"), LegendItem.createLineIcon(colorUnusedEdges, 1)));
		legendView.add(new LegendItem("item5", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURRVERTEXPAIR", langID, "The current vertex pair v<sub>i</sub>, v<sub>j</sub>"), LegendItem.createCircleIcon(colorVertexPair, Color.black, 1)));
		legendView.add(new LegendItem("item6", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_EDGESTOREMOVE", langID, "The edges of the cycle r that are substituted by (v<sub>i</sub>, v<sub>j</sub>)"), LegendItem.createLineIcon(colorEdgesToRemove, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item7", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_EDGETOADD", langID, "The edge (v<sub>i</sub>, v<sub>j</sub>) that substitutes the edges (v<sub>s</sub>, v<sub>i</sub>) ((v<sub>i</sub>, v<sub>s</sub>), resp.) and (v<sub>s</sub>, v<sub>j</sub>) (v<sub>j</sub>, v<sub>s</sub>), resp.)"), LegendItem.createLineIcon(colorEdgeToAdd, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item8", savingsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_SAVINGS_POSITIVESAVINGSVALUE", langID, "A positive savings value"), LegendItem.createRectangleIcon(colorPositiveSavingsValue, colorPositiveSavingsValue, 0)));
		legendView.add(new LegendItem("item9", savingsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_SAVINGS_NONPOSITIVESAVINGSVALUE", langID, "A non-positive savings value"), LegendItem.createRectangleIcon(colorNonPositiveSavingsValue, colorNonPositiveSavingsValue, 0)));
		legendView.add(new LegendItem("item10", listView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_LIST_CURRVERTEXPAIR", langID, "The current vertex pair v<sub>i</sub>, v<sub>j</sub>"), LegendItem.createRectangleIcon(colorVertexPair, colorVertexPair, 0)));
		legendView.add(new LegendItem("item11", cycleRView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_CYCLE_MODIFICATION", langID, "The cycle r becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
	}
	
	/**
	 * Indicates whether the user created graph contains negative weights (edge or vertices).
	 * 
	 * @return <code>true</code> if their are negative weights otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean containsNegativeWeights() {
		final Graph<WeightedVertex, Edge> graph = graphView.getGraph();
		
		for(int i = 0; i < graph.getOrder(); i++)
			if(graph.getVertex(i).getWeight() < 0.0f)
				return true;
		for(int i = 0; i < graph.getSize(); i++)
			if(graph.getEdge(i).getWeight() < 0.0f)
				return true;
		
		return false;
	}
	
	/**
	 * The runtime environment of the Savings algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class SavingsRTE extends AlgorithmRTE {

		/** the starting vertex */
		private WeightedVertex v_s;
		/** the delivery capacity */
		private float b_max;
		/** the cycle r */
		private Walk<WeightedVertex> r;
		/** the list of savings values */
		private List<VertexPair> savings;
		/** the list of the sorted positive savings values */
		private List<VertexPair> list;
		/** the current vertex pair v_i,v_j */
		private VertexPair currVertexPair;
		/** the id of the current first edge that has to be removed from r */
		private int edge1_toRemove;
		/** the id of the current second edge that has to be removed from r */
		private int edge2_toRemove;
		/** the id of the current surrogate edge */
		private int edge_toAdd;
		/** the cycle r' */
		private Walk<WeightedVertex> r_Apo;
		/** the user's choice of r */
		private Walk<WeightedVertex> userChoiceR;
		/** the user's choice of the savings */
		private List<VertexPair> userChoiceSavings;
		/** the user's choice of the sorted list */
		private List<VertexPair> userChoiceList;
		
		/**
		 * Creates the runtime environment.
		 * 
		 * @since 1.0
		 */
		public SavingsRTE() {
			super(SavingsAlgorithmPlugin.this, SavingsAlgorithmPlugin.this.algoText);
			
			v_s = null;
			b_max = 0.0f;
			userChoiceR = null;
			userChoiceSavings = null;
			userChoiceList = null;
		}
		
		/**
		 * Gets the starting vertex of the algorithm.
		 * 
		 * @return the starting vertex
		 * @since 1.0
		 */
		public WeightedVertex getStartVertex() {
			return v_s;
		}
		
		/**
		 * Sets the starting vertex of the algorithm and visualizes the vertex.
		 * 
		 * @param v the vertex
		 * @since 1.0
		 */
		public void setStartVertex(final WeightedVertex v) {
			v_s = v;
			visualizeVertices();
		}
		
		/**
		 * Sets the delivery capacity b<sub>max</sub> of the algorithm.
		 * 
		 * @param b_max the delivery capacity
		 * @since 1.0
		 */
		public void setDeliveryCapacity(final float b_max) {
			this.b_max = b_max;
		}
		
		/**
		 * Checks the delivery constraint using the current cycle {@link #r_Apo}.
		 * 
		 * @return <code>true</code> if the constraint is fulfilled otherwise <code>false</code>
		 * @since 1.0
		 */
		public boolean checkDeliveryConstraint() {
			if(r_Apo == null)
				return false;
			
			WeightedVertex v;
			float currCapacity = 0.0f;
			
			// start with the second vertex because the first one is v_s
			for(int i = 1; i <= r_Apo.length(); i++) {
				v = r_Apo.get(i);
				
				if(v.getID() == v_s.getID()) {
					if(currCapacity > b_max)
						return false;
					currCapacity = 0.0f;
				}
				else
					currCapacity += v.getWeight();
			}
			
			return true;
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<WeightedVertex, Edge> graph = SavingsAlgorithmPlugin.this.graphView.getGraph();
			int nextStep = 0;
			GraphScene<WeightedVertex, Edge> scene;
			VertexPair vp;
			GraphView<WeightedVertex, Edge>.VisualEdge ve;
			ExecutionTableItem item;
			GraphView<WeightedVertex, Edge>.VisualEdge ve1_toRemove = null;
			GraphView<WeightedVertex, Edge>.VisualEdge ve2_toRemove = null;
			GraphView<WeightedVertex, Edge>.VisualEdge ve_toAdd = null;
			
			switch(stepID) {
				case 1:
					// set r := (v_s,v_1,v_s,v_2,v_s,...)
					
					// visualize the starting vertex
					visualizeVertices();
					
					sleep(250);
					
					// if the related exercise is solved correct then adopt the Oscillation Tour the user has entered
					if(userChoiceR != null)
						r = userChoiceR;
					else {
						r = new Walk<WeightedVertex>(graph);
						r.add(v_s);
						
						for(int i = 0; i < v_s.getOutgoingEdgeCount(); i++) {
							r.add(graph.getVertexByID(v_s.getOutgoingEdge(i).getSuccessor(v_s).getID()));
							r.add(v_s);
						}
					}
					
					// clear the user's choice
					userChoiceR = null;
					
					// start the scene so that we can roll it back
					scene = new GraphScene<WeightedVertex, Edge>(SavingsAlgorithmPlugin.this.graphView);
					scene.begin();
					
					// colorize the used and unused edges UNDIRECTED edges
					for(int i = 0; i < SavingsAlgorithmPlugin.this.graphView.getVisualEdgeCount(); i++) {
						ve = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(i);
						if(r.contains(ve.getEdge()))
							ve.setColor(SavingsAlgorithmPlugin.this.colorUsedEdges);
						else
							ve.setColor(SavingsAlgorithmPlugin.this.colorUnusedEdges);
					}
					// add directed edges to the graph that show the current cycle
					for(int i = 1; i <= r.length(); i++) {
						// create a new directed graph that shows the direction of the cycle r
						ve = SavingsAlgorithmPlugin.this.graphView.addEdge(r.get(i - 1), r.get(i), true);
						ve.setColor(SavingsAlgorithmPlugin.this.colorCycleR);
						ve.setLineWidth(SavingsAlgorithmPlugin.this.lineWidthCycleR);
					}
					SavingsAlgorithmPlugin.this.graphView.repaint();
					
					// the scene is finished so add it to the current state (this has to be done because a state of a step is stored
					// before the step is executed but we need to add the scene during the execution)
					scene.end(false);
					asa.addAttachment("scene", scene);
					
					// visualize the cycle
					sleep(250);
					SavingsAlgorithmPlugin.this.cycleRView.setBackground(SavingsAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeCycleRAsText();
					sleep(250);
					SavingsAlgorithmPlugin.this.cycleRView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 2;
					break;
				case 2:
					// calculate the savings for each v_i,v_j in V\{v_s}: sav(v_i,v_j) = c(v_s,v_i) + c(v_s,v_j) - c(v_i,v_j)
					
					WeightedVertex v_i;
					WeightedVertex v_j;
					GraphView<WeightedVertex, Edge>.VisualVertex vv_i;
					GraphView<WeightedVertex, Edge>.VisualVertex vv_j;
					Edge eV_sV_i;
					Edge eV_sV_j;
					Edge eV_iV_j;
					
					sleep(250);
					
					savings.clear();
					
					if(userChoiceSavings != null)
						savings = userChoiceSavings;
					else {
						for(int i = 0; i < graph.getOrder(); i++) {
							for(int j = i; j < graph.getOrder(); j++) {
								if(i == j || i == v_s.getIndex() || j == v_s.getIndex())
									continue;
								
								v_i = graph.getVertex(i);
								v_j = graph.getVertex(j);
								eV_sV_i = graph.getEdge(v_s, v_i);
								eV_sV_j = graph.getEdge(v_s, v_j);
								eV_iV_j = graph.getEdge(v_i, v_j);
								
								// that case should never happen but check it to avoid errors
								if(eV_sV_i == null || eV_sV_j == null || eV_iV_j == null)
									return -1;
								
								// highlight v_i and v_j
								vv_i = SavingsAlgorithmPlugin.this.graphView.getVisualVertex(v_i);
								vv_j = SavingsAlgorithmPlugin.this.graphView.getVisualVertex(v_j);
								vv_i.setBackground(SavingsAlgorithmPlugin.this.colorVertexPair);
								vv_j.setBackground(SavingsAlgorithmPlugin.this.colorVertexPair);
								SavingsAlgorithmPlugin.this.graphView.repaint();
								
								sleep(500);
								
								vp = new VertexPair(v_i.getID(), v_j.getID(), eV_sV_i.getWeight() + eV_sV_j.getWeight() - eV_iV_j.getWeight());
								savings.add(vp);
								item = new ExecutionTableItem(new Object[] { v_i.getCaption(), v_j.getCaption(), vp.savings }, vp.id);
								item.setUserData(vp);
								SavingsAlgorithmPlugin.this.savingsView.add(item);
								
								// remove the highlight
								vv_i.setBackground(GraphView.DEF_VERTEXBACKGROUND);
								vv_j.setBackground(GraphView.DEF_VERTEXBACKGROUND);
								SavingsAlgorithmPlugin.this.graphView.repaint();
								
								sleep(500);
							}
						}
					}
					
					// clear the user's choice
					userChoiceSavings = null;
					
					nextStep = 3;
					break;
				case 3:
					// list all vertex pairs with positive savings value and sort them non-increasingly
					
					// if the related exercise succeeded then adopt the user sorted list
					if(userChoiceList != null)
						list = userChoiceList;
					else {
						sleep(250);
						
						for(int i = 0; i < savings.size(); i++) {
							vp = savings.get(i);
							item = SavingsAlgorithmPlugin.this.savingsView.getItem(i);
							
							// highlight the items according to its savings value
							if(vp.savings > 0.0f) {
								item.setBackground(SavingsAlgorithmPlugin.this.colorPositiveSavingsValue);
								sleep(250);
								// add the positive savings value to the list
								final ExecutionTableItem posSavValueItem = new ExecutionTableItem(new Object[] { item.getCellObject(0), item.getCellObject(1), item.getCellObject(2) }, vp.id);
								posSavValueItem.setUserData(vp);
								SavingsAlgorithmPlugin.this.listView.add(posSavValueItem);
							}
							else
								item.setBackground(SavingsAlgorithmPlugin.this.colorNonPositiveSavingsValue);
							
							sleep(250);
							
							// remove the highlight
							item.setBackground(Color.white);
						}
						
						// sort the list
						sleep(750);
						SavingsAlgorithmPlugin.this.listView.sortItems(2, SortOrder.DESCENDING);
						sleep(250);
						
						// adopt the sort order
						list.clear();
						for(int i = 0; i < SavingsAlgorithmPlugin.this.listView.getItemCount(); i++) {
							vp = null;
							for(int j = 0; j < savings.size(); j++) {
								if(savings.get(j).id == SavingsAlgorithmPlugin.this.listView.getItem(i).getID()) {
									vp = savings.get(j);
									break;
								}
							}
							
							if(vp == null)
								return -1;
							
							list.add(vp);
						}
					}
					
					// clear the user's choice
					userChoiceList = null;
					
					nextStep = 4;
					break;
				case 4:
					// for each vertex pair of the list:
					
					currVertexPair = forEachGetNext(list);
					
					sleep(500);
					
					// all pairs were processed? then the algorithm ends
					if(currVertexPair == null)
						nextStep = -1;
					else {
						// visualize the item in the table
						item = SavingsAlgorithmPlugin.this.listView.getItemByID(currVertexPair.id);
						item.setBackground(SavingsAlgorithmPlugin.this.colorVertexPair);

						sleep(500);
						
						// visualize the current pair
						visualizeVertices();
						
						sleep(500);
						
						// remove highlight
						item.setBackground(Color.white);
						
						nextStep = 5;
					}
					break;
				case 5:
					// if the edges (v_s,v_i) (or (v_i,v_s)) and (v_s,v_j) (or (v_j,v_s)) are still contained in r and if v_i and v_j are
					// in different cycles, merge the disjoint cycles within r in such a way to a new tour r' that both mentioned edges
					// are being substituted by (v_i,v_j)
					
					final List<Walk<WeightedVertex>> pitchCycles = getPitchCycles(graph);
					final int[] pitchCycleIndices = getPitchCycleIndices(graph, currVertexPair.v_i, currVertexPair.v_j, pitchCycles);
					final boolean diffPitchCycles = (pitchCycleIndices[0] >= 0 && pitchCycleIndices[1] >= 0 && pitchCycleIndices[0] != pitchCycleIndices[1]);
					final List<WeightedVertex> r_ApoTmp = new ArrayList<WeightedVertex>();
					final boolean containsV_iV_s = containsEdge(currVertexPair.v_i, v_s.getID());
					final boolean containsV_sV_i = containsEdge(v_s.getID(), currVertexPair.v_i);
					final boolean containsV_jV_s = containsEdge(currVertexPair.v_j, v_s.getID());
					final boolean containsV_sV_j = containsEdge(v_s.getID(), currVertexPair.v_j);
					Walk<WeightedVertex> currCycle;
					
					if(diffPitchCycles && containsV_iV_s && containsV_sV_j) {
						// 1. if their are the directed edges (v_i,v_s) and (v_s,v_j)?
						//    then connect the two pitch cycles
						
						for(int i = 0; i < pitchCycles.size(); i++) {
							currCycle = pitchCycles.get(i);
							
							// if we have found the pitch cycle of v_i then connect the two pitch cycles
							if(i == pitchCycleIndices[0]) {
								connectCycles(r_ApoTmp, currCycle.asList(), 2);
								connectCycles(r_ApoTmp, pitchCycles.get(pitchCycleIndices[1]).asList(), 1);
							}
							else if(i != pitchCycleIndices[1]) {
								// the pitch cycle of v_j can be skipped but all other cycle have to be added
								connectCycles(r_ApoTmp, currCycle.asList());
							}
						}
						
						// store the edges that will be removed from r
						ve1_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, currVertexPair.v_i, v_s.getID()));
						ve2_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, v_s.getID(), currVertexPair.v_j));
					}
					else if(diffPitchCycles && containsV_sV_i && containsV_jV_s) {
						// 2. if their are the directed edges (v_s,v_i) and (v_j,v_s)?
						//    then connect the two pitch cycles in reverse order so that the cycle of v_i is attached to the cycle of v_j
						
						for(int i = 0; i < pitchCycles.size(); i++) {
							currCycle = pitchCycles.get(i);
							
							// if we have found the pitch cycle of v_i then connect the two pitch cycles
							if(i == pitchCycleIndices[0]) {
								connectCycles(r_ApoTmp, pitchCycles.get(pitchCycleIndices[1]).asList(), 2);
								connectCycles(r_ApoTmp, currCycle.asList(), 1);
							}
							else if(i != pitchCycleIndices[1]) {
								// the pitch cycle of v_j can be skipped but all other cycle have to be added
								connectCycles(r_ApoTmp, currCycle.asList());
							}
						}
						
						// store the edges that will be removed from r
						ve1_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, v_s.getID(), currVertexPair.v_i));
						ve2_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, currVertexPair.v_j, v_s.getID()));
					}
					else if(diffPitchCycles && containsV_iV_s && containsV_jV_s) {
						// 3. if their are directed edges (v_i,v_s) and (v_j,v_s)?
						//    then reverse the pitch cycle of v_j and attach the reversed cycle of v_j to the cycle of v_i
						
						for(int i = 0; i < pitchCycles.size(); i++) {
							currCycle = pitchCycles.get(i);
							
							// if we have found the pitch cycle of v_i then reverse pitch cycle of v_j and attach it to pitch cycle of v_i
							if(i == pitchCycleIndices[0]) {
								connectCycles(r_ApoTmp, currCycle.asList(), 2);
								connectCycles(r_ApoTmp, reverseCycle(pitchCycles.get(pitchCycleIndices[1]).asList()), 1);
							}
							else if(i != pitchCycleIndices[1]) {
								// the pitch cycle of v_j can be skipped but all other cycle have to be added
								connectCycles(r_ApoTmp, currCycle.asList());
							}
						}
						
						// store the edges that will be removed from r
						ve1_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, currVertexPair.v_i, v_s.getID()));
						ve2_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, currVertexPair.v_j, v_s.getID()));
					}
					else if(diffPitchCycles && containsV_sV_i && containsV_sV_j) {
						// 4. if their are the directed edges (v_s,v_i) and (v_s,v_j)?
						//    then reverse the pitch cycle of v_i and attach the cycle of v_j to the reversed cycle of v_i
						
						for(int i = 0; i < pitchCycles.size(); i++) {
							currCycle = pitchCycles.get(i);
							
							// if we have found the pitch cycle of v_i then reverse pitch cycle of v_j and attach it to pitch cycle of v_i
							if(i == pitchCycleIndices[0]) {
								connectCycles(r_ApoTmp, reverseCycle(currCycle.asList()), 2);
								connectCycles(r_ApoTmp, pitchCycles.get(pitchCycleIndices[1]).asList(), 1);
							}
							else if(i != pitchCycleIndices[1]) {
								// the pitch cycle of v_j can be skipped but all other cycle have to be added
								connectCycles(r_ApoTmp, currCycle.asList());
							}
						}
						
						// store the edges that will be removed from r
						ve1_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, v_s.getID(), currVertexPair.v_i));
						ve2_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(getDirectedEdge(graph, v_s.getID(), currVertexPair.v_j));
					}
					
					if(r_ApoTmp.size() > 0) {
						// create r'
						r_Apo = new Walk<WeightedVertex>(graph, r_ApoTmp);
						
						// show the view of cycle r'
						SavingsAlgorithmPlugin.this.cycleR_ApoView.reset();
						SavingsAlgorithmPlugin.this.cycleR_ApoView.setVisible(true);
						
						// get the edge that will be added to r
						ve_toAdd = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(currVertexPair.v_i, currVertexPair.v_j));
						
						// highlight the edges
						ve1_toRemove.setColor(SavingsAlgorithmPlugin.this.colorEdgesToRemove);
						ve2_toRemove.setColor(SavingsAlgorithmPlugin.this.colorEdgesToRemove);
						ve_toAdd.setColor(SavingsAlgorithmPlugin.this.colorEdgeToAdd);
						ve_toAdd.setLineWidth(SavingsAlgorithmPlugin.this.lineWidthEdgeToAdd);
						SavingsAlgorithmPlugin.this.graphView.repaint();
						
						// visualize r'
						sleep(250);
						visualizeCycleR_ApoAsText();
						
						sleep(750);
						
						// remove the highlight
						ve1_toRemove.setColor(SavingsAlgorithmPlugin.this.colorCycleR);
						ve2_toRemove.setColor(SavingsAlgorithmPlugin.this.colorCycleR);
						ve_toAdd.setColor(SavingsAlgorithmPlugin.this.colorUnusedEdges);
						ve_toAdd.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
						SavingsAlgorithmPlugin.this.graphView.repaint();
						
						edge1_toRemove = ve1_toRemove.getEdge().getID();
						edge2_toRemove = ve2_toRemove.getEdge().getID();
						edge_toAdd = ve_toAdd.getEdge().getID();
						
						nextStep = 6;
					}
					else {
						r_Apo = null;
						edge1_toRemove = 0;
						edge2_toRemove = 0;
						edge_toAdd = 0;
						
						nextStep = 4;
					}
					
					break;
				case 6:
					// if r' fulfills the delivery constraint, set r := r'
					
					// get the visual components
					ve1_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdgeByID(edge1_toRemove);
					ve2_toRemove = SavingsAlgorithmPlugin.this.graphView.getVisualEdgeByID(edge2_toRemove);
					ve_toAdd = SavingsAlgorithmPlugin.this.graphView.getVisualEdgeByID(edge_toAdd);
					edge1_toRemove = 0;
					edge2_toRemove = 0;
					edge_toAdd = 0;
					
					// highlight the edges
					ve1_toRemove.setColor(SavingsAlgorithmPlugin.this.colorEdgesToRemove);
					ve2_toRemove.setColor(SavingsAlgorithmPlugin.this.colorEdgesToRemove);
					ve_toAdd.setColor(SavingsAlgorithmPlugin.this.colorEdgeToAdd);
					ve_toAdd.setLineWidth(SavingsAlgorithmPlugin.this.lineWidthEdgeToAdd);
					SavingsAlgorithmPlugin.this.graphView.repaint();
					
					sleep(750);
					
					// remove the highlight
					ve1_toRemove.setColor(SavingsAlgorithmPlugin.this.colorCycleR);
					ve2_toRemove.setColor(SavingsAlgorithmPlugin.this.colorCycleR);
					ve_toAdd.setColor(SavingsAlgorithmPlugin.this.colorUnusedEdges);
					ve_toAdd.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
					SavingsAlgorithmPlugin.this.graphView.repaint();
					
					if(checkDeliveryConstraint()) {
						// set r := r'
						r = r_Apo.clone();
						
						// visualize the new r by removing edge 1 and 2 and add the surrogate edge
						scene = new GraphScene<WeightedVertex, Edge>(SavingsAlgorithmPlugin.this.graphView);
						scene.begin();
						
						// remove all directed edges and colorize the undirected ones
						for(int i = SavingsAlgorithmPlugin.this.graphView.getVisualEdgeCount() - 1; i >= 0; i--) {
							ve = SavingsAlgorithmPlugin.this.graphView.getVisualEdge(i);
							
							if(ve.getEdge().isDirected())
								SavingsAlgorithmPlugin.this.graphView.removeEdge(ve);
							else if(r.contains(ve.getEdge()))
								ve.setColor(SavingsAlgorithmPlugin.this.colorUsedEdges);
							else
								ve.setColor(SavingsAlgorithmPlugin.this.colorUnusedEdges);
						}
						
						// display the new cycle by creating directed edges
						for(int i = 1; i <= r.length(); i++) {
							ve = SavingsAlgorithmPlugin.this.graphView.addEdge(r.get(i - 1), r.get(i), true);
							ve.setColor(SavingsAlgorithmPlugin.this.colorCycleR);
							ve.setLineWidth(SavingsAlgorithmPlugin.this.lineWidthCycleR);
						}
						
						// the scene is finished so add it to the current state (this has to be done because a state of a step is stored
						// before the step is executed but we need to add the scene during the execution)
						scene.end(false);
						asa.addAttachment("scene", scene);
						
						// repaint the graph
						SavingsAlgorithmPlugin.this.graphView.repaint();
						
						// visualize the cycle
						sleep(250);
						SavingsAlgorithmPlugin.this.cycleRView.setBackground(SavingsAlgorithmPlugin.this.colorModified);
						sleep(250);
						visualizeCycleRAsText();
						sleep(250);
						SavingsAlgorithmPlugin.this.cycleRView.setBackground(Color.white);
						sleep(250);
					}
					
					// hide the view of r'
					SavingsAlgorithmPlugin.this.cycleR_ApoView.setVisible(false);
					
					nextStep = 4;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addWalk("r", (r != null) ? r.cast() : null);
			state.addList("savings", savings);
			state.addList("list", list);
			state.addObject("currVertexPair", currVertexPair);
			state.addInt("edge1_toRemove", edge1_toRemove);
			state.addInt("edge2_toRemove", edge2_toRemove);
			state.addInt("edgeSurrogate", edge_toAdd);
			state.addWalk("r_Apo", (r_Apo != null) ? r_Apo.cast() : null);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			final WalkByID<WeightedVertex> rTmp = state.getWalk("r", SavingsAlgorithmPlugin.this.graphView.getGraph());
			r = (rTmp != null) ? rTmp.cast() : null;
			savings = state.getList("savings");
			list = state.getList("list");
			currVertexPair = state.getObject("currVertexPair");
			edge1_toRemove = state.getInt("edge1_toRemove");
			edge2_toRemove = state.getInt("edge2_toRemove");
			edge_toAdd = state.getInt("edgeSurrogate");
			final WalkByID<WeightedVertex> r_ApoTmp = state.getWalk("r_Apo", SavingsAlgorithmPlugin.this.graphView.getGraph());
			r_Apo = (r_ApoTmp != null) ? r_ApoTmp.cast() : null;
			// restore the graph scene if necessary
			final GraphScene<WeightedVertex, Edge> scene = state.getAttachment("scene");
			if(scene != null)
				scene.reverse();
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			state.addWalk("r", null);
			savings = state.addList("savings", new ArrayList<VertexPair>());
			list = state.addList("list", new ArrayList<VertexPair>());
			currVertexPair = state.addObject("currVertexPair", null);
			state.addInt("edge1_toRemove", 0);
			state.addInt("edge2_toRemove", 0);
			state.addInt("edgeSurrogate", 0);
			state.addWalk("r_Apo", null);
			state.addAttachment("scene", null);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			switch(stepID) {
				case 1:
					visualizeVertices();
					visualizeCycleRAsText();
					break;
				case 2:
					SavingsAlgorithmPlugin.this.savingsView.removeAllItems();
					break;
				case 3:
					SavingsAlgorithmPlugin.this.listView.removeAllItems();
					break;
				case 4:
					SavingsAlgorithmPlugin.this.cycleR_ApoView.setVisible(false);
					visualizeVertices();
					break;
				case 6:
					SavingsAlgorithmPlugin.this.cycleR_ApoView.setVisible(true);
					visualizeCycleRAsText();
					visualizeCycleR_ApoAsText();
					break;
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			switch(stepID) {
				case 1:
					userChoiceR = state.getWalk("r", SavingsAlgorithmPlugin.this.graphView.getGraph()).cast();
					break;
				case 2:
					userChoiceSavings = state.getList("savings");
					break;
				case 3:
					userChoiceList = state.getList("list");
					break;
			}
		}

		@Override
		protected View[] getViews() {
			return new View[] { SavingsAlgorithmPlugin.this.graphView, SavingsAlgorithmPlugin.this.cycleRView, SavingsAlgorithmPlugin.this.listView, SavingsAlgorithmPlugin.this.savingsView };
		}
		
		/**
		 * Visualizes the vertex {@link #v_s} and the vertices v_i and v_j from the {@link #currVertexPair}.
		 * All other vertices are painted in the default style
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			final int v_s = (this.v_s != null) ? this.v_s.getID() : 0;
			final int v_i = (currVertexPair != null) ? currVertexPair.v_i : 0;
			final int v_j = (currVertexPair != null) ? currVertexPair.v_j : 0;
			GraphView<WeightedVertex, Edge>.VisualVertex vv;
			
			for(int i = 0; i < SavingsAlgorithmPlugin.this.graphView.getVisualVertexCount(); i++) {
				vv = SavingsAlgorithmPlugin.this.graphView.getVisualVertex(i);
				
				if(vv.getVertex().getID() == v_s) {
					vv.setBackground(SavingsAlgorithmPlugin.this.colorStartVertex);
					vv.setEdgeWidth(SavingsAlgorithmPlugin.this.lineWidthStartVertex);
				}
				else if(vv.getVertex().getID() == v_i || vv.getVertex().getID() == v_j) {
					vv.setBackground(SavingsAlgorithmPlugin.this.colorVertexPair);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
				else {
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
			}
		}
		
		/**
		 * Visualizes the cycle r in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeCycleRAsText() {
			SavingsAlgorithmPlugin.this.cycleRView.setText((r != null) ? "r=" + r.toString() : "");
		}
		
		/**
		 * Visualizes the cycle r' in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeCycleR_ApoAsText() {
			SavingsAlgorithmPlugin.this.cycleR_ApoView.setText((r_Apo != null) ? "r'=" + r_Apo.toString() : "");
		}
		
		/**
		 * Gets the next element in the list.
		 * 
		 * @param list the list
		 * @return the next element or <code>null</code> if their are no more elements
		 * @since 1.0
		 */
		private VertexPair forEachGetNext(final List<VertexPair> list) {
			VertexPair next = null;
			
			// get next vertex
			if(list.size() > 0) {
				next = list.get(0);
				list.remove(0);
			}
			
			return next;
		}
		
		/**
		 * Indicates whether the cycle r contains the edge between v and u.
		 * 
		 * @param v the id of a vertex v
		 * @param u the id of a vertex u
		 * @return <code>true</code> if the edge is existing otherwise <code>false</code>
		 * @since 1.0
		 */
		private boolean containsEdge(final int v, final int u) {
			if(r == null)
				return false;
			
			for(int i = 0; i < r.length(); i++)
				if(r.get(i).getID() == v && r.get(i + 1).getID() == u)
					return true;
			
			return false;
		}
		
		/**
		 * Gets the directed edge v -> u in the graph.
		 * 
		 * @param graph the graph
		 * @param v the id of the vertex v
		 * @param u the id of the vertex u
		 * @return the directed edge or <code>null</code> if their is no directed edge between the vertices
		 * @since 1.0
		 */
		private Edge getDirectedEdge(final Graph<WeightedVertex, Edge> graph, final int v, final int u) {
			final List<Edge> edges = graph.getEdges(v, u);
			
			if(edges == null)
				return null;
			
			for(int i = 0; i < edges.size(); i++)
				if(edges.get(i).isDirected())
					return edges.get(i);
			
			return null;
		}
		
		/**
		 * Gets the pitch cycles of {@link #r}.
		 * 
		 * @param graph the related graph
		 * @return the pitch cycles
		 * @since 1.0
		 */
		private List<Walk<WeightedVertex>> getPitchCycles(final Graph<WeightedVertex, Edge> graph) {
			final List<Walk<WeightedVertex>> pitchCycles = new ArrayList<Walk<WeightedVertex>>();
			Walk<WeightedVertex> currCycle = new Walk<WeightedVertex>(graph);
			boolean open = false;
			WeightedVertex v;
			
			for(int i = 0; i <= r.length(); i++) {
				v = r.get(i);
				currCycle.add(v);
				
				if(!open && v.getID() == v_s.getID())
					open = true;
				else if(open && v.getID() == v_s.getID()) {
					pitchCycles.add(currCycle);
					currCycle = new Walk<WeightedVertex>(graph);
					currCycle.add(v);
					open = true;
				}
			}
			
			return pitchCycles;
		}
		
		/**
		 * Gets the indices of the pitch cycles the specified vertices are contained in.
		 * 
		 * @param graph the related graph
		 * @param v_i the id of the vertex v_i
		 * @param v_j the id of the vertex v_j
		 * @param pitchCycles the pitch cycles
		 * @return an array of length <code>2</code> with the indices of the pitch cycles in which the vertices v_i (index 0) and v_j (index 1) are contained
		 * @since 1.0
		 */
		private int[] getPitchCycleIndices(final Graph<WeightedVertex, Edge> graph, final int v_i, final int v_j, final List<Walk<WeightedVertex>> pitchCycles) {
			final int[] res = new int[2];
			Walk<WeightedVertex> currCycle;
			
			res[0] = -1;
			res[1] = -1;
			
			for(int i = 0; i < pitchCycles.size(); i++) {
				currCycle = pitchCycles.get(i);
				
				if(res[0] < 0 && currCycle.contains(graph.getVertexByID(v_i)))
					res[0] = i;
				else if(res[1] < 0 && currCycle.contains(graph.getVertexByID(v_j)))
					res[1] = i;
				
				if(res[0] >= 0 && res[1] >= 0)
					break;
			}
			
			return res;
		}
		
		/**
		 * Reverses the specified cycle.
		 * 
		 * @param cycle the cycle as a list
		 * @param the reverse cycle as a list (is equal to the specified list)
		 * @since 1.0
		 */
		private List<WeightedVertex> reverseCycle(final List<WeightedVertex> cycle) {
			WeightedVertex tmpV;
			
			// reverse the path from v_j to v'_i by swapping the vertices
			for(int i = 0, j = cycle.size() - 1; i < j; i++, j--) {
				tmpV = cycle.get(i);
				cycle.set(i, cycle.get(j));
				cycle.set(j, tmpV);
			}
			
			return cycle;
		}
		
		/**
		 * Connects the base cycle with the specified pitch cycle.
		 * 
		 * @param baseCycle the base cycle
		 * @param pitchCycle the pitch cycle
		 * @since 1.0
		 */
		private void connectCycles(final List<WeightedVertex> baseCycle, final List<WeightedVertex> pitchCycle) {
			connectCycles(baseCycle, pitchCycle, 0);
		}
		
		/**
		 * Connects the base cycle with the specified pitch cycle.
		 * 
		 * @param baseCycle the base cycle
		 * @param pitchCycle the pitch cycle
		 * @param option the connection option with <code>0</code> for normal meaning the pitch cycle is completely added to the base cycle, <code>1</code> for first vertex of the pitch cycle should not be added and <code>2</code> for last vertex of the pitch cycle should not be added
		 * @since 1.0
		 */
		private void connectCycles(final List<WeightedVertex> baseCycle, final List<WeightedVertex> pitchCycle, final int option) {
			final int start = (baseCycle.size() > 0 && pitchCycle.size() > 0 && baseCycle.get(baseCycle.size() - 1).getID() == pitchCycle.get(0).getID()) ? 1 : 0;
			
			for(int i = start; i < pitchCycle.size(); i++) {
				if(option == 2 && i == pitchCycle.size() - 1)
					continue;	// the last vertex of the pitch cycle should not be added?
				else if(option == 1 && i == 0)
					continue;	// the first vertex of the pitch cycle should not be added?
				
				baseCycle.add(pitchCycle.get(i));
			}
		}
		
	}

}
