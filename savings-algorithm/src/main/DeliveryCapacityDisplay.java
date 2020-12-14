package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import lavesdk.algorithm.plugin.views.custom.CustomVisualFormula;
import lavesdk.utils.MathUtils;

/**
 * A display to show the user the entered delivery capacity.
 * 
 * @author jdornseifer
 * @version 1.0
 */
public class DeliveryCapacityDisplay extends CustomVisualFormula {
	
	/** the background color of the display */
	private final Color capaBackground;
	/** the frame color of the display */
	private final Color capaFrame;
	
	/**
	 * Creates a new display.
	 * 
	 * @param capacity the capacity
	 * @since 1.0
	 */
	public DeliveryCapacityDisplay(final float capacity) {
		super("b_{max} = " + MathUtils.formatFloat(capacity), 5, 5);
		
		capaBackground = new Color(245, 251, 255);
		capaFrame = new Color(230, 236, 240);
	}
	
	@Override
	public void draw(Graphics2D g, Font f) {
		// first draw the formula so that the formula icon is up-to-date (for example because the zoom value changed)
		super.draw(g, f);
		
		// draw the display
		g.setColor(capaBackground);
		g.fillRect(x - 2, y - 2, getWidth() + 2, getHeight() + 2);
		g.setColor(capaFrame);
		g.drawRect(x - 2, y - 2, getWidth() + 2, getHeight() + 2);
		// draw the formula again
		super.draw(g, f);
	}

}
