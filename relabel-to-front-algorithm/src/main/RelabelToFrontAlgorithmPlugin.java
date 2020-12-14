package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import lavesdk.LAVESDKV;
import lavesdk.algorithm.AlgorithmRTE;
import lavesdk.algorithm.AlgorithmState;
import lavesdk.algorithm.AlgorithmStateAttachment;
import lavesdk.algorithm.RTEvent;
import lavesdk.algorithm.plugin.AlgorithmPlugin;
import lavesdk.algorithm.plugin.PluginHost;
import lavesdk.algorithm.plugin.ResourceLoader;
import lavesdk.algorithm.plugin.enums.MessageIcon;
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultNetworkView;
import lavesdk.algorithm.plugin.views.DefaultRNView;
import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.GraphScene;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.LegendView;
import lavesdk.algorithm.plugin.views.TextAreaView;
import lavesdk.algorithm.plugin.views.VertexOnlyTransferProtocol;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.plugin.views.custom.CustomVisualFormula;
import lavesdk.algorithm.plugin.views.renderers.DefaultNodeRenderer;
import lavesdk.algorithm.plugin.views.renderers.DefaultVertexRenderer;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.algorithm.text.Annotation;
import lavesdk.algorithm.text.AnnotationImagesList;
import lavesdk.configuration.Configuration;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.ExecutionTableColumn;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.Set;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.network.Arc;
import lavesdk.math.graph.network.Network;
import lavesdk.math.graph.network.Node;
import lavesdk.math.graph.network.RNEdge;
import lavesdk.math.graph.network.ResidualNetwork;
import lavesdk.math.graph.network.enums.FlowType;

