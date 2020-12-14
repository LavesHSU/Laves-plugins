package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
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
import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.algorithm.plugin.views.LegendView;
import lavesdk.algorithm.plugin.views.TextAreaView;
import lavesdk.algorithm.plugin.views.View;
import lavesdk.algorithm.plugin.views.ViewContainer;
import lavesdk.algorithm.plugin.views.ViewGroup;
import lavesdk.algorithm.text.AlgorithmParagraph;
import lavesdk.algorithm.text.AlgorithmStep;
import lavesdk.algorithm.text.AlgorithmText;
import lavesdk.configuration.Configuration;
import lavesdk.gui.dialogs.InputDialog;
import lavesdk.gui.dialogs.SolveExercisePane;
import lavesdk.gui.dialogs.SolveExerciseDialog.SolutionEntry;
import lavesdk.gui.dialogs.enums.AllowedGraphType;
import lavesdk.gui.widgets.ColorProperty;
import lavesdk.gui.widgets.ExecutionTableBorder;
import lavesdk.gui.widgets.ExecutionTableColumn;
import lavesdk.gui.widgets.ExecutionTableGroup;
import lavesdk.gui.widgets.ExecutionTableItem;
import lavesdk.gui.widgets.ExecutionTableItem.NumericInputParser;
import lavesdk.gui.widgets.LegendItem;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.Set;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.Path;
import lavesdk.math.graph.PathByID;
import lavesdk.math.graph.SimpleGraph;
import lavesdk.math.graph.Vertex;
import lavesdk.utils.GraphUtils;
import lavesdk.utils.MathUtils;

/**
 * Plugin that visualizes and teaches users the 2-opt algorithm.
 * 
 * @author jdornseifer
 * @version 1.2
 */
public class TwoOptAlgorithmPlugin implements AlgorithmPlugin {
	
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
	/** the view that displays the combinations */
	private ExecutionTableView combinationsView;
	/** the view that shows the legend of the algorithm */
	private LegendView legendView;
	/** the runtime environment of the 2-opt algorithm */
	private TwoOptRTE rte;
	/** toolbar extension to create graphs from adjacency matrices */
	private MatrixToGraphToolBarExtension<Vertex, Edge> matrixToGraph;
	/** toolbar extension to check whether a graph is complete or to create one */
	private CompleteGraphToolBarExtension<Vertex, Edge> completeExt;
	/** toolbar extension to layout a graph in a circle */
	private CircleLayoutToolBarExtension<Vertex, Edge> circleLayoutExt;
	/** the add icon resource */
	private Icon addIconRes;
	/** the remove icon resource */
	private Icon removeIconRes;
	/** the view group for A and B (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup ab;
	/** the view group for C and D (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup cd;
	/** the view group for A,B,C,D and E (see {@link #onCreate(ViewContainer, PropertiesListModel)}) */
	private ViewGroup abcde;
	
	// modifiable visualization data
	/** color to visualize the Hamiltonian cycle r */
	private Color colorCycleR;
	/** color to visualize the current edge pair */
	private Color colorCurrEdgePair;
	/** color to visualize a new edge pair combination */
	private Color colorNewEdgePairCombi;
	/** color to visualize an existing edge pair combination */
	private Color colorExistingEdgePairCombi;
	/** color to visualize the largest savings */
	private Color colorMaxSavings;
	/** color to visualize modified objects */
	private Color colorModified;
	/** line with of the edges of the Hamiltonian cycle r */
	private int lineWidthCycleR;
	
	/** configuration key for the {@link #colorCycleR} */
	private static final String CFGKEY_COLOR_CYCLER = "colorCycleR";
	/** configuration key for the {@link #colorCurrEdgePair} */
	private static final String CFGKEY_COLOR_CURREDGEPAIR = "colorCurrEdgePair";
	/** configuration key for the {@link #colorNewEdgePairCombi} */
	private static final String CFGKEY_COLOR_NEWEDGEPAIRCOMBI = "colorNewEdgePairCombi";
	/** configuration key for the {@link #colorExistingEdgePairCombi} */
	private static final String CFGKEY_COLOR_EXISTINGEDGEPAIRCOMBI = "colorExistingEdgePairCombi";
	/** configuration key for the {@link #colorMaxSavings} */
	private static final String CFGKEY_COLOR_MAXSAVINGS = "colorMaxSavings";
	/** configuration key for the {@link #colorModified} */
	private static final String CFGKEY_COLOR_MODIFIED = "colorModified";
	/** configuration key for the {@link #lineWidthCycleR} */
	private static final String CFGKEY_LINEWIDTH_CYCLER = "lineWidthCycleR";

