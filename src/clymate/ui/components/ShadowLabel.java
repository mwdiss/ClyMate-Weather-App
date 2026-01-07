package clymate.ui.components;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Extension of JLabel that renders a drop shadow behind the text. Critical for
 * readability against dynamic photographic backgrounds.
 *
 * @author Malith Dissanayake
 */
public class ShadowLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private Color shadowColor;
	private Color textColor;

	public ShadowLabel(String text, int size, boolean bold) {
		super(text);
		setFont(new java.awt.Font("Segoe UI", bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN, size));
		setHorizontalAlignment(CENTER);
		setTheme(true);
	}

	public ShadowLabel(String text) {
		this(text, 14, false);
	}

	public void setTheme(boolean isDark) {
		if (isDark) {
			textColor = Color.WHITE;
			shadowColor = new Color(0, 0, 0, 180);
		} else {
			// Light Mode: No shadow, dark text
			textColor = new Color(20, 30, 40);
			shadowColor = new Color(0, 0, 0, 0);
		}
		setForeground(textColor);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 1. Draw Bubble Background if Opaque
		if (isOpaque()) {
			g2.setColor(getBackground());
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
		}

		// 2. Draw Shadow offset by 1px
		if (shadowColor.getAlpha() > 0) {
			g2.setColor(shadowColor);
			g2.drawString(getText(), getInsets().left + 1, getFontMetrics(getFont()).getAscent() + getInsets().top + 1);
		}

		// 3. Draw Foreground Text
		g2.setColor(getForeground());
		g2.drawString(getText(), getInsets().left, getFontMetrics(getFont()).getAscent() + getInsets().top);

		g2.dispose();
	}
}