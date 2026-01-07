package clymate.ui.components;

import javax.swing.*;
import clymate.backend.*;
import clymate.ui.ClyMateFrame;
import clymate.ui.GlassPanel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The main UI component responsible for displaying weather data in the scroll
 * pane. Organized using sections: Header, Hourly, Details, and Forecast.
 * 
 * @author Malith Dissanayake
 */
public class DashboardView extends JPanel {

	private static final long serialVersionUID = 1L;

	private final ClyMateFrame parent;
	private String currentBg = "resources/bg/day-noon.png";

	// Text Elements
	private ShadowLabel cityLabel;
	private ShadowLabel timeLabel;
	private ShadowLabel conditionLabel;
	private ShadowLabel tipText;
	private ShadowLabel tempNumberLabel;
	private ShadowLabel tempUnitLabel;
	private JLabel mainIcon;

	// Structural Containers
	private GlassPanel detailPanel;
	private GlassPanel hourlyPanel;
	private GlassPanel weeklyPanel;
	private JPanel hourlySlotsContainer;
	private JPanel contentPanel;
	private JButton leftArrow;
	private JButton rightArrow;

	// State Variables
	private boolean isDarkMode = true;
	private boolean isCelsius = true;
	private WeatherData lastData;

	private final int SLOTS_PER_PAGE = 5;
	private int hourlyPageIndex = 0;

	// Tip Ticker
	private Timer tipTimer;
	private final List<String> tips = new ArrayList<>();
	private int tipIndex = 0;

	// Collection tracking for bulk theme updates
	private final List<ShadowLabel> textElements = new ArrayList<>();
	private final List<GlassPanel> glassPanels = new ArrayList<>();

	// Theme Colors
	private Color fgPrimary;
	private Color textBg;
	private Color creditsBg;
	private static final int ICON_SIZE_DETAIL = 35;
	private static final int ICON_SIZE_HOURLY = 65;

	public DashboardView(ClyMateFrame parent) {
		this.parent = parent;
		setLayout(new BorderLayout());
		setOpaque(false);
		computeThemeColors();

		contentPanel = new JPanel(new GridBagLayout());
		contentPanel.setOpaque(false);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(10, 30, 10, 30);

		// Section 1: Header (City, Time, Main Icon)
		gbc.gridy = 0;
		gbc.insets = new Insets(80, 30, 10, 30);
		contentPanel.add(createHeader(), gbc);

		// Section 2: Scrolling Ticker (Advice)
		gbc.gridy = 1;
		gbc.insets = new Insets(0, 30, 20, 30);
		contentPanel.add(createTicker(), gbc);

		// Section 3: Horizontal Hourly Forecast Carousel
		gbc.gridy = 2;
		contentPanel.add(createHourly(), gbc);

		// Section 4: Grid of Detailed Metrics (Humidity, UV, etc)
		gbc.gridy = 3;
		contentPanel.add(createDetails(), gbc);

		// Section 5: Vertical Daily Forecast
		gbc.gridy = 4;
		contentPanel.add(createWeekly(), gbc);

		// Section 6: Footer Credits
		gbc.gridy = 5;
		gbc.insets = new Insets(16, 30, 0, 30);
		ShadowLabel credits = new ShadowLabel("  © 2026 ClyMate App | Data by Open-Meteo", 12, false);
		register(credits);
		credits.setHorizontalAlignment(SwingConstants.CENTER);
		credits.setOpaque(true);
		credits.setBackground(creditsBg);
		credits.setBorder(BorderFactory.createEmptyBorder(6, 0, 12, 0));
		contentPanel.add(credits, gbc);

		// Spacer for clean scrolling
		gbc.gridy = 6;
		contentPanel.add(Box.createVerticalStrut(50), gbc);

		JScrollPane scroll = new JScrollPane(contentPanel);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(25);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

		// Logic for Sticky Header appearing in parent when scrolling
		scroll.getViewport().addChangeListener(_ -> {
			if (scroll.getViewport().getViewPosition().y > 350 && lastData != null) {
				parent.setStickyHeaderVisible(true, lastData.getCityName(),
						formatTempInline(lastData.getCurrentTemp()));
			} else {
				parent.setStickyHeaderVisible(false, "", "");
			}
		});

		add(scroll, BorderLayout.CENTER);
		initTipTimer();
	}

