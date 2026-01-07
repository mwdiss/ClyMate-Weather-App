package clymate.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Controller class for handling user search logic and history. Utilizes Java
 * Preferences API to persist recent searches.
 *
 * @author Malith Dissanayake
 */
public class SearchController {

	private static final int MAX_HISTORY = 5;
	// Uses a node based on the package to ensure uniqueness
	private static final Preferences prefs = Preferences.userNodeForPackage(SearchController.class);

	/**
	 * Validates input city name. Allows letters, spaces, hyphens, and periods (e.g.
	 * "St. Louis").
	 * 
	 * @param input the string to test.
	 * @return true if valid.
	 */
	public static boolean isValidInput(String input) {
		if (input == null || input.trim().length() < 2) {
			return false;
		}
		return Pattern.matches("^[a-zA-Z\\s\\-,.]+$", input);
	}

	/**
	 * Adds a city to the local search history with a timestamp.
	 * 
	 * @param cr The city result object.
	 */
	public static void addToHistory(WeatherService.CityResult cr) {
		String displayName = cr.toString();
		// Packed Format: Name|Lat|Lon|Country|TimestampMillis
		String entry = displayName + "|" + cr.getLat() + "|" + cr.getLon() + "|" + cr.getCountry() + "|"
				+ System.currentTimeMillis();

		String historyStr = prefs.get("history_v4", ""); // Versioned key
		ArrayList<String> history;

		if (historyStr.isEmpty()) {
			history = new ArrayList<>();
		} else {
			history = new ArrayList<>(List.of(historyStr.split("##")));
		}

		// Remove duplicates of the same city
		history.removeIf(s -> s.startsWith(displayName + "|"));

		// Add new to front
		history.add(0, entry);

		// Enforce max size
		if (history.size() > MAX_HISTORY) {
			history.remove(MAX_HISTORY - 1);
		}

		prefs.put("history_v4", String.join("##", history));
	}

	/**
	 * Retrieves recent search history.
	 * 
	 * @return List of raw strings representing history items.
	 */
	public static List<String> getHistory() {
		String raw = prefs.get("history_v4", "");
		if (raw.isEmpty())
			return new ArrayList<>();
		return List.of(raw.split("##"));
	}
}