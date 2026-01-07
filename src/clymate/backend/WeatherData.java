package clymate.backend;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Model class representing comprehensive weather information. This POJO
 * (Plain Old Java Object) stores current conditions, hourly forecasts, and
 * daily forecasts parsed from the API response.
 *
 * @author Malith Dissanayake
 */
public class WeatherData {

	/** Current temperature in the API's default unit (Celsius). */
	private double currentTemp;

	/** Relative humidity percentage. */
	private double humidity;

	/** Wind speed in the API's default unit. */
	private double windSpeed;

	/** Current UV index value. */
	private double uvIndex;

	/** Temperature from the previous day for comparison. */
	private double yesterdayTemp;

	/** WMO Weather code representing the condition (e.g., Rain, Clear). */
	private int weatherCode;

	/** Probability of precipitation (percentage). */
	private int precipProb;

	/** Offset in seconds from UTC for the location's timezone. */
	private int utcOffsetSeconds;

	/** Name of the city. */
	private String cityName;

	/** Name of the country. */
	private String country;

	/** Sunrise time string. */
	private String sunrise;

	/** Sunset time string. */
	private String sunset;

	/** List containing forecast data for upcoming days. */
	private List<DailyForecast> forecast = new ArrayList<>();

	/** List containing forecast data for the next 24 hours. */
	private List<HourlyForecast> hourlyForecast = new ArrayList<>();

	/**
	 * Inner class representing data for a single day's forecast.
	 */
	public static class DailyForecast {
		public String dayName;
		public String dateText;
		public double max;
		public double min;
		public double windMax;
		public int code;
		public int precip;

		/**
		 * Constructor for DailyForecast.
		 * 
		 * @param d   Day name (e.g., "Mon").
		 * @param dt  Date text (e.g., "Jan 1").
		 * @param max Maximum temperature.
		 * @param min Minimum temperature.
		 * @param w   Maximum wind speed.
		 * @param c   Weather condition code.
		 * @param p   Precipitation probability.
		 */
		public DailyForecast(String d, String dt, double max, double min, double w, int c, int p) {
			this.dayName = d;
			this.dateText = dt;
			this.max = max;
			this.min = min;
			this.windMax = w;
			this.code = c;
			this.precip = p;
		}
	}

	/**
	 * Inner class representing data for a specific hour.
	 */
	public static class HourlyForecast {
		public String time;
		public double temp;
		public double wind;
		public int code;
		public int rainChance;

		/**
		 * Constructor for HourlyForecast.
		 * 
		 * @param t  Display time string.
		 * @param te Temperature.
		 * @param c  Weather condition code.
		 * @param r  Rain probability.
		 * @param w  Wind speed.
		 */
		public HourlyForecast(String t, double te, int c, int r, double w) {
			this.time = t;
			this.temp = te;
			this.code = c;
			this.rainChance = r;
			this.wind = w;
		}
	}

	// -- Accessor Methods --

	/** @return The name of the city. */
	public String getCityName() {
		return cityName;
	}

	/** @return The name of the country. */
	public String getCountry() {
		return country;
	}

	/** @return The local time offset from UTC in seconds. */
	public int getUtcOffset() {
		return utcOffsetSeconds;
	}

	/** @return Current temperature. */
	public double getCurrentTemp() {
		return currentTemp;
	}

	/** @return Humidity percentage. */
	public double getHumidity() {
		return humidity;
	}

	/** @return Wind speed. */
	public double getWindSpeed() {
		return windSpeed;
	}

	/** @return Weather condition code. */
	public int getWeatherCode() {
		return weatherCode;
	}

	/** @return UV Index value. */
	public double getUvIndex() {
		return uvIndex;
	}

	/** @return Precipitation probability. */
	public int getPrecipProb() {
		return precipProb;
	}

	/** @return String representing sunrise time. */
	public String getSunrise() {
		return sunrise;
	}

	/** @return String representing sunset time. */
	public String getSunset() {
		return sunset;
	}

	/** @return The temperature 24 hours ago. */
	public double getYesterdayTemp() {
		return yesterdayTemp;
	}

	/** @return List of hourly forecast objects. */
	public List<HourlyForecast> getHourlyForecast() {
		return hourlyForecast;
	}

	/** @return List of daily forecast objects. */
	public List<DailyForecast> getForecast() {
		return forecast;
	}

	// -- Mutator Methods used by WeatherService parser --

	public void setBasicInfo(String c, String co, String tz, int off) {
		this.cityName = c;
		this.country = co;
		this.utcOffsetSeconds = off;
	}

	public void setCurrentData(double t, double h, double w, int c) {
		this.currentTemp = t;
		this.humidity = h;
		this.windSpeed = w;
		this.weatherCode = c;
	}

	public void setExtraData(double uv, int p, String r, String s, double yest) {
		this.uvIndex = uv;
		this.precipProb = p;
		this.sunrise = r;
		this.sunset = s;
		this.yesterdayTemp = yest;
	}

	/**
	 * Helper to add a new hourly data point.
	 * 
	 * @param t    Time
	 * @param tm   Temperature
	 * @param c    Code
	 * @param rain Rain probability
	 * @param w    Wind speed
	 */
	public void addHourly(String t, double tm, int c, int rain, double w) {
		hourlyForecast.add(new HourlyForecast(t, tm, c, rain, w));
	}

	/**
	 * Helper to add a new daily forecast point.
	 * 
	 * @param d    Day Name
	 * @param dt   Date
	 * @param max  Max Temp
	 * @param min  Min Temp
	 * @param wind Wind Speed
	 * @param c    Weather Code
	 * @param p    Precipitation
	 */
	public void addDaily(String d, String dt, double max, double min, double wind, int c, int p) {
		forecast.add(new DailyForecast(d, dt, max, min, wind, c, p));
	}
}