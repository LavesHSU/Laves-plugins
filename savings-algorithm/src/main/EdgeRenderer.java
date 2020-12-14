package main;

import java.awt.Graphics2D;

import lavesdk.algorithm.plugin.views.renderers.DefaultEdgeRenderer;
import lavesdk.math.graph.Edge;

public class EdgeRenderer extends DefaultEdgeRenderer<Edge> {
	
	@Override
	public void draw(Graphics2D g, Edge o) {
		// the label of directed edges may not be painted
		paintLabels = !o.isDirected();
		super.draw(g, o);
	}

}
