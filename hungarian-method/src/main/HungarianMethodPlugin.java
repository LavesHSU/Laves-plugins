package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import lavesdk.algorithm.plugin.extensions.BipartiteGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.BipartiteLayoutToolBarExtension;
import lavesdk.algorithm.plugin.extensions.CompleteBipartiteGraphToolBarExtension;
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
import lavesdk.algorithm.plugin.views.renderers.DefaultEdgeRenderer;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.algorithm.text.Annotation;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
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
import lavesdk.math.graph.matching.Matching;
import lavesdk.math.graph.matching.MatchingByID;
import lavesdk.utils.GraphUtils;

/**
 * Plugin that visualizes and teaches users the Hungarian method.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class HungarianMethodPlugin implements AlgorithmPlugin {
	
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
	/** the view that shows the set S */
	private TextAreaView setView;
	/** the view that shows the matching */
	private TextAreaView matchingView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the algorithm */
	private HungarianRTE rte;
	/** toolbar extension to check whether a graph is bipartite */
	private BipartiteGraphToolBarExtension<Vertex, Edge> bipartiteExt;
	/** toolbar extension to check whether a graph is complete bipartite or to create one */
	private CompleteBipartiteGraphToolBarExtension<Vertex, Edge> completeBipartiteExt;
	/** toolbar extension to layout a graph bipartite */
	private BipartiteLayoutToolBarExtension<Vertex, Edge> bipartiteLayoutExt;
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
	/** color to visualize matched edges */
	private Color colorMatchedEdges;
	/** color to visualize vertices of set S */
	private Color colorSetS;
	/** color to visualize the current vertex of set S (v_s) */
	private Color colorCurrVertex;
	/** color to visualize an augmenting path */
	private Color colorAugmentingPath;
	/** line width of matched edges */
	private int lineWidthMatchedEdges;
	/** line width of the current vertex of set S (v_s) */
	private int lineWidthCurrVertex;
	
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #colorMatchedEdges} */
	private static final String CFGKEY_COLOR_MATCHEDEDGES = "colorMatchedEdges";
	/** configuration key for the {@link #colorSetS} */
	private static final String CFGKEY_COLOR_SETS = "colorSetS";
	/** configuration key for the {@link #colorAugmentingPath} */
	private static final String CFGKEY_COLOR_AUGMENTINGPATH = "colorAugmentingPath";
	/** configuration key for the {@link #colorCurrVertex} */
	private static final String CFGKEY_COLOR_CURRVERTEX = "colorCurrVertex";
	/** configuration key for the {@link #lineWidthMatchedEdges} */
	private static final String CFGKEY_LINEWIDTH_MATCHEDEDGES = "lineWidthMatchedEdges";
	/** configuration key for the {@link #lineWidthCurrVertex} */
	private static final String CFGKEY_LINEWIDTH_CURRVERTEX = "lineWidthCurrVertex";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langHungarian.txt"));
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
		this.setView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_SET_TITLE", langID, "Set S"), true, langFile, langID);
		this.matchingView = new TextAreaView(LanguageFile.getLabel(langFile, "VIEW_MATCHING_TITLE", langID, "Matching M"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new HungarianRTE();
		this.bipartiteExt = new BipartiteGraphToolBarExtension<Vertex, Edge>(graphView, langFile, langID, true);
		this.completeBipartiteExt = new CompleteBipartiteGraphToolBarExtension<Vertex, Edge>(host, graphView, langFile, langID, false);
		this.bipartiteLayoutExt = new BipartiteLayoutToolBarExtension<Vertex, Edge>(graphView, false, langFile, langID, false);
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		setView.setAutoRepaint(true);
		matchingView.setAutoRepaint(true);
		
		// the labels should not be painted in the graph
		graphView.setEdgeRenderer(new DefaultEdgeRenderer<Edge>(false));
		
		// load the visualization colors from the configuration of the plugin
		colorSetS = this.config.getColor(CFGKEY_COLOR_SETS, new Color(180, 210, 230));
		colorCurrVertex = this.config.getColor(CFGKEY_COLOR_CURRVERTEX, new Color(255, 230, 105));
		colorMatchedEdges = this.config.getColor(CFGKEY_COLOR_MATCHEDEDGES, Color.black);
		colorAugmentingPath = this.config.getColor(CFGKEY_COLOR_AUGMENTINGPATH, new Color(90, 190, 20));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		lineWidthCurrVertex = this.config.getInt(CFGKEY_LINEWIDTH_CURRVERTEX, 2);
		lineWidthMatchedEdges = this.config.getInt(CFGKEY_LINEWIDTH_MATCHEDEDGES, 3);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		setView.loadConfiguration(config, "execTableView");
		matchingView.loadConfiguration(config, "setsView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "Hungarian method");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "Finds a maximal matching in a graph.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A bipartite graph G = (V<sub>1</sub> &cup; V<sub>2</sub>, E), where V<sub>1</sub> is the partition with fewer or equivalent elements and a matching M (poss. M = &Oslash;).");
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
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extensions to check whether the created graph is bipartite or complete bipartite, to create a complete bipartite graph<br>by indicating the number of vertices in the particular subsets or you can arrange the vertices of your created graph in a predefined layout.<br><br><b>Starting the algorithm</b>:<br>Before you start the algorithm you can indicate a matching the algorithm should begin with by selecting the edges in the graph.<br>If you don't select anything the algorithm starts with M = &Oslash;.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
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
		 * ///|///////////
		 * /A/|/    C    /	A = algorithm text view
		 * ///|///////////	B = legend view
		 * ---|-----|-----	C = graph view
		 * ///|/////|/////	D = set view
		 * /B/|/ D /|/ E /	E = matching view
		 * ///|/////|/////
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		de = new ViewGroup(ViewGroup.HORIZONTAL);
		cde = new ViewGroup(ViewGroup.VERTICAL);
		abcde = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// bottom right group for D and E
		de.add(setView);
		de.add(matchingView);
		de.restoreWeights(config, "weights_de", new float[] { 0.5f, 0.5f });
		
		// group for C and (D,E)
		cde.add(graphView);
		cde.add(de);
		cde.restoreWeights(config, "weights_cde", new float[] { 0.7f, 0.3f });
		
		// group for (A,B) and (C, (D,E))
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
		setView.saveConfiguration(config, "setView");
		matchingView.saveConfiguration(config, "matchingView");
		
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
		setView.reset();
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
		plm.add(new ColorProperty(CFGKEY_COLOR_SETS, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_SETS", langID, "Background color of the vertices of set S"), colorSetS));
		plm.add(new ColorProperty(CFGKEY_COLOR_CURRVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURRVERTEX", langID, "Background color of the vertex v<sub>s</sub>"), colorCurrVertex));
		plm.add(new ColorProperty(CFGKEY_COLOR_MATCHEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MATCHEDEDGES", langID, "Color of the matching edges"), colorMatchedEdges));
		plm.add(new ColorProperty(CFGKEY_COLOR_AUGMENTINGPATH, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_AUGMENTINGPATH", langID, "Color of the augmenting path"), colorAugmentingPath));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwCurrVertex = new NumericProperty(CFGKEY_LINEWIDTH_CURRVERTEX, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_CURRVERTEX", langID, "Line width of the vertex v<sub>s</sub>"), lineWidthCurrVertex, true);
		lwCurrVertex.setMinimum(1);
		lwCurrVertex.setMaximum(5);
		plm.add(lwCurrVertex);
		final NumericProperty lwMatchedEdge = new NumericProperty(CFGKEY_LINEWIDTH_MATCHEDEDGES, LanguageFile.getLabel(langFile, "CUSTOMIE_LINEWIDTH_MATCHEDEDGES", langID, "Line width of the matching edges"), lineWidthMatchedEdges, true);
		lwMatchedEdge.setMinimum(1);
		lwMatchedEdge.setMaximum(5);
		plm.add(lwMatchedEdge);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorSetS = config.addColor(CFGKEY_COLOR_SETS, plm.getColorProperty(CFGKEY_COLOR_SETS).getValue());
		colorCurrVertex = config.addColor(CFGKEY_COLOR_CURRVERTEX, plm.getColorProperty(CFGKEY_COLOR_CURRVERTEX).getValue());
		colorMatchedEdges = config.addColor(CFGKEY_COLOR_MATCHEDEDGES, plm.getColorProperty(CFGKEY_COLOR_MATCHEDEDGES).getValue());
		colorAugmentingPath = config.addColor(CFGKEY_COLOR_AUGMENTINGPATH, plm.getColorProperty(CFGKEY_COLOR_AUGMENTINGPATH).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthCurrVertex = config.addInt(CFGKEY_LINEWIDTH_CURRVERTEX, plm.getNumericProperty(CFGKEY_LINEWIDTH_CURRVERTEX).getValue().intValue());
		lineWidthMatchedEdges = config.addInt(CFGKEY_LINEWIDTH_MATCHEDEDGES, plm.getNumericProperty(CFGKEY_LINEWIDTH_MATCHEDEDGES).getValue().intValue());
		
		// recreate the legend
		createLegend();
	}

	@Override
	public ToolBarExtension[] getToolBarExtensions() {
		return new ToolBarExtension[] { bipartiteExt, completeBipartiteExt, bipartiteLayoutExt };
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
		// request the matching the user wants to start with
		final Matching<Edge> m = getInputMatching();
		
		// the graph may be bipartite to execute the algorithm
		// furthermore check whether the input matching
		if(!isGraphBipartite()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NOTBIPARTITE", langID, "The created graph is not bipartite!/nThe Hungarian method can only be applied to bipartite graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NOTBIPARTITE_TITLE", langID, "No bipartite graph"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(m == null) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDMATCHING", langID, "The selected edges do not result in a valid matching!/nDeselect the edges to start with a zero matching."), LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDMATCHING_TITLE", langID, "Invalid matching"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			graphView.setEditable(false);
			graphView.deselectAll();
			
			setView.reset();
			matchingView.reset();
			
			// set the matching the user has entered to start with (after the edit mode is disabled and the matching view is reset
			// because the matching should be visualized)
			rte.setStartMatching(m);
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
		
		// define an exercise for the steps 1, 5 and 8
		final AlgorithmExercise<Set<?>> step1_5_8 = new AlgorithmExercise<Set<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1_5_8", langID, "What is <i>S</i>?"), 1.0f) {
			
			@Override
			protected Set<?>[] requestSolution() {
				final SolutionEntry<JTextField> entryS = new SolutionEntry<JTextField>("S=", new JTextField());
				
				if(!SolveExercisePane.showDialog(HungarianMethodPlugin.this.host, this, new SolutionEntry<?>[] { entryS }, HungarianMethodPlugin.this.langFile, HungarianMethodPlugin.this.langID, LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_HINT_SETINPUT", HungarianMethodPlugin.this.langID, "Use a comma as the delimiter!")))
					return null;
				
				final ElementParser<String> parser = new StringElementParser();
				final Set<String> S = Set.parse(entryS.getComponent().getText(), parser);
				
				return new Set<?>[] { S };
			}
			
			@Override
			protected String getResultAsString(Set<?> result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return "S=" + super.getResultAsString(result, index);
			}
			
			@Override
			protected boolean examine(Set<?>[] results, AlgorithmState state) {
				// convert the input set to a set with the identifiers of the vertices to use auto examination
				final Set<Integer> S = HungarianMethodPlugin.this.toIDs(results[0]);
				return doAutoExamine(state, new String[] { "S" }, new Set<?>[] { S });
			}
		};
		
		// create paragraphs
		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_INITIALIZATION", langID, "1. Initialization:"), 1);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOPCRITERION", langID, "2. Stop criterion:"), 2);
		final AlgorithmParagraph apParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_AUGMENTINGPATH", langID, "3. Augmenting Path:"), 3);
		final AlgorithmParagraph improveParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_IMPROVEMENT", langID, "4. Improvement:"), 4);
		
		// 1. initialization
		step = new AlgorithmStep(initParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_INIT", langID, "Let _latex{$S \\subseteq V_1$} be the set of vertices from _latex{$V_1$} that are not endpoint of an matching edge.\n\n"), 1);
		step.setExercise(step1_5_8);
		
		// 2. stop criterion
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_STOP", langID, "If _latex{$S = \\emptyset$} then _latex{$M$} is maximal. Stop.\n\n"), 2);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "Will the algorithm stop?"), 1.0f) {
			
			private final String labelYes = LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_STEP2_YES", HungarianMethodPlugin.this.langID, "Yes");
			private final String labelNo = LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_STEP2_NO", HungarianMethodPlugin.this.langID, "No");
			
			@Override
			protected Boolean[] requestSolution() {
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton(labelYes);
				final JRadioButton rdobtn2 = new JRadioButton(labelNo);
				
				group.add(rdobtn1);
				group.add(rdobtn2);
				
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<JRadioButton>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<JRadioButton>("", rdobtn2);
				
				if(!SolveExercisePane.showDialog(HungarianMethodPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, HungarianMethodPlugin.this.langFile, HungarianMethodPlugin.this.langID))
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
				final Set<Integer> S = state.getSet("S");
				
				return (results[0] != null && results[0] == S.isEmpty());
			}
		});
		
		// 3. augmenting path
		step = new AlgorithmStep(apParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_CHOOSEVERTEX", langID, "Choose a vertex _latex{$v_s \\in S$}.\n"), 3);
		step.setExercise(new AlgorithmExercise<Integer>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Select v<sub>s</sub>."), 1.0f, graphView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				HungarianMethodPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				HungarianMethodPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				HungarianMethodPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				HungarianMethodPlugin.this.graphView.setShowCursorToolAlways(false);
				HungarianMethodPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Integer[] requestSolution() {
				if(HungarianMethodPlugin.this.graphView.getSelectedVertexCount() != 1)
					return null;
				else
					return new Integer[] { HungarianMethodPlugin.this.graphView.getSelectedVertex(0).getVertex().getID() };
			}
			
			@Override
			protected String getResultAsString(Integer result, int index) {
				if(result == null)
					return super.getResultAsString(result, index);
				else
					return HungarianMethodPlugin.this.graphView.getVisualVertexByID(result.intValue()).getVertex().getCaption();
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, Integer[] solutions) {
				state.addInt("v_s", solutions[0]);
			}
			
			@Override
			protected boolean examine(Integer[] results, AlgorithmState state) {
				final Set<Integer> S = state.getSet("S");
				return S.contains(results[0]);
			}
		});
		
		step = new AlgorithmStep(apParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_FINDAUGMENTINGPATH", langID, "Starting from _latex{$v_s$} find any path that is eligible as an augmenting path.\n"), 4);
		step.setExercise(new AlgorithmExercise<Path<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "Find an augmenting path if possible and select the related vertices in the graph one after another so that a correct path develops (<i>if their is no augmenting path then do not choose anything</i>)."), 2.0f, graphView) {
			
			@Override
			public boolean hasInputHint() {
				return true;
			}
			
			@Override
			public Annotation getInputHintMessage(LanguageFile langFile, String langID) {
				return new Annotation(LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_STEP4_INPUTHINT", langID, "<b>Select an augmenting path</b>:<br>Select the vertices of the augmenting path in the graph one after another by using the mouse, while pressing the <b>Ctrl</b>-key<br>on your keyboard.<br>Make sure that a correct path develops. Afterwards click on the \"Solve Exercise\"-button of the task."));
			}
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				HungarianMethodPlugin.this.graphView.setSelectionType(SelectionType.VERTICES_ONLY);
				HungarianMethodPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				HungarianMethodPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				HungarianMethodPlugin.this.graphView.setShowCursorToolAlways(false);
				HungarianMethodPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Path<?>[] requestSolution() {
				// if their is nothing selected then the user thinks that their is no augmenting path so return an empty path
				if(HungarianMethodPlugin.this.graphView.getSelectedVertexCount() == 0)
					return new Path<?>[] { new Path<Vertex>(HungarianMethodPlugin.this.graphView.getGraph()) };
				else {
					Path<Vertex> p = new Path<Vertex>(HungarianMethodPlugin.this.graphView.getGraph());
					
					// create the path based on the selection order of the vertices
					try {
						for(int i = 0; i < HungarianMethodPlugin.this.graphView.getSelectedVertexCount(); i++)
							p.add(HungarianMethodPlugin.this.graphView.getSelectedVertex(i).getVertex());
					}
					catch(IllegalArgumentException e) {
						// invalid path? then show a message
						HungarianMethodPlugin.this.host.showMessage(HungarianMethodPlugin.this, LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_STEP4_MSG_ERR_NOPATH", HungarianMethodPlugin.this.langID, "The specified vertices do not describe a valid path in the graph!%nChoose the vertices one after another so that a valid path develops."), LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_STEP4_MSG_ERR_NOPATH_TITLE", HungarianMethodPlugin.this.langID, "Invalid path"), MessageIcon.ERROR);
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
				final Path<Vertex> p = (Path<Vertex>)solutions[0];
				state.addPath("AP", p.cast());
			}
			
			@Override
			protected boolean examine(Path<?>[] results, AlgorithmState state) {
				@SuppressWarnings("unchecked")
				final Path<Vertex> p = (Path<Vertex>)results[0];
				final Graph<Vertex, Edge> g = HungarianMethodPlugin.this.graphView.getGraph();
				final Vertex start = g.getVertexByID(state.getInt("v_s"));
				final Matching<Edge> m = state.getMatching("M", g).cast();
				
				// the first vertex of the path is not the start vertex? then the exercise fails
				if(p.length() > 0 && p.get(0) != start)
					return false;
				
				final Path<Vertex> ap = GraphUtils.findAugmentingPath(g, start, m);
				
				// if their is no augmenting path in the graph check whether the user believe this too
				// otherwise check whether the user selects an augmenting path
				if(ap == null)
					return p.length() == 0;
				else
					return GraphUtils.isAugmentingPath(p, m);
			}
		});
		
		step = new AlgorithmStep(apParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_NOAUGMENTINGPATH", langID, "If no such augmenting path can be found then put _latex{$S = S \\setminus \\{v_s\\}$} and go to 2. "), 5);
		step.setExercise(step1_5_8);
		
		step = new AlgorithmStep(apParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP6_AUGMENTINGPATH", langID, "Otherwise choose an arbitrary augmenting path (from the just determined) and go to step 4.\n\n"), 6);
		
		// 4. improvement
		step = new AlgorithmStep(improveParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP7_IMPROVE1", langID, "Enlarge the matching with the found augmenting path.\n"), 7);
		step.setExercise(new AlgorithmExercise<Matching<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP7", langID, "What is <i>M</i> after this step (<i>select all matched edges in the graph</i>)?"), 1.0f, graphView) {
			
			@Override
			public boolean hasInputHint() {
				return true;
			}
			
			@Override
			public Annotation getInputHintMessage(LanguageFile langFile, String langID) {
				return new Annotation(LanguageFile.getLabel(HungarianMethodPlugin.this.langFile, "EXERCISE_STEP7_INPUTHINT", langID, "<b>Select matched edges</b>:<br>Select the matched edges in the graph by using the mouse and pressing the <b>Ctrl</b>-key on your keyboard.<br>Afterwards click on the \"Solve Exercise\"-button of the task."));
			}
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				HungarianMethodPlugin.this.graphView.setSelectionType(SelectionType.EDGES_ONLY);
				HungarianMethodPlugin.this.graphView.setShowCursorToolAlways(true);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				HungarianMethodPlugin.this.graphView.setSelectionType(SelectionType.BOTH);
				HungarianMethodPlugin.this.graphView.setShowCursorToolAlways(false);
				HungarianMethodPlugin.this.graphView.deselectAll();
			}
			
			@Override
			protected Matching<?>[] requestSolution() {
				// if their are no edges selected then break up
				if(HungarianMethodPlugin.this.graphView.getSelectedEdgeCount() == 0)
					return null;
				
				Matching<Edge> m = new Matching<Edge>(HungarianMethodPlugin.this.graphView.getGraph());
				
				try {
					// add the selected edges to the matching
					for(int i = 0; i < HungarianMethodPlugin.this.graphView.getSelectedEdgeCount(); i++)
						m.add(HungarianMethodPlugin.this.graphView.getSelectedEdge(i).getEdge());
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
				final MatchingByID<Edge> m = state.getMatching("M", HungarianMethodPlugin.this.graphView.getGraph());
				return results[0].cast().equals(m);
			}
		});
		
		step = new AlgorithmStep(improveParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP8_IMPROVE2", langID, "Set _latex{$S = S \\setminus \\{v_s\\}$} and go to step 2."), 8);
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
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_SETS", langID, "The vertices of set S that are free meaning no endpoints of a matched edge"), LegendItem.createCircleIcon(colorSetS, Color.black, 1)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURRVERTEX", langID, "The current vertex v<sub>s</sub>"), LegendItem.createCircleIcon(colorCurrVertex, Color.black, lineWidthCurrVertex)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_MATCHEDEDGES", langID, "The matched edges of matching M"), LegendItem.createLineIcon(colorMatchedEdges, lineWidthMatchedEdges, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_AUGMENTINGPATH", langID, "A found path that is qualified for an augmenting path"), LegendItem.createLineIcon(colorAugmentingPath, 2, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item5", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_MATCHINGENLARGEMENT", langID, "The unmatched edges of the augmenting path which enlarge the matching M"), LegendItem.createLineIcon(colorModified, 2, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item6", setView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_SET_MODIFICATION", langID, "The set S becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
		legendView.add(new LegendItem("item7", matchingView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_MATCHING_MODIFICATION", langID, "The matching M becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
	}
	
	/**
	 * Indicates whether the graph the user has created is bipartite.
	 * 
	 * @return <code>true</code> if the graph is bipartite otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean isGraphBipartite() {
		final Graph<Vertex, Edge> graph = graphView.getGraph();
		
		return GraphUtils.isBipartite(graph);
	}
	
	/**
	 * Gets the matching the user has entered or an empty matching if the user does not select a matching.
	 * 
	 * @return the matching or <code>null</code> if the user selects an invalid matching
	 * @since 1.0
	 */
	private Matching<Edge> getInputMatching() {
		final Matching<Edge> m = new Matching<>(graphView.getGraph());
		
		try {
			for(int i = 0; i < graphView.getSelectedEdgeCount(); i++)
				m.add(graphView.getSelectedEdge(i).getEdge());
		}
		catch(IllegalArgumentException e) {
			return null;
		}
		
		return m;
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
	 * The runtime environment of the Hungarian method.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class HungarianRTE extends AlgorithmRTE {
		
		/** the matching the user has entered to start with */
		private Matching<Edge> startMatching;
		/** the matching M or <code>null</code> if their is no matching */
		private Matching<Edge> M;
		/** the set S of vertices of V1 that are unmatched */
		private Set<Integer> S;
		/** the current augmenting path or <code>null</code> if their is no augmenting path */
		private Path<Vertex> AP;
		/** the current vertex of set S */
		private int v_s;
		/** the user's choice of v_s */
		private int userChoiceV_S;
		/** the user's choice of an augmenting path */
		private Path<Vertex> userChoiceAP;
		
		/**
		 * Creates a new runtime environment.
		 * 
		 * @since 1.0
		 */
		public HungarianRTE() {
			super(HungarianMethodPlugin.this, HungarianMethodPlugin.this.algoText);
			
			userChoiceV_S = 0;
			userChoiceAP = null;
		}
		
		/**
		 * Sets the matching with which the algorithm should be initialized and visualizes it.
		 * 
		 * @param m the matching the user has entered
		 * @throws IllegalArgumentException
		 * <ul>
		 * 		<li>if m is null</li>
		 * </ul>
		 * @since 1.0
		 */
		public void setStartMatching(final Matching<Edge> m) throws IllegalArgumentException {
			if(m == null)
				throw new IllegalArgumentException("No valid argument!");
			
			startMatching = m;
			
			// assign the start matching to M so that M can be visualized
			M = startMatching;
			visualizeMatching();
			visualizeMatchingAsText();
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = HungarianMethodPlugin.this.graphView.getGraph();
			GraphView<Vertex, Edge>.VisualEdge ve;
			int nextStep = -1;
			
			switch(stepID) {
				case 1:
					// initialize S with the vertices of V1 that are not matched
					
					// important: clone the start matching otherwise it is modified too
					M = startMatching.clone();
					
					// visualize the matching
					sleep(250);
					visualizeMatching();
					sleep(1000);
					HungarianMethodPlugin.this.matchingView.setBackground(HungarianMethodPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					sleep(250);
					HungarianMethodPlugin.this.matchingView.setBackground(Color.white);
					
					// the graph is absolutely bipartite (check in beforeStart(...))
					final List<List<Vertex>> subsets = GraphUtils.getBipartiteVertexSets(graph, false);
					// the partition 1 is always the partition with fewer or equivalent elements
					final List<Vertex> V1 = (subsets.get(0).size() <= subsets.get(1).size()) ? subsets.get(0) : subsets.get(1);
					
					for(Vertex v : V1)
						if(!M.isMatched(v))
							S.add(v.getID());
					
					// visualize set S
					sleep(250);
					HungarianMethodPlugin.this.setView.setBackground(HungarianMethodPlugin.this.colorModified);
					sleep(250);
					visualizeVertices();
					visualizeSetAsText();
					sleep(250);
					HungarianMethodPlugin.this.setView.setBackground(Color.white);
					sleep(1000);
					
					nextStep = 2;
					break;
				case 2:
					// if S is empty then stop
					
					sleep(1000);
					
					if(S.isEmpty())
						nextStep = -1;
					else
						nextStep = 3;
					break;
				case 3:
					// choose a vertex v_s of S
					
					// does the user makes a choice in an exercise?
					if(userChoiceV_S > 0)
						v_s = S.contains(userChoiceV_S) ? userChoiceV_S : S.get(0);
					else
						v_s = S.get(0);	// otherwise we always take the first one
					
					// clear the user choice
					userChoiceV_S = 0;
					
					sleep(250);
					visualizeVertices();
					sleep(1000);
					
					if(v_s < 1)
						nextStep = -1;
					else
						nextStep = 4;
					break;
				case 4:
					// find an augmenting path starting with v_s
					
					final Path<Vertex> ap = GraphUtils.findAugmentingPath(graph, graph.getVertexByID(v_s), M);
					
					// if their is no augmenting path then the user choice is irrelevant
					if(ap == null)
						AP = null;
					else {
						// if their is an augmenting path then take the users choice if it is valid otherwise the found augmenting path
						if(userChoiceAP != null)
							AP = userChoiceAP;
						else
							AP = ap;
					}
					
					// clear the user choice
					userChoiceAP = null;
					
					// visualize the augmenting path if necessary
					sleep(250);
					visualizeAugmentingPath(HungarianMethodPlugin.this.colorAugmentingPath);
					sleep(1500);
					
					if(AP == null)
						nextStep = 5;
					else
						nextStep = 6;
					break;
				case 5:
					// no augmenting path then put S = S \ {v_s}
					
					S.remove(v_s);
					
					// visualize the changes
					sleep(250);
					HungarianMethodPlugin.this.setView.setBackground(HungarianMethodPlugin.this.colorModified);
					sleep(250);
					visualizeSetAsText();
					sleep(250);
					HungarianMethodPlugin.this.setView.setBackground(Color.white);
					sleep(250);
					visualizeVertices();
					sleep(1000);
					
					nextStep = 2;
					break;
				case 6:
					// their is an augmenting path then go to 7
					sleep(1000);
					
					nextStep = 7;
					break;
				case 7:
					// enlarge the matching using the augmenting path so that unmatched edges become matched ones and vice versa
					
					final List<Edge> matchEdges = new ArrayList<Edge>();
					final List<Edge> unmatchEdges = new ArrayList<Edge>();
					
					// matched edges become unmatched ones and unmatched edges become matched ones
					for(int i = 0; i < AP.length(); i++) {
						if(i % 2 == 0)
							matchEdges.add(graph.getEdge(AP.get(i), AP.get(i + 1)));
						else
							unmatchEdges.add(graph.getEdge(AP.get(i), AP.get(i + 1)));
					}
					
					// unmatch edges
					for(Edge e : unmatchEdges)
						M.remove(e);
					// match edges
					for(Edge e : matchEdges) {
						M.add(e);
						ve = HungarianMethodPlugin.this.graphView.getVisualEdge(e);
						if(ve != null)
							ve.setColor(colorModified);
					}
					// show the visualization of the unmatched edges which become matched ones
					HungarianMethodPlugin.this.graphView.repaint();
					
					// visualize the matching
					sleep(250);
					HungarianMethodPlugin.this.matchingView.setBackground(HungarianMethodPlugin.this.colorModified);
					sleep(250);
					visualizeMatchingAsText();
					sleep(250);
					HungarianMethodPlugin.this.matchingView.setBackground(Color.white);
					sleep(500);
					visualizeMatching();
					sleep(1000);
					
					nextStep = 8;
					break;
				case 8:
					// put S = S \ {v_s}
					
					S.remove(v_s);
					
					// visualize set S
					sleep(250);
					HungarianMethodPlugin.this.setView.setBackground(HungarianMethodPlugin.this.colorModified);
					sleep(250);
					visualizeSetAsText();
					sleep(250);
					HungarianMethodPlugin.this.setView.setBackground(Color.white);
					sleep(250);
					visualizeVertices();
					sleep(1000);
					
					nextStep = 2;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addMatching("M", (M != null) ? M.cast() : null);
			state.addSet("S", S);
			state.addInt("v_s", v_s);
			state.addPath("AP", (AP != null) ? AP.cast() : null);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			final MatchingByID<Edge> m = state.getMatching("M", HungarianMethodPlugin.this.graphView.getGraph());
			M = (m != null) ? m.cast() : startMatching;
			S = state.getSet("S");
			v_s = state.getInt("v_s");
			final PathByID<Vertex> ap = state.getPath("AP", HungarianMethodPlugin.this.graphView.getGraph());
			AP = (ap != null) ? ap.cast() : null;
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			state.addMatching("M", null);
			S = state.addSet("S", new Set<Integer>());
			v_s = state.addInt("v_s", 0);
			state.addPath("AP", null);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			visualizeVertices();
			visualizeMatching();
			visualizeMatchingAsText();
			visualizeSetAsText();
			
			if(stepID >= 5 && stepID <= 7)
				visualizeAugmentingPath();
			else if(stepID == 1) {
				HungarianMethodPlugin.this.matchingView.setText("");
				HungarianMethodPlugin.this.setView.setText("");
			}
		}
		
		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			switch(stepID) {
				case 3:
					userChoiceV_S = state.getInt("v_s");
					break;
				case 4:
					final PathByID<Vertex> ucAP = state.getPath("AP", HungarianMethodPlugin.this.graphView.getGraph());
					userChoiceAP = (ucAP != null) ? ucAP.cast() : null;
					break;
			}
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { HungarianMethodPlugin.this.graphView, HungarianMethodPlugin.this.setView, HungarianMethodPlugin.this.matchingView };
		}
		
		/**
		 * Visualizes the vertices of the graph that are in set S.
		 * <br><br>
		 * Therefore all vertices of the graph are visited and colored.
		 * 
		 * @since 1.0
		 */
		private void visualizeVertices() {
			GraphView<Vertex, Edge>.VisualVertex vv;
			
			for(int i = 0; i < HungarianMethodPlugin.this.graphView.getVisualVertexCount(); i++) {
				vv = HungarianMethodPlugin.this.graphView.getVisualVertex(i);
				
				// visualize the start vertex and vertices of set S otherwise reset the visual style of the vertex
				if(vv.getVertex().getID() == v_s) {
					vv.setBackground(HungarianMethodPlugin.this.colorCurrVertex);
					vv.setEdgeWidth(HungarianMethodPlugin.this.lineWidthCurrVertex);
				}
				else if(S.contains(vv.getVertex().getID())) {
					vv.setBackground(HungarianMethodPlugin.this.colorSetS);
					vv.setEdgeWidth(1);
				}
				else {
					vv.setBackground(GraphView.DEF_VERTEXBACKGROUND);
					vv.setEdgeWidth(GraphView.DEF_VERTEXEDGEWIDTH);
				}
			}
			
			// show the visualization
			HungarianMethodPlugin.this.graphView.repaint();
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
			
			for(int i = 0; i < HungarianMethodPlugin.this.graphView.getVisualEdgeCount(); i++) {
				ve = HungarianMethodPlugin.this.graphView.getVisualEdge(i);
				
				// visualize matched and unmatched edges
				if(M.contains(ve.getEdge())) {
					ve.setColor(HungarianMethodPlugin.this.colorMatchedEdges);
					ve.setLineWidth(HungarianMethodPlugin.this.lineWidthMatchedEdges);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			// show the visualization
			HungarianMethodPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the current augmenting path.
		 * <br><br>
		 * To abrogate the colored path invoke {@link #visualizeMatching()} which overrides all edges.
		 * 
		 * @since 1.0
		 */
		private void visualizeAugmentingPath() {
			visualizeAugmentingPath(HungarianMethodPlugin.this.colorAugmentingPath);
		}
		
		/**
		 * Visualizes the current augmenting path.
		 * <br><br>
		 * To abrogate the colored path invoke {@link #visualizeMatching()} which overrides all edges.
		 * 
		 * @param color the color that should be used to visualize the augmenting path
		 * @since 1.0
		 */
		private void visualizeAugmentingPath(final Color color) {
			if(AP == null)
				return;
			
			final Graph<Vertex, Edge> graph = HungarianMethodPlugin.this.graphView.getGraph();
			GraphView<Vertex, Edge>.VisualEdge ve;
			
			for(int i = 0; i < AP.length(); i++) {
				ve = HungarianMethodPlugin.this.graphView.getVisualEdge(graph.getEdge(AP.get(i), AP.get(i + 1)));
				if(ve != null) {
					ve.setColor(color);
					ve.setLineWidth(ve.getLineWidth() + 1);
				}
			}
			
			// show the visualization
			HungarianMethodPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the set S in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeSetAsText() {
			HungarianMethodPlugin.this.setView.setText((S != null) ? "S=" + toCaptions(S) : "");
		}
		
		/**
		 * Visualizes the matching M in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeMatchingAsText() {
			HungarianMethodPlugin.this.matchingView.setText((M != null) ? "M=" + M.toString() : "");
		}
		
		/**
		 * Converts the given set of vertex identifiers to a set of related vertex captions.
		 * 
		 * @param set the set of vertex identifiers
		 * @return the converted set
		 * @since 1.0
		 */
		private Set<String> toCaptions(final Set<Integer> set) {
			final Graph<Vertex, Edge> graph = HungarianMethodPlugin.this.graphView.getGraph();
			final Set<String> res = new Set<String>(set.size());
			
			for(Integer id : set)
				res.add(graph.getVertexByID(id).getCaption());
			
			return res;
		}
		
	}

}
