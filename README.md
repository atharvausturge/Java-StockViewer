# Java Stock Viewer 📈

A robust desktop application built with Java that allows users to search for stocks, view real-time data, and visualize historical price trends through interactive charts.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Swing](https://img.shields.io/badge/Java_Swing-GUI-blue?style=for-the-badge)

## 📋 Table of Contents
- [About](#about)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Usage](#usage)
- [Screenshots](#screenshots)
- [Future Improvements](#future-improvements)
- [License](#license)

## 🧐 About
The **Java Stock Viewer** is a tool designed to fetch financial data from external APIs (such as Alpha Vantage) and present it in a user-friendly graphical interface. It helps users track the performance of specific companies by analyzing their stock ticker symbols.

## ✨ Features
*   **Ticker Search:** Search for any publicly traded company using their stock symbol (e.g., AAPL, TSLA, GOOGL).
*   **Real-time Data:** View the current opening, closing, high, and low prices.
*   **Data Visualization:** Visual graphs representing stock performance over time.
*   **Time Frames:** (If applicable) Toggle between Daily, Weekly, and Monthly views.
*   **Clean UI:** A straightforward Java Swing interface for easy navigation.

## 🛠 Tech Stack
*   **Language:** Java (JDK 8 or higher)
*   **GUI Framework:** Java Swing / AWT
*   **Networking:** `java.net.HttpURLConnection` for API requests
*   **JSON Parsing:** `org.json` (or similar library used in the source)
*   **Charting:** JFreeChart (or custom graphics components)
*   **API:** Alpha Vantage API (or similar financial data provider)

## ⚙️ Prerequisites
Before running this project, ensure you have the following installed:
*   [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/) - Version 8 or higher.
*   An IDE (IntelliJ IDEA, Eclipse, or VS Code) is recommended but not required.
*   **API Key:** You may need a free API key from [Alpha Vantage](https://www.alphavantage.co/) (or the specific provider used in the code) to fetch data.

## 🚀 Installation & Setup

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/atharvausturge/Java-StockViewer.git
    cd Java-StockViewer
    ```

2.  **Configure API Key**
    *   Create a .env file and implement with your own API key.

3.  **Compile the Code**
    *   *Using Command Line:*
        ```bash
        javac -d bin src/*.java
        ```
    *   *Using an IDE:*
        Open the project folder and let the IDE index the files.

4.  **Run the Application**
    *   *Using Command Line:*
        ```bash
        java -cp bin MainClassName
        ```
        *(Note: Replace `MainClassName` with the actual name of the file containing the `public static void main` method, e.g., `StockViewer` or `Main`)*.

## 🖥 Usage
1.  Launch the application.
2.  In the search bar, type a stock ticker symbol (e.g., `IBM`).
3.  Click the **Search** or **Refresh** button.
4.  Wait for the data to fetch and the graph to render.



## 🔮 Future Improvements
*   Add support for cryptocurrency data.
*   Implement a caching mechanism to reduce API calls.
*   Add a "Watchlist" feature to save favorite stocks.
*   Improve UI styling with FlatLaf or JavaFX.
*   Add Monte Carlo Simulation
*   Implement a paper trading aspect

## 🤝 Contributing
Contributions, issues, and feature requests are welcome!
1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 👤 Author
**Atharva Usturge**
*   GitHub: [@atharvausturge](https://github.com/atharvausturge)

## 📄 License
This project is open-source. Please check the repository for specific license details.

---

*Disclaimer: This application is for educational purposes only and should not be used as the sole basis for financial decisions.*
