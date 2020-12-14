package main;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

import lavesdk.algorithm.plugin.views.renderers.DefaultVertexRenderer;
import lavesdk.utils.MathUtils;

/**
 * A custom vertex renderer for {@link WeightedVertex}s.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class WeightedVertexRenderer extends DefaultVertexRenderer<WeightedVertex> {
	
	/** the x position of the weight */
	private int attachmentX;
	/** the y position of the weight */
	private int attachmentY;
	
	/**
	 * Creates a new renderer.
	 * 
	 * @since 1.0
	 */
	public WeightedVertexRenderer() {
		super();
		
		attachmentX = 0;
		attachmentY = 0;
	}
	
	@Override
	public void setAttachmentPoint(int x, int y) {
		super.setAttachmentPoint(x, y);
		
		attachmentX = x;
		attachmentY = y;
	}
	
	@Override
	public void draw(Graphics2D g, WeightedVertex o) {
		super.draw(g, o);
		
		final String weightAsString = MathUtils.formatFloat(o.getWeight());
		final FontMetrics fm = g.getFontMetrics();
		g.drawString(weightAsString, (attachmentX < xCenter) ? attachmentX - fm.stringWidth(weightAsString) - 2 : attachmentX + 2, (attachmentY > yCenter) ? attachmentY + fm.getAscent() + fm.getLeading() : attachmentY - 2);
	}

}
