package clymate.backend;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 * Utility class for managing assets (images, icons) and creating dynamic
 * resource paths based on weather data.
 *
 * @author Malith Dissanayake
 */
public class AssetUtils {

	// Caches to improve performance and reduce disk I/O
	private static final Map<String, FlatSVGIcon> iconCache = new HashMap<>();
	private static final Map<String, ImageIcon> bgCache = new HashMap<>();

	/**
	 * Translates WMO weather codes to human readable strings.
	 */
	public static String getWeatherConditionText(int code) {
		if (code == 0)
			return "Clear Sky";
		if (code <= 3)
			return "Partly Cloudy";
		if (code <= 48)
			return "Fog / Mist";
		if (code <= 57)
			return "Drizzle";
		if (code <= 67)
			return "Rain";
		if (code <= 77)
			return "Snow";
		if (code <= 82)
			return "Showers";
		if (code <= 86)
			return "Snow Showers";
		if (code >= 95)
			return "Thunderstorm";
		return "Unknown";
	}

	/**
	 * Generates context-aware advice based on weather parameters.
	 */
	public static String getSmartAdvice(WeatherData d) {
		int code = d.getWeatherCode();
		if (code >= 95)
			return "Storm Warning: Stay Indoors";
		if ((code >= 51 && code <= 67) || d.getPrecipProb() > 40)
			return "Rain Likely: Grab an Umbrella";
		if (code >= 71)
			return "Snowfall: Drive Carefully";
		if (code >= 45 && code <= 48)
			return "Low Visibility: Caution";

		// Time-based advice using offset
		if (d.getUtcOffset() != 0) {
			int h = ZonedDateTime.now(ZoneOffset.ofTotalSeconds(d.getUtcOffset())).getHour();
			if (h >= 6 && h <= 7)
				return "Golden Hour: Watch the Sunrise";
			if (h >= 17 && h <= 18)
				return "Golden Hour: Catch the Sunset";
		}

		if (d.getUvIndex() > 7)
			return "Extreme UV: Wear Sunscreen";
		if (d.getHumidity() > 90)
			return "Very Humid Today";

		return "Enjoy your day";
	}

	/**
	 * Selects appropriate weather icon based on code and day/night cycle.
	 */
	public static FlatSVGIcon getWeatherIcon(int code, boolean isDay, int w, int h) {
		String name;
		if (code >= 95)
			name = "thunder.svg";
		else if (code >= 71)
			name = "snowy-6.svg";
		else if (code >= 51 || code >= 80)
			name = "rainy-6.svg";
		else if (code >= 45)
			name = isDay ? "fog-day.svg" : "fog-night.svg";
		else if (code >= 1 && code <= 3)
			name = isDay ? "cloudy-day-3.svg" : "cloudy-night-3.svg";
		else
			name = isDay ? "sun.svg" : "moon.svg";

		return getIcon(name, w, h);
	}

	/**
	 * Loads and caches an SVG icon.
	 */
	public static FlatSVGIcon getIcon(String name, int w, int h) {
		String key = name + "_" + w + "_" + h;
		if (iconCache.containsKey(key))
			return iconCache.get(key);
		try {
			File f = new File("resources/icons/" + name);
			if (!f.exists())
				f = new File("resources/icons/sun.svg"); // Fail-safe default

			if (f.exists()) {
				FlatSVGIcon icon = new FlatSVGIcon(f).derive(w, h);
				iconCache.put(key, icon);
				return icon;
			}
		} catch (Exception e) {
			// Squelch errors for missing resources
		}
		return null;
	}

	/**
	 * Loads and caches a background image.
	 */
	public static ImageIcon getImage(String path) {
		if (bgCache.containsKey(path))
			return bgCache.get(path);
		try {
			File f = new File(path);
			if (f.exists()) {
				ImageIcon img = new ImageIcon(f.getAbsolutePath());
				bgCache.put(path, img);
				return img;
			}
		} catch (Exception e) {
			// Squelch
		}
		return null;
	}

	/**
	 * Determines the correct dynamic background file path based on logic. Logic:
	 * Combines Weather Code + Local Time of City.
	 */
	public static String getBackgroundPath(int code, int utcOffsetSeconds) {
		int hour = ZonedDateTime.now(ZoneOffset.ofTotalSeconds(utcOffsetSeconds)).getHour();
		boolean isNight = (hour >= 19 || hour < 5);
		String s = (hour >= 5 && hour < 12) ? "morning" : (hour >= 12 && hour < 17) ? "noon" : "evening";
		String b = "resources/bg/";

		if (code >= 95)
			return b + "thunder-" + (isNight ? "night.png" : (hour < 12 ? "morning.png" : "evening.png"));
		if (code >= 71)
			return b + "snow-heavy.png";
		if (code >= 45 && code <= 48)
			return b + "fog-" + s + ".png";
		if ((code >= 51 && code <= 67) || code >= 80)
			return b + "rain-" + s + ".png";
		if (code >= 1 && code <= 3)
			return b + "cloud-" + (isNight ? "night.png" : s + ".png");

		return b + (isNight ? ("moon-" + (hour < 22 ? "evening.png" : "night.png")) : ("sun-" + s + ".png"));
	}
}