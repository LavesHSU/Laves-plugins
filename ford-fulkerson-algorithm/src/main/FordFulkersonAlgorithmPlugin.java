package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
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
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultNetworkView;
import lavesdk.algorithm.plugin.views.DefaultRNView;
import lavesdk.algorithm.plugin.views.GraphScene;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.LegendView;
import lavesdk.algorithm.plugin.views.VertexOnlyTransferProtocol;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.plugin.views.GraphView.SelectionType;
import lavesdk.algorithm.plugin.views.custom.CustomVisualFormula;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.algorithm.text.Annotation;
import lavesdk.algorithm.text.AnnotationImagesList;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.MultiGraph;
import lavesdk.math.graph.Path;
import lavesdk.math.graph.PathByID;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.network.Arc;
import lavesdk.math.graph.network.Network;
import lavesdk.math.graph.network.Node;
import lavesdk.math.graph.network.RNEdge;
import lavesdk.math.graph.network.ResidualNetwork;
import lavesdk.math.graph.network.enums.FlowType;
import lavesdk.utils.GraphUtils;
import lavesdk.utils.MathUtils;

/**
 * Plugin that visualizes and teaches users the Ford-Fulkerson algorithm.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class FordFulkersonAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the view that displays the algorithm text */
	private AlgorithmTextView algoTextView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the Ford-Fulkerson algorithm */
	private FordFulkersonRTE rte;
	/** the list of annotation images */
	private AnnotationImagesList imgList;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for C and D (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cd;
	/** the view group for A,B,C and D (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcd;
	
	// modifiable visualization data
	/** color to visualize the source of the network */
	private Color colorSource;
	/** color to visualize the sink of the network */
	private Color colorSink;
	/** color to visualize the path in the residual network */
	private Color colorPath;
	/** color to visualize the current edge its flow is modified */
	private Color colorCurrEdge;
	/** line width of the path in the residual network */
	private int lineWidthPath;
	/** line width of the edge in the path with a minimum weight */
	private int lineWidthMinWeightEdge;
	
	/** configuration key for the {@link #colorSource} */
	private static final String CFGKEY_COLOR_SOURCE = "colorSource";
	/** configuration key for the {@link #colorSink} */
	private static final String CFGKEY_COLOR_SINK = "colorSink";
	/** configuration key for the {@link #colorPath} */
	private static final String CFGKEY_COLOR_PATH = "colorPath";
	/** configuration key for the {@link #colorCurrEdge} */
	private static final String CFGKEY_COLOR_CURREDGE = "colorCurrEdge";
	/** configuration key for the {@link #lineWidthPath} */
	private static final String CFGKEY_LINEWIDTH_PATH = "lineWidthPath";
	/** configuration key for the {@link #lineWidthMinWeightEdge} */
	private static final String CFGKEY_LINEWIDTH_MINWEIGHTEDGE = "lineWidthMinWeightEdge";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langFordFulkerson.txt"));
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
		imgList.add("path", resLoader.getResource("main/resources/path.png"));
		imgList.add("network-flow-changed", resLoader.getResource("main/resources/network-flow-changed.png"));
		
		// create plugin
		this.host = host;
		this.config = (config != null) ? config : new Configuration();
		this.vgfFileFilter = new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf");
		this.pngFileFilter = new FileNameExtensionFilter("Portable Network Graphic (*.png)", "png");
		this.networkView = new DefaultNetworkView(LanguageFile.getLabel(langFile, "VIEW_NETWORK_TITLE", langID, "Network"), new Network<Node, Arc>(FlowType.FLOW, new Node("s"), new Node("s")), null, true, langFile, langID);
		this.residualNetworkView = new DefaultRNView(LanguageFile.getLabel(langFile, "VIEW_RESIDUALNETWORK_TITLE", langID, "Residual Network"), langFile, langID, networkView);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new FordFulkersonRTE();
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		
		// the residual network may never be edited by the user
		residualNetworkView.setEditable(false);
		
		// load the visualization colors from the configuration of the plugin
		colorSource = this.config.getColor(CFGKEY_COLOR_SOURCE, new Color(70, 155, 215));
		colorSink = this.config.getColor(CFGKEY_COLOR_SINK, new Color(135, 195, 235));
		colorPath = this.config.getColor(CFGKEY_COLOR_SOURCE, new Color(200, 145, 145));
		colorCurrEdge = this.config.getColor(CFGKEY_COLOR_CURREDGE, new Color(255, 220, 80));
		lineWidthPath = this.config.getInt(CFGKEY_LINEWIDTH_PATH, 2);
		lineWidthMinWeightEdge = this.config.getInt(CFGKEY_LINEWIDTH_MINWEIGHTEDGE, 4);
		
		// load view configurations
		networkView.loadConfiguration(config, "networkView");
		algoTextView.loadConfiguration(config, "algoTextView");
		residualNetworkView.loadConfiguration(config, "residualNetworkView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Ford-Fulkerson algorithm");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A network (G, u, s, t) with flow f.");
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
		// set a new network in the view
		networkView.setGraph(new Network<Node, Arc>(FlowType.FLOW, new Node("s"), new Node("t")));
		networkView.layoutGraph(networkView.createCircleGraphLayout());
		networkView.repaint();
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|///////////////
		 * / /|/             /	A = algorithm text view
		 * /A/|/     C       /	B = legend view
		 * / /|/             /	C = network view
		 * ///|///////////////	D = graph view (residual network)
		 * ---|---------------
		 * ///|///////////////
		 * /B/|/     D       /
		 * ///|///////////////
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		cd = new ViewGroup(ViewGroup.VERTICAL);
		abcd = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// middle group for C and D
		cd.add(networkView);
		cd.add(residualNetworkView);
		cd.restoreWeights(config, "weights_cd", new float[] { 0.5f, 0.5f });
		
		// group for (A,B) and (C,D)
		abcd.add(ab);
		abcd.add(cd);
		abcd.restoreWeights(config, "weights_abcd", new float[] { 0.4f, 0.6f });
		
		container.setLayout(new BorderLayout());
		container.add(abcd, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		networkView.saveConfiguration(config, "networkView");
		algoTextView.saveConfiguration(config, "algoTextView");
		residualNetworkView.saveConfiguration(config, "residualNetworkView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(cd != null)
			cd.storeWeights(config, "weights_cd");
		if(abcd != null)
			abcd.storeWeights(config, "weights_abcd");
		
		// reset view content where it is necessary
		networkView.reset();
		residualNetworkView.reset();
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
		plm.add(new ColorProperty(CFGKEY_COLOR_PATH, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_PATH", langID, "Color of the path in the residual network"), colorPath));
		plm.add(new ColorProperty(CFGKEY_COLOR_CURREDGE, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURREDGE", langID, "Color of the current edge its flow is modified"), colorCurrEdge));
		
		final NumericProperty lwPath = new NumericProperty(CFGKEY_LINEWIDTH_PATH, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_PATH", langID, "Line width of the path in the residual network"), lineWidthPath, true);
		lwPath.setMinimum(1);
		lwPath.setMaximum(5);
		plm.add(lwPath);
		final NumericProperty lwMinWeightEdge = new NumericProperty(CFGKEY_LINEWIDTH_MINWEIGHTEDGE, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_MINWEIGHTEDGE", langID, "Line width of the edge in the path with a minimum weight"), lineWidthMinWeightEdge, true);
		lwMinWeightEdge.setMinimum(lwPath.getMinimum());
		lwMinWeightEdge.setMaximum(lwPath.getMaximum() + 3);
		plm.add(lwMinWeightEdge);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorSource = config.addColor(CFGKEY_COLOR_SOURCE, plm.getColorProperty(CFGKEY_COLOR_SOURCE).getValue());
		colorSink = config.addColor(CFGKEY_COLOR_SINK, plm.getColorProperty(CFGKEY_COLOR_SINK).getValue());
		colorPath = config.addColor(CFGKEY_COLOR_PATH, plm.getColorProperty(CFGKEY_COLOR_PATH).getValue());
		colorCurrEdge = config.addColor(CFGKEY_COLOR_CURREDGE, plm.getColorProperty(CFGKEY_COLOR_CURREDGE).getValue());
		lineWidthPath = config.addInt(CFGKEY_LINEWIDTH_PATH, plm.getNumericProperty(CFGKEY_LINEWIDTH_PATH).getValue().intValue());
		lineWidthMinWeightEdge = config.addInt(CFGKEY_LINEWIDTH_MINWEIGHTEDGE, plm.getNumericProperty(CFGKEY_LINEWIDTH_MINWEIGHTEDGE).getValue().intValue());
		
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
		final Network<Node, Arc> network = networkView.getGraph();
		
		if(!network.checkFlowConservationCondition()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDFLOW", langID, "You have entered an invalid flow!\nThe flow does not complies with the flow conservation condition."), LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDFLOW_TITLE", langID, "Invalid flow"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			final CustomVisualFormula flowStrength = new CustomVisualFormula("w(f) = " + network.getFlowStrength(), 5, 5);
	
			residualNetworkView.setGraph(new ResidualNetwork(network));
			residualNetworkView.repaint();
			
			networkView.setEditable(false);
			networkView.addVisualObject(flowStrength);
			
			rte.setFlowStrengthDisplay(flowStrength);
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
		networkView.setEditable(true);
		networkView.removeAllVisualObjects();
		residualNetworkView.reset();
		rte.setFlowStrengthDisplay(null);
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
		final AlgorithmParagraph rnParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_RESIDUALNETWORK", langID, "1. Residual Network:"), 1);
		final AlgorithmParagraph apParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_AUGMENTINGPATH", langID, "2. Augmenting Path:"), 2);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOP", langID, "3. Stop:"), 3);
		final AlgorithmParagraph expParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_EXPANSION", langID, "4. Expansion:"), 4);
		
		// 1. residual network
		step = new AlgorithmStep(rnParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1", langID, "Determine the residual network using _latex{$f$}.\n\n"), 1);
		step.setAnnotation(new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_ANNOTATION", langID, "<b>Residual network</b><br>A residual network (G', u', s, t) to a flow f indicates the residual capacity of a network (G, u, s, t).<br>The weights u' for each edge (v, v') are defined as follows:<br>u'(v, v') := u(v, v') - f(v, v') as well as u'(v', v) := f(v, v')<br>The residual graph G' has the same vertex set as G and in addition to the forward edges e = (v, u) &isin; E with u'(e) > 0 the graph G' contains the backward edges e' = (u, v) too, if u'(e') > 0.<br><br><b>Example</b>:<br><table border=\"0\"><tr><td valign=\"top\">Network (G, u, s, t)</td><td valign=\"top\">Residual network (G', u', s, t)</td><td valign=\"top\"></td></tr><tr><td valign=\"top\"><img src=\"network\"></td><td valign=\"top\"><img src=\"residual-network\"></td><td valign=\"top\">Look at the red marked edges it is:<br>u'(s, 1) = u(s, 1) - f(s, 1) = 5 - 2 = 3 and u'(1, s) = f(s, 1) = 2<br>E.g. the edges (2, s) or (1, 2) are not available in the residual network,<br>because u'(2, s) = f(s, 2) = 0 and u'(1, 2) = u(1, 2) - f(1, 2) = 1 - 1 = 0.</td></tr></table>"), imgList));
		step.setExercise(new AlgorithmExercise<Graph<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1", langID, "Determine the residual network."), 2.0f, residualNetworkView) {

			private boolean createNewGraph = true;
			
			@Override
			protected void exited() {
				createNewGraph = true;
			}
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				if(createNewGraph) {
					FordFulkersonAlgorithmPlugin.this.residualNetworkView.setGraph(new ResidualNetwork(FordFulkersonAlgorithmPlugin.this.networkView.getGraph()));
					FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
					// disable the create flag so that the graph is not deleted if the user enters a false solution
					createNewGraph = false;
				}
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setEditable(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setEditable(false);
			}
			
			@Override
			protected Graph<?, ?>[] requestSolution() {
				return new Graph<?, ?>[] { FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph() };
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Graph<?, ?>[] solutions) {
				state.addBoolean("userDefinedResidualNetwork", true);
			}

			@Override
			protected boolean examine(Graph<?, ?>[] results, AlgorithmState state) {
				final MultiGraph<Vertex, RNEdge> rn = FordFulkersonAlgorithmPlugin.this.networkView.getGraph().getResidualNetwork();
				return results[0].equals(rn);
			}
			
		});
		
		step = new AlgorithmStep(apParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2", langID, "Determine a path _latex{$w$} from _latex{$s$} to _latex{$t$} in the residual network, if such a path exists.\n\n"), 2);
		step.setExercise(new AlgorithmExercise<Path<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Determine a path <i>w</i> (<i>select the nodes of the path in the residual network one after another so that a valid path develops or select nothing if their is no path</i>)."), 1.0f, residualNetworkView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setSelectionType(SelectionType.VERTICES_ONLY);
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setSelectionType(SelectionType.BOTH);
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setShowCursorToolAlways(false);
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.deselectAll();
			}
			
			@Override
			protected Path<?>[] requestSolution() {
				// if their is nothing selected then the user thinks that their is no path so return an empty path
				if(FordFulkersonAlgorithmPlugin.this.residualNetworkView.getSelectedVertexCount() == 0)
					return new Path<?>[] { new Path<Vertex>(FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph()) };
				else {
					Path<Vertex> p = new Path<Vertex>(FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph());
					
					// create the path based on the selection order of the vertices
					try {
						for(int i = 0; i < FordFulkersonAlgorithmPlugin.this.residualNetworkView.getSelectedVertexCount(); i++)
							p.add(FordFulkersonAlgorithmPlugin.this.residualNetworkView.getSelectedVertex(i).getVertex());
					}
					catch(IllegalArgumentException e) {
						// invalid path? then show a message
						FordFulkersonAlgorithmPlugin.this.host.showMessage(FordFulkersonAlgorithmPlugin.this, LanguageFile.getLabel(FordFulkersonAlgorithmPlugin.this.langFile, "EXERCISE_STEP2_MSG_ERR_NOPATH", FordFulkersonAlgorithmPlugin.this.langID, "The specified nodes do not describe a valid path in the residual network!%nChoose the nodes one after another so that a valid path develops."), LanguageFile.getLabel(FordFulkersonAlgorithmPlugin.this.langFile, "EXERCISE_STEP2_MSG_ERR_NOPATH_TITLE", FordFulkersonAlgorithmPlugin.this.langID, "Invalid path"), MessageIcon.ERROR);
						// exercise has to be repeated
						p = null;
					}
					
					if(p != null)
						return new Path<?>[] { p };
					else
						return null;
				}
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Path<?>[] solutions) {
				@SuppressWarnings("unchecked")
				final Path<Vertex> w = (Path<Vertex>)solutions[0];
				state.addPath("w", w.cast());
			}
			
			@Override
			protected boolean examine(Path<?>[] results, AlgorithmState state) {
				final Network<Node, Arc> network = FordFulkersonAlgorithmPlugin.this.networkView.getGraph();
				final Graph<Vertex, RNEdge> residualNetwork = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph();
				final Path<Vertex> p = GraphUtils.findShortestPathFromTo(residualNetwork, residualNetwork.getVertexByCaption(network.getSource().getCaption()), residualNetwork.getVertexByCaption(network.getSink().getCaption()));
				@SuppressWarnings("unchecked")
				final Path<Vertex> w = (Path<Vertex>)results[0];
				
				if(w.length() == 0)
					return p.length() == 0;
				else
					return p.length() > 0 && w.get(0).getCaption().equals(network.getSource().getCaption()) && w.get(w.length()).getCaption().equals(network.getSink().getCaption());
			}
		});
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_STOP", langID, "If their is no such a path _latex{$w$} "), 3);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Will the algorithm stop?"), 1.0f) {
			
			private final String labelYes = LanguageFile.getLabel(FordFulkersonAlgorithmPlugin.this.langFile, "EXERCISE_STEP3_YES", FordFulkersonAlgorithmPlugin.this.langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(FordFulkersonAlgorithmPlugin.this.langFile, "EXERCISE_STEP3_NO", FordFulkersonAlgorithmPlugin.this.langID, "No");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelYes);
				final JRadioButton rdobtn2 = new JRadioButton(labelNo);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(FordFulkersonAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, FordFulkersonAlgorithmPlugin.this.langFile, FordFulkersonAlgorithmPlugin.this.langID))
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
				final PathByID<Vertex> path = state.getPath("w", FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph());
				
				return (results[0] != null && results[0] == (path == null || path.length() == 0));
			}
		});

		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_STOP", langID, "then stop. Flow _latex{$f$} has maximum strength.\n\n"), 4);
		
		step = new AlgorithmStep(expParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5", langID, "For each edge _latex{$e$} in the path _latex{$w$} set: _latex{$f(e) = f(e) \\; + \\; \\underset{e \\in w}{min} u'(e)$} and go to step 1."), 5);
		step.setAnnotation(new Annotation(LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_ANNOTATION", langID, "<b>Example</b><br><img src=\"path\">&nbsp;&nbsp;&nbsp;&nbsp;<img src=\"network-flow-changed\"><br>Figure 1 shows a found path <i>w</i> (marked red) in the residual network.<br>The minimum weight is 1 = min{u'(s, 1), u'(1, 3), u'(3, t)} = min{2, 1, 3}.<br>Figure 2 shows the changed flow f in the network for each edge of the path: f(s, 1) = 3 + 1 = 4, f(1, 3) = 3 + 1 = 4 and f(3, t) = 4 + 1."), imgList));
		step.setExercise(new AlgorithmExercise<Map<?, ?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP5", langID, "What is the flow <i>f</i> in the network after this step?<br>(<i>Tip: you can select an arc and enter the flow using the keyboard</i>)"), 2.0f, networkView) {
			
			private GraphScene<Node, Arc> scene;
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				FordFulkersonAlgorithmPlugin.this.networkView.setSelectionType(SelectionType.EDGES_ONLY);
				FordFulkersonAlgorithmPlugin.this.networkView.setHideGraphToolsAlways(true);
				FordFulkersonAlgorithmPlugin.this.networkView.setApplyInputToFlow(true);
				// disable the restorable edit mode so that the initial state of the network is not restored now
				FordFulkersonAlgorithmPlugin.this.networkView.setRestorableEditMode(false);
				FordFulkersonAlgorithmPlugin.this.networkView.setEditable(true);
				
				scene = new GraphScene<Node, Arc>(FordFulkersonAlgorithmPlugin.this.networkView);
				scene.begin();
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				FordFulkersonAlgorithmPlugin.this.networkView.setSelectionType(SelectionType.BOTH);
				FordFulkersonAlgorithmPlugin.this.networkView.setHideGraphToolsAlways(false);
				FordFulkersonAlgorithmPlugin.this.networkView.setApplyInputToFlow(false);
				FordFulkersonAlgorithmPlugin.this.networkView.setEditable(false);
				// enable the restorable flag only after disabling the edit mode otherwise the initial state of the network view cannot be restored
				FordFulkersonAlgorithmPlugin.this.networkView.setRestorableEditMode(true);
				
				// restore the state of the network before the user has modified the flow of the edges
				scene.end(true);
				scene.reverse();
			}
			
			@Override
			protected Map<?, ?>[] requestSolution() {
				final Network<Node, Arc> network = FordFulkersonAlgorithmPlugin.this.networkView.getGraph();
				final Map<Integer, Float> flow = new HashMap<Integer, Float>();
				
				for(int i = 0; i < network.getSize(); i++)
					flow.put(network.getEdge(i).getID(), network.getEdge(i).getFlow());
				
				return new Map<?, ?>[]{ flow };
			}
			
			@Override
			protected String getResultAsString(Map<?, ?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else {
					final Network<Node, Arc> network = FordFulkersonAlgorithmPlugin.this.networkView.getGraph();
					final StringBuilder s = new StringBuilder();
					@SuppressWarnings("unchecked")
					final Map<Integer, Float> flow = (Map<Integer, Float>)result;
					final Iterator<Integer> it = flow.keySet().iterator();
					Arc arc;
					Float f;
					boolean delimiter = false;
					
					s.append("[");
					while(it.hasNext()) {
						if(delimiter)
							s.append(", ");
						
						arc = network.getEdgeByID(it.next());
						f = flow.get(arc.getID());
						s.append("(" + arc.getPredecessor() + "," + arc.getSuccessor() + ") " + MathUtils.formatFloat(f.floatValue()) + "/" + MathUtils.formatFloat(arc.getWeight()));
						delimiter = true;
					}
					s.append("]");
					
					return s.toString();
				}
			}
			
			@Override
			protected boolean examine(Map<?, ?>[] results, AlgorithmState state) {
				final Network<Node, Arc> network = FordFulkersonAlgorithmPlugin.this.networkView.getGraph();
				@SuppressWarnings("unchecked")
				final Map<Integer, Float> flow = (Map<Integer, Float>)results[0];
				Float f;

				for(int i = 0; i < network.getSize(); i++) {
					f = flow.get(network.getEdge(i).getID());
					if(f == null || f.floatValue() != network.getEdge(i).getFlow())
						return false;
				}
				
				return true;
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
		
		legendView.add(new LegendItem("item1", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_SOURCE", langID, "The node s (source)"), LegendItem.createCircleIcon(colorSource, Color.black, 1)));
		legendView.add(new LegendItem("item2", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_SINK", langID, "The node t (sink)"), LegendItem.createCircleIcon(colorSink, Color.black, 1)));
		legendView.add(new LegendItem("item3", networkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_NETWORK_CURREDGE", langID, "The current edge its flow becomes modified"), LegendItem.createLineIcon(colorCurrEdge, lineWidthPath, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item4", residualNetworkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_RESIDUALNETWORK_PATH", langID, "The path w"), LegendItem.createLineIcon(colorPath, lineWidthPath, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item5", residualNetworkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_RESIDUALNETWORK_MINWEIGHTEDGE", langID, "The edge in the path w with a minimum weight"), LegendItem.createLineIcon(colorPath, lineWidthMinWeightEdge, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item6", residualNetworkView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_RESIDUALNETWORK_CURREDGE", langID, "The current edge of the path w"), LegendItem.createLineIcon(colorCurrEdge, lineWidthPath, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
	}
	
	/**
	 * The runtime environment of the Ford-Fulkerson algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.1
	 */
	private class FordFulkersonRTE extends AlgorithmRTE {
		
		/** the path w in the residual network */
		private Path<Vertex> w;
		/** flag that indicates whether the user has defined a residual network in a related exercise */
		private boolean userDefinedResidualNetwork;
		/** the user's choice of the path w */
		private Path<Vertex> userChoiceW;
		/** the custom visual formula that displays the current flow strength */
		private CustomVisualFormula flowStrengthDisplay;
		
		public FordFulkersonRTE() {
			super(FordFulkersonAlgorithmPlugin.this, FordFulkersonAlgorithmPlugin.this.algoText);
			
			userDefinedResidualNetwork = false;
			userChoiceW = null;
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
			final Network<Node, Arc> network = FordFulkersonAlgorithmPlugin.this.networkView.getGraph();
			ResidualNetwork residualNetwork = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph();
			int nextStep = -1;
			
			switch(stepID) {
				case 1:
					// determine the residual network
					
					// clear the current path because the base (residual network) is changing
					w = null;
					
					// highlight the source and the sink in the network
					GraphView<Node, Arc>.VisualVertex source = FordFulkersonAlgorithmPlugin.this.networkView.getVisualVertex(network.getSource());
					GraphView<Node, Arc>.VisualVertex sink = FordFulkersonAlgorithmPlugin.this.networkView.getVisualVertex(network.getSink());
					source.setBackground(FordFulkersonAlgorithmPlugin.this.colorSource);
					sink.setBackground(FordFulkersonAlgorithmPlugin.this.colorSink);
					FordFulkersonAlgorithmPlugin.this.networkView.repaint();

					// create a vertex transfer protocol so that the vertices of the residual network can adopt the positions of their complements
					// in the network or if the user has defined a residual network then create a VTP of this graph
					final VertexOnlyTransferProtocol<?, ?> votp = userDefinedResidualNetwork ? new VertexOnlyTransferProtocol<Vertex, RNEdge>(FordFulkersonAlgorithmPlugin.this.residualNetworkView, false) : new VertexOnlyTransferProtocol<Node, Arc>(FordFulkersonAlgorithmPlugin.this.networkView, false);
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
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.setGraph(residualNetwork);
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.transferGraph(votp);
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
					}
					
					// highlight the source and the sink in the residual network
					visualizeVerticesInResidualNetwork();
					
					sleep(1000);
					
					// clear the flag
					userDefinedResidualNetwork = false;
					
					nextStep = 2;
					break;
				case 2:
					// determine a path w from s to t in the residual network
					
					// if the user solved the related exercise then tak his choice of a path otherwise find one
					if(userChoiceW != null)
						w = userChoiceW;
					else
						w = GraphUtils.findShortestPathFromTo(residualNetwork, residualNetwork.getVertexByCaption(network.getSource().getCaption()), residualNetwork.getVertexByCaption(network.getSink().getCaption()));
					
					sleep(250);
					
					// visualize the path
					visualizePathInResidualNetwork();
					
					sleep(1000);
					
					nextStep = 3;
					break;
				case 3:
					// if their is no such path w then go to step 4 (stepid) otherwise go to step 5 (stepid)
					
					sleep(750);
					
					if(w == null || w.length() == 0)
						nextStep = 4;
					else
						nextStep = 5;
					break;
				case 4:
					// then stop
					
					sleep(750);
					
					nextStep = -1;
					break;
				case 5:
					// for each edge e in w: f(e) = f(e) + min u'(e)
					//										e in w
					
					GraphView<Vertex, RNEdge>.VisualEdge veMinWeight = null;
					GraphView<Vertex, RNEdge>.VisualEdge veRN;
					GraphView<Node, Arc>.VisualEdge veN;
					Vertex v;
					Vertex u;
					RNEdge e;
					Arc arc;
					float minWeight = Float.MAX_VALUE;
					
					// find the minimum weight u'(e)
					for(int i = 1; i <= w.length(); i++) {
						e = residualNetwork.getEdge(w.get(i - 1), w.get(i));
						
						if(e.getWeight() < minWeight) {
							minWeight = e.getWeight();
							veMinWeight = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getVisualEdge(e);
						}
					}
					
					sleep(250);
					
					// highlight the edge with a minimum weight
					if(veMinWeight != null) {
						veMinWeight.setLineWidth(FordFulkersonAlgorithmPlugin.this.lineWidthMinWeightEdge);
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
					}
					
					sleep(250);
					
					// record the modifications of the flow
					final GraphScene<Node, Arc> networkScene = new GraphScene<Node, Arc>(FordFulkersonAlgorithmPlugin.this.networkView);
					networkScene.begin();
					
					boolean reflow;
					
					for(int i = 1; i <= w.length(); i++) {
						v = w.get(i - 1);
						u = w.get(i);
						arc = network.getEdge(network.getVertexByCaption(v.getCaption()), network.getVertexByCaption(u.getCaption()));
						if(arc == null) {
							// arc v -> u does not exist? then choose the reverse one u -> v
							arc = network.getEdge(network.getVertexByCaption(u.getCaption()), network.getVertexByCaption(v.getCaption()));
							// that is, the flow must reflow
							reflow = true;
						}
						else
							reflow = false;
						
						veRN = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getVisualEdge(residualNetwork.getEdge(v, u));
						veN = FordFulkersonAlgorithmPlugin.this.networkView.getVisualEdge(arc);
						
						// highlight the current edge in the residual network
						veRN.setColor(FordFulkersonAlgorithmPlugin.this.colorCurrEdge);
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
						
						sleep(500);
						
						// highlight the current edge in the network
						veN.setColor(FordFulkersonAlgorithmPlugin.this.colorCurrEdge);
						veN.setLineWidth(FordFulkersonAlgorithmPlugin.this.lineWidthPath);
						FordFulkersonAlgorithmPlugin.this.networkView.repaint();

						sleep(250);
						
						// modify the flow
						arc.setFlow(!reflow ? arc.getFlow() + minWeight : arc.getFlow() - minWeight);
						FordFulkersonAlgorithmPlugin.this.networkView.repaint();
						
						sleep(500);
						
						// remove the highlight
						veRN.setColor(FordFulkersonAlgorithmPlugin.this.colorPath);
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
						veN.setColor(GraphView.DEF_EDGECOLOR);
						veN.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
						FordFulkersonAlgorithmPlugin.this.networkView.repaint();
						
						sleep(250);
					}
					
					networkScene.end(true);
					asa.addAttachment("network_scene", networkScene);
					
					// remove the highlight from the edge with a minimum weight
					if(veMinWeight != null) {
						veMinWeight.setLineWidth(FordFulkersonAlgorithmPlugin.this.lineWidthPath);
						FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
					}
					
					visualizeFlowStrength();
					
					nextStep = 1;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			if(state.getStepID() == 1) {
				// if the state of the first step is stored then attach the current graph of the residual network so that this graph
				// can be restored (because in step 1 a new graph is created)
				state.addAttachment("residual_network", FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph());
			}
			state.addPath("w", (w != null) ? w.cast() : null);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			// if their is a residual network that has to be restored do it before the path is restored because the path is
			// based onto the residual network
			final Graph<Vertex, RNEdge> graph = state.getAttachment("residual_network");
			final VertexOnlyTransferProtocol<?, ?> votp = state.getAttachment("votp");
			if(graph != null && votp != null) {
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.setGraph(graph);
				if(graph.getOrder() > 0)
					FordFulkersonAlgorithmPlugin.this.residualNetworkView.transferGraph(votp);
				FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
			}
			
			final PathByID<Vertex> p = state.getPath("w", FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph());
			w = (p != null) ? p.cast() : null;
			final GraphScene<Vertex, Edge> network_scene = state.getAttachment("network_scene");
			if(network_scene != null)
				network_scene.reverse();
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			state.addPath("w", null);
			w = null;
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			switch(stepID) {
				case 1:
					visualizeVerticesInResidualNetwork();
					break;
				case 2:
				case 5:
					visualizePathInResidualNetwork();
					visualizeFlowStrength();
					break;
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			switch(stepID) {
				case 1:
					userDefinedResidualNetwork = state.getBoolean("userDefinedResidualNetwork");
					break;
				case 2:
					final PathByID<Vertex> p = state.getPath("w", FordFulkersonAlgorithmPlugin.this.residualNetworkView.getGraph());
					userChoiceW = (p != null) ? p.cast() : null;
					break;
			}
		}

		@Override
		protected View[] getViews() {
			return new View[] { FordFulkersonAlgorithmPlugin.this.networkView, FordFulkersonAlgorithmPlugin.this.residualNetworkView };
		}
		
		/**
		 * Visualizes the source and sink node in the residual nertwork.
		 * 
		 * @since 1.0
		 */
		private void visualizeVerticesInResidualNetwork() {
			final Network<Node, Arc> network = FordFulkersonAlgorithmPlugin.this.networkView.getGraph();
			
			final GraphView<Vertex, RNEdge>.VisualVertex source = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getVisualVertexByCaption(network.getSource().getCaption());
			final GraphView<Vertex, RNEdge>.VisualVertex sink = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getVisualVertexByCaption(network.getSink().getCaption());
			
			if(source != null && sink != null) {
				source.setBackground(FordFulkersonAlgorithmPlugin.this.colorSource);
				sink.setBackground(FordFulkersonAlgorithmPlugin.this.colorSink);
			}
			
			FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
		}
		
		/**
		 * Visualizes the path {@link #w} in the residual network.
		 * 
		 * @since 1.0
		 */
		private void visualizePathInResidualNetwork() {
			GraphView<Vertex, RNEdge>.VisualEdge ve;
			
			for(int i = 0; i < FordFulkersonAlgorithmPlugin.this.residualNetworkView.getVisualEdgeCount(); i++) {
				ve = FordFulkersonAlgorithmPlugin.this.residualNetworkView.getVisualEdge(i);
				
				if(w != null && w.contains(ve.getEdge())) {
					ve.setColor(FordFulkersonAlgorithmPlugin.this.colorPath);
					ve.setLineWidth(FordFulkersonAlgorithmPlugin.this.lineWidthPath);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			FordFulkersonAlgorithmPlugin.this.residualNetworkView.repaint();
 		}
		
		/**
		 * Visualizes the flow strength of the network using the {@link #flowStrengthDisplay}.
		 * 
		 * @since 1.0
		 */
		private void visualizeFlowStrength() {
			if(flowStrengthDisplay == null)
				return;

			flowStrengthDisplay.setExpression("w(f) = " + FordFulkersonAlgorithmPlugin.this.networkView.getGraph().getFlowStrength());
			FordFulkersonAlgorithmPlugin.this.networkView.repaint();
		}
		
	}

}
