package main;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import lavesdk.algorithm.plugin.views.ExecutionTableView;
import lavesdk.gui.widgets.ExecutionTableItem.InputParser;
import lavesdk.language.LanguageFile;
import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.Vertex;

/**
 * {@link InputParser} for {@link EdgePair}s in an {@link ExecutionTableView}.
 * 
 * @author jdornseifer
 * @version 1.1
 */
public class EdgePairInputParser implements InputParser<EdgePair> {
	
	/** the execution table view */
	private final ExecutionTableView view;
	/** the related graph */
	private final Graph<Vertex, Edge> graph;
	/** the language file */
	private final LanguageFile langFile;
	/** the language id */
	private final String langID;
	
	/** the edge-pair pattern "(...,...) (...,...)" */
	private static final Pattern edgePairPattern = Pattern.compile("\\s*\\((.+?),(.+?)\\)\\s*\\((.+?),(.+?)\\)\\s*");
	
	/**
	 * Creates a new edge pair input parser.
	 * 
	 * @param view the view that displays the combinations of edge-pairs
	 * @param graph the related graph
	 * @param langFile the language file
	 * @param langID the language id
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if view is null</li>
	 * 		<li>if graph is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public EdgePairInputParser(final ExecutionTableView view, final Graph<Vertex, Edge> graph, final LanguageFile langFile, final String langID) throws IllegalArgumentException {
		if(view == null || graph == null)
			throw new IllegalArgumentException("No valid argument!");
		
		this.view = view;
		this.graph = graph;
		this.langFile = langFile;
		this.langID = langID;
	}

	@Override
	public Object prepareEditor(Object o) {
		return o;
	}

	@Override
	public EdgePair parse(String input) {
		final Matcher m = edgePairPattern.matcher(input);
		Vertex v_i = null;
		Vertex v_j = null;
		Vertex v_i_Apo = null;
		Vertex v_j_Apo = null;
		
		// if the input matches the pattern then extract the vertices
		if(m.matches()) {
			v_i = graph.getVertexByCaption(m.group(1).trim());
			v_j = graph.getVertexByCaption(m.group(2).trim());
			v_i_Apo = graph.getVertexByCaption(m.group(3).trim());
			v_j_Apo = graph.getVertexByCaption(m.group(4).trim());
		}
		
		// no valid input? then display a message and quit
		if(v_i == null || v_j == null || v_i_Apo == null || v_j_Apo == null) {
			JOptionPane.showMessageDialog(view, LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDEDGEPAIRINPUT", langID, "Your input is incorrect!\nPlease enter an edge-pair in the following pattern: (...,...) (...,...), where the ellipsis have to be\nreplaced with the corresponding vertex captions and ensure that specified vertices are existing."), LanguageFile.getLabel(langFile, "MSG_INFO_INVALIDEDGEPAIRINPUT_TITLE", langID, "Invalid input"), JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		return new EdgePair(graph, v_i.getID(), v_j.getID(), v_i_Apo.getID(), v_j_Apo.getID());
	}

}
