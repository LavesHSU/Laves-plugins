package main;

import lavesdk.math.graph.Vertex;
import lavesdk.serialization.Serializer;

/**
 * Represents a weighted vertex in a graph.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class WeightedVertex extends Vertex {
	
	/** the weight of the vertex */
	private float weight;

	/**
	 * Creates a new weighted vertex.
	 * 
	 * @param caption the caption of the vertex
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>if caption is null</li>
	 * </ul>
	 * @since 1.0
	 */
	public WeightedVertex(String caption) throws IllegalArgumentException {
		super(caption);
	}
	
	/**
	 * Gets the weight of the vertex.
	 * 
	 * @return the vertex' weight
	 * @since 1.0
	 */
	public float getWeight() {
		return weight;
	}
	
	/**
	 * Sets the weight of the vertex.
	 * 
	 * @param weight the weight
	 * @since 1.0
	 */
	public void setWeight(final float weight) {
		this.weight = weight;
	}
	
	@Override
	public void serialize(Serializer s) {
		super.serialize(s);
		
		s.addFloat("weight", weight);
	}
	
	@Override
	public void deserialize(Serializer s) {
		super.deserialize(s);
		
		weight = s.getFloat("weight");
	}

}