	@Override
	public void initialize(PluginHost host, ResourceLoader resLoader, Configuration config) {
		// load the language file of the plugin
		try {
			this.langFile = new LanguageFile(resLoader.getResourceAsStream("main/resources/langTwoOpt.txt"));
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
		this.combinationsView = new ExecutionTableView(LanguageFile.getLabel(langFile, "VIEW_COMBI_TITLE", langID, "Combinations"), true, langFile, langID);
		// load the algorithm text after the visualization views are created because the algorithm exercises have resource to the views
		this.algoText = loadAlgorithmText();
		this.algoTextView = new AlgorithmTextView(host, LanguageFile.getLabel(langFile, "VIEW_ALGOTEXT_TITLE", langID, "Algorithm"), algoText, true, langFile, langID);
		this.legendView = new LegendView(LanguageFile.getLabel(langFile, "VIEW_LEGEND_TITLE", langID, "Legend"), true, langFile, langID);
		this.rte = new TwoOptRTE();
		this.matrixToGraph = new MatrixToGraphToolBarExtension<>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.completeExt = new CompleteGraphToolBarExtension<Vertex, Edge>(host, graphView, AllowedGraphType.UNDIRECTED_ONLY, langFile, langID, true);
		this.circleLayoutExt = new CircleLayoutToolBarExtension<Vertex, Edge>(graphView, langFile, langID, false);
		this.addIconRes = resLoader.getResourceAsIcon("main/resources/plus.png");
		this.removeIconRes = resLoader.getResourceAsIcon("main/resources/minus.png");
		
		// set auto repaint mode so that it is not necessary to call repaint() after changes were made
		algoTextView.setAutoRepaint(true);
		cycleView.setAutoRepaint(true);
		combinationsView.setAutoRepaint(true);
		
		// the widths of the columns are defined manually
		combinationsView.setAutoResizeColumns(false);
		
		// load the visualization colors from the configuration of the plugin
		colorCycleR = this.config.getColor(CFGKEY_COLOR_CYCLER, new Color(200, 145, 145));
		colorCurrEdgePair = this.config.getColor(CFGKEY_COLOR_CYCLER, new Color(235, 190, 80));
		colorNewEdgePairCombi = this.config.getColor(CFGKEY_COLOR_CYCLER, new Color(105, 150, 180));
		colorExistingEdgePairCombi = this.config.getColor(CFGKEY_COLOR_CYCLER, new Color(180, 180, 180));
		colorMaxSavings = this.config.getColor(CFGKEY_COLOR_MAXSAVINGS, new Color(120, 210, 80));
		colorModified = this.config.getColor(CFGKEY_COLOR_MODIFIED, new Color(255, 180, 130));
		lineWidthCycleR = this.config.getInt(CFGKEY_LINEWIDTH_CYCLER, 3);
		
		// load view configurations
		graphView.loadConfiguration(config, "graphView");
		algoTextView.loadConfiguration(config, "algoTextView");
		cycleView.loadConfiguration(config, "cycleView");
		combinationsView.loadConfiguration(config, "combinationsView");
		legendView.loadConfiguration(config, "legendView");
		
		// create the legend
		createLegend();
	}

	@Override
	public String getName() {
		return LanguageFile.getLabel(langFile, "ALGO_NAME", langID, "2-opt");
	}

	@Override
	public String getDescription() {
		return LanguageFile.getLabel(langFile, "ALGO_DESC", langID, "An improvement algorithm to find a Hamiltonian cycle that contains each vertex of a graph exactly once.");
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
		return LanguageFile.getLabel(langFile, "ALGO_ASSUMPTIONS", langID, "A non-negative weighted, undirected graph K<sub>n</sub> with n > 2 and a Hamiltonian cycle r.");
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
		return LanguageFile.getLabel(langFile, "ALGO_INSTRUCTIONS", langID, "<b>Creating problem entities</b>:<br>Create your own graph and make sure that the graph complies with the assumptions of the algorithm. You can use<br>the toolbar extensions to check whether the created graph is complete, to create a complete graph by indicating the number of vertices, to<br>create a graph by use of an adjacency matrix or you can arrange the vertices of your created graph in a circle.<br><br><b>Starting the algorithm</b>:<br>Before you start the algorithm select a Hamiltonian cycle <i>r</i> the algorithm should begin with. Create the starting Hamiltonian cycle<br>by selecting the vertices one after another so that a valid cycle develops (that is, the cycle is created using the selection order of the<br>vertices). It is also possible that you start the algorithm without selecting any vertices and after that you open the input dialog in the<br>information message to enter a Hamiltonian cycle with the keyboard.<br><br><b>Exercise Mode</b>:<br>Activate the exercise mode to practice the algorithm in an interactive way. After you have started the algorithm<br>exercises are presented that you have to solve.<br>If an exercise can be solved directly in a view of the algorithm the corresponding view is highlighted with a border, there you can<br>enter your solution and afterwards you have to press the button to solve the exercise. Otherwise (if an exercise is not related to a specific<br>view) you can directly press the button to solve the exercise which opens a dialog where you can enter your solution of the exercise.");
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
		// set a new graph in the view
		graphView.setGraph(new SimpleGraph<Vertex, Edge>(false));
		graphView.repaint();
		
		/*
		 * the plugin's layout:
		 * 
		 * ///|/////////|/////
		 * / /|/       /|/   /	A = algorithm text view
		 * /A/|/  C    /|/   /	B = legend view
		 * / /|/       /|/   /	C = graph view
		 * ///|/////////|/ E /	D = text area view (cycle view)
		 * ---|---------|/   /	E = execution table view (combinations view)
		 * ///|/////////|/   /
		 * /B/|/  D    /|/   /
		 * ///|/////////|/////
		 */
		ab = new ViewGroup(ViewGroup.VERTICAL);
		cd = new ViewGroup(ViewGroup.VERTICAL);
		abcde = new ViewGroup(ViewGroup.HORIZONTAL);
		
		// left group for A and B
		ab.add(algoTextView);
		ab.add(legendView);
		ab.restoreWeights(config, "weights_ab", new float[] { 0.6f, 0.4f });
		
		// middle group for C and D
		cd.add(graphView);
		cd.add(cycleView);
		cd.restoreWeights(config, "weights_cd", new float[] { 0.6f, 0.4f });
		
		// group for (A,B), (C,D) and E
		abcde.add(ab);
		abcde.add(cd);
		abcde.add(combinationsView);
		abcde.restoreWeights(config, "weights_abcde", new float[] { 0.3f, 0.4f, 0.3f });
		
		container.setLayout(new BorderLayout());
		container.add(abcde, BorderLayout.CENTER);
	}

	@Override
	public void onClose() {
		// save view configurations
		graphView.saveConfiguration(config, "graphView");
		algoTextView.saveConfiguration(config, "algoTextView");
		cycleView.saveConfiguration(config, "cycleView");
		combinationsView.saveConfiguration(config, "combinationsView");
		legendView.saveConfiguration(config, "legendView");
		
		// save weights
		if(ab != null)
			ab.storeWeights(config, "weights_ab");
		if(cd != null)
			cd.storeWeights(config, "weights_cd");
		if(abcde != null)
			abcde.storeWeights(config, "weights_abcde");
		
		// reset view content where it is necessary
		graphView.reset();
		cycleView.reset();
		combinationsView.reset();
	}

	@Override
	public boolean hasCustomization() {
		return true;
	}

	@Override
	public void loadCustomization(PropertiesListModel plm) {
		plm.add(new ColorProperty("algoTextHighlightForeground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTFOREGROUND", langID, "Foreground color of the current step in the algorithm"), algoTextView.getHighlightForeground()));
		plm.add(new ColorProperty("algoTextHighlightBackground", LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_ALGOTEXTHIGHLIGHTBACKGROUND", langID, "Background color of the current step in the algorithm"), algoTextView.getHighlightBackground()));
		plm.add(new ColorProperty(CFGKEY_COLOR_CYCLER, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CYCLER", langID, "Color of the Hamiltonian cycle r"), colorCycleR));
		plm.add(new ColorProperty(CFGKEY_COLOR_CURREDGEPAIR, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_CURREDGEPAIR", langID, "Color of the current edge-pair (v<sub>i</sub>,v<sub>j</sub>) (v'<sub>i</sub>,v'<sub>j</sub>)"), colorCurrEdgePair));
		plm.add(new ColorProperty(CFGKEY_COLOR_NEWEDGEPAIRCOMBI, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_NEWEDGEPAIRCOMBI", langID, "Color of a new combination of an edge-pair"), colorNewEdgePairCombi));
		plm.add(new ColorProperty(CFGKEY_COLOR_EXISTINGEDGEPAIRCOMBI, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_EXISTINGEDGEPAIRCOMBI", langID, "Color of an existing combination of an edge-pair"), colorExistingEdgePairCombi));
		plm.add(new ColorProperty(CFGKEY_COLOR_MAXSAVINGS, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MAXSAVINGS", langID, "Background color of an item in the combinations table with a largest savings"), colorMaxSavings));
		plm.add(new ColorProperty(CFGKEY_COLOR_MODIFIED, LanguageFile.getLabel(langFile, "CUSTOMIZE_COLOR_MODIFICATIONS", langID, "Color of modifications to objects"), colorModified));
		
		final NumericProperty lwCycleR = new NumericProperty(CFGKEY_LINEWIDTH_CYCLER, LanguageFile.getLabel(langFile, "CUSTOMIZE_LINEWIDTH_CYCLER", langID, "Line with of the Hamiltonian cycle r"), lineWidthCycleR, true);
		lwCycleR.setMinimum(1);
		lwCycleR.setMaximum(5);
		plm.add(lwCycleR);
	}

	@Override
	public void applyCustomization(PropertiesListModel plm) {
		algoTextView.setHighlightForeground(plm.getColorProperty("algoTextHighlightForeground").getValue());
		algoTextView.setHighlightBackground(plm.getColorProperty("algoTextHighlightBackground").getValue());
		colorCycleR = config.addColor(CFGKEY_COLOR_CYCLER, plm.getColorProperty(CFGKEY_COLOR_CYCLER).getValue());
		colorCurrEdgePair = config.addColor(CFGKEY_COLOR_CURREDGEPAIR, plm.getColorProperty(CFGKEY_COLOR_CURREDGEPAIR).getValue());
		colorNewEdgePairCombi = config.addColor(CFGKEY_COLOR_NEWEDGEPAIRCOMBI, plm.getColorProperty(CFGKEY_COLOR_NEWEDGEPAIRCOMBI).getValue());
		colorExistingEdgePairCombi = config.addColor(CFGKEY_COLOR_EXISTINGEDGEPAIRCOMBI, plm.getColorProperty(CFGKEY_COLOR_EXISTINGEDGEPAIRCOMBI).getValue());
		colorMaxSavings = config.addColor(CFGKEY_COLOR_MAXSAVINGS, plm.getColorProperty(CFGKEY_COLOR_MAXSAVINGS).getValue());
		colorModified = config.addColor(CFGKEY_COLOR_MODIFIED, plm.getColorProperty(CFGKEY_COLOR_MODIFIED).getValue());
		lineWidthCycleR = config.addInt(CFGKEY_LINEWIDTH_CYCLER, plm.getNumericProperty(CFGKEY_LINEWIDTH_CYCLER).getValue().intValue());
		
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
		if(graphView.getGraph().getOrder() <= 3) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_INSUFFICIENTVERTEXCOUNT", langID, "The created graph does not comply with the assumptions!\nThe vertex count is insufficient."), LanguageFile.getLabel(langFile, "MSG_INFO_INSUFFICIENTVERTEXCOUNT_TITLE", langID, "Invalid graph"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(containsGraphNegativeWeights()) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS", langID, "The created graph contains edges with a negative weight!\nThe 2-opt algorithm can only be applied to non-negative weighted graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NEGATIVEWEIGHTS_TITLE", langID, "Negative weights"), MessageIcon.INFO);
			e.doit = false;
		}
		else if(!GraphUtils.isComplete(graphView.getGraph())) {
			host.showMessage(this, LanguageFile.getLabel(langFile, "MSG_INFO_NOTCOMPLETE", langID, "The created graph is not complete!\nThe 2-opt algorithm can only be applied to complete graphs."), LanguageFile.getLabel(langFile, "MSG_INFO_NOTCOMPLETE_TITLE", langID, "No complete graph"), MessageIcon.INFO);
			e.doit = false;
		}
		
		if(e.doit) {
			Path<Vertex> p = new Path<Vertex>(graphView.getGraph());
			
			// create the starting Hamiltonian cycle
			try {
				for(int i = 0; i < graphView.getSelectedVertexCount(); i++)
					p.add(graphView.getSelectedVertex(i).getVertex());
				
				// close the path so that it is a cycle
				if(graphView.getSelectedVertexCount() > 0)
					p.add(graphView.getSelectedVertex(0).getVertex());
			}
			catch(IllegalArgumentException ex) {
				p = null;
			}
			
			// the algorithm needs a valid Hamiltonian cycle to start with
			// (the length of a Hamiltonian cycle must be equal to the number of vertices in the graph which ensures that the path contains all vertices)
			if(p == null || !p.isClosed() || p.length() != graphView.getGraph().getOrder()) {
				final Object[] options = new Object[] { LanguageFile.getLabel(langFile, "DLG_BTN_OK", langID, "Ok"), LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTCYCLE_CREATE", langID, "Input...")};
				final JOptionPane pane = new JOptionPane(LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTCYCLE", langID, "Please select a valid starting Hamiltonian cycle!\nCreate the Hamiltonian cycle by selecting the vertices in the graph one after another so that a valid cycle develops\n(that is, the cycle is created using the selection order of the vertices).\n\nRemember that a Hamiltonian cycle contains all vertices of a graph!"),
														 JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options[0]);
				final JDialog dlg = pane.createDialog(LanguageFile.getLabel(langFile, "MSG_INFO_SELECTSTARTCYCLE_TITLE", langID, "Select Hamiltonian cycle"));
				host.adaptDialog(dlg);
				dlg.setVisible(true);
				
				final Object option = pane.getValue();
				if(option == null || option.equals(options[0])) {
					e.doit = false;
					return;
				}
				else if(option.equals(options[1])) {
					// open a dialog so that the user can input a cycle
					final InputDialog inputDlg = new InputDialog(host, LanguageFile.getLabel(langFile, "CYCLEINPUTDLG_TITLE", langID, "Enter starting Hamiltonian Cycle"),
																 LanguageFile.getLabel(langFile, "CYCLEINPUTDLG_DESC", langID, "Use a comma as the delimiter!<br>Enter the starting Hamiltonian cycle in the following form:<br>v<sub>1</sub>, v<sub>2</sub>, ..., v<sub>1</sub>"),
																 "r = ", langFile, langID);
					inputDlg.setVisible(true);
					
					p = null;
					
					if(!inputDlg.isCanceled()) {
						p = GraphUtils.toPath(inputDlg.getInput(), graphView.getGraph());
						if(p == null || !p.isClosed() || p.length() != graphView.getGraph().getOrder()) {
							host.showMessage(TwoOptAlgorithmPlugin.this, LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDCYCLEINPUT", langID, "Your input is incorrect!\nPlease enter the Hamiltonian cycle in the specified form and only use vertex captions that are existing."), LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDCYCLEINPUT_TITLE", langID, "Invalid input"), MessageIcon.INFO);
							p = null;
						}
					}
					
					if(p == null) {
						e.doit = false;
						return;
					}
				}
			}
			
			// the edit mode of the graph view must be disabled before the start cycle is set to the rte (otherwise
			// the cycle becomes visible in edit moe)
			graphView.deselectAll();
			graphView.setEditable(false);

			// reset the views
			combinationsView.reset();
			cycleView.reset();
			
			// set the starting Hamiltonian cycle the user has selected (but only after the edit mode is disabled
			// and the cycle view is reset because the cycle should be visualized)
			rte.setStartingHamiltonianCycle(p);
			
			// and create the columns of the table
			ExecutionTableColumn column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_COMBI_COLUMN_OUT", langID, "Out (v<sub>i</sub>,v<sub>j</sub>) (v'<sub>i</sub>,v'<sub>j</sub>)"));
			column.setWidth(125);
			combinationsView.add(column);
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_COMBI_COLUMN_IN", langID, "In (v<sub>i</sub>,v'<sub>i</sub>) (v<sub>j</sub>,v'<sub>j</sub>)"));
			column.setWidth(125);
			combinationsView.add(column);
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_COMBI_COLUMN_SAVINGS", langID, "Savings"));
			column.setWidth(125);
			combinationsView.add(column);
			column = new ExecutionTableColumn(LanguageFile.getLabel(langFile, "VIEW_COMBI_COLUMN_LENGTH", langID, "Length r'"));
			column.setWidth(75);
			combinationsView.add(column);
			
			// create column groups
			final ExecutionTableBorder groupBorder = new ExecutionTableBorder(2, Color.black);
			combinationsView.addColumnGroup(new ExecutionTableGroup(groupBorder, 0));
			combinationsView.addColumnGroup(new ExecutionTableGroup(groupBorder, 1));
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
		final AlgorithmParagraph itParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_ITERATION", langID, "1. Iteration:"), 1);
		final AlgorithmParagraph varParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_VARIATION", langID, "2. Variation:"), 2);
		final AlgorithmParagraph stopParagraph = new AlgorithmParagraph(text, LanguageFile.getLabel(langFile, "ALGOTEXT_PARAGRAPH_STOPCRITERION", langID, "3. Stop criterion:"), 3);
		
		// 1. iteration
		step = new AlgorithmStep(itParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP1_IT", langID, "Choose two edges _latex{$(v_i,v_j)$}, _latex{$(v'_i,v'_j)$} from _latex{$r$}, which have no common endpoint.\nExecute step 2 for all possible combinations of such edge-pairs.\n\n"), 1);
		step.setExercise(new AlgorithmExercise<List<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP1", langID, "Specify all possible combinations of edge-pairs and their substitutions in the table (<i>use the buttons in the header bar of the combinations view to add or remove items and regard that the input pattern has to be \"(...,...) (...,...)\", where the ellipsis have to be replaced with the corresponding vertex captions</i>)."), 2.0f, combinationsView) {
			
			private CombiViewExerciseExt ext;
			private int firstIndex = -1;
			private boolean lastExaminationSucceeded = true;
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				// create the extension
				ext = new CombiViewExerciseExt(TwoOptAlgorithmPlugin.this.combinationsView, TwoOptAlgorithmPlugin.this.graphView.getGraph(), TwoOptAlgorithmPlugin.this.addIconRes, TwoOptAlgorithmPlugin.this.removeIconRes, TwoOptAlgorithmPlugin.this.langFile, TwoOptAlgorithmPlugin.this.langID);
				// add the header bar extension so that the user can add or remove items
				ext.apply();
				
				// the user max only input in the first and second column (out/in column)
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(0).setEditable(true);
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(1).setEditable(true);
				
				// save the first index of the item the user can edit
				if(lastExaminationSucceeded)
					firstIndex = TwoOptAlgorithmPlugin.this.combinationsView.getItemCount();
				else
					for(int i = firstIndex; i < TwoOptAlgorithmPlugin.this.combinationsView.getItemCount(); i++)
						TwoOptAlgorithmPlugin.this.combinationsView.getItem(i).setEditable(false);
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				// remove the extension
				ext.remove();
				ext = null;
				
				// deactivate the edit mode
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(0).setEditable(false);
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(1).setEditable(false);
				
				// if the user gives up then remove the added items of the user because the solution cannot be used
				if(omitted)
					for(int i = TwoOptAlgorithmPlugin.this.combinationsView.getItemCount() - 1; i >= firstIndex; i--)
						TwoOptAlgorithmPlugin.this.combinationsView.remove(TwoOptAlgorithmPlugin.this.combinationsView.getItem(i));
				else {
					// otherwise turn off the edit mode of the items
					for(int i = 0; i < TwoOptAlgorithmPlugin.this.combinationsView.getItemCount(); i++)
						TwoOptAlgorithmPlugin.this.combinationsView.getItem(i).setEditable(false);
				}
				
				// reset flag
				lastExaminationSucceeded = true;
			}
			
			@Override
			protected List<?>[] requestSolution() {
				final List<EdgePair> pairs = new ArrayList<EdgePair>();
				final List<EdgePair> substitutedPairs = new ArrayList<EdgePair>();
				
				for(int i = firstIndex; i < TwoOptAlgorithmPlugin.this.combinationsView.getItemCount(); i++) {
					pairs.add((EdgePair)TwoOptAlgorithmPlugin.this.combinationsView.getItem(i).getCellObject(0));
					substitutedPairs.add((EdgePair)TwoOptAlgorithmPlugin.this.combinationsView.getItem(i).getCellObject(1));
				}
				
				return new List<?>[] { pairs, substitutedPairs };
			}
			
			@Override
			protected boolean getApplySolutionToAlgorithm() {
				return true;
			}
			
			@Override
			protected void applySolutionToAlgorithm(AlgorithmState state, List<?>[] solutions) {
				@SuppressWarnings("unchecked")
				final List<EdgePair> pairs = (List<EdgePair>)solutions[0];
				
				// convert the list into a set
				state.addSet("edgePairs", new Set<EdgePair>(pairs));
			}
			
			@Override
			protected boolean examine(List<?>[] results, AlgorithmState state) {
				final Graph<Vertex, Edge> graph = TwoOptAlgorithmPlugin.this.graphView.getGraph();
				final Path<Vertex> r = state.getPath("r", graph).cast();
				final Set<EdgePair> edgePairs = new Set<EdgePair>();
				final Set<EdgePair> substitutedPairs = new Set<EdgePair>();
				EdgePair edgePair;
				int v_i;
				int v_j;
				int v_i_Apo;
				int v_j_Apo;
				
				// create all possible combinations
				for(int i = 0; i < r.length(); i++) {
					v_i = r.get(i).getID();
					v_j = r.get(i + 1).getID();
					
					for(int j = i + 2, k = 1; k <= r.length() - 3; j++, k++) {
						if(j >= r.length())
							j = j - r.length();
						
						v_i_Apo = r.get(j).getID();
						v_j_Apo = r.get(j + 1).getID();
						edgePair = new EdgePair(graph, v_i, v_j, v_i_Apo, v_j_Apo);
						edgePairs.add(edgePair);
						substitutedPairs.add(EdgePair.substitute(edgePair, graph));
					}
				}
				
				// check whether the list the user has entered complies with the combinations set and the substituted pairs
				lastExaminationSucceeded = edgePairs.size() == results[0].size() && edgePairs.containsAll(results[0]) && substitutedPairs.size() == results[1].size() && substitutedPairs.containsAll(results[1]);
				return lastExaminationSucceeded;
			}
		});
		
		// 2. variation
		step = new AlgorithmStep(varParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP2_VARIATION", langID, "Determine the length of the Hamiltonian cycle _latex{$r'$}, where compared to _latex{$r$} edges _latex{$(v_i,v_j)$} and _latex{$(v'_i,v'_j)$} are substituted by edges _latex{$(v_i,v'_i)$}, _latex{$(v_j,v'_j)$} and the path from _latex{$v_j$} to _latex{$v'_i$} will be traversed in reverse direction.\n\n"), 2);
		step.setExercise(new AlgorithmExercise<List<?>>(LanguageFile.getLabel(langFile, "EXERCISE_STEP2", langID, "What are the savings and the consequent lengths of <i>r'</i> for the particular edge-pairs?"), 2.0f, combinationsView) {
			
			@Override
			protected void beforeRequestSolution(AlgorithmState state) {
				ExecutionTableItem item;
				
				// activate the edit mode for columns savings and length
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(2).setEditable(true);
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(3).setEditable(true);
				
				// get the last item group that contains information about the item range that is editable
				final ExecutionTableGroup group = TwoOptAlgorithmPlugin.this.combinationsView.getItemGroup(TwoOptAlgorithmPlugin.this.combinationsView.getItemGroupCount() - 1);
				for(int i = group.getStart(); i < group.getStart() + group.getAmount(); i++) {
					item = TwoOptAlgorithmPlugin.this.combinationsView.getItem(i);
					item.setEditable(true);
					item.setDefaultInputParser(new NumericInputParser());
				}
			}
			
			@Override
			protected void afterRequestSolution(boolean omitted) {
				// deactivate the edit mode
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(2).setEditable(false);
				TwoOptAlgorithmPlugin.this.combinationsView.getColumn(3).setEditable(false);
				final ExecutionTableGroup group = TwoOptAlgorithmPlugin.this.combinationsView.getItemGroup(TwoOptAlgorithmPlugin.this.combinationsView.getItemGroupCount() - 1);
				for(int i = group.getStart(); i < group.getStart() + group.getAmount(); i++)
					TwoOptAlgorithmPlugin.this.combinationsView.getItem(i).setEditable(false);
			}
			
			@Override
			protected List<?>[] requestSolution() {
				final ExecutionTableGroup group = TwoOptAlgorithmPlugin.this.combinationsView.getItemGroup(TwoOptAlgorithmPlugin.this.combinationsView.getItemGroupCount() - 1);
				final List<Float> savings = new ArrayList<Float>();
				final List<Float> lengths = new ArrayList<Float>();
				ExecutionTableItem item;
				Number currSavings;
				Number currLength;
				
				for(int i = group.getStart(); i < group.getStart() + group.getAmount(); i++) {
					item = TwoOptAlgorithmPlugin.this.combinationsView.getItem(i);
					currSavings = (Number)item.getCellObject(2);
					currLength = (Number)item.getCellObject(3);
					savings.add((currSavings != null) ? currSavings.floatValue() : null);
					lengths.add((currLength != null) ? currLength.floatValue() : null);
				}
				
				return new List<?>[] { savings, lengths };
			}
			
			@Override
			protected boolean examine(List<?>[] results, AlgorithmState state) {
				final Set<EdgePair> pairs = state.getSet("edgePairs");
				final PathByID<Vertex> r = state.getPath("r", TwoOptAlgorithmPlugin.this.graphView.getGraph());
				@SuppressWarnings("unchecked")
				final List<Float> savings = (List<Float>)results[0];
				@SuppressWarnings("unchecked")
				final List<Float> lengths = (List<Float>)results[1];
				EdgePair ep;
				
				// check whether the user has entered the right savings and lengths
				for(int i = 0; i < pairs.size(); i++) {
					ep = pairs.get(i);
					if(savings.get(i).floatValue() != ep.getSavings() || (lengths.get(i).floatValue() != r.getWeight() - ep.getSavings()))
						return false;
				}
				
				return true;
			}
		});
		
		// 3. stop
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP3_STOP", langID, "If the shortest Hamiltonian cycle _latex{$r'$} found in step 2 is shorter than _latex{$r$} "), 3);
		step.setExercise(new AlgorithmExercise<Boolean>(LanguageFile.getLabel(langFile, "EXERCISE_STEP3", langID, "Is their a shorter Hamiltonian cycle than <i>r</i>?"), 1.0f) {
			
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
				
				if(!SolveExercisePane.showDialog(TwoOptAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entryYes,  entryNo }, TwoOptAlgorithmPlugin.this.langFile, TwoOptAlgorithmPlugin.this.langID))
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
				final int maxSavingsEdgePair = state.getInt("maxSavingsEdgePair");
				
				return (results[0] != null && results[0] == (maxSavingsEdgePair >= 0));
			}
		});
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP4_STOP", langID, "then set _latex{$r = r'$} and go to step 1. "), 4);
		step.setExercise(new AlgorithmExercise<String>(LanguageFile.getLabel(langFile, "EXERCISE_STEP4", langID, "What is the new Hamiltonian cycle <i>r</i>?"), 1.0f) {
			
			@Override
			protected String[] requestSolution() {
				final SolutionEntry<JTextField> entry = new SolutionEntry<JTextField>("r=", new JTextField());
				
				if(!SolveExercisePane.showDialog(TwoOptAlgorithmPlugin.this.host, this, new SolutionEntry<?>[] { entry }, TwoOptAlgorithmPlugin.this.langFile, TwoOptAlgorithmPlugin.this.langID, LanguageFile.getLabel(TwoOptAlgorithmPlugin.this.langFile, "EXERCISE_HINT_CYCLEINPUT", TwoOptAlgorithmPlugin.this.langID, "Use a comma as the delimiter!<br>Enter the starting Hamiltonian cycle in the following form:<br>v<sub>1</sub>, v<sub>2</sub>, ..., v<sub>1</sub>")))
					return null;
				
				final Path<Vertex> p = GraphUtils.toPath(entry.getComponent().getText(), TwoOptAlgorithmPlugin.this.graphView.getGraph());
				
				if(p == null) {
					TwoOptAlgorithmPlugin.this.host.showMessage(TwoOptAlgorithmPlugin.this, LanguageFile.getLabel(TwoOptAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT", TwoOptAlgorithmPlugin.this.langID, "Your input is incorrect!\nPlease enter the Hamiltonian cycle in the specified form and only use vertex captions that are existing."), LanguageFile.getLabel(TwoOptAlgorithmPlugin.this.langFile, "MSG_INFO_INVALIDCYCLEINPUT_TITLE", TwoOptAlgorithmPlugin.this.langID, "Invalid input"), MessageIcon.INFO);
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
				final PathByID<Vertex> r = state.getPath("r", TwoOptAlgorithmPlugin.this.graphView.getGraph());
				final Path<Vertex> p = GraphUtils.toPath(results[0], TwoOptAlgorithmPlugin.this.graphView.getGraph());
				
				return r.equals(p.cast());
			}
			
		});
		
		step = new AlgorithmStep(stopParagraph, LanguageFile.getLabel(langFile, "ALGOTEXT_STEP5_STOP", langID, "Otherwise stop."), 5);
		
		return text;
	}
	
	/**
	 * Creates the legend of the plugin.
	 * 
	 * @since 1.0
	 */
	private void createLegend() {
		legendView.removeAll();
		
		legendView.add(new LegendItem("item1", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CYCLER", langID, "The Hamiltonian cycle r"), LegendItem.createLineIcon(colorCycleR, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item2", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_CURREDGEPAIR", langID, "The current edge-pair (v<sub>i</sub>,v<sub>j</sub>) (v'<sub>i</sub>,v'<sub>j</sub>)"), LegendItem.createLineIcon(colorCurrEdgePair, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item3", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_NEWEDGEPAIRCOMBI", langID, "A new combination of an edge-pair"), LegendItem.createLineIcon(colorNewEdgePairCombi, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item4", graphView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_GRAPH_EXISTINGEDGEPAIRCOMBI", langID, "An existing combination of an edge-pair"), LegendItem.createLineIcon(colorExistingEdgePairCombi, lineWidthCycleR, LegendItem.LINETYPE_BOTTOMLEFT_TO_TOPRIGHT)));
		legendView.add(new LegendItem("item5", combinationsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_COMBI_CURREDGEPAIR", langID, "The current edge-pair that is inspected"), LegendItem.createRectangleIcon(colorCurrEdgePair, colorCurrEdgePair, 0)));
		legendView.add(new LegendItem("item6", combinationsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_COMBI_NEWEDGEPAIRCOMBI", langID, "The new edge-pair combination"), LegendItem.createRectangleIcon(colorNewEdgePairCombi, colorNewEdgePairCombi, 0)));
		legendView.add(new LegendItem("item7", combinationsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_COMBI_EXISTINGEDGEPAIRCOMBI", langID, "The existing edge-pair combination"), LegendItem.createRectangleIcon(colorExistingEdgePairCombi, colorExistingEdgePairCombi, 0)));
		legendView.add(new LegendItem("item8", combinationsView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_COMBI_MAXSAVINGS", langID, "The edge-pair combination with a largest savings and thereby a shorter Hamiltonian cycle"), LegendItem.createRectangleIcon(colorMaxSavings, colorMaxSavings, 0)));
		legendView.add(new LegendItem("item9", cycleView.getTitle(), LanguageFile.getLabel(langFile, "LEGEND_CYCLE_MODIFICATION", langID, "The Hamiltonian cycle r becomes modified"), LegendItem.createRectangleIcon(colorModified, colorModified, 0)));
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
	 * The runtime environment of the 2-opt algorithm.
	 * 
	 * @author jdornseifer
	 * @version 1.0
	 */
	private class TwoOptRTE extends AlgorithmRTE {
		
		/** the label of the cycle length */
		private final String labelLengthCycle;
		/** the starting Hamiltonian cycle r */
		private Path<Vertex> startR;
		/** the Hamiltonian cycle r */
		private Path<Vertex> r;
		/** the set of edge pairs */
		private Set<EdgePair> edgePairs;
		/** the index of the edge pair with the maximum savings */
		private int maxSavingsEdgePair;
		/** the edge pairs set the user has entered */
		private Set<EdgePair> userChoiceEdgePairs;
		
		/**
		 * Creates the runtime environment.
		 * 
		 * @since 1.0
		 */
		public TwoOptRTE() {
			super(TwoOptAlgorithmPlugin.this, TwoOptAlgorithmPlugin.this.algoText);
			
			labelLengthCycle = LanguageFile.getLabel(TwoOptAlgorithmPlugin.this.langFile, "VIEW_CYCLE_LENGTH", TwoOptAlgorithmPlugin.this.langID, "Length:");
			r = null;
			userChoiceEdgePairs = null;
		}
		
		/**
		 * Sets the starting Hamiltonian cycle of the algorithm and visualizes it.
		 * 
		 * @param r the starting Hamiltonian cycle
		 * @since 1.0
		 */
		public void setStartingHamiltonianCycle(final Path<Vertex> r) {
			startR = r.clone();
			this.r = startR.clone();
			
			// visualize the current Hamiltonian cycle
			visualizeCycleAsText();
			visualizeCycle();
		}

		@Override
		protected int executeStep(int stepID, AlgorithmStateAttachment asa) throws Exception {
			final Graph<Vertex, Edge> graph = TwoOptAlgorithmPlugin.this.graphView.getGraph();
			int nextStep = -1;
			EdgePair ep;
			GraphView<Vertex, Edge>.VisualEdge veV_iV_j;
			GraphView<Vertex, Edge>.VisualEdge veV_i_ApoV_j_Apo;
			ExecutionTableItem item;
			
			switch(stepID) {
				case 1:
					// choose two edges (v_i,v_j), (v'_i,v'_j) from r which have no common endpoint and
					// execute step 2 for all possible combinations of such edge-pairs
					
					// if the user has entered a correct solution in the related exercise then take this one
					if(userChoiceEdgePairs != null)
						edgePairs = userChoiceEdgePairs;
					else {
						int v_i;
						int v_j;
						int v_i_Apo;
						int v_j_Apo;
						
						sleep(250);
						
						edgePairs = new Set<EdgePair>();
						
						// create all combinations (because it is a set equal combinations are filtered out automatically)
						for(int i = 0; i < r.length(); i++) {
							v_i = r.get(i).getID();
							v_j = r.get(i + 1).getID();
							
							// visualize the (v_i,v_j) edge
							veV_iV_j = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(v_i, v_j));
							if(veV_iV_j != null) {
								veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
								veV_iV_j.getPredecessor().setForeground(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
								veV_iV_j.getSuccessor().setForeground(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
								TwoOptAlgorithmPlugin.this.graphView.repaint();
							}
							
							sleep(250);
							
							for(int j = i + 2, k = 1; k <= r.length() - 3; j++, k++) {
								// this loop is a circulation so check whether it reaches the first vertex of the cycle
								if(j >= r.length())
									j = j - r.length();
								
								v_i_Apo = r.get(j).getID();
								v_j_Apo = r.get(j + 1).getID();
								ep = new EdgePair(graph, v_i, v_j, v_i_Apo, v_j_Apo);
								
								// visualize the second edge of the current pair
								veV_i_ApoV_j_Apo = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(v_i_Apo, v_j_Apo));
								if(veV_i_ApoV_j_Apo != null) {
									veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
									veV_i_ApoV_j_Apo.getPredecessor().setForeground(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
									veV_i_ApoV_j_Apo.getSuccessor().setForeground(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
									TwoOptAlgorithmPlugin.this.graphView.repaint();
								}
								
								sleep(500);
								
								// add the pair (if their already exists a pair then it is not added)
								if(edgePairs.add(ep)) {
									// add the pair to the table
									item = new ExecutionTableItem(new Object[] { ep, EdgePair.substitute(ep, graph) });
									TwoOptAlgorithmPlugin.this.combinationsView.add(item);
									
									// highlight the item and the edges
									item.setBackground(TwoOptAlgorithmPlugin.this.colorNewEdgePairCombi);
									if(veV_iV_j != null && veV_i_ApoV_j_Apo != null) {
										veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorNewEdgePairCombi);
										veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorNewEdgePairCombi);
										TwoOptAlgorithmPlugin.this.graphView.repaint();
									}
								}
								else {
									// find the corresponding item in the table
									int itemIndex = -1;
									for(int l = 0; l < edgePairs.size(); l++) {
										if(edgePairs.get(l).equals(ep)) {
											itemIndex = l;
											break;
										}
									}
									
									item = (itemIndex >= 0) ? TwoOptAlgorithmPlugin.this.combinationsView.getItem(TwoOptAlgorithmPlugin.this.combinationsView.getItemCount() - edgePairs.size() + itemIndex) : null;
									
									// highlight the item and the edges
									if(item != null)
										item.setBackground(TwoOptAlgorithmPlugin.this.colorExistingEdgePairCombi);
									if(veV_iV_j != null && veV_i_ApoV_j_Apo != null) {
										veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorExistingEdgePairCombi);
										veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorExistingEdgePairCombi);
										TwoOptAlgorithmPlugin.this.graphView.repaint();
									}
								}
								
								sleep(250);
								
								// remove the highlight from the item and the edges
								if(item != null)
									item.setBackground(Color.white);
								if(veV_iV_j != null && veV_i_ApoV_j_Apo != null) {
									veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
									veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
									veV_i_ApoV_j_Apo.getPredecessor().setForeground(GraphView.DEF_VERTEXFOREGROUND);
									veV_i_ApoV_j_Apo.getSuccessor().setForeground(GraphView.DEF_VERTEXFOREGROUND);
									TwoOptAlgorithmPlugin.this.graphView.repaint();
								}
							}
							
							sleep(250);
							
							// remove the highlight from the first edge
							if(veV_iV_j != null) {
								veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
								veV_iV_j.getPredecessor().setForeground(GraphView.DEF_VERTEXFOREGROUND);
								veV_iV_j.getSuccessor().setForeground(GraphView.DEF_VERTEXFOREGROUND);
								TwoOptAlgorithmPlugin.this.graphView.repaint();
							}
						}
					}
					
					// clear the user's choice
					userChoiceEdgePairs = null;
					
					// add a group to separate the iterations
					TwoOptAlgorithmPlugin.this.combinationsView.addItemGroup(new ExecutionTableGroup(new ExecutionTableBorder(2, Color.black), TwoOptAlgorithmPlugin.this.combinationsView.getItemCount() - edgePairs.size(), edgePairs.size()));
					
					nextStep = 2;
					break;
				case 2:
					// determine the length of the Hamiltonian cycle r', where compared to r edges (v_i,v_j) and (v'_i,v'_j) are substituted
					// by edges (v_i,v'_i),(v_j,v'_j) and the path from v_j to v'_i will be traversed in reverse direction
					
					sleep(250);
					
					// the savings are pre-calculated for each edge pair so visualize them and compute the length of r' using the savings
					for(int i = 0; i < edgePairs.size(); i++) {
						ep = edgePairs.get(i);
						item = TwoOptAlgorithmPlugin.this.combinationsView.getItem(TwoOptAlgorithmPlugin.this.combinationsView.getItemCount() - edgePairs.size() + i);
						
						// get the visual edges of the pair
						veV_iV_j = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(ep.getV_i(), ep.getV_j()));
						veV_i_ApoV_j_Apo = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(ep.getV_i_Apo(), ep.getV_j_Apo()));
						// highlight the edge pair that is currently under investigation
						if(veV_iV_j != null && veV_i_ApoV_j_Apo != null) {
							veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
							veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
							TwoOptAlgorithmPlugin.this.graphView.repaint();
						}
						
						sleep(500);
						
						// highlight the corresponding item
						item.setBackground(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
						
						sleep(500);
						
						// visualize the savings
						item.setCellObject(2, ep.savingsToString());
						sleep(250);
						// the length of r' = length(r) - savings
						item.setCellObject(3, MathUtils.formatFloat(r.getWeight() - ep.getSavings()));
						
						sleep(500);
						
						// remove the highlight from the current edge pair and the item
						if(veV_iV_j != null && veV_i_ApoV_j_Apo != null) {
							veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
							veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
							TwoOptAlgorithmPlugin.this.graphView.repaint();
						}
						item.setBackground(Color.white);
					}
					
					nextStep = 3;
					break;
				case 3:
					// if the shortest Hamiltonian cycle r' is shorter than r then go to step 4 (stepid) otherwise go to step 5 (stepid)
					
					float maxSavings = 0;
					
					maxSavingsEdgePair = -1;
					
					// find a largest savings
					for(int i = 0; i < edgePairs.size(); i++) {
						ep = edgePairs.get(i);
						
						if(ep.getSavings() > maxSavings) {
							maxSavings = ep.getSavings();
							maxSavingsEdgePair = i;
						}
					}
					
					// is their an edge pair that improves the Hamiltonian cycle?
					if(maxSavingsEdgePair >= 0) {
						sleep(500);
						
						// highlight the item with the largest savings
						item = TwoOptAlgorithmPlugin.this.combinationsView.getItem(TwoOptAlgorithmPlugin.this.combinationsView.getItemCount() - edgePairs.size() + maxSavingsEdgePair);
						item.setBackground(TwoOptAlgorithmPlugin.this.colorMaxSavings);
						
						sleep(1000);
						
						// remove the highlight from the item
						item.setBackground(Color.white);
						TwoOptAlgorithmPlugin.this.combinationsView.repaint();
						
						nextStep = 4;
					}
					else
						nextStep = 5;
					break;
				case 4:
					// set r = r' and go to step 1 (stepid)
					
					// get the edge pair for substitution
					ep = edgePairs.get(maxSavingsEdgePair);
					
					sleep(250);
					
					// visualize the substitution of the edge pair
					visualizeSubstitution();
					
					final List<Vertex> r_Apo = r.asList();
					int v_jIndex = -1;
					int v_i_ApoIndex = -1;
					Vertex tmpV;
					
					// find the indices of v_j and v'_i in r'
					for(int i = 0; i < r_Apo.size(); i++) {
						if(r_Apo.get(i).getID() == ep.getV_j())
							v_jIndex = i;
						else if(r_Apo.get(i).getID() == ep.getV_i_Apo())
							v_i_ApoIndex = i;
						
						if(v_jIndex != -1 && v_i_ApoIndex != -1)
							break;
					}
					
					// that should not happen but check it to be on the safe side
					if(v_jIndex < 0 || v_i_ApoIndex < 0) {
						nextStep = -1;
						break;
					}
					
					// reverse the path from v_j to v'_i by swapping the vertices
					for(int i = v_jIndex, j = v_i_ApoIndex; i < j; i++, j--) {
						tmpV = r_Apo.get(i);
						r_Apo.set(i, r_Apo.get(j));
						r_Apo.set(j, tmpV);
					}
					
					r = new Path<Vertex>(graph, r_Apo);
					
					// visualize the new Hamiltonian cycle
					sleep(250);
					TwoOptAlgorithmPlugin.this.cycleView.setBackground(TwoOptAlgorithmPlugin.this.colorModified);
					sleep(250);
					visualizeCycleAsText();
					sleep(250);
					TwoOptAlgorithmPlugin.this.cycleView.setBackground(Color.white);
					sleep(250);
					visualizeCycle();
					sleep(1000);
					
					nextStep = 1;
					break;
				case 5:
					// otherwise stop
					
					sleep(1000);
					
					nextStep = -1;
					break;
			}
			
			return nextStep;
		}

		@Override
		protected void storeState(AlgorithmState state) {
			state.addPath("r", r.cast());
			state.addSet("edgePairs", edgePairs);
			state.addInt("maxSavingsEdgePair", maxSavingsEdgePair);
		}

		@Override
		protected void restoreState(AlgorithmState state) {
			final PathByID<Vertex> tmpR = state.getPath("", TwoOptAlgorithmPlugin.this.graphView.getGraph());
			r = (tmpR != null) ? tmpR.cast() : startR.clone();
			edgePairs = state.getSet("edgePairs");
			maxSavingsEdgePair = state.getInt("maxSavingsEdgePair");
		}

		@Override
		protected void createInitialState(AlgorithmState state) {
			state.addPath("r", null);
			edgePairs = state.addSet("edgePairs", new Set<EdgePair>());
			maxSavingsEdgePair = state.addInt("maxSavingsEdgePair", -1);
		}

		@Override
		protected void rollBackStep(int stepID, int nextStepID) {
			ExecutionTableGroup group;
			ExecutionTableItem item;
			
			switch(stepID) {
				case 1:
					// get the last item group
					group = TwoOptAlgorithmPlugin.this.combinationsView.getItemGroup(TwoOptAlgorithmPlugin.this.combinationsView.getItemGroupCount() - 1);
					
					// the item group has the information of the last added items
					for(int i = group.getStart() + group.getAmount() - 1; i >= group.getStart(); i--)
						TwoOptAlgorithmPlugin.this.combinationsView.remove(TwoOptAlgorithmPlugin.this.combinationsView.getItem(i));
					// remove the group
					TwoOptAlgorithmPlugin.this.combinationsView.removeItemGroup(group);
					break;
				case 2:
					// get the last item group
					group = TwoOptAlgorithmPlugin.this.combinationsView.getItemGroup(TwoOptAlgorithmPlugin.this.combinationsView.getItemGroupCount() - 1);
					
					// clear the savings and the lengths
					for(int i = group.getStart(); i < group.getStart() + group.getAmount(); i++) {
						item = TwoOptAlgorithmPlugin.this.combinationsView.getItem(i);
						item.setCellObject(2, null);
						item.setCellObject(3, null);
					}
					visualizeCycle();
					break;
				case 3:
					visualizeCycle();
					break;
				case 4:
					visualizeCycle();
					visualizeCycleAsText();
					visualizeSubstitution();
					break;
			}
		}

		@Override
		protected void adoptState(int stepID, AlgorithmState state) {
			if(stepID == 1)
				userChoiceEdgePairs = state.getSet("edgePairs");
		}
		
		@Override
		protected View[] getViews() {
			return new View[] { TwoOptAlgorithmPlugin.this.graphView, TwoOptAlgorithmPlugin.this.cycleView, TwoOptAlgorithmPlugin.this.combinationsView };
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
			
			for(int i = 0; i < TwoOptAlgorithmPlugin.this.graphView.getVisualEdgeCount(); i++) {
				ve = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(i);
				
				// visualize edges of the Hamiltonian cycle r
				if(r.contains(ve.getEdge())) {
					ve.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
					ve.setLineWidth(TwoOptAlgorithmPlugin.this.lineWidthCycleR);
				}
				else {
					ve.setColor(GraphView.DEF_EDGECOLOR);
					ve.setLineWidth(GraphView.DEF_EDGELINEWIDTH);
				}
			}
			
			// show the visualization
			TwoOptAlgorithmPlugin.this.graphView.repaint();
		}
		
		/**
		 * Visualizes the Hamiltonian cycle r in the corresponding text area view.
		 * 
		 * @since 1.0
		 */
		private void visualizeCycleAsText() {
			TwoOptAlgorithmPlugin.this.cycleView.setText(((r != null) ? "r=" + r.toString() : "") + "\n" + labelLengthCycle + " " + MathUtils.formatFloat(r.getWeight()));
		}
		
		/**
		 * Visualizes the substitution of the edge pair that produces a shorter Hamiltonian cycle.
		 * 
		 * @since 1.0
		 */
		private void visualizeSubstitution() {
			final Graph<Vertex, Edge> graph = TwoOptAlgorithmPlugin.this.graphView.getGraph();
			final EdgePair ep = edgePairs.get(maxSavingsEdgePair);
			final EdgePair substitutedEP = EdgePair.substitute(ep, graph);
			
			// visualize the current edges and the substitution edges in the graph view
			final GraphView<Vertex, Edge>.VisualEdge veV_iV_j = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(ep.getV_i(), ep.getV_j()));
			final GraphView<Vertex, Edge>.VisualEdge veV_i_ApoV_j_Apo = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(ep.getV_i_Apo(), ep.getV_j_Apo()));
			final GraphView<Vertex, Edge>.VisualEdge veV_iV_i_Apo = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(substitutedEP.getV_i(), substitutedEP.getV_j()));
			final GraphView<Vertex, Edge>.VisualEdge veV_jV_j_Apo = TwoOptAlgorithmPlugin.this.graphView.getVisualEdge(graph.getEdge(substitutedEP.getV_i_Apo(), substitutedEP.getV_j_Apo()));
			if(veV_iV_j != null && veV_i_ApoV_j_Apo != null && veV_iV_i_Apo != null && veV_jV_j_Apo != null) {
				// visualize the current edges
				veV_iV_j.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
				veV_i_ApoV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCurrEdgePair);
				// visualize the substituted edge pair
				veV_iV_i_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
				veV_iV_i_Apo.setLineWidth(TwoOptAlgorithmPlugin.this.lineWidthCycleR);
				veV_jV_j_Apo.setColor(TwoOptAlgorithmPlugin.this.colorCycleR);
				veV_jV_j_Apo.setLineWidth(TwoOptAlgorithmPlugin.this.lineWidthCycleR);
				TwoOptAlgorithmPlugin.this.graphView.repaint();
			}
		}
		
	}

}
