# ClyMate Weather App

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/UI-Swing-orange?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)
![API](https://img.shields.io/badge/API-Open--Meteo-green?style=for-the-badge)
![License](https://img.shields.io/badge/License-Academic-lightgrey?style=for-the-badge)

A desktop weather dashboard with advanced features, is a powerful combination of **Java Swing** and **FlatLaf** technologies. Instead of simply sticking to classic Swing layout, ClyMate redefines UI with a cutting-edge "Glass-Morphism" concept that includes transparency, drop shadows, as well as responsive vector graphics. The application delivers 3 kinds of weather data: present, hourly and 16-day forecast using REST API integration accurately.

> **v1.0 Initial Stable Release**

## ✨ Key Features

* **Glassmorphic UI:** Custom `JPanels` use alpha-compositing techniques to create a frosted glass visual effect with layered transparency.
* **Live Data:** Weather information is fetched from **Open-Meteo**, while geolocation data is retrieved from **IP-API** to provide real-time context.
* **Dynamic Visuals:** Background visuals automatically adapt based on current weather conditions (rain, snow, clear) and the time of day.
* **Threading:** All network operations execute on background `SwingWorker` threads to maintain a responsive, non-blocking UI.
* **Smart Search:** Search history is persistently stored with timestamps using **Java Preferences**, operating seamlessly in the background.
* **Unit Conversion:** Instant switching between Metric (°C, km/h) and Imperial (°F, mph) measurement systems.

## 🛠️ Technology Stack

*   **Language:** Java 17+
*   **GUI Toolkit:** Swing + AWT
*   **Theme Engine:** [FlatLaf](https://www.formdev.com/flatlaf/) + FlatLaf Extras (SVG support)
*   **Data Parsing:** `org.json`
*   **Assets:** SVG Icons & High-res PNG Backgrounds

## 🚀 Getting Started

### Prerequisites
*   JDK 17 or higher.
*   External JARs added to classpath:
    *   `flatlaf-3.7.jar`
    *   `flatlaf-extras-3.7.jar`
    *   `jsvg-2.0.0.jar`
    *   `json-20251224.jar`

### Installation
1.  **Clone the repo:**
    ```bash
    git clone https://github.com/YourUsername/ClyMate-Weather-App.git
    ```
2.  **Add Dependencies:** Ensure the JARs listed above are in your IDE's build path / library settings.
3.  **Run:** Execute the `clymate.AppLauncher` class.

## 🤝 Credits
*   **Author:** MWDiss
*   **Data:** [Open-Meteo](https://open-meteo.com/) (CC-BY 4.0)
*   **Reference:** Eck, D. J. (2022). *Introduction to Programming Using Java*.
