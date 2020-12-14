package main;

import java.io.Serializable;

import lavesdk.math.graph.Edge;
import lavesdk.math.graph.Graph;
import lavesdk.math.graph.Vertex;
import lavesdk.utils.MathUtils;

/**
 * Represents a 2-opt edge pair (v_i,v_j), (v'_i,v'_j).
 * <br><br>
 * Use {@link #substitute(EdgePair, Graph)} to substitute a pair.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class EdgePair implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/** the id of the vertex v_i */
	private final int v_i;
	/** the id of the vertex v_j */
	private final int v_j;
	/** the id of the vertex v'_i */
	private final int v_i_Apo;
	/** the id of the vertex v'_j */
	private final int v_j_Apo;
	/** the savings of the edge pair */
	private final float savings;
	/** the string representation if the savings */
	private final String savingsToString;
	/** the string representation of the edge pair */
	private final String toString;
	
	/**
	 * Creates a new 2-opt edge pair.
	 * 
	 * @param graph the graph
	 * @param v_i the id of the vertex v_i
	 * @param v_j the id of the vertex v_j
	 * @param v_i_Apo the id of the vertex v'_i
	 * @param v_j_Apo the id of the vertex v'_j
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if graph is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public EdgePair(final Graph<Vertex, Edge> graph, final int v_i, final int v_j, final int v_i_Apo, final int v_j_Apo) throws IllegalArgumentException {
		if(graph == null)
			throw new IllegalArgumentException("No valid argument!");
		
		this.v_i = v_i;
		this.v_j = v_j;
		this.v_i_Apo = v_i_Apo;
		this.v_j_Apo = v_j_Apo;
		
		// create the string representation of the edge pair
		final Vertex vi = graph.getVertexByID(this.v_i);
		final Vertex vj = graph.getVertexByID(this.v_j);
		final Vertex vi_Apo = graph.getVertexByID(this.v_i_Apo);
		final Vertex vj_Apo = graph.getVertexByID(this.v_j_Apo);
		final Edge eV_iV_j = graph.getEdge(this.v_i, this.v_j);
		final Edge eV_iV_i_Apo = graph.getEdge(this.v_i, this.v_i_Apo);
		final Edge eV_i_ApoV_j_Apo = graph.getEdge(this.v_i_Apo, this.v_j_Apo);
		final Edge eV_jV_j_Apo = graph.getEdge(this.v_j, this.v_j_Apo);
		
		if(vi == null || vj == null || vi_Apo == null || vj_Apo == null)
			toString = "";
		else
			toString = "(" + vi.getCaption() + ", " + vj.getCaption() + ") (" + vi_Apo.getCaption() + ", " + vj_Apo.getCaption() + ")";
		
		// calculate the savings
		if(eV_iV_j != null && eV_iV_i_Apo != null && eV_i_ApoV_j_Apo != null && eV_jV_j_Apo != null) {
			savings = (eV_iV_j.getWeight() + eV_i_ApoV_j_Apo.getWeight()) - (eV_iV_i_Apo.getWeight() + eV_jV_j_Apo.getWeight());
			savingsToString = "(" + MathUtils.formatFloat(eV_iV_j.getWeight()) + " + " + MathUtils.formatFloat(eV_i_ApoV_j_Apo.getWeight()) + ") - (" + MathUtils.formatFloat(eV_iV_i_Apo.getWeight()) + " + " + MathUtils.formatFloat(eV_jV_j_Apo.getWeight()) + ") = " + MathUtils.formatFloat(savings);
		}
		else {
			savings = 0.0f;
			savingsToString = "";
		}
	}
	
	/**
	 * Gets the vertex v_i.
	 * 
	 * @return the id of the vertex v_i
	 * @since 1.0
	 */
	public int getV_i() {
		return v_i;
	}
	
	/**
	 * Gets the vertex v_j.
	 * 
	 * @return the id of the vertex v_j
	 * @since 1.0
	 */
	public int getV_j() {
		return v_j;
	}
	
	/**
	 * Gets the vertex v'_i.
	 * 
	 * @return the id of the vertex v'_i
	 * @since 1.0
	 */
	public int getV_i_Apo() {
		return v_i_Apo;
	}
	
	/**
	 * Gets the vertex v'_j.
	 * 
	 * @return the id of the vertex v'_j
	 * @since 1.0
	 */
	public int getV_j_Apo() {
		return v_j_Apo;
	}
	
	/**
	 * Gets the savings of the edge pair.
	 * 
	 * @return the savings
	 * @since 1.0
	 */
	public float getSavings() {
		return savings;
	}
	
	/**
	 * Gets the string representation of the savings.
	 * 
	 * @return the savings as a string
	 * @since 1.0
	 */
	public String savingsToString() {
		return savingsToString;
	}
	
	@Override
	public String toString() {
		return toString;
	}
	
	/**
	 * Indicates whether two edge pairs are the same.
	 * <br><br>
	 * <b>Examples of two equal edge pairs</b>:<br>
	 * <code>(1,2) (4,5) = (4,5) (1,2) = (2,1) (5,4) = (5,4) (2,1)</code> meaning
	 * <code>(v_i,v_j) (v'_i,v'_j) = (v'_i,v'_j) (v_i,v_j) = (v_j,v_i) (v'_j,v'_i) = (v'_j,v'_i) (v_j,v_i)</code>
	 * 
	 * @param ep another edge pair
	 * @return <code>true</code> if they are equal otherwise <code>false</code>
	 * @since 1.0
	 */
	public boolean equals(final EdgePair ep) {
		if(ep == null)
			return false;
		
		/*
		 * (a,b) (c,d) = (w,x) (y,z)
		 *  e1     e2     e3     e4
		 * => if e1 = e3 && e2 = e4 || e1 = e4 && e2 = e3
		 * => e1 = e3 that is, a = c && b = d || a = d && b = c
		 */
		
		return (equalEdges(this.v_i, this.v_j, ep.v_i, ep.v_j) && equalEdges(this.v_i_Apo, this.v_j_Apo, ep.v_i_Apo, ep.v_j_Apo)) ||
			   (equalEdges(this.v_i, this.v_j, ep.v_i_Apo, ep.v_j_Apo) && equalEdges(this.v_i_Apo, this.v_j_Apo, ep.v_i, ep.v_j));
	}
	
	/**
	 * Indicates whether two edges identified by vertex identifiers are equal.
	 * 
	 * @param e1_i the id of vertex i of edge 1
	 * @param e1_j the id of vertex j of edge 1
	 * @param e2_i the id of vertex i of edge 2
	 * @param e2_j the id of vertex j of edge 2
	 * @return <code>true</code> if both edges are equal otherwise <code>false</code>
	 * @since 1.0
	 */
	private boolean equalEdges(final int e1_i, final int e1_j, final int e2_i, final int e2_j) {
		return (e1_i == e2_i && e1_j == e2_j) || (e1_i == e2_j && e1_j == e2_i);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof EdgePair)
			return equals((EdgePair)obj);
		else
			return false;
	}
	
	/**
	 * Substitutes the specified edge pair meaning the returned instance corresponds to (v_i,v'_i), (v_j,v'_j).
	 * 
	 * @param ep the edge pair to substitute
	 * @param graph the corresponding graph
	 * @return the substituted edge pair
	 * @since 1.0
	 */
	public static EdgePair substitute(final EdgePair ep, final Graph<Vertex, Edge> graph) {
		return new EdgePair(graph, ep.v_i, ep.v_i_Apo, ep.v_j, ep.v_j_Apo);
	}

}
