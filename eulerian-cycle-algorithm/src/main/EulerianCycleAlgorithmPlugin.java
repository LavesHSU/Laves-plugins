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
import lavesdk.algorithm.plugin.extensions.MatrixToGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.RandomGraphToolBarExtension;
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
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.MultiGraph;
import lavesdk.math.graph.Trail;
import lavesdk.math.graph.TrailByID;
import lavesdk.math.graph.Vertex;
import lavesdk.utils.GraphUtils;

/**
 * Plugin that visualizes and teaches users the Eulerian cycle algorithm to find an Eulerian cycle in an Eulerian graph.
 * 
 * @author jdornseifer
 * @version 1.3
 */
public class EulerianCycleAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the view that displays the cycle W as a string */
	private TextAreaView cycleWView;
	/** the view that displays the cycle W' as a string */
	private TextAreaView cycleW_ApoView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment */
	private EulerianCycleRTE rte;
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
	/** the view group for C and D,E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cde;
	/** the view group for A,B,C,D and E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcde;
	
	// modifiable visualization data
	/** color to visualize modifications */
	private Color colorModified;
	/** color to visualize the vertex v1 */
	private Color colorV1;
	/** color to visualize the vertex v_k */
	private Color colorV_k;
	/** color to visualize the cycle W */
	private Color colorW;
	/** color to visualize the cycle W' */
	private Color colorW_Apostrophe;
	/** line width of the vertex v1 */
	private int lineWidthV1;
	/** line width of the vertex v_k */
	private int lineWidthV_k;
	/** line width of the cycle W */
	private int lineWidthW;
	/** line width of the cycle W' */
	private int lineWidthW_Apostrophe;
	
	/** configuration key for the {@link #creatorPrefsDirectedValue} */
	private static final String CFGKEY_CREATORPROP_DIRECTED = "creatorPropDirected";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #colorV1} */
	private static final String CFGKEY_COLOR_V1 = "colorV1";
	/** configuration key for the {@link #colorV_k} */
	private static final String CFGKEY_COLOR_V_k = "colorV_k";
	/** configuration key for the {@link #colorW} */
	private static final String CFGKEY_COLOR_W = "colorW";
	/** configuration key for the {@link #colorW_Apostrophe} */
	private static final String CFGKEY_COLOR_W_APOSTROPHE = "colorW'";
	/** configuration key for the {@link #lineWidthV1} */
	private static final String CFGKEY_LINEWIDTH_V1 = "lineWidthV1";
	/** configuration key for the {@link #lineWidthV_k} */
	private static final String CFGKEY_LINEWIDTH_V_k = "lineWidthV_k";
	/** configuration key for the {@link #lineWidthW} */
	private static final String CFGKEY_LINEWIDTH_W = "lineWidthW";
	/** configuration key for the {@link #lineWidthW_Apostrophe} */
	private static final String CFGKEY_LINEWIDTH_W_APOSTROPHE = "lineWidthW'";
	
	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langEulerianCycle.txt"));
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
		this.graphView = new DefaultGraphView(LanguageFile.getLabel(langFile, "VIEW_GRAPH_TITLE", langID, "Graph"), new MultiGraph<>(false), null, true, langFile, langID);
		this.cycleWView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_CYCLE_W_TITLE", langID, "Cycle W'"), true, langFile, langID);
		this.cycleW_ApoView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_CYCLE_W_APOSTROPHE_TITLE", langID, "Cycle W'"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new EulerianCycleRTE();
		this.matrixToGraph = new MatrixToGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.BOTH, langFile, langID, true);
		this.randomGraph = new RandomGraphToolBarExtension<>(host, graphView, AllowedGraphType.BOTH, langFile, langID, false);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, true);
		this.creatorPrefsDirected = LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED", langID, "directed");
		this.creatorPrefsUndirected = LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED", langID, "undirected");
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		cycleWView.setAutoRepaint(true);
		cycleW_ApoView.setAutoRepaint(true);
		
		// load the creator preference data from the configuration
		creatorPrefsDirectedValue = this.config.getBoolean(CFGKEY_CREATORPROP_DIRECTED, false);
		
		// load the visualization colors from the configuration of the plugin
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		colorV1 = this.config.getColor(CFGKEY_COLOR_V1, new Color(85, 150, 190));
		colorV_k = this.config.getColor(CFGKEY_COLOR_V_k, new Color(255, 220, 80));
		colorW = this.config.getColor(CFGKEY_COLOR_W, new Color(25, 90, 140));
		colorW_Apostrophe = this.config.getColor(CFGKEY_COLOR_W_APOSTROPHE, new Color(255, 220, 80));
		lineWidthV1 = this.config.getInt(CFGKEY_LINEWIDTH_V1, 2);
		lineWidthV_k = this.config.getInt(CFGKEY_LINEWIDTH_V_k, 1);
		lineWidthW = this.config.getInt(CFGKEY_LINEWIDTH_W, 3);
		lineWidthW_Apostrophe = this.config.getInt(CFGKEY_LINEWIDTH_W_APOSTROPHE, 2);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		cycleWView.loadConfiguration(config, "cycleView");
		cycleW_ApoView.loadConfiguration(config, "cycleW_ApoView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}
	
	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Eulerian cycle algorithm");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds an Eulerian cycle in an Eulerian graph.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A connected Eulerian graph <i>G = (V, E)</i>.");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Chinese postman problem");
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
		graphView.setGraph(new MultiGraph<Vertex, Edge>(creatorPrefsDirectedValue));
		graphView.repaint();
		// change the graph type the user can create with the toolbar extension
		matrixToGraph.setAllowedGraphType(creatorPrefsDirectedValue ? AllowedGraphType.DIRECTED_ONLY : AllowedGraphType.UNDIRECTED_ONLY);
		randomGraph.setAllowedGraphType(creatorPrefsDirectedValue ? AllowedGraphType.DIRECTED_ONLY : AllowedGraphType.UNDIRECTED_ONLY);
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|///////////////
		 * / /|/             /	A = algorithm text view
		 * /A/|/      C      /	B = legend view
		 * / /|/             /	C = graph view
		 * ///|///////////////	D = text area view (cycle W view)
		 * ---|---------------	E = text area view (cycle W' view)
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
		de.add(cycleWView);
		de.add(cycleW_ApoView);
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
		cycleWView.saveConfiguration(config, "cycleView");
		cycleW_ApoView.saveConfiguration(config, "cycleW_ApoView");
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
		cycleWView.reset();
		cycleW_ApoView.reset();
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_V1, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_V1", langID, "Background color of the vertex v<sub>1</sub>"), colorV1));
		plm.add(new ColorProperty(CFGKEY_COLOR_V_k, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_V_k", langID, "Background color of the vertex v<sub>k</sub>"), colorV_k));
		plm.add(new ColorProperty(CFGKEY_COLOR_W, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_W", langID, "Color of the cycle W"), colorW));
		plm.add(new ColorProperty(CFGKEY_COLOR_W_APOSTROPHE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_W_APOSTROPHE", langID, "Color of the cycle W'"), colorW_Apostrophe));
		
		final NumericProperty lwV1 = new NumericProperty(CFGKEY_LINEWIDTH_V1, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_V1", langID, "Line width of the vertex v<sub>1</sub>"), lineWidthV1, true);
		lwV1.setMinimum(1);
		lwV1.setMaximum(5);
		plm.add(lwV1);
		final NumericProperty lwV_k = new NumericProperty(CFGKEY_LINEWIDTH_V_k, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_V_k", langID, "Line width of the vertex v<sub>k</sub>"), lineWidthV_k, true);
		lwV_k.setMinimum(1);
		lwV_k.setMaximum(5);
		plm.add(lwV_k);
		final NumericProperty lwW = new NumericProperty(CFGKEY_LINEWIDTH_W, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_W", langID, "Line width of the cycle W"), lineWidthW, true);
		lwW.setMinimum(1);
		lwW.setMaximum(5);
		plm.add(lwW);
		final NumericProperty lwW_Apostrophe = new NumericProperty(CFGKEY_LINEWIDTH_W_APOSTROPHE, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_W_APOSTROPHE", langID, "Line width of the cycle W'"), lineWidthW_Apostrophe, true);
		lwW_Apostrophe.setMinimum(1);
		lwW_Apostrophe.setMaximum(5);
		plm.add(lwW_Apostrophe);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorV1 = config.addColor(CFGKEY_COLOR_V1, plm.getColorProperty(CFGKEY_COLOR_V1).getValue());
		colorV_k = config.addColor(CFGKEY_COLOR_V_k, plm.getColorProperty(CFGKEY_COLOR_V_k).getValue());
		colorW = config.addColor(CFGKEY_COLOR_W, plm.getColorProperty(CFGKEY_COLOR_W).getValue());
		colorW_Apostrophe = config.addColor(CFGKEY_COLOR_W_APOSTROPHE, plm.getColorProperty(CFGKEY_COLOR_W_APOSTROPHE).getValue());
		lineWidthV1 = config.addInt(CFGKEY_LINEWIDTH_V1, plm.getNumericProperty(CFGKEY_LINEWIDTH_V1).getValue().intValue());
		lineWidthV_k = config.addInt(CFGKEY_LINEWIDTH_V_k, plm.getNumericProperty(CFGKEY_LINEWIDTH_V_k).getValue().intValue());
		lineWidthW = config.addInt(CFGKEY_LINEWIDTH_W, plm.getNumericProperty(CFGKEY_LINEWIDTH_W).getValue().intValue());
		lineWidthW_Apostrophe = config.addInt(CFGKEY_LINEWIDTH_W_APOSTROPHE, plm.getNumericProperty(CFGKEY_LINEWIDTH_W_APOSTROPHE).getValue().intValue());
		
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
		if(!GraphUtils.isEulerian(graphView.getGraph())) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NOTEULERIAN", langID, "The created graph is not eulerian!%nThe algorithm can only be applied to Eulerian graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NOTEULERIAN_TITLE", langID, "No Eulerian graph"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			graphView.setEditable(false);
			graphView.deselectAll();
			
			cycleWView.reset();
			cycleW_ApoView.reset();
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
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOPCRITERION", langID, "2. Stop criterion:"), 2);
		final AlgorithmParagraph addParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ADDITIONALCYCLE", langID, "3. Additional cycle:"), 3);
		final AlgorithmParagraph enlargeParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ENLARGMENT", langID, "4. Enlargment:"), 4);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INIT", langID, "Choose a _latex{$v_1 \\in V$}.\n"), 1);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1", langID, "Select a vertex <i>v<sub>1</sub></i>."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				EulerianCycleAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				EulerianCycleAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				EulerianCycleAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				EulerianCycleAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				EulerianCycleAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(EulerianCycleAlgorithmPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { EulerianCycleAlgorithmPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return EulerianCycleAlgorithmPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Integer[] solutions) {
				state.addInt("v1", solutions[0]);
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				return true;
			}
		});
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_INIT", langID, "Find a cycle _latex{$W$} ensuing from _latex{$v_1$}.\n\n"), 2);
		step.setExercise(new AlgorithmExercise<String>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Specify a cycle <i>W</i>."), 1.0f) {
			
			@Override
			protected String[] requestSolution() {
				final SolutionEntry<JTextField> entry = new SolutionEntry<JTextField>("W=", new JTextField());
				
				if(!SolveExercisePane.showDialog(EulerianCycleAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entry }, EulerianCycleAlgorithmPlugin.this.langFile, EulerianCycleAlgorithmPlugin.this.langID, LanguageFile.getLabel(EulerianCycleAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", EulerianCycleAlgorithmPlugin.this.langID, "Use a comma as the delimiter!<br>Enter the cycle in the following form: v<sub>i</sub>, v<sub>j</sub>, v<sub>k</sub>, ..., v<sub>i</sub>.")))
					return null;
				
				return new String[] { entry.getComponent().getText() };
			}
			
			@Override
			protected String getResultAsString(String result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					if(result.startsWith("(") && result.endsWith(")"))
						return "W=" + result;
					else
						return "W=(" + result + ")";
				}
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, String[] solutions) {
				state.addTrail("W", GraphUtils.toTrail(solutions[0], EulerianCycleAlgorithmPlugin.this.graphView.getGraph(), null).cast());
			}
			
			@Override
			protected boolean examine(String[] results, AlgorithmState state) {
				final Trail<Vertex> t = GraphUtils.toTrail(results[0], EulerianCycleAlgorithmPlugin.this.graphView.getGraph(), null);
				return t != null && t.isClosed();
			}
		});
		
		// 2. stop criterion
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_STOP", langID, "If _latex{$W$} contains all edges of _latex{$G$} then stop.\n"), 3);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Will the algorithm stop?"), 1.0f) {
			
			private final String labelYes = LanguageFile.getLabel(langFile, "EXERCISE_STEP3_YES", langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(langFile, "EXERCISE_STEP3_NO", langID, "No");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelYes);
				final JRadioButton rdobtn2 = new JRadioButton(labelNo);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(EulerianCycleAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, EulerianCycleAlgorithmPlugin.this.langFile, EulerianCycleAlgorithmPlugin.this.langID))
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
				final Graph<Vertex, Edge> graph = EulerianCycleAlgorithmPlugin.this.graphView.getGraph();
				final TrailByID<Vertex> t = state.getTrail("W", graph);
				
				return (t != null && results[0] != null && (t.length() == graph.getSize()) == results[0]);
			}
		});
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_STOP", langID, "Otherwise find a vertex _latex{$v_k$} that is endpoint of at least two edges, one of them is contained in _latex{$W$} and one of them not.\n\n"), 4);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "Find a vertex <i>v<sub>k</sub></i> (<i>select the vertex in the graph</i>)."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				EulerianCycleAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				EulerianCycleAlgorithmPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				EulerianCycleAlgorithmPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				EulerianCycleAlgorithmPlugin.this.graphView.setShowCursorToolAlways(false);
				EulerianCycleAlgorithmPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(EulerianCycleAlgorithmPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { EulerianCycleAlgorithmPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return EulerianCycleAlgorithmPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Integer[] solutions) {
				state.addInt("v_k", solutions[0]);
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				final Graph<Vertex, Edge> graph = EulerianCycleAlgorithmPlugin.this.graphView.getGraph();
				final TrailByID<Vertex> t = state.getTrail("W", graph);
				final Vertex v_k = graph.getVertexByID(results[0]);
				boolean contained = false;
				boolean notContained = false;
				boolean contains;
				
				for(int i = 0; i < v_k.getOutgoingEdgeCount(); i++) {
					contains = t.contains(v_k.getOutgoingEdge(i));
					contained = contained || contains;
					notContained = notContained || !contains;
				}
				
				return contained && notContained;
			}
		});
		
		// 3. additional cycle
		step = new AlgorithmStep(addParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_ADDITIONALCYCLE", langID, "Find a cycle _latex{$W'$} ensuing from _latex{$v_1$} that does not contain any edge of _latex{$W$}.\n\n"), 5);
		step.setExercise(new AlgorithmExercise<String>(LanguageFile.getLabel(langFile, "EXERCISE_STEP5", langID, "Specify a cycle <i>W'</i>."), 1.0f) {
			
			private Trail<Vertex> W_Apo = null;
			
			@Override
			protected String[] requestSolution() {
				final SolutionEntry<JTextField> entry = new SolutionEntry<JTextField>("W'=", new JTextField());
				
				if(!SolveExercisePane.showDialog(EulerianCycleAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entry }, EulerianCycleAlgorithmPlugin.this.langFile, EulerianCycleAlgorithmPlugin.this.langID, LanguageFile.getLabel(EulerianCycleAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", EulerianCycleAlgorithmPlugin.this.langID, "Use a comma as the delimiter!<br>Enter the cycle in the following form: v<sub>i</sub>, v<sub>j</sub>, v<sub>k</sub>, ..., v<sub>i</sub>.")))
					return null;
				
				return new String[] { entry.getComponent().getText() };
			}
			
			@Override
			protected String getResultAsString(String result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					if(result.startsWith("(") && result.endsWith(")"))
						return "W'=" + result;
					else
						return "W'=(" + result + ")";
				}
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, String[] solutions) {
				state.addTrail("W_Apo", W_Apo.cast());
				W_Apo = null;
			}
			
			@Override
			protected boolean examine(String[] results, AlgorithmState state) {
				final Graph<Vertex, Edge> graph = EulerianCycleAlgorithmPlugin.this.graphView.getGraph();
				final int v_k = state.getInt("v_k");
				final TrailByID<Vertex> w = state.getTrail("W", graph);
				W_Apo = GraphUtils.toTrail(results[0], EulerianCycleAlgorithmPlugin.this.graphView.getGraph(), w.cast());
				
				if(W_Apo == null || W_Apo.length() < 1)
					return false;
				else if(W_Apo.get(0).getID() != v_k || !W_Apo.isClosed())
					return false;
				
				// the specified trail W' may not contain edges of W
				for(int i = 0; i < W_Apo.length(); i++)
					if(w.contains(W_Apo.getEdge(i)))
						return false;
				
				return true;
			}
		});
		
		// 4. enlargement
		step = new AlgorithmStep(enlargeParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_ENLARGMENT", langID, "Enlarge the cycle _latex{$W$} by inserting the cycle _latex{$W'$} at the position of _latex{$v_k$}.\n"), 6);
		step.setExercise(new AlgorithmExercise<String>(LanguageFile.getLabel(langFile, "EXERCISE_STEP6", langID, "What is <i>W</i> after this step?"), 1.0f) {
			
			@Override
			protected String[] requestSolution() {
				final SolutionEntry<JTextField> entry = new SolutionEntry<JTextField>("W=", new JTextField());
				
				if(!SolveExercisePane.showDialog(EulerianCycleAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entry }, EulerianCycleAlgorithmPlugin.this.langFile, EulerianCycleAlgorithmPlugin.this.langID, LanguageFile.getLabel(EulerianCycleAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", EulerianCycleAlgorithmPlugin.this.langID, "Use a comma as the delimiter!<br>Enter the cycle in the following form: v<sub>i</sub>, v<sub>j</sub>, v<sub>k</sub>, ..., v<sub>i</sub>.")))
					return null;
				
				return new String[] { entry.getComponent().getText() };
			}
			
			@Override
			protected String getResultAsString(String result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					if(result.startsWith("(") && result.endsWith(")"))
						return "W=" + result;
					else
						return "W=(" + result + ")";
				}
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, String[] solutions) {
				state.addTrail("W", GraphUtils.toTrail(solutions[0], EulerianCycleAlgorithmPlugin.this.graphView.getGraph(), null).cast());
			}
			
			@Override
			protected boolean examine(String[] results, AlgorithmState state) {
				final Graph<Vertex, Edge> graph = EulerianCycleAlgorithmPlugin.this.graphView.getGraph();
				final Trail<Vertex> t = GraphUtils.toTrail(results[0], EulerianCycleAlgorithmPlugin.this.graphView.getGraph(), null);
				final Trail<Vertex> w = state.getTrail("W", graph).cast();
				final Trail<Vertex> w_apo = state.getTrail("W_Apo", graph).cast();
				final Vertex start = w_apo.get(0);
				int possibilities = 0;
				
				// input trail is invalid?
				if(t == null)
					return false;
				
				// check the number of possibilities where W' can be inserted
				for(int i = 0; i <= w.length(); i++)
					if(w.get(i) == start)
						possibilities++;
				
				// for each possibility compare the user's answer with the combination of W and W'
				for(int p = possibilities; p >= 1; p--) {
					final Trail<Vertex> w_Tmp = w.clone();
					w_Tmp.insert(w_apo, p);
					if(compare(t, w_Tmp))
						return true;
				}
				
				return false;
			}
			
			private boolean compare(final Trail<Vertex> t, final Trail<Vertex> w) {
				if(t.length() != w.length())
					return false;
				
				for(int i = 0; i <= t.length(); i++)
					if(t.get(i) != w.get(i))
						return false;
				
				return true;
			}
		});
		
		step = new AlgorithmStep(enlargeParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_ENLARGMENT", langID, "Go to step 2."), 7);
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_V1", langID, "The vertex v<sub>1</sub>"), LegendItem.createCircleIcon(colorV1, Color.black, lineWidthV1)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_V_k", langID, "The current vertex v<sub>k</sub>"), LegendItem.createCircleIcon(colorV_k, Color.black, lineWidthV_k)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_W", langID, "The current cycle W"), LegendItem.createLineIcon(colorW, lineWidthW)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_W_APOSTROPHE", langID, "The current cycle W'"), LegendItem.createLineIcon(colorW_Apostrophe, lineWidthW_Apostrophe)));
		legendView.add(new LegendItem("item5", cycleWView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_CYCLE_W_MODIFICATION", langID, "The cycle W becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item6", cycleW_ApoView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_CYCLE_W_APOSTROPHE_MODIFICATION", langID, "The cycle W' becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
	}
	
	/**
	 * The runtime environment of the Eulerian cycle algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class EulerianCycleRTE extends AlgorithmRTE {
		
		/** the vertex v1 */
		private int v1;
		/** the vertex v_k */
		private int v_k;
		/** the cycle W (might be <code>null</code>) */
		private Trail<Vertex> W;
		/** the cycle W' (might be <code>null</code>) */
		private Trail<Vertex> W_Apo;
		/** the user's choice of v1 */
		private int userChoiceV1;
		/** the user's choice of v_k */
		private int userChoiceV_k;
		/** the user's choice of the cycle W */
		private Trail<Vertex> userChoiceW;
		/** the user's choice of the cycle W' */
		private Trail<Vertex> userChoiceW_Apo;
		
		public EulerianCycleRTE() {
			super(EulerianCycleAlgorithmPlugin.this, EulerianCycleAlgorithmPlugin.this.algoText);
			
			userChoiceV1 = 0;
			userChoiceV_k = 0;
			userChoiceW = null;
			userChoiceW_Apo = null;
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = EulerianCycleAlgorithmPlugin.this.graphView.getGraph();
			int nextStep = -1;
			Vertex v;
			GraphView<Vertex, Edge>.VisualEdge ve;
			
			switch(stepID) {
				case 1:
					// choose v1
					
					// if the user has chosen a vertex then take this one
					if(userChoiceV1 > 0)
						v1 = userChoiceV1;
					else {
						// we always choose the first vertex in the graph
						v1 = (graph.getOrder() > 0) ? graph.getVertex(0).getID() : 0;
					}
					
					// clear the user's choice of v1
					userChoiceV1 = 0;
					
					sleep(250);
					visualizeVertices();
					sleep(750);
					
					if(v1 < 1)
						nextStep = -1;
					else
						nextStep = 2;
					break;
				case 2:
					// determine a cycle W
					
					// take the user's choice of the cycle if their is one otherwise find a cycle in the graph
					if(userChoiceW != null)
						W = userChoiceW;
					else
						W = findCircuit(graph.getVertexByID(v1), graph);
					
					// clear the user's choice
					userChoiceW = null;
					
					sleep(250);
					
					// visualize W (animate it instead of using visualizeEdges())
					for(int i = 0; i < W.length(); i++) {
						ve = EulerianCycleAlgorithmPlugin.this.graphView.getVisualEdge(W.getEdge(i));
						
						// highlight the edge
						ve.setColor(EulerianCycleAlgorithmPlugin.this.colorW);
						ve.setLineWidth(EulerianCycleAlgorithmPlugin.this.lineWidthW);
						EulerianCycleAlgorithmPlugin.this.graphView.repaint();
						
						sleep(500);
					}
					
					EulerianCycleAlgorithmPlugin.this.cycleWView.setBackground(EulerianCycleAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeWAsText();
					sleep(250);
					EulerianCycleAlgorithmPlugin.this.cycleWView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 3;
					break;
				case 3:
					// if W contains all edges of G then stop otherwise go to 4 (stepid)
					
					sleep(1000);
					
					if(W.length() == graph.getSize())
						nextStep = -1;
					else
						nextStep = 4;
					
					break;
				case 4:
					// find v_k that is an endpoint of at least two edges, one of them is contained in W and one of them not
					
					// take the user's choice if the user solves the related exercise correctly
					if(userChoiceV_k > 0)
						v_k = userChoiceV_k;
					else {
						boolean contains;
						boolean contained;
						boolean notContained;
						
						v_k = 0;
						
						for(int i = 0; i < graph.getOrder(); i++) {
							v = graph.getVertex(i);
							contained = false;
							notContained = false;
							
							// v_k should not be v1 so that both can be visualize
							// (if v1 has free edges then another vertex must have free edges to)
							if(v.getID() == v1)
								continue;
							
							for(int j = 0; j < v.getOutgoingEdgeCount(); j++) {
								contains = W.contains(v.getOutgoingEdge(j));
								contained = contained || contains;
								notContained = notContained || !contains;
								
								if(contained && notContained)
									break;
							}
							
							if(contained && notContained) {
								v_k = v.getID();
								break;
							}
						}
					}
					
					// clear the user's choice
					userChoiceV_k = 0;
					
					// visualize v_k
					sleep(500);
					visualizeVertices();
					sleep(500);
					
					// this case should not happen but if it did then quit the algorithm
					if(v_k < 1)
						nextStep = -1;
					else
						nextStep = 5;
					break;
				case 5:
					// find a cycle W' ensuing from v_k with edges that are not contained in W
					
					// take the user's choice if the user solves the related exercise correctly
					if(userChoiceW_Apo != null)
						W_Apo = userChoiceW_Apo;
					else
						W_Apo = findCircuit(graph.getVertexByID(v_k), graph, W);
					
					// clear the user's choice
					userChoiceW_Apo = null;
					
					sleep(250);
					
					// visualize W' (animate it instead of using visualizeEdges())
					for(int i = 0; i < W_Apo.length(); i++) {
						ve = EulerianCycleAlgorithmPlugin.this.graphView.getVisualEdge(W_Apo.getEdge(i));
						
						// highlight the edge
						ve.setColor(EulerianCycleAlgorithmPlugin.this.colorW_Apostrophe);
						ve.setLineWidth(EulerianCycleAlgorithmPlugin.this.lineWidthW_Apostrophe);
						EulerianCycleAlgorithmPlugin.this.graphView.repaint();
						
						sleep(500);
					}
					
					EulerianCycleAlgorithmPlugin.this.cycleW_ApoView.setBackground(EulerianCycleAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeW_ApoAsText();
					sleep(250);
					EulerianCycleAlgorithmPlugin.this.cycleW_ApoView.setBackground(Color.white);
					sleep(250);
					
					nextStep = 6;
					break;
				case 6:
					// enlarge W by inserting W' at v_k
					
					// take the user's choice if the user solves the related exercise correctly
					if(userChoiceW != null)
						W = userChoiceW;
					else
						W.insert(W_Apo, true);
					
					// clear the user's choice
					userChoiceW = null;
					
					// clear W' so that it is not visualized any more
					W_Apo = null;
					
					sleep(500);
					
					EulerianCycleAlgorithmPlugin.this.cycleWView.setBackground(EulerianCycleAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeWAsText();
					sleep(250);
					EulerianCycleAlgorithmPlugin.this.cycleWView.setBackground(Color.white);
					sleep(250);
					
					// visualize the edges
					visualizeEdges();
					
					sleep(1000);
					
					nextStep = 7;
					break;
				case 7:
					// go to 2. (stepid=3)
					
					sleep(1000);
					
					nextStep = 3;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addInt("v1", v1);
			state.addInt("v_k", v_k);
			state.addTrail("W", (W != null) ? W.cast() : null);
			state.addTrail("W_Apo", (W_Apo != null) ? W_Apo.cast() : null);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			v1 = state.getInt("v1");
			v_k = state.getInt("v_k");
			final TrailByID<Vertex> w = state.getTrail("W", EulerianCycleAlgorithmPlugin.this.graphView.getGraph());
			W = (w != null) ? w.cast() : null;
			final TrailByID<Vertex> w_apo = state.getTrail("W_Apo", EulerianCycleAlgorithmPlugin.this.graphView.getGraph());
			W_Apo = (w_apo != null) ? w_apo.cast() : null;
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			v1 = state.addInt("v1", 0);
			v_k = state.addInt("v_k", 0);
			state.addTrail("W", null);
			state.addTrail("W_Apo", null);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			if(stepID == 1) {
				visualizeVertices();
				visualizeWAsText();
			}
			else if(stepID == 2 || stepID == 6) {
				visualizeEdges();
				visualizeWAsText();
			}
			else if(stepID == 4)
				visualizeVertices();
			else if(stepID == 5) {
				visualizeEdges();
				visualizeW_ApoAsText();
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			if(stepID == 1)
				userChoiceV1 = state.getInt("v1");
			else if(stepID == 2 || stepID == 6)
				userChoiceW = state.getTrail("W", EulerianCycleAlgorithmPlugin.this.graphView.getGraph()).cast();
			else if(stepID == 4)
				userChoiceV_k = state.getInt("v_k");
			else if(stepID == 5)
				userChoiceW_Apo = state.getTrail("W_Apo", EulerianCycleAlgorithmPlugin.this.graphView.getGraph()).cast();
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { EulerianCycleAlgorithmPlugin.this.graphView, EulerianCycleAlgorithmPlugin.this.cycleWView, EulerianCycleAlgorithmPlugin.this.cycleW_ApoView };
		}
		
		/**
		 * Visualizes the vertices {@link #v1} and {@link #v_k}. All other vertices are displayed in the default background color
		 * and the default edge width.
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			GraphView<Vertex, Edge>.VisualVertex vv;
			
			for(int i = 0; i < EulerianCycleAlgorithmPlugin.this.graphView.getVisualVertexCount(); i++) {
				vv = EulerianCycleAlgorithmPlugin.this.graphView.getVisualVertex(i);
				
				if(vv.getVertex().getID() == v1) {
					vv.setBackground(EulerianCycleAlgorithmPlugin.this.colorV1);
					vv.setEdgeWidth(EulerianCycleAlgorithmPlugin.this.lineWidthV1);
				}
				else if(vv.getVertex().getID() == v_k) {
					vv.setBackground(EulerianCycleAlgorithmPlugin.this.colorV_k);
					vv.setEdgeWidth(EulerianCycleAlgorithmPlugin.this.lineWidthV_k);
				}
				else {
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
			}
			
			// show the visualization
			EulerianCycleAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes all edges of the graph based on {@link #W} and {@link #W_Apo}. Edges that are not contained in
		 * {@link #W} or {@link #W_Apo} are displayed in the default color and line width.
		 * 
		 * @since 1.0
		 */
		private void visualizeEdges() {
			GraphView<Vertex, Edge>.VisualEdge ve;
			
			for(int i = 0; i < EulerianCycleAlgorithmPlugin.this.graphView.getVisualEdgeCount(); i++) {
				ve = EulerianCycleAlgorithmPlugin.this.graphView.getVisualEdge(i);
				
				if(W != null && W.contains(ve.getEdge())) {
					ve.setColor(EulerianCycleAlgorithmPlugin.this.colorW);
					ve.setLineWidth(EulerianCycleAlgorithmPlugin.this.lineWidthW);
				}
				else if(W_Apo != null && W_Apo.contains(ve.getEdge())) {
					ve.setColor(EulerianCycleAlgorithmPlugin.this.colorW_Apostrophe);
					ve.setLineWidth(EulerianCycleAlgorithmPlugin.this.lineWidthW_Apostrophe);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			// show the visualization
			EulerianCycleAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the cycle {@link #W} as a text representation.
		 * 
		 * @since 1.0
		 */
		private void visualizeWAsText() {
			EulerianCycleAlgorithmPlugin.this.cycleWView.setText((W != null) ? "W=" + W.toString() : "");
		}
		
		/**
		 * Visualizes the cycle {@link #W_Apo} as a text representation.
		 * 
		 * @since 1.0
		 */
		private void visualizeW_ApoAsText() {
			EulerianCycleAlgorithmPlugin.this.cycleW_ApoView.setText((W_Apo != null) ? "W'=" + W_Apo.toString() : "");
		}
		
		/**
		 * Finds a circuit (closed trail) in an <b>Eulerian graph</b>.
		 * 
		 * @param start the start vertex
		 * @param graph the graph which must be eulerian
		 * @return the circuit
		 * @since 1.0
		 */
		private Trail<Vertex> findCircuit(final Vertex start, final Graph<Vertex, Edge> graph) {
			return findCircuit(start, graph, null);
		}
		
		/**
		 * Finds a circuit (closed trail) in an <b>Eulerian graph</b>.
		 * 
		 * @param start the start vertex
		 * @param graph the graph which must be eulerian
		 * @param t the trail its edges may not be in the circuit that has to be found or <code>null</code>
		 * @return the circuit that only contains edges that are not in t
		 * @since 1.0
		 */
		private Trail<Vertex> findCircuit(final Vertex start, final Graph<Vertex, Edge> graph, final Trail<Vertex> t) {
			final Trail<Vertex> trail = new Trail<Vertex>(graph);
			
			// search a valid circuit
			findCircuit(start, null, trail, (t != null) ? t : trail);
			
			return trail;
		}
		
		/**
		 * Finds a circuit (closed trail) in an Eulerian graph.
		 * <br><br>
		 * <b>Notice</b>:<br>
		 * To find a circuit in an eulerian graph invoke {@link #findCircuit(Vertex, Graph)} or {@link #findCircuit(Vertex, Graph, Trail)}.
		 * Do not use this method outside of the named one.
		 * 
		 * @param v the current vertex
		 * @param e the edge the trail should travel over or <code>null</code>
		 * @param trail the circuit
		 * @param t the trail its edges may not be in the circuit that has to be found
		 * @return <code>true</code> if a closed trail is found otherwise <code>false</code>
		 * @since 1.0
		 */
		private boolean findCircuit(final Vertex v, final Edge e, final Trail<Vertex> trail, final Trail<Vertex> t) {
			Edge edge;
			boolean res = false;
			
			// add the vertex
			trail.add(v, e);
			
			// cycle found? then quit
			if(trail.isClosed())
				return true;
			
			// check all unvisited edges and go to a next vertex taking an edge that was not visited before
			for(int i = 0; i < v.getOutgoingEdgeCount(); i++) {
				edge = v.getOutgoingEdge(i);
				
				// t and trail can be different but check t first which is more performant if
				// t == trail
				if(t.contains(edge) || trail.contains(edge))
					continue;
				else
					res = findCircuit(edge.getSuccessor(v), edge, trail, t);
				
				if(res)
					break;
			}
			
			return res;
		}
		
	}

}