	private JPanel createHeader() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setOpaque(false);
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
		g.gridy = 0;
		g.anchor = GridBagConstraints.CENTER;

		cityLabel = register(new ShadowLabel("Locating...", 42, true));
		p.add(cityLabel, g);

		g.gridy++;
		timeLabel = register(new ShadowLabel("--:--", 18, false));
		p.add(timeLabel, g);

		g.gridy++;
		mainIcon = new JLabel();
		mainIcon.setOpaque(false);
		p.add(mainIcon, g);

		g.gridy++;
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
		tempPanel.setOpaque(false);

		tempNumberLabel = register(new ShadowLabel("", 92, true));
		tempNumberLabel.setOpaque(false);

		tempUnitLabel = register(new ShadowLabel("", 34, true));
		tempUnitLabel.setOpaque(false);

		tempPanel.add(tempNumberLabel);
		tempPanel.add(tempUnitLabel);
		p.add(tempPanel, g);

		g.gridy++;
		conditionLabel = register(new ShadowLabel("", 26, true));
		p.add(conditionLabel, g);

		return p;
	}

	private JPanel createTicker() {
		JPanel w = new JPanel(new FlowLayout(FlowLayout.CENTER));
		w.setOpaque(false);

		GlassPanel gp = new GlassPanel(120);
		glassPanels.add(gp);
		gp.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

		tipText = register(new ShadowLabel("Loading...", 16, true));
		tipText.setOpaque(false);
		// Slightly reduce font size dynamically
		tipText.setFont(tipText.getFont().deriveFont(tipText.getFont().getSize2D() * 0.99f));

		gp.add(tipText);
		w.add(gp);
		return w;
	}

	private JPanel createHourly() {
		hourlyPanel = new GlassPanel(100);
		glassPanels.add(hourlyPanel);
		hourlyPanel.setLayout(new BorderLayout());
		hourlyPanel.setPreferredSize(new Dimension(0, 190));
		hourlyPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

		ShadowLabel head = register(new ShadowLabel("Hourly Forecast", 17, true));
		head.setHorizontalAlignment(SwingConstants.LEFT);
		head.setOpaque(false);
		hourlyPanel.add(head, BorderLayout.NORTH);

		JPanel carousel = new JPanel(new BorderLayout());
		carousel.setOpaque(false);
		leftArrow = mkArrow("<");
		rightArrow = mkArrow(">");
		leftArrow.addActionListener(_ -> nav(-1));
		rightArrow.addActionListener(_ -> nav(1));

		hourlySlotsContainer = new JPanel(new GridLayout(1, SLOTS_PER_PAGE, 0, 0));
		hourlySlotsContainer.setOpaque(false);

		carousel.add(leftArrow, BorderLayout.WEST);
		carousel.add(hourlySlotsContainer, BorderLayout.CENTER);
		carousel.add(rightArrow, BorderLayout.EAST);

		hourlyPanel.add(carousel, BorderLayout.CENTER);
		return hourlyPanel;
	}

	private JPanel createDetails() {
		detailPanel = new GlassPanel(130);
		glassPanels.add(detailPanel);
		detailPanel.setLayout(new GridLayout(2, 3, 10, 15));
		detailPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		return detailPanel;
	}

	private JPanel createWeekly() {
		weeklyPanel = new GlassPanel(150);
		glassPanels.add(weeklyPanel);
		weeklyPanel.setLayout(new BoxLayout(weeklyPanel, BoxLayout.Y_AXIS));
		weeklyPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
		return weeklyPanel;
	}

	public void fetchData(WeatherService.CityResult city) {
		fetchDataDirect(city.getLat(), city.getLon(), city.getName(), city.getCountry());
	}

	public void fetchDataDirect(double lat, double lon, String name, String country) {
		new SwingWorker<WeatherData, Void>() {
			@Override
			protected WeatherData doInBackground() throws Exception {
				return WeatherService.getWeather(lat, lon, name, country);
			}

			@Override
			protected void done() {
				try {
					lastData = get();
					populateUI(lastData);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}.execute();
	}

	private void populateUI(WeatherData d) {
		cityLabel.setText(d.getCityName() + ", " + d.getCountry());
		setMainTemp(d.getCurrentTemp());
		conditionLabel.setText(AssetUtils.getWeatherConditionText(d.getWeatherCode()));
		mainIcon.setIcon(AssetUtils.getWeatherIcon(d.getWeatherCode(), true, 110, 110));

		ZonedDateTime zdt = ZonedDateTime.now(java.time.ZoneOffset.ofTotalSeconds(d.getUtcOffset()));
		String sign = zdt.getOffset().getTotalSeconds() >= 0 ? "+" : "";
		timeLabel.setText(zdt.format(DateTimeFormatter.ofPattern("hh:mm a | EEE, MMM d")) + " (GMT" + sign
				+ zdt.getOffset().getId().replace("Z", "0") + ")");

		// Generate Tips based on weather comparison (e.g. Warmer/Cooler)
		tips.clear();
		double diffVal = d.getCurrentTemp() - d.getYesterdayTemp();
		String diff = Math.abs(diffVal) < 1 ? "Similar temperature to yesterday."
				: (formatDiffInline(diffVal) + (diffVal > 0 ? " Warmer" : " Cooler") + " than yesterday.");

		tips.add(diff);
		tips.add(AssetUtils.getSmartAdvice(d));
		restartTipTimer();

		hourlyPageIndex = 0;
		updateCarousel();

		// Populate Detail Grid
		detailPanel.removeAll();
		addDetail("humidity.svg", "Humidity", (int) d.getHumidity() + "%");
		addDetail("wind1.svg", "Wind", formatSpeed(d.getWindSpeed()));
		addDetail("uv-index.svg", "UV Index", "" + d.getUvIndex());
		addDetail("rain-chance.svg", "Rain Chance", d.getPrecipProb() + "%");
		addDetail("sunrise.svg", "Sunrise", d.getSunrise());
		addDetail("sunset.svg", "Sunset", d.getSunset());

		// Populate Weekly List
		weeklyPanel.removeAll();
		JPanel headP = new JPanel(new BorderLayout());
		headP.setOpaque(false);
		ShadowLabel head = register(new ShadowLabel("16-Day Forecast", 17, true));
		head.setHorizontalAlignment(SwingConstants.LEFT);
		head.setOpaque(false);
		headP.add(head, BorderLayout.WEST);
		headP.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		weeklyPanel.add(headP);

		for (WeatherData.DailyForecast day : d.getForecast()) {
			JPanel row = new JPanel(new BorderLayout());
			row.setOpaque(false);
			row.setMaximumSize(new Dimension(3000, 50));

			ShadowLabel dayLabel = register(new ShadowLabel(day.dayName + " " + day.dateText, 16, true));
			dayLabel.setPreferredSize(new Dimension(140, 30));
			dayLabel.setHorizontalAlignment(SwingConstants.LEFT);
			dayLabel.setOpaque(false);

			JPanel center = new JPanel(new FlowLayout());
			center.setOpaque(false);
			center.add(new JLabel(AssetUtils.getWeatherIcon(day.code, true, 28, 28)));

			ShadowLabel cond = new ShadowLabel(AssetUtils.getWeatherConditionText(day.code), 14, false);
			cond.setTheme(isDarkMode);
			cond.setForeground(fgPrimary);
			center.add(cond);

			JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			right.setOpaque(false);
			right.add(new JLabel(AssetUtils.getIcon("wind1.svg", 18, 18)));

			ShadowLabel wL = new ShadowLabel(formatSpeed(day.windMax), 14, false);
			wL.setTheme(isDarkMode);
			wL.setForeground(fgPrimary);
			right.add(wL);

			right.add(Box.createHorizontalStrut(10));
			right.add(new JLabel(AssetUtils.getIcon("rain-chance.svg", 18, 18)));

			ShadowLabel rL = new ShadowLabel(day.precip + "%", 14, false);
			rL.setTheme(isDarkMode);
			rL.setForeground(isDarkMode ? new Color(130, 210, 255) : new Color(50, 100, 255));
			right.add(rL);

			right.add(Box.createHorizontalStrut(15));
			ShadowLabel tL = new ShadowLabel(formatTempInline(day.max) + " / " + formatTempInline(day.min), 16, true);
			tL.setTheme(isDarkMode);
			tL.setForeground(fgPrimary);
			right.add(tL);

			row.add(dayLabel, BorderLayout.WEST);
			row.add(center, BorderLayout.CENTER);
			row.add(right, BorderLayout.EAST);
			weeklyPanel.add(row);
			JSeparator s = new JSeparator();
			s.setForeground(new Color(255, 255, 255, 30));
			weeklyPanel.add(s);
		}

		// Set Background based on condition and time
		currentBg = AssetUtils.getBackgroundPath(d.getWeatherCode(), d.getUtcOffset());
		parent.repaint();
		revalidate();
	}

	private void updateCarousel() {
		hourlySlotsContainer.removeAll();
		if (lastData == null) {
			hourlySlotsContainer.revalidate();
			return;
		}
		List<WeatherData.HourlyForecast> list = lastData.getHourlyForecast();
		int s = hourlyPageIndex * SLOTS_PER_PAGE;
		int e = Math.min(s + SLOTS_PER_PAGE, list.size());

		for (int i = s; i < e; i++) {
			WeatherData.HourlyForecast h = list.get(i);
			JPanel cell = new JPanel(new GridBagLayout());
			cell.setOpaque(false);
			GridBagConstraints g = new GridBagConstraints();
			g.gridx = 0;
			g.gridy = 0;

			ShadowLabel t = register(new ShadowLabel(h.time, 16, false));
			t.setOpaque(false);
			cell.add(t, g);
			g.gridy++;

			cell.add(new JLabel(AssetUtils.getWeatherIcon(h.code, true, ICON_SIZE_HOURLY, ICON_SIZE_HOURLY)), g);
			g.gridy++;

			ShadowLabel temp = register(new ShadowLabel(formatTempInline(h.temp), 18, true));
			temp.setOpaque(false);
			cell.add(temp, g);
			g.gridy++;

			JPanel rP = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
			rP.setOpaque(false);
			rP.add(new JLabel(AssetUtils.getIcon("rain-chance.svg", 18, 18)));

			ShadowLabel rT = new ShadowLabel(h.rainChance + "%", 15, true);
			rT.setTheme(isDarkMode);
			rT.setForeground(isDarkMode ? new Color(130, 210, 255) : new Color(50, 100, 255));
			rP.add(rT);
			cell.add(rP, g);

			hourlySlotsContainer.add(cell);
		}

		leftArrow.setEnabled(hourlyPageIndex > 0);
		rightArrow.setEnabled(e < list.size());
		hourlySlotsContainer.revalidate();
		hourlySlotsContainer.repaint();
	}

	private void nav(int d) {
		if (lastData == null)
			return;
		int max = (int) Math.ceil((double) lastData.getHourlyForecast().size() / SLOTS_PER_PAGE);
		hourlyPageIndex = Math.max(0, Math.min(hourlyPageIndex + d, max - 1));
		updateCarousel();
	}

	private void addDetail(String icon, String label, String value) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		p.setOpaque(false);

		JLabel ico = new JLabel(AssetUtils.getIcon(icon, ICON_SIZE_DETAIL, ICON_SIZE_DETAIL));
		ico.setOpaque(false);
		p.add(ico);

		ShadowLabel lbl = register(new ShadowLabel(label + ":", 15, false));
		lbl.setHorizontalAlignment(SwingConstants.LEFT);
		lbl.setOpaque(false);
		p.add(lbl);

		ShadowLabel val = register(new ShadowLabel(value, 17, true));
		val.setHorizontalAlignment(SwingConstants.LEFT);
		val.setOpaque(false);
		p.add(val);

		detailPanel.add(p);
	}

	private void setMainTemp(double c) {
		long n = Math.round(isCelsius ? c : (c * 1.8 + 32));
		tempNumberLabel.setText(String.valueOf(n));
		tempUnitLabel.setText(isCelsius ? "C°" : "F°");
	}

	private String formatTempInline(double c) {
		return Math.round(isCelsius ? c : (c * 1.8 + 32)) + (isCelsius ? "°" : "°");
	}

	private String formatDiffInline(double c) {
		return Math.round(isCelsius ? c : (c * 1.8)) + (isCelsius ? "°" : "°");
	}

	private String formatSpeed(double k) {
		return isCelsius ? Math.round(k) + " km/h" : String.format("%.1f mph", k / 1.609);
	}

	private JButton mkArrow(String txt) {
		JButton b = new JButton(txt);
		b.setContentAreaFilled(false);
		b.setBorder(null);
		b.setForeground(fgPrimary);
		b.setFont(new Font("Consolas", Font.BOLD, 30));
		b.setCursor(new Cursor(Cursor.HAND_CURSOR));
		b.setOpaque(false);
		return b;
	}

	private ShadowLabel register(ShadowLabel l) {
		l.setTheme(isDarkMode);
		l.setForeground(fgPrimary);
		textElements.add(l);
		return l;
	}

	private void initTipTimer() {
		if (tipTimer != null)
			tipTimer.stop();
		tipTimer = new Timer(3000, _ -> {
			if (tips.isEmpty())
				return;
			tipIndex = (tipIndex + 1) % tips.size();
			tipText.setText(tips.get(tipIndex));
		});
		tipTimer.setRepeats(true);
	}

	private void restartTipTimer() {
		if (tipTimer == null)
			initTipTimer();
		tipTimer.stop();
		if (!tips.isEmpty()) {
			tipIndex = 0;
			tipText.setText(tips.get(0));
			tipTimer.restart();
		}
	}

	private void computeThemeColors() {
		if (isDarkMode) {
			fgPrimary = new Color(235, 245, 255);
			textBg = new Color(0, 0, 0, 110);
			creditsBg = new Color(0, 0, 0, 180);
		} else {
			fgPrimary = new Color(20, 30, 40);
			textBg = new Color(255, 255, 255, 180);
			creditsBg = new Color(255, 255, 255, 220);
		}
	}

	public String getCurrentBg() {
		return currentBg;
	}

	public void updateTheme(boolean d) {
		isDarkMode = d;
		computeThemeColors();

		for (ShadowLabel l : textElements) {
			l.setTheme(isDarkMode);
			l.setForeground(fgPrimary);
			if (l.isOpaque())
				l.setBackground(textBg);
		}
		for (GlassPanel g : glassPanels)
			g.setTheme(isDarkMode);

		if (leftArrow != null) {
			leftArrow.setForeground(fgPrimary);
			rightArrow.setForeground(fgPrimary);
		}
		// Force redraw of manually painted items in loop
		if (lastData != null)
			populateUI(lastData);
		repaint();
	}

	public void toggleUnits() {
		isCelsius = !isCelsius;
		if (lastData != null)
			populateUI(lastData);
	}
}