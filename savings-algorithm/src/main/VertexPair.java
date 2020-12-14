package main;

import java.io.Serializable;

/**
 * Represents a vertex pair.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class VertexPair implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/** the id of the vertex v_i */
	public final int v_i;
	/** the id of the vertex v_j */
	public final int v_j;
	/** the savings */
	public final float savings;
	/** the id of the vertex pair which is the concatenation of {@link #v_i} and {@link #v_j} */
	public final int id;
	
	/**
	 * Creates a new vertex pair.
	 * 
	 * @param v_i the id of the vertex v_i
	 * @param v_j the id of the vertex v_j
	 * @param savings the savings value
	 */
	public VertexPair(final int v_i, final int v_j, final float savings) {
		this.v_i = v_i;
		this.v_j = v_j;
		this.savings = savings;
		this.id = v_i * 10 + v_j;
	}
	
	/**
	 * Indicates whether this vertex pair equals the specified one meaning that
	 * <code>(this.v_i == e.v_i && this.v_j == e.v_j || this.v_i == e.v_j && this.v_j == e.v_i) && this.savings == e.savings</code>.
	 * 
	 * @param e another entry
	 * @return <code>true</code> if the entries are equal otherwise <code>false</code>
	 * @since 1.0
	 */
	public boolean equals(VertexPair e) {
		if(e == null)
			return false;
		else
			return (this.v_i == e.v_i && this.v_j == e.v_j || this.v_i == e.v_j && this.v_j == e.v_i) && this.savings == e.savings;
	}
	
	/**
	 * Indicates whether this vertex pair equals the specified one meaning that
	 * <code>(this.v_i == e.v_i && this.v_j == e.v_j || this.v_i == e.v_j && this.v_j == e.v_i) && this.savings == e.savings</code>.
	 * 
	 * @param obj another entry
	 * @return <code>true</code> if the entries are equal otherwise <code>false</code>
	 * @since 1.0
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof VertexPair)
			return equals((VertexPair)obj);
		else
			return false;
	}
	
	@Override
	public String toString() {
		return "savings=" + savings + "; v_i=" + v_i + "(id), v_j=" + v_j + "(id)";
	}

}
