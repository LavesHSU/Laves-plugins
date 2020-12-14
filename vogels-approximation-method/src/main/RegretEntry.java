package main;

import java.io.Serializable;

/**
 * Represents an entry in the map of the regrets.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class RegretEntry implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/** the id of the vertex v */
	public final int v;
	/** the id of the vertex v1 of the regret */
	public final int v1;
	/** the id of the vertex v2 of the regret */
	public final int v2;
	/** the regret */
	public final float regret;
	
	/**
	 * Creates a new regret entry.
	 * 
	 * @param v the id of the vertex v
	 * @param v1 the id of the vertex v1 of the regret
	 * @param v2 the id of the vertex v2 of the regret
	 * @param regret the regret
	 * @since 1.0
	 */
	public RegretEntry(final int v, final int v1, final int v2, final float regret) {
		this.v = v;
		this.v1 = v1;
		this.v2 = v2;
		this.regret = regret;
	}
	
	/**
	 * Indicates whether this regret entry equals the specified one meaning that <code>this.v == e.v && this.regret == e.regret</code>.
	 * 
	 * @param e another entry
	 * @return <code>true</code> if the entries are equal otherwise <code>false</code>
	 * @since 1.0
	 */
	public boolean equals(RegretEntry e) {
		if(e == null)
			return false;
		else
			return this.v == e.v && this.regret == e.regret;
	}
	
	/**
	 * Indicates whether this regret entry equals the specified one meaning that <code>this.v == e.v && this.regret == e.regret</code>.
	 * 
	 * @param obj another entry
	 * @return <code>true</code> if the entries are equal otherwise <code>false</code>
	 * @since 1.0
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof RegretEntry)
			return equals((RegretEntry)obj);
		else
			return false;
	}
	
	@Override
	public String toString() {
		return "regret=" + regret + "; v=" + v + "(id), v1=" + v1 + "(id), v2=" + v2 + "(id)";
	}
	
}
