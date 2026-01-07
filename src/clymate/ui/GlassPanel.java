package clymate.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Custom JPanel implementation that renders a translucent background with
 * rounded corners, simulating a glass effect.
 *
 * @author Malith Dissanayake
 */
public class GlassPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private int alpha;
	private int cornerRadius = 30;
	private Color baseColor = Color.BLACK;

	/**
	 * @param alpha The opacity level (0-255).
	 */
	public GlassPanel(int alpha) {
		this.alpha = alpha;
		this.setOpaque(false);
	}

	public void setTheme(boolean isDark) {
		if (isDark) {
			baseColor = Color.BLACK;
		} else {
			// Light Mode: Milky Grayish
			baseColor = new Color(220, 225, 230);
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha));
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

		g2.dispose();
		super.paintComponent(g);
	}
}