/**
 * Plugin that visualizes and teaches users the Relabel-to-front algorithm.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class RelabelToFrontAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the view of the network */
	private DefaultNetworkView networkView;
	/** the view of the residual network */
	private DefaultRNView residualNetworkView;
	/** the view that displays height(v) */
	private ExecutionTableView heightView;
	/** the view that displays the last changed height list */
	private TextAreaView heightLastChangedView;
	/** the view that displays the algorithm text */
	private AlgorithmTextView algoTextView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the Relabel-to-front algorithm */
	private RelabelToFrontRTE rte;
	/** the list of annotation images */
	private AnnotationImagesList imgList;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for C and D (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cd;
	/** the view group for E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ef;
	/** the view group for A,B,C,D,E and F (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcdef;
	
	// modifiable visualization data
	/** color to visualize the source of the network */
	private Color colorSource;
	/** color to visualize the sink of the network */
	private Color colorSink;
	/** color to visualize modified objects */
	private Color colorModified;
	/** color to visualize the node v */
	private Color colorV;
	/** color to visualize the node u */
	private Color colorU;
	/** color to visualize changed flow in the network */
	private Color colorFlowChanged;
	/** color to visualize the neighbors in the network */
	private Color colorNeighbors;
	/** color to visualize the edge between v and u in the residual network */
	private Color colorEdgeVU;
	/** line width of the node v */
	private int lineWidthV;
	/** line width of the node u */
	private int lineWidthU;
	/** line width of the edge between v and u in the residual network */
	private int lineWidthEdgeVU;
	/** line width of the edge its flow changed */
	private int lineWidthFlowChanged;
	
	/** configuration key for the {@link #colorSource} */
	private static final String CFGKEY_COLOR_SOURCE = "colorSource";
	/** configuration key for the {@link #colorSink} */
	private static final String CFGKEY_COLOR_SINK = "colorSink";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #colorV} */
	private static final String CFGKEY_COLOR_V = "colorV";
	/** configuration key for the {@link #colorU} */
	private static final String CFGKEY_COLOR_U = "colorU";
	/** configuration key for the {@link #colorFlowChanged} */
	private static final String CFGKEY_COLOR_FLOWCHANGED = "colorFlowChanged";
	/** configuration key for the {@link #colorNeighbors} */
	private static final String CFGKEY_COLOR_NEIGHBORS = "colorNeighbors";
	/** configuration key for the {@link #colorEdgeVU} */
	private static final String CFGKEY_COLOR_EDGEVU = "colorEdgeVU";
	/** configuration key for the {@link #lineWidthV} */
	private static final String CFGKEY_LINEWIDTH_V = "lineWidthV";
	/** configuration key for the {@link #lineWidthU} */
	private static final String CFGKEY_LINEWIDTH_U = "lineWidthU";
	/** configuration key for the {@link #lineWidthEdgeVU} */
	private static final String CFGKEY_LINEWIDTH_EDGEVU = "lineWidthEdgeVU";
	/** configuration key for the {@link #lineWidthFlowChanged} */
	private static final String CFGKEY_LINEWIDTH_FLOWCHANGED = "lineWidthFlowChanged";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langRelabelToFront.txt"));
			// include the language file of the host to only use one language file
			this.langFile.include(host.getLanguageFile());
		} catch (IOException e) {
			this.langFile = null;
		}
		this.langID = host.getLanguageID();
		
		// create the annotation images list
		imgList = new AnnotationImagesList();
		imgList.add("network", resLoader.getResource("main/resources/network.png"));
		imgList.add("residual-network", resLoader.getResource("main/resources/residual-network.png"));
		imgList.add("excess-def-en", resLoader.getResource("main/resources/excess-def-en.png"));
		imgList.add("excess-def-de", resLoader.getResource("main/resources/excess-def-de.png"));
		
		// create plugin
		this.host = host;
		this.config = (config != null) ? config : new Configuration();
		this.vgfFileFilter = new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf");
		this.pngFileFilter = new FileNameExtensionFilter("Portable Network Graphic (*.png)", "png");
		this.networkView = new DefaultNetworkView(LanguageFile.getLabel(langFile, "VIEW_NETWORK_TITLE", langID, "Network"), new Network<Node, Arc>(FlowType.PREFLOW, new Node("s"), new Node("s")), null, true, langFile, langID);
		this.residualNetworkView = new DefaultRNView(LanguageFile.getLabel(langFile, "VIEW_RESIDUALNETWORK_TITLE", langID, "Residual Network"), false, langFile, langID, networkView);
		this.heightView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_HEIGHT_TITLE", langID, "height(v)"), true, langFile, langID);
		this.heightLastChangedView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_HEIGHTLASTCHANGED_TITLE", langID, "height(v): Last Changes"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new RelabelToFrontRTE();
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		heightView.setAutoRepaint(true);
		heightLastChangedView.setAutoRepaint(true);
		
		// the residual network may never be edited by the user and its visibility is controlled manually by the algorithm
		residualNetworkView.setEditable(false);
		residualNetworkView.setVisible(false);
		
		// load the visualization colors from the configuration of the plugin
		colorSource = this.config.getColor(CFGKEY_COLOR_SOURCE, new Color(70, 155, 215));
		colorSink = this.config.getColor(CFGKEY_COLOR_SINK, new Color(135, 195, 235));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		colorV = this.config.getColor(CFGKEY_COLOR_V, new Color(200, 145, 145));
		colorU = this.config.getColor(CFGKEY_COLOR_U, new Color(255, 220, 80));
		colorFlowChanged = this.config.getColor(CFGKEY_COLOR_FLOWCHANGED, new Color(0, 80, 130));
		colorNeighbors = this.config.getColor(CFGKEY_COLOR_NEIGHBORS, new Color(255, 220, 80));
		colorEdgeVU = this.config.getColor(CFGKEY_COLOR_EDGEVU, new Color(0, 80, 130));
		lineWidthV = this.config.getInt(CFGKEY_LINEWIDTH_V, 2);
		lineWidthU = this.config.getInt(CFGKEY_LINEWIDTH_U, 2);
		lineWidthEdgeVU = this.config.getInt(CFGKEY_LINEWIDTH_EDGEVU, 2);
		lineWidthFlowChanged = this.config.getInt(CFGKEY_LINEWIDTH_FLOWCHANGED, 2);
		
		// load view configurations
		networkView.loadConfiguration(config, "networkView");
		residualNetworkView.loadConfiguration(config, "residualNetworkView");
		heightView.loadConfiguration(config, "heightView");
		heightLastChangedView.loadConfiguration(config, "heightLastChangedView");
		algoTextView.loadConfiguration(config, "algoTextView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Relabel-to-front");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds a flow of maximum strength in a network.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A network (G, u, s, t).");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Flow problem");
	}

	@Override
	public String getSubject() {
		return LanguageFile.getLabel(langFile, "ALGO_SUBJECT", langID, "Logistics");
	}
	
	@Override
	public String getInstructions() {
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own network and make sure that the network complies with the assumptions of the algorithm.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
	}

	@Override
	public String getVersion() {
		return "1.0";
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
		return false;
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
		// set a new network in the view
		networkView.setGraph(new Network<Node, Arc>(FlowType.PREFLOW, new Node("s"), new Node("t")));
		networkView.layoutGraph(networkView.createCircleGraphLayout());
		networkView.repaint();
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|/////////|/////
		 * / /|/        |/   /	A = algorithm text view
		 * /A/|/   C    |/ E /	B = legend view
		 * / /|/        |/   /	C = network view
		 * ///|/////////|/////	D = graph view (residual network)
		 * ---|---------|-----	E = execution table view (height(v))
		 * ///|/////////|/////	F = text area view (list last changes)
		 * /B/|/   D    |/ F /
		 * ///|/////////|/////
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
		cd.add(networkView);
		cd.add(residualNetworkView);
		cd.restoreWeights(config, "weights_cd", new float[] { 0.5f, 0.5f });
		
		// right group for E and F
		ef.add(heightView);
		ef.add(heightLastChangedView);
		ef.restoreWeights(config, "weights_ef", new float[] { 0.7f, 0.3f });
		
		// group for (A,B),(C,D) and (E,F)
		abcdef.add(ab);
		abcdef.add(cd);
		abcdef.add(ef);
		abcdef.restoreWeights(config, "weights_abcdef", new float[] { 0.3f, 0.5f, 0.2f });
		
		container.setLayout(new BorderLayout());
		container.add(abcdef, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		networkView.saveConfiguration(config, "networkView");
		algoTextView.saveConfiguration(config, "algoTextView");
		residualNetworkView.saveConfiguration(config, "residualNetworkView");
		heightView.saveConfiguration(config, "heightView");
		heightLastChangedView.saveConfiguration(config, "heightLastChangedView");
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
		networkView.reset();
		residualNetworkView.reset();
		heightView.reset();
		heightLastChangedView.reset();
		
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_SOURCE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SOURCE", langID, "Background color of the source node in the network"), colorSource));
		plm.add(new ColorProperty(CFGKEY_COLOR_SINK, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SINK", langID, "Background color of the sink node in the network"), colorSink));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		plm.add(new ColorProperty(CFGKEY_COLOR_V, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_V", langID, "Color of the node v"), colorV));
		plm.add(new ColorProperty(CFGKEY_COLOR_U, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_U", langID, "Color of the node u"), colorU));
		plm.add(new ColorProperty(CFGKEY_COLOR_FLOWCHANGED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_FLOWCHANGED", langID, "Color of the edge its flow is changed"), colorFlowChanged));
		plm.add(new ColorProperty(CFGKEY_COLOR_NEIGHBORS, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_NEIGHBORS", langID, "Color of the neighbors of the node v"), colorNeighbors));
		plm.add(new ColorProperty(CFGKEY_COLOR_EDGEVU, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_EDGEVU", langID, "Color of the edge between the nodes v and u"), colorEdgeVU));
		
		final NumericProperty lwV = new NumericProperty(CFGKEY_LINEWIDTH_V, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_V", langID, "Line width of the node v"), lineWidthV, true);
		lwV.setMinimum(1);
		lwV.setMaximum(5);
		plm.add(lwV);
		final NumericProperty lwU = new NumericProperty(CFGKEY_LINEWIDTH_U, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_U", langID, "Line width of the node u"), lineWidthU, true);
		lwU.setMinimum(1);
		lwU.setMaximum(5);
		plm.add(lwU);
		final NumericProperty lwEdgeVU = new NumericProperty(CFGKEY_LINEWIDTH_EDGEVU, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_EDGEVU", langID, "Line width of the edge between the nodes v and u"), lineWidthEdgeVU, true);
		lwEdgeVU.setMinimum(1);
		lwEdgeVU.setMaximum(5);
		plm.add(lwEdgeVU);
		final NumericProperty lwFlowChanged = new NumericProperty(CFGKEY_LINEWIDTH_FLOWCHANGED, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_FLOWCHANGED", langID, "Line width of the edge its flow is changed"), lineWidthFlowChanged, true);
		lwFlowChanged.setMinimum(1);
		lwFlowChanged.setMaximum(5);
		plm.add(lwFlowChanged);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorSource = config.addColor(CFGKEY_COLOR_SOURCE, plm.getColorProperty(CFGKEY_COLOR_SOURCE).getValue());
		colorSink = config.addColor(CFGKEY_COLOR_SINK, plm.getColorProperty(CFGKEY_COLOR_SINK).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		colorV = config.addColor(CFGKEY_COLOR_V, plm.getColorProperty(CFGKEY_COLOR_V).getValue());
		colorU = config.addColor(CFGKEY_COLOR_U, plm.getColorProperty(CFGKEY_COLOR_U).getValue());
		colorFlowChanged = config.addColor(CFGKEY_COLOR_FLOWCHANGED, plm.getColorProperty(CFGKEY_COLOR_FLOWCHANGED).getValue());
		colorNeighbors = config.addColor(CFGKEY_COLOR_NEIGHBORS, plm.getColorProperty(CFGKEY_COLOR_NEIGHBORS).getValue());
		colorEdgeVU = config.addColor(CFGKEY_COLOR_EDGEVU, plm.getColorProperty(CFGKEY_COLOR_EDGEVU).getValue());
		lineWidthV = config.addInt(CFGKEY_LINEWIDTH_V, plm.getNumericProperty(CFGKEY_LINEWIDTH_V).getValue().intValue());
		lineWidthU = config.addInt(CFGKEY_LINEWIDTH_U, plm.getNumericProperty(CFGKEY_LINEWIDTH_U).getValue().intValue());
		lineWidthEdgeVU = config.addInt(CFGKEY_LINEWIDTH_EDGEVU, plm.getNumericProperty(CFGKEY_LINEWIDTH_EDGEVU).getValue().intValue());
		lineWidthFlowChanged = config.addInt(CFGKEY_LINEWIDTH_FLOWCHANGED, plm.getNumericProperty(CFGKEY_LINEWIDTH_FLOWCHANGED).getValue().intValue());
		
		// recreate the legend
		createLegend();
	}
	
	@Override
	public ToolBarExtension[] getToolBarExtensions() {
		return null;
	}

	@Override
	public void save(File file) {
		try {
			if(vgfFileFilter.accept(file))
				networkView.save(file);
			else if(pngFileFilter.accept(file))
				networkView.saveAsPNG(file);
		}
		catch(IOException e) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_ERROR_SAVEFILE", langID, "File could not be saved!") + "\n\n" + e.getMessage(), LanguageFile.getLabel(langFile, "MSG_ERROR_SAVEFILE_TITLE", langID, "Save File"), MessageIcon.ERROR);
		}
	}

	@Override
	public void open(File file) {
		try {
			if(vgfFileFilter.accept(file))
				networkView.load(file);
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
		final CustomVisualFormula flowStrength = new CustomVisualFormula("w(f) = 0", 5, 5);
		final Network<Node, Arc> n = networkView.getGraph();

		residualNetworkView.setGraph(new ResidualNetwork(networkView.getGraph()));
		residualNetworkView.setVisible(false);
		
		networkView.setEditable(false);
		networkView.setVertexRenderer(new DefaultNodeRenderer());	// use the default node renderer to display the excesses
		networkView.addVisualObject(flowStrength);
		
		rte.setFlowStrengthDisplay(flowStrength);
		
		// create the columns for the nodes
		heightView.reset();
		for(int i = 0; i < n.getOrder(); i++)
			heightView.add(new ExecutionTableColumn(n.getVertex(i).getCaption(), n.getVertex(i).getID()));
		
		// reset the excesses and the flow
		for(int i = 0; i < n.getOrder(); i++)
			n.getVertex(i).setExcess(0.0f);
		for(int i = 0; i < n.getSize(); i++)
			n.getEdge(i).setFlow(0);
		
		networkView.repaint();
	}

	@Override
	public void beforeResume(RTEvent e) {
	}

	@Override
	public void beforePause(RTEvent e) {
	}

	@Override
	public void onStop() {
		networkView.setVertexRenderer(new DefaultVertexRenderer<Node>());	// reset the node renderer
		networkView.setEditable(true);
		networkView.removeAllVisualObjects();
		residualNetworkView.reset();
		rte.setFlowStrengthDisplay(null);
		residualNetworkView.setVisible(false);
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
		final AlgorithmParagraph drainageParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_DRAINAGE", langID, "3. Drainage:"), 3);
		final AlgorithmParagraph relabelParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_RELABEL", langID, "4. Relabel:"), 4);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INIT", langID, "Let _latex{$f$} be a preflow with _latex{$f(s,v) := u(s,v)$} if _latex{$(s,v) \\in E$} and _latex{$f(e) := 0$} otherwise.\n"), 1);
		
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_INIT", langID, "Set _latex{$height(s) = n$} (_latex{$n = |V|$}) and _latex{$height(v) = 0$} for all _latex{$v \\in V \\setminus \\{s\\}$}.\n\n"), 2);
		final Annotation step2_12_annotation = new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_12_ANNOTATION", langID, "The height of the source and the sink remains constant at <i>n</i> resp. 0 during the entire algorithm."));
		step.setAnnotation(step2_12_annotation);
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_STOP", langID, "Determine the excess for each node except _latex{$s$} and _latex{$t$}.\n"), 3);
		step.setAnnotation(new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_ANNOTATION", langID, "<b>Excess</b><br>Let <i>(G, u, s, t)</i> be a network with preflow <i>f</i>. For every node <i>v</i> except <i>s</i> and <i>t</i> depicts<br><img src=\"excess-def-en\"><br> the excess of <i>v</i>."), imgList));
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_STOP", langID, "If _latex{$excess(v) = 0$} for all _latex{$v \\in V \\setminus \\{s,t\\}$}, then stop.\n"), 4);
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_STOP", langID, "Otherwise choose the node _latex{$v$} with _latex{$excess(v) > 0$}, where the height _latex{$height(v)$} was changed last.\n\n"), 5);
		step.setAnnotation(new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_ANNOTATION", langID, "To determine which height of which node was changed last, you can specify a list that contains all nodes sequentially.<br> If the height of a node changes, this node is set to the beginning of the list.")));
		
		step = new AlgorithmStep(drainageParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_DRAINAGE", langID, "Let the excess of _latex{$v$} drain by implementing the following steps for all neighbors _latex{$u \\in V$} of _latex{$v$}:\n"), 6);
		
		step = new AlgorithmStep(drainageParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_DRAINAGE", langID, "If both conditions _latex{$u'(v,u) > 0$} and _latex{$height(v) > height(u)$} are fulfilled, "), 7, 4);
		step.setAnnotation(new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_ANNOTATION", langID, "<b>Meaning</b><br><ul><li>u'(v,u): there is capacity on the (residual) edge of <i>v</i> to <i>u</i>.<br>(The edge (v,u) must not exist in the network, but only in the residual network.)</li><li>height(v) > height(u): <i>v</i> is higher than <i>u</i>.</li></ul><b>Residual network</b><br>A residual network (G', u', s, t) to a flow f indicates the residual capacity of a network (G, u, s, t).<br>The weights u' for each edge (v, v') are defined as follows:<br>u'(v, v') := u(v, v') - f(v, v') as well as u'(v', v) := f(v, v')<br>The residual graph G' has the same vertex set as G and in addition to the forward edges e = (v, u) &isin; E with u'(e) > 0 the graph G' contains the backward edges e' = (u, v) too, if u'(e') > 0.<br><br><b>Example</b>:<br><table border=\"0\"><tr><td valign=\"top\">Network (G, u, s, t)</td><td valign=\"top\">Residual network (G', u', s, t)</td><td valign=\"top\"></td></tr><tr><td valign=\"top\"><img src=\"network\"></td><td valign=\"top\"><img src=\"residual-network\"></td><td valign=\"top\">Look at the red marked edges it is:<br>u'(s, 1) = u(s, 1) - f(s, 1) = 5 - 2 = 3 and u'(1, s) = f(s, 1) = 2<br>E.g. the edges (2, s) or (1, 2) are not available in the residual network,<br>because u'(2, s) = f(s, 2) = 0 and u'(1, 2) = u(1, 2) - f(1, 2) = 1 - 1 = 0.</td></tr></table>"), imgList));
		
		step = new AlgorithmStep(drainageParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP8_DRAINAGE", langID, "then set _latex{$f(v,u) = f(v,u) \\; + \\; min\\{excess(v), \\; u'(v,u)\\}$}. "), 8, 4);
		
		step = new AlgorithmStep(drainageParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP9_DRAINAGE", langID, "Determine the new _latex{$excess(v)$}.\n\n"), 9, 4);
		
		step = new AlgorithmStep(relabelParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP10_RELABEL", langID, "If _latex{$excess(v) = 0$} "), 10);
		
		step = new AlgorithmStep(relabelParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP11_RELABEL", langID, "then go to step 2.\n"), 11);
		
		step = new AlgorithmStep(relabelParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP12_RELABEL", langID, "Otherwise set _latex{$height(v) = height(v) + 1$} and go to step 3."), 12);
		step.setAnnotation(step2_12_annotation);
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_SOURCE", langID, "The node s (source)"), LegendItem.createCircleIcon(colorSource, Color.black, 1)));
		legendView.add(new LegendItem("item2", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_SINK", langID, "The node t (sink)"), LegendItem.createCircleIcon(colorSink, Color.black, 1)));
		legendView.add(new LegendItem("item3", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_V", langID, "The node v"), LegendItem.createCircleIcon(colorV, Color.black, lineWidthV)));
		legendView.add(new LegendItem("item4", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_U", langID, "The node u"), LegendItem.createCircleIcon(colorU, Color.black, lineWidthU)));
		legendView.add(new LegendItem("item5", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_NEIGHBORS", langID, "The neighbors of the node v"), LegendItem.createCircleIcon(colorNeighbors, Color.black, 1)));
		legendView.add(new LegendItem("item6", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_FLOWCHANGED", langID, "The edge its flow is changed"), LegendItem.createLineIcon(colorFlowChanged, lineWidthFlowChanged, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item7", residualNetworkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_RESIDUALNETWORK_V", langID, "The node v"), LegendItem.createCircleIcon(colorV, Color.black, 1)));
		legendView.add(new LegendItem("item8", residualNetworkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_RESIDUALNETWORK_U", langID, "The node u"), LegendItem.createCircleIcon(colorU, Color.black, 1)));
		legendView.add(new LegendItem("item9", residualNetworkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_RESIDUALNETWORK_EDGEVU", langID, "The edge u'(v,u) between the nodes v and u"), LegendItem.createLineIcon(colorEdgeVU, lineWidthEdgeVU, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item10", heightView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_HEIGHT_MODIFICATION", langID, "The height of a node becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item11", heightLastChangedView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_HEIGHTLASTCHANGED_MODIFICATION", langID, "The list becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
	}
	
	/**
	 * The runtime environment of the Relabel-to-front algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class RelabelToFrontRTE extends AlgorithmRTE {
		
		/** the height of the nodes */
		private Map<Integer, Integer> height;
		/** the list of vertices their height has changed last */
		private List<Integer> heightLastChanged;
		/** id of the vertex v */
		private int v;
		/** the list of neighbors of {@link #v} */
		private List<Integer> neighbors;
		/** the current neighbor */
		private int u;
		/** the capacity of the edge (v,u) in the residual network */
		private float u_apo_v_u;
		/** flag that indicates whether the user has defined a residual network in a related exercise */
		private boolean userDefinedResidualNetwork;
		/** the custom visual formula that displays the current flow strength */
		private CustomVisualFormula flowStrengthDisplay;
		
		public RelabelToFrontRTE() {
			super(RelabelToFrontAlgorithmPlugin.this, RelabelToFrontAlgorithmPlugin.this.algoText);
			
			userDefinedResidualNetwork = false;
		}
		
		/**
		 * Sets the custom visual object that should display the current flow strength of the network.
		 * 
		 * @param display the display or <code>null</code>
		 * @since 1.0
		 */
		public void setFlowStrengthDisplay(final CustomVisualFormula display) {
			flowStrengthDisplay = display;
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Network<Node, Arc> network = RelabelToFrontAlgorithmPlugin.this.networkView.getGraph();
			ResidualNetwork residualNetwork = RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getGraph();
			Node n;
			Arc a;
			int nextStepID = -1;
			GraphScene<Node, Arc> excess_scene;
			
			switch(stepID) {
				case 1:
					// Let f be a preflow with f(s,v) := u(s,v) if (s,v) in E and f(e) := 0 otherwise.
					
					// highlight the source and the sink in the network
					GraphView<Node, Arc>.VisualVertex source = RelabelToFrontAlgorithmPlugin.this.networkView.getVisualVertex(network.getSource());
					GraphView<Node, Arc>.VisualVertex sink = RelabelToFrontAlgorithmPlugin.this.networkView.getVisualVertex(network.getSink());
					source.setBackground(RelabelToFrontAlgorithmPlugin.this.colorSource);
					sink.setBackground(RelabelToFrontAlgorithmPlugin.this.colorSink);
					RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					
					sleep(750);
					
					GraphScene<Node, Arc> preflow_scene = new GraphScene<Node, Arc>(RelabelToFrontAlgorithmPlugin.this.networkView);
					preflow_scene.begin();
					
					final Set<Arc> E = network.getEdgeSet();
					
					for(int i = 0; i < E.size(); i++) {
						a = E.get(i);
						a.setFlow((a.getPredecessor() == network.getSource()) ? a.getWeight() : 0.0f);
					}
					
					preflow_scene.end(true);
					asa.addAttachment("preflow_scene", preflow_scene);
					
					RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					
					nextStepID = 2;
					break;
				case 2:
					// Set height(s) = n (n = |V|) and height(v) = 0 for all v in V \ {s}.
					
					heightLastChanged.clear();
					
					for(int i = 0; i < network.getOrder(); i++) {
						n = network.getVertex(i);
						height.put(n.getID(), (n == network.getSource()) ? network.getOrder() : 0);
						
						// the height of the source and sink does not change during the execution
						if(n != network.getSource() && n != network.getSink())
							heightLastChanged.add(n.getID());
					}
					
					sleep(1000);
					
					// visualize the current height(v)
					RelabelToFrontAlgorithmPlugin.this.heightView.add(new ExecutionTableItem(height, true));
					
					sleep(250);
					
					// visualize the last changed list
					RelabelToFrontAlgorithmPlugin.this.heightLastChangedView.setBackground(RelabelToFrontAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeHeightLastChanged();
					sleep(250);
					RelabelToFrontAlgorithmPlugin.this.heightLastChangedView.setBackground(Color.white);
					
					sleep(250);
					
					nextStepID = 3;
					break;
				case 3:
					// Determine the excess for each vertex except s and t.
					
					sleep(750);
					
					excess_scene = new GraphScene<Node, Arc>(RelabelToFrontAlgorithmPlugin.this.networkView);
					excess_scene.begin();
					
					network.determineExcesses();
					
					excess_scene.end(true);
					asa.addAttachment("excess_scene", excess_scene);
					
					RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					
					sleep(750);
					
					nextStepID = 4;
					break;
				case 4:
					// If for every node v in V \ {s,t}, excess(v) = 0 applies, then stop.
					
					sleep(1000);
					
					boolean stop = true;
					
					for(int i = 0; i < network.getOrder(); i++) {
						n = network.getVertex(i);
						
						if(n == network.getSource() || n == network.getSink())
							continue;
						
						if(n.getExcess() != 0.0f)
							stop = false;
						
						if(!stop)
							break;
					}
					
					if(stop)
						nextStepID = -1;
					else
						nextStepID = 5;
					break;
				case 5:
					// Otherwise choose the node v with excess(v) > 0, where the height height(v) was changed last.
					
					v = -1;
					u = -1;
					neighbors = null;
					
					for(int i = 0; i < heightLastChanged.size(); i++) {
						n = network.getVertexByID(heightLastChanged.get(i));
						if(n != null && n.getExcess() > 0.0f)
							v = n.getID();
						
						if(v >= 0)
							break;
					}
					
					if(v < 0)
						nextStepID = -1;
					else {
						// visualize the node v
						sleep(250);
						visualizeNodes();
						sleep(750);
						
						// clear and activate the residual network for the further execution
						RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setGraph(new ResidualNetwork(network));
						RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setVisible(true);
						RelabelToFrontAlgorithmPlugin.this.residualNetworkView.repaint();
						
						nextStepID = 6;
					}
					break;
				case 6:
					// Let the excess of v drain, as for every node u in V, which is the neighbor of v, implement the following:
					
					if(neighbors == null) {
						Edge e;
						n = network.getVertexByID(v);
						neighbors = new ArrayList<Integer>(n.getOutgoingEdgeCount());
						
						// neighbors = predecessors or successors of v
						for(int i = 0; i < n.getIncidentEdgeCount(); i++) {
							e = n.getIncidentEdge(i);
							if(e.getPredecessor() == n)
								neighbors.add(e.getSuccessor().getID());
							else
								neighbors.add(e.getPredecessor().getID());
						}
					}
					else {
						if(neighbors.isEmpty())
							neighbors = null;
					}
					
					// create a vertex transfer protocol so that the vertices of the residual network can adopt the positions of their complements
					// in the network or if the user has defined a residual network then create a VTP of this graph
					final VertexOnlyTransferProtocol<?, ?> votp = userDefinedResidualNetwork ? new VertexOnlyTransferProtocol<Vertex, RNEdge>(RelabelToFrontAlgorithmPlugin.this.residualNetworkView, false) : new VertexOnlyTransferProtocol<Node, Arc>(RelabelToFrontAlgorithmPlugin.this.networkView, false, true);
					votp.prepare();
					
					// attach the VTP so that the previous residual network can be restored using the vertex positions
					asa.addAttachment("votp", votp);

					// only compute a residual network if necessary meaning if the exercise mode is not enabled or the user could not
					// solve the related exercise
					if(!userDefinedResidualNetwork) {
						// compute the residual network
						residualNetwork = network.getResidualNetwork();
						
						sleep(500);
						
						// visualize the residual network
						RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setGraph(residualNetwork);
						RelabelToFrontAlgorithmPlugin.this.residualNetworkView.transferGraph(votp);
					}
					
					// get next neighbor u
					if(neighbors != null) {
						u = neighbors.remove(0);
						
						// get the capacity of the edge (v,u) in the residual network
						final Vertex _v = residualNetwork.getVertexByCaption(network.getVertexByID(v).getCaption());
						final Vertex _u = residualNetwork.getVertexByCaption(network.getVertexByID(u).getCaption());
						final RNEdge e = (_v != null && _u != null) ? residualNetwork.getEdge(_v, _u) : null;
						u_apo_v_u = (e != null) ? e.getWeight() : 0.0f;
					}
					else {
						u = -1;
						u_apo_v_u = 0.0f;
					}
					
					if(u < 0)
						nextStepID = 10;
					else {
						// visualize data in network and residual network
						sleep(500);
						visualizeNodes();
						visualizeVerticesInResidualNetwork();
						visualizeEdgesInResidualNetwork();
						sleep(750);
						
						nextStepID = 7;
					}
					break;
				case 7:
					// If both conditions u'(v,u) > 0 and height(v) > height(u) are fulfilled,
					
					sleep(750);
					
					if(u_apo_v_u > 0 && height.get(v) > height.get(u))
						nextStepID = 8;
					else
						nextStepID = 6;
					break;
				case 8:
					// then set f(v,u) = f(v,u) + min{excess(v), u'(v,u)}
					
					GraphScene<Node, Arc> networkScene = new GraphScene<Node, Arc>(networkView);
					networkScene.begin();
					
					n = network.getVertexByID(v);
					a = network.getEdge(v, u);
					boolean drainBack = (a == null);
					
					// arc does not exist? then choose (u,v) to let the flow drain back to v
					if(drainBack)
						a = network.getEdge(u, v);
					
					final GraphView<Node, Arc>.VisualEdge va = RelabelToFrontAlgorithmPlugin.this.networkView.getVisualEdge(a);
					if(va != null) {
						va.setColor(RelabelToFrontAlgorithmPlugin.this.colorFlowChanged);
						va.setLineWidth(RelabelToFrontAlgorithmPlugin.this.lineWidthFlowChanged);
						RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					}
					
					sleep(250);
					
					// change flow and visualize flow strength (which repaints the network view)
					if(!drainBack)
						a.setFlow(a.getFlow() + Math.min(n.getExcess(), u_apo_v_u));
					else
						a.setFlow(a.getFlow() - Math.min(n.getExcess(), u_apo_v_u));
					visualizeFlowStrength();
					
					sleep(250);
					
					if(va != null) {
						va.setColor(GraphView.DEF_EDGECOLOR);
						va.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
						RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					}
					
					networkScene.end(true);
					asa.addAttachment("network_scene", networkScene);
					
					nextStepID = 9;
					break;
				case 9:
					// and determine the new excess(v).
					
					sleep(750);
					
					excess_scene = new GraphScene<Node, Arc>(RelabelToFrontAlgorithmPlugin.this.networkView);
					excess_scene.begin();
					
					n = network.getVertexByID(v);
					Node.determineExcess(n);
					
					excess_scene.end(true);
					asa.addAttachment("excess_scene", excess_scene);
					
					RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					
					sleep(250);
					
					nextStepID = 6;
					break;
				case 10:
					// If excess(v) = 0
					
					// clear residual network
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setVisible(false);
					
					sleep(1000);
					
					n = network.getVertexByID(v);
					if(n.getExcess() == 0.0f)
						nextStepID = 11;
					else
						nextStepID = 12;
					break;
				case 11:
					// then go to step 2.
					
					v = -1;
					u = -1;
					neighbors = null;
					
					visualizeNodes();
					
					sleep(1000);
					
					nextStepID = 3;
					break;
				case 12:
					// Otherwise set height(v) = height(v) + 1 and go to step 3.
					
					final ExecutionTableItem item = new ExecutionTableItem(height, true);
					RelabelToFrontAlgorithmPlugin.this.heightView.add(item);
					RelabelToFrontAlgorithmPlugin.this.heightView.repaint();
					
					height.put(v, height.get(v) + 1);
					moveToFront(v, heightLastChanged);
					
					// visualize the changes
					sleep(250);
					item.setCellBackgroundByID(v, RelabelToFrontAlgorithmPlugin.this.colorModified);
					RelabelToFrontAlgorithmPlugin.this.heightLastChangedView.setBackground(RelabelToFrontAlgorithmPlugin.this.colorModified);
					sleep(250);
					item.setCellDataByID(height);
					visualizeHeightLastChanged();
					sleep(250);
					RelabelToFrontAlgorithmPlugin.this.heightLastChangedView.setBackground(Color.white);
					item.setCellBackgroundByID(v, Color.white);
					sleep(250);
					
					// make the residual network visible again
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setVisible(true);
					
					nextStepID = 6;
					break;
			}
			
			return nextStepID;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addMap("height", height);
			state.addList("heightLastChanged", heightLastChanged);
			state.addInt("v", v);
			state.addList("neighbors", neighbors);
			state.addInt("u", u);
			state.addFloat("u_apo_v_u", u_apo_v_u);
			
			if(state.getStepID() == 11) {
				// store the last version of the residual network so that it can be restored
				state.addAttachment("residual_network", RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getGraph());
			}
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			height = state.getMap("height");
			heightLastChanged = state.getList("heightLastChanged");
			v = state.getInt("v");
			neighbors = state.getList("neighbors");
			u = state.getInt("u");
			u_apo_v_u = state.getFloat("u_apo_v_u");
			
			// if their is a residual network that has to be restored do it before the path is restored because the path is
			// based onto the residual network
			final Graph<Vertex, RNEdge> graph = state.getAttachment("residual_network");
			final VertexOnlyTransferProtocol<?, ?> votp = state.getAttachment("votp");
			if(graph != null && votp != null) {
				RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setGraph(graph);
				if(graph.getOrder() > 0)
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.transferGraph(votp);
				RelabelToFrontAlgorithmPlugin.this.residualNetworkView.repaint();
			}
			
			GraphScene<Node, Arc> preflow_scene = state.getAttachment("preflow_scene");
			if(preflow_scene != null)
				preflow_scene.reverse();
			
			GraphScene<Node, Arc> excess_scene = state.getAttachment("excess_scene");
			if(excess_scene != null)
				excess_scene.reverse();
			final GraphScene<Vertex, Edge> network_scene = state.getAttachment("network_scene");
			if(network_scene != null)
				network_scene.reverse();
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			height = state.addMap("height", new HashMap<Integer, Integer>());
			heightLastChanged = state.addList("heightLastChanged", new ArrayList<Integer>());
			v = state.addInt("v", -1);
			neighbors = state.addList("neighbors", null);
			u = state.addInt("u", -1);
			u_apo_v_u = state.addFloat("u_apo_v_u", 0.0f);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			switch(stepID) {
				case 1:
				case 3:
					RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					break;
				case 2:
				case 12:
					RelabelToFrontAlgorithmPlugin.this.heightView.remove(RelabelToFrontAlgorithmPlugin.this.heightView.getLastItem());
					visualizeHeightLastChanged();
					break;
				case 5:
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setVisible(false);
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.repaint();
					visualizeNodes();
					break;
				case 6:
					visualizeNodes();
					visualizeVerticesInResidualNetwork();
					visualizeEdgesInResidualNetwork();
					break;
				case 8:
					visualizeFlowStrength();
					break;
				case 9:
					RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
					break;
				case 10:
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.setVisible(true);
					RelabelToFrontAlgorithmPlugin.this.residualNetworkView.repaint();
					break;
				case 11:
					visualizeNodes();
					break;
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			
		}

		@Override
		protected View[] getViews() {
			return new View[] { RelabelToFrontAlgorithmPlugin.this.networkView, RelabelToFrontAlgorithmPlugin.this.residualNetworkView };
		}
		
		/**
		 * Visualizes the nodes in the network which are v, u and the current neighbors.
		 * 
		 * @since 1.0
		 */
		private void visualizeNodes() {
			final Network<Node, Arc> network = RelabelToFrontAlgorithmPlugin.this.networkView.getGraph();
			GraphView<Node, Arc>.VisualVertex vv;
			
			for(int i = 0; i < RelabelToFrontAlgorithmPlugin.this.networkView.getVisualVertexCount(); i++) {
				vv = RelabelToFrontAlgorithmPlugin.this.networkView.getVisualVertex(i);
				
				if(vv.getVertex().getID() == v) {
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorV);
					vv.setEdgeWidth(RelabelToFrontAlgorithmPlugin.this.lineWidthV);
				}
				else if(vv.getVertex().getID() == u) {
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorU);
					vv.setEdgeWidth(RelabelToFrontAlgorithmPlugin.this.lineWidthU);
				}
				else if(neighbors != null && neighbors.contains(vv.getVertex().getID())) {
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorNeighbors);
					vv.setEdgeWidth(1);
				}
				else if(vv.getVertex() == network.getSource()) {
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorSource);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
				else if(vv.getVertex() == network.getSink()) {
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorSink);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
				else {
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
			}
			
			RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
		}
		
		/**
		 * Visualizes the source and sink node as well as the current v, u in the residual nertwork.
		 * <br><br>
		 * <b>Note</b>:<br>
		 * All other nodes are painted in the default background color.
		 * 
		 * @since 1.0
		 */
		private void visualizeVerticesInResidualNetwork() {
			final Network<Node, Arc> network = RelabelToFrontAlgorithmPlugin.this.networkView.getGraph();
			final ResidualNetwork residualNetwork = RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getGraph();
			
			GraphView<Vertex, RNEdge>.VisualVertex vv;
			final Node v = network.getVertexByID(this.v);
			final Node u = network.getVertexByID(this.u);
			final Vertex _v = (v != null) ? residualNetwork.getVertexByCaption(v.getCaption()) : null;
			final Vertex _u = (u != null) ? residualNetwork.getVertexByCaption(u.getCaption()) : null;
			
			for(int i = 0; i < RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getVisualVertexCount(); i++) {
				vv = RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getVisualVertex(i);
				
				if(vv.getVertex() == _v)
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorV);
				else if(vv.getVertex() == _u)
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorU);
				else if(vv.getVertex().getCaption().equals(network.getSource().getCaption()))
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorSource);
				else if(vv.getVertex().getCaption().equals(network.getSink().getCaption()))
					vv.setBackground(RelabelToFrontAlgorithmPlugin.this.colorSink);
				else
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
				
				vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
			}
			
			RelabelToFrontAlgorithmPlugin.this.residualNetworkView.repaint();
		}
		
		/**
		 * Visualizes the edge between the nodes v and u.
		 * <br><br>
		 * <b>Note/b>:<br>
		 * All other edges are painted in the default color.
		 * 
		 * @since 1.0
		 */
		private void visualizeEdgesInResidualNetwork() {
			final Network<Node, Arc> network = RelabelToFrontAlgorithmPlugin.this.networkView.getGraph();
			final ResidualNetwork residualNetwork = RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getGraph();
			final Node v = network.getVertexByID(this.v);
			final Node u = network.getVertexByID(this.u);
			final Vertex _v = (v != null) ? residualNetwork.getVertexByCaption(v.getCaption()) : null;
			final Vertex _u = (u != null) ? residualNetwork.getVertexByCaption(u.getCaption()) : null;
			final RNEdge e = (_v != null && _u != null) ? residualNetwork.getEdge(_v, _u) : null;
			GraphView<Vertex, RNEdge>.VisualEdge ve;
			
			for(int i = 0; i < RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getVisualEdgeCount(); i++) {
				ve = RelabelToFrontAlgorithmPlugin.this.residualNetworkView.getVisualEdge(i);
				
				if(ve.getEdge() == e) {
					ve.setColor(RelabelToFrontAlgorithmPlugin.this.colorEdgeVU);
					ve.setLineWidth(RelabelToFrontAlgorithmPlugin.this.lineWidthEdgeVU);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			RelabelToFrontAlgorithmPlugin.this.residualNetworkView.repaint();
		}
		
		/**
		 * Visualizes the list of last changes to height(v).
		 * 
		 * @since 1.0
		 */
		private void visualizeHeightLastChanged() {
			final Network<Node, Arc> network = RelabelToFrontAlgorithmPlugin.this.networkView.getGraph();
			final StringBuilder sb = new StringBuilder();
			Node n;
			
			for(Integer id : heightLastChanged) {
				n = network.getVertexByID(id.intValue());
				
				if(sb.length() > 0)
					sb.append(", ");
				
				sb.append(n.getCaption());
			}
			
			heightLastChangedView.setText(sb.toString());
		}
		
		/**
		 * Visualizes the flow strength of the network using the {@link #flowStrengthDisplay}.
		 * 
		 * @since 1.0
		 */
		private void visualizeFlowStrength() {
			if(flowStrengthDisplay == null)
				return;

			flowStrengthDisplay.setExpression("w(f) = " + RelabelToFrontAlgorithmPlugin.this.networkView.getGraph().getFlowStrength());
			RelabelToFrontAlgorithmPlugin.this.networkView.repaint();
		}
		
		/**
		 * Moves an element to the front.
		 * 
		 * @param n the element
		 * @param l the list
		 * @since 1.0
		 */
		private void moveToFront(final int n, final List<Integer> l) {
			int index = -1;
			
			// find index of n
			for(int i = 0; i < l.size(); i++) {
				if(l.get(i).equals(n)) {
					index = i;
					break;
				}
			}
			
			if(index < 0)
				return;
			
			// shift the elements before n to the right and set n at the front
			for(int i = index; i > 0; i--)
				l.set(i, l.get(i - 1));
			l.set(0, n);
		}
		
	}

}
