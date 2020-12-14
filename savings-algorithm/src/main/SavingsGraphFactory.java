package main;

import lavesdk.math.graph.Edge;
import lavesdk.math.graph.GraphFactory;

/**
 * The graph factory of a graph of the Savings algorithm.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class SavingsGraphFactory extends GraphFactory<WeightedVertex, Edge> {

	@Override
	public WeightedVertex createVertex(String caption) throws IllegalArgumentException {
		return new WeightedVertex(caption);
	}

	@Override
	public Edge createEdge(WeightedVertex predecessor, WeightedVertex successor) throws IllegalArgumentException {
		return createEdge(predecessor, successor, false);
	}

	@Override
	public Edge createEdge(WeightedVertex predecessor, WeightedVertex successor, boolean directed) throws IllegalArgumentException {
		return createEdge(predecessor, successor, directed, 0.0f);
	}

	@Override
	public Edge createEdge(WeightedVertex predecessor, WeightedVertex successor, float weight) throws IllegalArgumentException {
		return createEdge(predecessor, successor, false, weight);
	}

	@Override
	public Edge createEdge(WeightedVertex predecessor, WeightedVertex successor, boolean directed, float weight) throws IllegalArgumentException {
		return new Edge(predecessor, successor, directed, weight);
	}

}
