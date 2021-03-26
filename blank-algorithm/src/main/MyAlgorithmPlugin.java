package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

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
import lavesdk.algorithm.plugin.extensions.MatrixToGraphToolBarExtension;
import lavesdk.algorithm.plugin.extensions.ToolBarExtension;
import lavesdk.algorithm.plugin.views.AlgorithmTextView;
import lavesdk.algorithm.plugin.views.DefaultGraphView;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.enums.AllowedGraphType;
import lavesdk.gui.widgets.BooleanProperty;
import lavesdk.gui.widgets.BooleanPropertyGroup;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.math.graph.Graph;
import lavesdk.math.Set;


public class MyAlgorithmPlugin implements AlgorithmPlugin {

	/** the host */
	private PluginHost host;
	/** the language file of the plugin */
	private LanguageFile langFile;
	/** the algorithm text */
	private AlgorithmText algoText;
	/** the runtime environment of the algorithm */
	private AlgorithmTextView algoTextView;
	/** the view that displays the list L */
	private MyAlgorithmRTE rte;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup vg;

	/** configuration key for the {@link #lineWidthMatchedEdges} */
	private static final String CFGKEY_CREATORPROP_DIRECTED = "creatorPrefsDirectedValue);";	
	/** configuration key for the {@link #lineWidthMatchedEdges} */
	private static final String CFGKEY_COLOR_A = "colorA";	
	private static final String CFGKEY_COLOR_E = "colorE";	

	private String langID;
	private DefaultGraphView graphView;
	
	// modifiable visualization data
	/** color to visualize the current edge */
	private Configuration config;
	private boolean creatorPrefsDirectedValue;
	private Color colorA;
	private Color colorE;
	
	// Method initialize
	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// (1) load the language file of the plugin
		try {
			langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/myLangFile.txt"));
			langFile.include(host.getLanguageFile());
		} catch (IOException e) {
			langFile = null;
		}
		langID = host.getLanguageID();

