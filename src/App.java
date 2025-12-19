import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
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

        // Load API key (from env var STOCK_API_KEY or config.properties)
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

        // Results area
        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(resultsArea);

        // Chart area
        ChartPanel chart = new ChartPanel();
        chart.setPreferredSize(new java.awt.Dimension(800, 400));

        // Key Press to get Stock Symbol and call API
        textBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stockSymbol = textBox.getText().trim();
                if (stockSymbol.isEmpty()) {
                    resultsArea.setText("Please enter a stock symbol.");
                    return;
                }

                if (apiKey == null || apiKey.isEmpty()) {
                    resultsArea.setText("No API key configured. Set `STOCK_API_KEY` or edit `config.properties`.");
                    return;
                }

                // Disable input while fetching
                textBox.setEnabled(false);
                resultsArea.setText("Fetching data for '" + stockSymbol + "'...\n");

                // Fetch in background thread to avoid blocking the UI
                new Thread(() -> {
                    String response = fetchTimeSeries(stockSymbol, apiKey);
                    java.util.List<String> labels = new java.util.ArrayList<>();
                    java.util.List<Double> values = new java.util.ArrayList<>();
                    try {
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
                            resultsArea.setText("Plotted " + values.size() + " data points for " + stockSymbol);
                        } else {
                            resultsArea.setText(response);
                        }
                        textBox.setEnabled(true);
                    });
                }).start();
            }
        });

        // Add the input panel and results to the frame
        frame.add(inputPanel, BorderLayout.NORTH);
        // split center: chart on top, raw JSON below
        JPanel center = new JPanel();
        center.setLayout(new BorderLayout());
        center.add(chart, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        frame.add(center, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    // Fetch stock quote from Alpha Vantage (GLOBAL_QUOTE). Returns raw response string (JSON or error).
    private static String fetchQuote(String symbol, String apiKey) {
        try {
            String urlStr = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + URLEncoder.encode(symbol, "UTF-8") + "&apikey=" + URLEncoder.encode(apiKey, "UTF-8");
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

    // Fetch daily time series from Alpha Vantage
    private static String fetchTimeSeries(String symbol, String apiKey) {
        try {
            String urlStr = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + URLEncoder.encode(symbol, "UTF-5") + "&outputsize=compact&apikey=" + URLEncoder.encode(apiKey, "UTF-8");
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

    // Parse a time series from Alpha Vantage JSON (daily or intraday). Fills the provided labels and values lists.
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
                    // date line: "YYYY-MM-DD": {
                    int q2 = line.indexOf('"', 1);
                    int q3 = line.indexOf('"', q2 + 1);
                    if (q2 == -1 || q3 == -1) continue;
                    String date = line.substring(1, q3);

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
