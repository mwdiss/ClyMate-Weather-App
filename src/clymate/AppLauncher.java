package clymate;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;
import clymate.ui.ClyMateFrame;

/**
 * The main entry point for the ClyMate Weather Application. This class handles
 * the initial configuration of the Swing Look and Feel and launches the main
 * application window on the Event Dispatch Thread.
 *
 * @author Malith Dissanayake
 */
public class AppLauncher {

	/**
	 * The main method executes the application. It suppresses unnecessary logs from
	 * the SVG library and initializes the FlatLaf theme.
	 *
	 * @param args Command line arguments (not used).
	 */
	public static void main(String[] args) {
		// Silence JSVG CSS Warnings to keep console clean
		Logger.getLogger("com.github.weisj.jsvg.parser.css.impl.SimpleCssParser").setLevel(Level.OFF);
		Logger.getLogger("com.github.weisj.jsvg.parser.css.impl.Lexer").setLevel(Level.OFF);

		// Enable modern window decorations provided by FlatLaf
		System.setProperty("flatlaf.useWindowDecorations", "true");
		System.setProperty("flatlaf.menuBarEmbedded", "true");

		// Setup the specific Look and Feel
		FlatLightLaf.setup();

		// Launch the UI on the Swing Event Dispatch Thread (EDT) for thread safety
		SwingUtilities.invokeLater(() -> {
			ClyMateFrame frame = new ClyMateFrame();
			frame.setVisible(true);
		});
	}
}