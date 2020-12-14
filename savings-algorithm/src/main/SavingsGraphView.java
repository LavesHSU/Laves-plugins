package main;

import lavesdk.algorithm.plugin.views.GraphView;
import lavesdk.gui.EDT;
import lavesdk.gui.GuiJob;
import lavesdk.gui.GuiRequest;
import lavesdk.gui.widgets.NumericProperty;
import lavesdk.gui.widgets.PropertiesListModel;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.SimpleGraph;

/**
 * The customized graph view.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class SavingsGraphView extends GraphView<WeightedVertex, Edge> {

	private static final long serialVersionUID = 1L;
	
	/** the label of the weight property */
	private final String vertexWeightLabel;

	/**
	 * Creates a new graph view.
	 * 
	 * @param title the title
	 * @param langFile the language file
	 * @param langID the language id
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if title is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public SavingsGraphView(String title, LanguageFile langFile, String langID) throws IllegalArgumentException {
		super(title, new SimpleGraph<WeightedVertex, Edge>(false), new SavingsGraphFactory(), false, langFile, langID);
		
		vertexWeightLabel = LanguageFile.getLabel(langFile, "VIEW_GRAPH_VERTEXPROP_WEIGHT", langID, "Weight");
		setVertexRenderer(new WeightedVertexRenderer());
		setEdgeRenderer(new EdgeRenderer());
	}
	
	/**
	 * Adds a new edge to the graph.
	 * <br><br>
	 * <b>This method is thread-safe!</b>
	 * 
	 * @param predecessor the predecessor of the edge
	 * @param successor the successor of the edge
	 * @param directed <code>true</code> for directed and <code>false</code> for undirected
	 * @return the visual edge or <code>null</code> if the edge could not be added
	 * @since 1.0
	 */
	public VisualEdge addEdge(final WeightedVertex predecessor, final WeightedVertex successor, final boolean directed) {
		return EDT.execute(new GuiRequest<VisualEdge>() {
			@Override
			protected VisualEdge execute() throws Throwable {
				return createVisualEdge(getVisualVertex(predecessor), getVisualVertex(successor), directed);
			}
		});
	}
	
	/**
	 * Removes the specified visual edge and its corresponding {@link Edge} from the graph view and its {@link Graph}.
	 * <br><br>
	 * <b>This method is thread-safe!</b>
	 * 
	 * @param ve the visual edge to be removed
	 * @since 1.0
	 */
	public void removeEdge(final VisualEdge ve) {
		EDT.execute(new GuiJob() {
			
			@Override
			protected void execute() throws Throwable {
				removeVisualEdge(ve);
			}
		});
	}
	
	@Override
	protected void loadAdvancedVertexProperties(PropertiesListModel plm, WeightedVertex vertex) {
		super.loadAdvancedVertexProperties(plm, vertex);
		
		final NumericProperty weightProp = new NumericProperty(vertexWeightLabel, "", vertex.getWeight());
		plm.add(weightProp);
	}
	
	@Override
	protected void applyAdvancedVertexProperties(PropertiesListModel plm, WeightedVertex vertex) {
		super.applyAdvancedVertexProperties(plm, vertex);
		
		final NumericProperty weightProp = plm.getNumericProperty(vertexWeightLabel);
		if(weightProp != null)
			vertex.setWeight(weightProp.getValue().floatValue());
	}

}
