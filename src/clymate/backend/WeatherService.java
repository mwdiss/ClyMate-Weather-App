package clymate.backend;

import java.net.URI;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service class handling all network communication and JSON parsing. Utilizes
 * Open-Meteo API for weather data and IP-API for geolocation.
 *
 * @author Malith Dissanayake
 */
public class WeatherService {

	/**
	 * Represents a city search result from the Geocoding API.
	 */
	public static class CityResult {
		private String name;
		private String region;
		private String country;
		private double lat;
		private double lon;

		public CityResult(JSONObject j) {
			this.name = j.getString("name");
			this.country = j.optString("country", "");
			this.region = j.optString("admin1", "");
			this.lat = j.getDouble("latitude");
			this.lon = j.getDouble("longitude");
		}

		public String getName() {
			return name;
		}

		public String getCountry() {
			return country;
		}

		public double getLat() {
			return lat;
		}

		public double getLon() {
			return lon;
		}

		@Override
		public String toString() {
			return name + (region.isEmpty() ? "" : ", " + region) + ", " + country;
		}
	}

	/**
	 * fetches the user's approximate location using their IP address.
	 * 
	 * @return The city name derived from IP-API, or "London" if failed.
	 */
	public static String getIpLocation() {
		try {
			String response = makeRequest("http://ip-api.com/json");
			JSONObject json = new JSONObject(response);
			if ("success".equals(json.optString("status"))) {
				return json.getString("city");
			}
		} catch (Exception e) {
			System.err.println("Auto-location failed: " + e.getMessage());
		}
		return "London";
	}

	/**
	 * Searches for cities matching the query string.
	 * 
	 * @param query The user's input city name.
	 * @return A list of CityResult objects matching the query.
	 * @throws Exception If network error occurs.
	 */
	public static List<CityResult> searchCities(String query) throws Exception {
		String encodedName = query.trim().replace(" ", "%20").replace(",", "%2C");
		String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedName
				+ "&count=5&language=en&format=json";

		JSONObject json = new JSONObject(makeRequest(url));
		List<CityResult> results = new ArrayList<>();

		if (json.has("results")) {
			JSONArray arr = json.getJSONArray("results");
			for (int i = 0; i < arr.length(); i++) {
				results.add(new CityResult(arr.getJSONObject(i)));
			}
		}
		return results;
	}

	/**
	 * Fetches comprehensive weather data for a specific coordinate. This integrates
	 * Current, Hourly (24h), and Daily (16d) data in one call.
	 *
	 * @param lat     Latitude
	 * @param lon     Longitude
	 * @param city    City Name (display)
	 * @param country Country Name (display)
	 * @return A populated WeatherData object.
	 * @throws Exception If API request or parsing fails.
	 */
	public static WeatherData getWeather(double lat, double lon, String city, String country) throws Exception {
		// Construct the massive API URL query
		String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s"
				+ "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
				+ "&hourly=temperature_2m,weather_code,precipitation_probability,wind_speed_10m"
				+ "&daily=temperature_2m_max,temperature_2m_min,weather_code,uv_index_max,precipitation_probability_max,wind_speed_10m_max,sunrise,sunset"
				+ "&timezone=auto&past_days=1&forecast_days=16", lat, lon);

		String resp = makeRequest(url);
		JSONObject root = new JSONObject(resp);
		JSONObject current = root.getJSONObject("current");
		JSONObject daily = root.getJSONObject("daily");
		JSONObject hourly = root.getJSONObject("hourly");

		WeatherData data = new WeatherData();
		data.setBasicInfo(city, country, root.getString("timezone"), root.getInt("utc_offset_seconds"));

		data.setCurrentData(current.getDouble("temperature_2m"), current.getDouble("relative_humidity_2m"),
				current.getDouble("wind_speed_10m"), current.getInt("weather_code"));

		data.setExtraData(daily.getJSONArray("uv_index_max").optDouble(1, 0.0),
				daily.getJSONArray("precipitation_probability_max").optInt(1, 0),
				parseTime(daily.getJSONArray("sunrise").getString(1)),
				parseTime(daily.getJSONArray("sunset").getString(1)),
				daily.getJSONArray("temperature_2m_max").optDouble(0, 0.0) // Past day 0 is yesterday
		);

		// Parse Daily Forecast
		JSONArray timeArr = daily.getJSONArray("time");
		JSONArray maxArr = daily.getJSONArray("temperature_2m_max");
		JSONArray minArr = daily.getJSONArray("temperature_2m_min");
		JSONArray codes = daily.getJSONArray("weather_code");
		JSONArray precip = daily.getJSONArray("precipitation_probability_max");
		JSONArray windMax = daily.getJSONArray("wind_speed_10m_max");

		int limit = Math.min(timeArr.length(), maxArr.length());
		DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d");

		// Start from index 2 (Tomorrow). Index 0 is yesterday, 1 is today.
		for (int i = 2; i < limit; i++) {
			LocalDate date = LocalDate.parse(timeArr.getString(i));
			data.addDaily(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), date.format(df),
					maxArr.optDouble(i, 0.0), minArr.optDouble(i, 0.0), windMax.optDouble(i, 0.0), codes.optInt(i, 0),
					precip.optInt(i, 0));
		}

		// Parse Hourly Forecast
		// Logic: Find the current hour in the city's local time, start parsing 24 hours
		// from there.
		JSONArray hTime = hourly.getJSONArray("time");
		JSONArray hTemp = hourly.getJSONArray("temperature_2m");
		JSONArray hCode = hourly.getJSONArray("weather_code");
		JSONArray hRain = hourly.getJSONArray("precipitation_probability");
		JSONArray hWind = hourly.getJSONArray("wind_speed_10m");

		String localHourStr = ZonedDateTime.now(ZoneId.of(root.getString("timezone")))
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH"));

		int startIdx = -1;
		for (int i = 0; i < hTime.length(); i++) {
			if (hTime.getString(i).startsWith(localHourStr)) {
				startIdx = i;
				break;
			}
		}

		if (startIdx != -1) {
			for (int i = 0; i < 24; i++) {
				int idx = startIdx + i;
				if (idx >= hTime.length())
					break;

				String rawT = hTime.getString(idx);
				// "Now" for first item, otherwise parse hour (e.g. 14:00)
				String display = (i == 0) ? "Now" : rawT.substring(11, 16);

				if (!display.equals("Now")) {
					int hInt = Integer.parseInt(display.substring(0, 2));
					String suffix = (hInt >= 12) ? " PM" : " AM";
					int h12 = (hInt > 12) ? hInt - 12 : ((hInt == 0) ? 12 : hInt);
					display = h12 + suffix;
				}

				data.addHourly(display, hTemp.optDouble(idx, 0.0), hCode.optInt(idx, 0), hRain.optInt(idx, 0),
						hWind.optDouble(idx, 0.0));
			}
		}

		return data;
	}

	/**
	 * Executes an HTTP GET request.
	 * 
	 * @param urlString The URL to fetch.
	 * @return The response body as a String.
	 * @throws Exception If connection fails.
	 */
	private static String makeRequest(String urlString) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(5000); // 5 sec timeout

		try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A")) {
			return scanner.hasNext() ? scanner.next() : "";
		}
	}

	/**
	 * Helper to format ISO DateTime to a readable time string (e.g. "06:30 AM").
	 */
	private static String parseTime(String iso) {
		return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				.format(DateTimeFormatter.ofPattern("hh:mm a"));
	}
}