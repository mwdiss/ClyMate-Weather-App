package clymate.ui;

import javax.swing.*;
import clymate.backend.*;
import clymate.ui.components.DashboardView;
import clymate.ui.components.ShadowLabel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Main application window frame. Implements a JLayeredPane architecture to
 * manage UI depth (Background, Content, Floating Overlays).
 *
 * @author Malith Dissanayake
 */
public class ClyMateFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private DashboardView dashboard;
	private JTextField searchBar;
	private JButton themeToggle;
	private JButton unitToggle;
	private JPanel topBarPanel;
	private JLayeredPane mainContent;

	// Overlays for Search Results and Errors
	private GlassPanel selectionPanel;
	private GlassPanel errorPanel;
	private JList<Object> cityList;

	// Application State
	private boolean isDarkMode = true;
	private boolean isCelsius = true;

	// "Sticky" header element that appears when scrolling down
	private JPanel stickyHeader;
	private JLabel stickyCity;
	private JLabel stickyTemp;
	private Color stickyBgColor = new Color(30, 30, 30, 220);

	/**
	 * Constructor initializes the UI components and layout.
	 */
	public ClyMateFrame() {
		setTitle("ClyMate Weather");
		setSize(1000, 750);
		setMinimumSize(new Dimension(850, 600));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		mainContent = new JLayeredPane() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				// Paint Dynamic Background Image
				ImageIcon img = AssetUtils.getImage(dashboard.getCurrentBg());
				if (img != null) {
					g.drawImage(img.getImage(), 0, 0, getWidth(), getHeight(), this);
				}
				// Overlay semi-transparent tint for readability
				g.setColor(new Color(isDarkMode ? 0 : 255, isDarkMode ? 0 : 255, isDarkMode ? 0 : 255,
						isDarkMode ? 60 : 40));
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		setContentPane(mainContent);

		dashboard = new DashboardView(this);
		// Z-Index 0: Dashboard Content
		mainContent.add(dashboard, JLayeredPane.DEFAULT_LAYER);

		createTopBar();
		createStickyHeader();
		createOverlays();

		// Ensure overlays appear above content
		mainContent.add(selectionPanel, Integer.valueOf(400));
		mainContent.add(errorPanel, Integer.valueOf(500));

		// Click Listener to dismiss popups when clicking outside
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_PRESSED) {
				MouseEvent me = (MouseEvent) event;
				if (selectionPanel.isVisible()) {
					Point p = me.getLocationOnScreen();
					SwingUtilities.convertPointFromScreen(p, selectionPanel);
					boolean insideList = selectionPanel.contains(p);

					Point p2 = me.getLocationOnScreen();
					SwingUtilities.convertPointFromScreen(p2, searchBar);
					boolean insideSearch = searchBar.contains(p2);

					if (!insideList && !insideSearch) {
						SwingUtilities.invokeLater(() -> selectionPanel.setVisible(false));
					}
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK);

		// Component Resizing Logic
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				int w = getWidth();
				int h = getHeight();
				dashboard.setBounds(0, 0, w, h);
				if (topBarPanel != null) {
					topBarPanel.setBounds(20, 20, w - 55, 50);
				}
				if (stickyHeader != null && stickyHeader.isVisible()) {
					Dimension d = stickyHeader.getPreferredSize();
					stickyHeader.setBounds((w - (d.width + 40)) / 2, 85, d.width + 40, 40);
				}
				resizeOverlays();
				mainContent.repaint();
			}
		});

		// Start async geolocation task on startup
		SwingUtilities.invokeLater(this::startAutoLocate);
		applyTheme();
	}

	private void createStickyHeader() {
		stickyHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8)) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(stickyBgColor);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
			}
		};
		stickyHeader.setOpaque(false);
		stickyCity = new JLabel("");
		stickyCity.setFont(new Font("Segoe UI", Font.BOLD, 14));

		stickyTemp = new JLabel("");
		stickyTemp.setFont(new Font("Segoe UI", Font.PLAIN, 14));

		stickyHeader.add(stickyCity);
		stickyHeader.add(new JLabel("|") {
			{
				setForeground(Color.GRAY);
			}
		});
		stickyHeader.add(stickyTemp);
		stickyHeader.setVisible(false);

		mainContent.add(stickyHeader, Integer.valueOf(300));
	}

	public void setStickyHeaderVisible(boolean visible, String city, String temp) {
		if (visible) {
			stickyCity.setText(city);
			stickyTemp.setText(temp);
			Dimension d = stickyHeader.getPreferredSize();
			int w = d.width + 40;
			stickyHeader.setBounds((getWidth() - w) / 2, 85, w, 40);
			// Hide sticky header if search dropdown is open to prevent visual clutter
			if (!stickyHeader.isVisible() && !selectionPanel.isVisible()) {
				stickyHeader.setVisible(true);
			}
		} else {
			if (stickyHeader.isVisible())
				stickyHeader.setVisible(false);
		}
	}

	private void createTopBar() {
		topBarPanel = new JPanel(new GridBagLayout());
		topBarPanel.setOpaque(false);
		// Initial bounds, resized later
		topBarPanel.setBounds(20, 20, 945, 50);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		searchBar = new JTextField();
		searchBar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search City...");
		searchBar.putClientProperty(FlatClientProperties.STYLE, "arc:999; margin:0,20,0,10; borderWidth:0");
		searchBar.setFont(new Font("Segoe UI", Font.PLAIN, 16));

		// Trigger search on Enter Key
		searchBar.addActionListener(_ -> resolveCity(searchBar.getText()));

		searchBar.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (stickyHeader.isVisible())
					stickyHeader.setVisible(false);
				// Show history dropdown if search bar empty
				if (searchBar.getText().isEmpty() && !selectionPanel.isVisible()) {
					showHistory();
				}
			}
		});
		topBarPanel.add(searchBar, gbc);

		gbc.weightx = 0;
		gbc.insets = new Insets(0, 15, 0, 0);
		JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		btnWrap.setOpaque(false);

		unitToggle = createSolidButton("");
		themeToggle = createSolidButton("");

		// Unit Switch logic (Requirement 6)
		unitToggle.addActionListener(_ -> {
			dashboard.toggleUnits();
			isCelsius = !isCelsius;
			updateButtonIcons();
		});

		// Theme Switch logic (Dark/Light Mode)
		themeToggle.addActionListener(_ -> {
			isDarkMode = !isDarkMode;
			applyTheme();
		});

		btnWrap.add(unitToggle);
		btnWrap.add(themeToggle);
		topBarPanel.add(btnWrap, gbc);

		mainContent.add(topBarPanel, Integer.valueOf(200));
	}

	private void updateButtonIcons() {
		unitToggle.setIcon(AssetUtils.getIcon(isCelsius ? "celsius1.svg" : "fahrenheit1.svg", 27, 27));
		themeToggle.setIcon(AssetUtils.getIcon(isDarkMode ? "moon1.svg" : "sun1.svg", 25, 25));
	}

	/**
	 * Resolves city string to geo-coordinates via threading (SwingWorker).
	 * 
	 * @param query The input string.
	 */
	private void resolveCity(String query) {
		if (!SearchController.isValidInput(query)) {
			showError("Invalid characters.");
			return;
		}
		selectionPanel.setVisible(false);

		// Run API call in background thread (Rule 18: Threading)
		new SwingWorker<List<WeatherService.CityResult>, Void>() {
			@Override
			protected List<WeatherService.CityResult> doInBackground() throws Exception {
				return WeatherService.searchCities(query);
			}

			@Override
			protected void done() {
				try {
					List<WeatherService.CityResult> res = get();
					if (res.isEmpty()) {
						showError("City not found");
					} else if (res.size() == 1) {
						SearchController.addToHistory(res.get(0));
						dashboard.fetchData(res.get(0));
					} else {
						// Ambiguous result - ask user to select from list
						showSelection(res);
					}
				} catch (Exception e) {
					showError("Connection Failed");
				}
			}
		}.execute();
	}

	private void showHistory() {
		List<String> rawHistory = SearchController.getHistory();
		if (rawHistory.isEmpty())
			return;

		DefaultListModel<Object> m = (DefaultListModel<Object>) cityList.getModel();
		m.clear();
		m.addElement("  Recent Searches:");

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");

		for (String s : rawHistory) {
			String[] parts = s.split("\\|");
			// Requirement 8: Show Timestamp in history
			String display = parts[0];
			if (parts.length > 4) {
				long time = Long.parseLong(parts[4]);
				display += " (" + sdf.format(new Date(time)) + ")";
			}
			m.addElement(display);
		}
		selectionPanel.setVisible(true);
		resizeOverlays();
		mainContent.moveToFront(selectionPanel);
	}

	private void createOverlays() {
		errorPanel = new GlassPanel(200);
		errorPanel.setBackground(new Color(220, 40, 40));
		errorPanel.add(new ShadowLabel("Error"));
		errorPanel.setVisible(false);

		selectionPanel = new GlassPanel(255);
		selectionPanel.setLayout(new BorderLayout());
		selectionPanel.setVisible(false);

		cityList = new JList<>(new DefaultListModel<>());
		cityList.setBackground(new Color(0, 0, 0, 0));
		cityList.setFixedCellHeight(40);

		// Custom Renderer for History/Results list
		cityList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				String text = value.toString();
				boolean isHeader = text.startsWith("  ");
				JLabel l = (JLabel) super.getListCellRendererComponent(list, text, index, isSelected && !isHeader,
						cellHasFocus);

				l.setOpaque(isSelected && !isHeader);
				l.setBackground(isSelected ? new Color(50, 100, 200) : null);

				if (isHeader) {
					l.setForeground(Color.GRAY);
					l.setFont(new Font("Segoe UI", Font.BOLD, 12));
				} else {
					l.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
					l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
				}
				return l;
			}
		});

		// Handle Click Selection
		cityList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Object val = cityList.getSelectedValue();
				if (val == null || val.toString().startsWith("  "))
					return;

				int idx = cityList.getSelectedIndex();
				if (val instanceof WeatherService.CityResult) {
					WeatherService.CityResult cr = (WeatherService.CityResult) val;
					dashboard.fetchData(cr);
					SearchController.addToHistory(cr);
				} else if (val instanceof String) {
					List<String> rawHist = SearchController.getHistory();
					// Offset by 1 for header
					if (idx - 1 < rawHist.size()) {
						String raw = rawHist.get(idx - 1);
						String[] p = raw.split("\\|");
						if (p.length >= 4) {
							dashboard.fetchDataDirect(Double.parseDouble(p[1]), Double.parseDouble(p[2]),
									p[0].split(",")[0], p[3]);
						}
					}
				}
				selectionPanel.setVisible(false);
				searchBar.setText("");
			}
		});
		selectionPanel.add(new JScrollPane(cityList));
	}

	private void showSelection(List<WeatherService.CityResult> res) {
		DefaultListModel<Object> m = (DefaultListModel<Object>) cityList.getModel();
		m.clear();
		m.addElement("  Select Location:");
		for (WeatherService.CityResult r : res) {
			m.addElement(r);
		}
		selectionPanel.setVisible(true);
		resizeOverlays();
		mainContent.moveToFront(selectionPanel);
	}

	private JButton createSolidButton(String t) {
		JButton b = new JButton(t);
		b.setContentAreaFilled(true);
		b.setBorderPainted(false);
		b.setFocusPainted(false);
		b.setPreferredSize(new Dimension(45, 45));
		b.setCursor(new Cursor(Cursor.HAND_CURSOR));
		b.putClientProperty("JButton.buttonType", "roundRect");
		b.putClientProperty("JComponent.roundRect", true);
		return b;
	}

	private void showError(String m) {
		((ShadowLabel) errorPanel.getComponent(0)).setText(m);
		errorPanel.setVisible(true);
		mainContent.moveToFront(errorPanel);
		new Timer(3000, _ -> errorPanel.setVisible(false)).start();
	}

	private void resizeOverlays() {
		if (searchBar != null) {
			selectionPanel.setBounds(30, 75, 450, 300);
			errorPanel.setBounds((getWidth() - 300) / 2, 90, 300, 40);
		}
	}

	private void startAutoLocate() {
		new SwingWorker<String, Void>() {
			protected String doInBackground() {
				return WeatherService.getIpLocation();
			}

			protected void done() {
				try {
					resolveCity(get());
				} catch (Exception e) {
					resolveCity("New York"); // Fallback
				}
			}
		}.execute();
	}

	/**
	 * Applies UI changes when Dark/Light mode is toggled.
	 */
	private void applyTheme() {
		try {
			UIManager.setLookAndFeel(isDarkMode ? new FlatDarkLaf() : new FlatLightLaf());
			SwingUtilities.updateComponentTreeUI(this);

			getRootPane().putClientProperty("JRootPane.titleBarBackground",
					isDarkMode ? Color.BLACK : new Color(230, 230, 235));
			getRootPane().putClientProperty("JRootPane.titleBarForeground", isDarkMode ? Color.WHITE : Color.BLACK);

			Color solidBg = isDarkMode ? new Color(60, 60, 60) : Color.WHITE;
			Color solidFg = isDarkMode ? Color.WHITE : Color.BLACK;

			searchBar.setBackground(solidBg);
			searchBar.setForeground(solidFg);
			unitToggle.setBackground(solidBg);
			themeToggle.setBackground(solidBg);

			updateButtonIcons();

			stickyBgColor = isDarkMode ? new Color(30, 30, 30, 220) : new Color(255, 255, 255, 220);
			stickyCity.setForeground(solidFg);
			stickyTemp.setForeground(solidFg);

			dashboard.updateTheme(isDarkMode);
			selectionPanel.setTheme(isDarkMode);
			mainContent.repaint();
		} catch (Exception e) {
			// Ignore look and feel errors
		}
	}
}