		// (2) save the parameters
		this.host = host;
		this.config = (config != null) ? config : new Configuration();
		this.graphView = new DefaultGraphView(LanguageFile.getLabel(langFile, "VIEW_GRAPH_TITLE", langID, "Graph"), new SimpleGraph<>(false), null, true, langFile, langID);
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.rte = new MyAlgorithmRTE();

		
		// create the toolbar extensions
				matrixToGraphExt = new MatrixToGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.BOTH,
						langFile, langID, true);

		// (3) load configuration data
		colorA = config.getColor(CFGKEY_COLOR_A, new Color(0, 255, 255));
		colorE = config.getColor(CFGKEY_COLOR_E, new Color(255, 0, 255));
	}
	
	// Methods to describe the algorithm
	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "blank algorithm");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "This is a test algorithm");
	}
	
	@Override
	public String getType() {
		return LanguageFile.getLabel(langFile, "ALGO_TYPE", langID, "Test");
	}

	@Override
	public String getAuthor() {
		return "Laves HSU";
	}
	
	@Override
	public String getAuthorContact() {
		return "laves@hsu-hh.de";
	}

	@Override
	public String getAssumptions() {
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "Here you can add assumptions");
	}

	@Override
	public String getProblemAffiliation() {
		return LanguageFile.getLabel(langFile, "ALGO_PROBLEMAFFILIATION", langID, "Example problem");
	}

	@Override
	public String getSubject() {
		return LanguageFile.getLabel(langFile, "ALGO_SUBJECT", langID, "Giving an Example");
	}

	@Override
	public String getInstructions() {
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm.<br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way.");
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public LAVESDKV getUsedSDKVersion() {
		return new LAVESDKV(1, 5);
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
	
	// Creator preferences and customization
	@Override
	public boolean hasCreatorPreferences() {
		return true;
	}
	
	@Override
	public void loadCreatorPreferences(PropertiesListModel plm) {
		final BooleanPropertyGroup group = new BooleanPropertyGroup(plm);
		plm.add(new BooleanProperty(LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED", langID, "directed"),
				LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED_DESC", langID,"Apply algorithm to a directed graph"),
				creatorPrefsDirectedValue, group));
		plm.add(new BooleanProperty(LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED", langID, "undirected"),
				LanguageFile.getLabel(langFile, "CREATORPREFS_UNDIRECTED_DESC", langID,	"Apply algorithm to an undirected graph"),
				!creatorPrefsDirectedValue, group));
		// ...
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty(CFGKEY_COLOR_A,
				LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_A", langID, "Color of the variable a"), colorA));
		plm.add(new ColorProperty(CFGKEY_COLOR_E,
				LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_E", langID, "Color of the variable a"), colorE));
		// ...
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		Color cA = plm.getColorProperty(CFGKEY_COLOR_A).getValue();
		Color cE = plm.getColorProperty(CFGKEY_COLOR_E).getValue();
		colorA = config.addColor(CFGKEY_COLOR_A, cA);
		colorE = config.addColor(CFGKEY_COLOR_E, cE);
		// ...
	}
	
	
	// Create the plugin view (major events)
	@Override
	public void onCreate(ViewContainer container, PropertiesListModel creatorProperties) {
		// (1) get the chosen creator preference
		String creatorPrefsDirected = LanguageFile.getLabel(langFile, "CREATORPREFS_DIRECTED", langID, "directed");
		creatorPrefsDirectedValue = (creatorProperties != null)	? creatorProperties.getBooleanProperty(creatorPrefsDirected).getValue(): false;
		
		// (2) update the configuration
		config.addBoolean(CFGKEY_CREATORPROP_DIRECTED, creatorPrefsDirectedValue);
		
		// (3) change the graph in the view
		graphView.setGraph(new SimpleGraph<Vertex, Edge>(creatorPrefsDirectedValue));
		graphView.repaint();
		
		// (4) create a view group for the views
		vg = new ViewGroup(ViewGroup.HORIZONTAL);
		vg.add(algoTextView);
		vg.add(graphView);
		vg.restoreWeights(config, "weights_vg", new float[] { 0.4f, 0.6f });
		container.setLayout(new BorderLayout());
		container.add(vg, BorderLayout.CENTER);
	}
	
	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		
		// reset view content where it is necessary
		graphView.reset();
	}

	// Create exercises
	@Override
	public boolean hasExerciseMode() {
		return true;
	}
	
	private AlgorithmText loadAlgorithmText() {
		AlgorithmStep step;
		final AlgorithmText text = new AlgorithmText();
		// create paragraphs
 		final AlgorithmParagraph initParagraph = new AlgorithmParagraph(text, "1. Initialization:", 1);
 		final AlgorithmParagraph itParagraph = new AlgorithmParagraph(text,	"2. Iteration:", 2);
		
 		// 1. initialization
 		step = new AlgorithmStep(initParagraph,
 		"Let _latex{a} be an arbitrary edge and _latex{c := 0}.", 1);
 		
 		// 2. iteration
 		step = new AlgorithmStep(itParagraph, "For each _latex{e \\in E}", 2);
 		
		step = new AlgorithmStep(itParagraph, "If _latex{w(e) == w(a)} then", 3, 1);
		step.setExercise(new AlgorithmExercise<Boolean>("Does <i>w(e)</i> equals <i>w(a)</i>?", 1.0f) {
			@Override
			protected Boolean[] requestSolution() {
				// (1) create two grouped radio buttons for the answers yes and no
				final ButtonGroup group = new ButtonGroup();
				final JRadioButton rdobtn1 = new JRadioButton("Yes");
				final JRadioButton rdobtn2 = new JRadioButton("No");
				group.add(rdobtn1);
				group.add(rdobtn2);
				// (2) create two entries for the options
				final SolutionEntry<JRadioButton> entryYes = new SolutionEntry<>("", rdobtn1);
				final SolutionEntry<JRadioButton> entryNo = new SolutionEntry<>("", rdobtn2);
				// (3) and show these entries in a dialog
				if (!SolveExercisePane.showDialog(host, this, new SolutionEntry<?>[] { entryYes, entryNo }, langFile,
						langID))
					return null;
				return new Boolean[] { (!rdobtn1.isSelected() && !rdobtn2.isSelected()) ? null : rdobtn1.isSelected() };
			}

			@Override
			protected String getResultAsString(Boolean result, int index) {
				if (result == null)
					return super.getResultAsString(result, index);
				else
					return (result.booleanValue() == true) ? "Yes" : "No";
			}

			@Override
			protected boolean examine(Boolean[] results, AlgorithmState state) {
				final int idOfe = state.getInt("e");
				final int idOfa = state.getInt("a");
				final Edge e = graphView.getGraph().getEdgeByID(idOfe);
				final Edge a = graphView.getGraph().getEdgeByID(idOfa);
				return (results[0] != null && results[0] == (e.getWeight() == a.getWeight()));
			}
		});
		
		step = new AlgorithmStep(itParagraph, "_latex{c := c + 1}", 4, 2);

 		return text;
	}
	
	// Save and open files
	@Override
	public void save(File file) {
		final FileNameExtensionFilter vgfFilter = new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf");
		final FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("Portable Network Graphic (*.png)",
				"png");
		try {
			if (vgfFilter.accept(file))
				graphView.save(file);
			else if (pngFilter.accept(file))
				graphView.saveAsPNG(file);
		} catch (IOException e) {
			host.showMessage(this, "File could not be saved!\n\n" + e.getMessage(), "Save File", MessageIcon.ERROR);
		}
	}

	@Override
	public void open(File file) {
		final FileNameExtensionFilter vgfFilter = new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf");
		try {
			if (vgfFilter.accept(file))
				graphView.load(file);
		} catch (IOException e) {
			host.showMessage(this, "File could not be opened!\n\n" + e.getMessage(), "Open File", MessageIcon.ERROR);
		}
	}	
	
	@Override
	public FileNameExtensionFilter[] getSaveFileFilters() {
		return new FileNameExtensionFilter[] { new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf"),
				new FileNameExtensionFilter("Portable Network Graphic (*.png)", "png") };
	}

	@Override
	public FileNameExtensionFilter[] getOpenFileFilters() {
		return new FileNameExtensionFilter[] { new FileNameExtensionFilter("Visual Graph File (*.vgf)", "vgf") };
	}		


	// Extend the toolbar
	private MatrixToGraphToolBarExtension<Vertex, Edge> matrixToGraphExt;

	// added lines in initialize "create the toolbar extensions"

	@Override
	public ToolBarExtension[] getToolBarExtensions() {
		return new ToolBarExtension[] { matrixToGraphExt };
	}

	// Runtime events (minor events)
	@Override
	public void beforeStart(RTEvent e) {
		// we have to check whether the created graph has at least one edge,
		// which is an assumption of our algorithm
		if(graphView.getGraph().getSize() < 1) {
			// cancel the start
			e.doit = false;
			// inform the user
			host.showMessage(this, "The graph must have at least one edge!",
			"Invalid graph", MessageIcon.INFO);
			}
			if(e.doit) {
			// clear any selection and disable edit mode of the graph view
			graphView.deselectAll();
			graphView.setEditable(false);
			}
	}

	@Override
	public void onStop() {
		graphView.setEditable(true);
	}
		
	@Override
	public void beforeResume(RTEvent e) {
	}

	@Override
	public void beforePause(RTEvent e) {
	}

	@Override
	public void onRunning() {
	}

	@Override
	public void onPause() {
	}
	
	private class MyAlgorithmRTE extends AlgorithmRTE {
		public MyAlgorithmRTE() throws IllegalArgumentException {
			super(MyAlgorithmPlugin.this, MyAlgorithmPlugin.this.algoText);
		}
		// algorithm variables
		private int a;
		private int c;
		private int e;
		private Set<Integer> forEach;
		
		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			Graph<Vertex, Edge> graph = MyAlgorithmPlugin.this.graphView.getGraph();
			int iNextStepID = -1;
			switch (stepID) {
			case 1: // Let a be an arbitrary edge and c:=0
				a = graph.getEdge(0).getID();
				c = 0;
				forEach = graph.getEdgeByIDSet();
				sleep(500);
				visualizeEdges();
				sleep(500);
				iNextStepID = 2;
				break;
			case 2: // For each e in E
				if (forEach.size() > 0) {
					e = forEach.get(0);
					forEach.remove(e);
					sleep(500);
					visualizeEdges();
					sleep(500);
					iNextStepID = 3;
				} else
					iNextStepID = -1;
				break;
			case 3: // If w(e) == w(a) then
				final Edge _e = graph.getEdgeByID(e);
				final Edge _a = graph.getEdgeByID(a);
				if (_e.getWeight() == _a.getWeight())
					iNextStepID = 4;
				else
					iNextStepID = 2;
				break;
			case 4: // c:=c+1
				c = c + 1;
				iNextStepID = 2;
				break;
			}
			return iNextStepID;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addInt("a", a);
			state.addInt("c", c);
			state.addInt("e", e);
			state.addSet("forEach", forEach);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			a = state.getInt("a");
			c = state.getInt("c");
			e = state.getInt("e");
			forEach = state.getSet("forEach");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			a = state.addInt("a", -1);
			c = state.addInt("c", 0);
			e = state.addInt("e", -1);
			forEach = state.addSet("forEach", new Set<Integer>());
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			switch (stepID) {
			case 1:
			case 2:
				visualizeEdges();
				break;
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
		}

		@Override
		protected View[] getViews() {
			return null;
		}
		
		private void visualizeEdges() {
			GraphView<Vertex, Edge> graphView = MyAlgorithmPlugin.this.graphView;
			GraphView<Vertex, Edge>.VisualEdge ve;
			for (int i = 0; i < graphView.getVisualEdgeCount(); i++) {
				ve = graphView.getVisualEdge(i);
				if (ve.getEdge().getID() == a) {
					ve.setColor(MyAlgorithmPlugin.this.colorA);
					ve.setLineWidth(3);
				} else if (ve.getEdge().getID() == e) {
					ve.setColor(MyAlgorithmPlugin.this.colorE);
					ve.setLineWidth(2);
				} else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			graphView.repaint();
		}
	}

}
