/**
 * Atharva Usturge 
 * Java Swing Stock Viewer 
 */


import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class App {
    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();
        frame.setTitle("Stock Viewer");
        frame.setSize(1080,720);

        // Load API key 
        String apiKey = Config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Warning: No stock API key found. Set env var `STOCK_API_KEY` or edit `config.properties`.");
        } else {
            String source = (System.getenv("STOCK_API_KEY") != null && !System.getenv("STOCK_API_KEY").isEmpty()) ? "environment variable" : "config.properties";
            System.out.println("Stock API key loaded from " + source + " (value hidden)");
        }

        // Create panel to hold input components
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        // Label
        JLabel label = new JLabel("Enter Stock Symbol: ");
        inputPanel.add(label, BorderLayout.WEST);

        // Create and add a text box
        JTextField textBox = new JTextField(20);
        inputPanel.add(textBox, BorderLayout.CENTER);

        // Chart area
        ChartPanel chart = new ChartPanel();
        chart.setPreferredSize(new java.awt.Dimension(800, 400));

        // Key Press to get Stock Symbol and call API.
        textBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stockSymbol = textBox.getText().trim();
                if (stockSymbol.isEmpty()) {
                    System.out.println("Please enter a stock symbol.");
                    return;
                }

                if (apiKey == null || apiKey.isEmpty()) {
                    System.err.println("No API key configured. Set `STOCK_API_KEY` or edit `config.properties`.");
                    return;
                }
                // Disable input while fetching
                textBox.setEnabled(false);
                System.out.println("Fetching data for '" + stockSymbol + "'...");

                // Fetch in background thread 
                new Thread(() -> {
                    String response = fetchTimeSeries(stockSymbol, apiKey);
                    java.util.List<String> labels = new java.util.ArrayList<>();
                    java.util.List<Double> values = new java.util.ArrayList<>();
                    try {
                        // Parse the response into (date,label) pairs. The parser is lightweight
                        // and looks for the Alpha Vantage "Time Series (Daily)" block and
                        // extracts the "4. close" fields. 200 points max for performance.
                        parseTimeSeries(response, labels, values, 200);
                    } catch (Exception ex) {
                        // ignore parse errors and fall back to raw display
                    }

                    SwingUtilities.invokeLater(() -> {
                        if (!values.isEmpty()) {
                            // parsed newest->oldest, reverse to oldest first
                            java.util.Collections.reverse(values);
                            java.util.Collections.reverse(labels);
                            chart.setSeries(labels, values);
                            System.out.println("Plotted " + values.size() + " data points for " + stockSymbol);
                        } else {
                            System.err.println("Failed to parse data. Response: " + response);
                        }
                        textBox.setEnabled(true);
                    });
                }).start();
            }
        });

        // Add the input panel and chart to the frame
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(chart, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    // Fetch stock quote from Alpha Vantage (GLOBAL_QUOTE). Returns raw response string (JSON or error).

    private static String fetchTimeSeries(String symbol, String apiKey) {
        try {
            String urlStr = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + URLEncoder.encode(symbol, "UTF-8") + "&outputsize=compact&apikey=" + URLEncoder.encode(apiKey, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            return sb.toString();
        } catch (Exception ex) {
            return "Error fetching data: " + ex.getMessage();
        }
    }

    
    private static void parseTimeSeries(String response, java.util.List<String> labels, java.util.List<Double> values, int maxPoints) {
        if (response == null || response.isEmpty()) return;
        String marker = "\"Time Series (Daily)\"";
        int idx = response.indexOf(marker);
        if (idx == -1) {
            // try common intraday marker
            marker = "\"Time Series (60min)\"";
            idx = response.indexOf(marker);
        }
        if (idx == -1) {
            // try generic "Time Series"
            marker = "\"Time Series";
            idx = response.indexOf(marker);
        }
        if (idx == -1) return;

        java.io.BufferedReader rdr = new java.io.BufferedReader(new java.io.StringReader(response));
        String line;
        boolean inSeries = false;
        int count = 0;
        try {
            while ((line = rdr.readLine()) != null) {
                line = line.trim();
                if (!inSeries) {
                    if (line.startsWith(marker)) {
                        inSeries = true;
                    }
                    continue;
                }

                // end of series
                if (line.equals("}") || line.startsWith("}\"")) break;

                if (line.startsWith("\"")) {
                    // Candidate quoted key. Only treat it as a DATE key when it begins
                    // with a 4-digit year (e.g. "2025-12-19": { ). This avoids
                    // accidentally interpreting lines like "1. open" or "5. volume" as dates.
                    int endQuote = line.indexOf('"', 1);
                    if (endQuote == -1) continue;
                    String key = line.substring(1, endQuote);
                    // simple date check: starts with 4 digits (year)
                    if (key.length() < 4 || !Character.isDigit(key.charAt(0)) || !Character.isDigit(key.charAt(1)) || !Character.isDigit(key.charAt(2)) || !Character.isDigit(key.charAt(3))) {
                        continue; // not a date key
                    }
                    String date = key;

                    // read until we find "4. close"
                    String closeLine = rdr.readLine();
                    while (closeLine != null && !closeLine.trim().startsWith("\"4. close\"")) {
                        closeLine = rdr.readLine();
                    }
                    if (closeLine != null) {
                        String[] parts = closeLine.split(":");
                        if (parts.length >= 2) {
                            String val = parts[1].replaceAll("[\",]", "").trim();
                            try {
                                double d = Double.parseDouble(val);
                                labels.add(date);
                                values.add(d);
                                count++;
                                if (count >= maxPoints) break;
                            } catch (NumberFormatException nfe) {
                                // ignore
                            }
                        }
                    }
                }
            }
        } catch (java.io.IOException ioe) {
            // shouldn't happen on StringReader
        }
    }
}
