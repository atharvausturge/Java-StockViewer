import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;


public class ChartPanel extends JPanel {
    private List<Double> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    
    // Cache for last computed chart geometry to enable click detection
    private int lastPadding = 40;
    private int lastLabelPadding = 40;
    private int lastGraphWidth = 0;
    private int lastGraphHeight = 0;
    private double lastMin = 0;
    private double lastMax = 0;
    private double lastRange = 1;

    public ChartPanel() {
        // Add mouse listener for click detection on data points
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handlePointClick(e.getX(), e.getY());
            }
        });
    }

    public void setSeries(List<String> labels, List<Double> values) {
        // Replace stored series (defensive null handling) and trigger a repaint
        this.labels = labels == null ? new ArrayList<>() : labels;
        this.values = values == null ? new ArrayList<>() : values;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // Use antialiasing for nicer lines and points
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padding = 40;
        int labelPadding = 40;

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        // Nothing to draw if we have no data
        if (values == null || values.isEmpty()) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("No data to display", padding, h/2);
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Double v : values) {
            if (v != null) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        if (min == Double.MAX_VALUE || max == -Double.MAX_VALUE) return;

        // Compute vertical range and avoid division by zero for flat series
        double range = max - min;
        if (range == 0) range = max * 0.1 + 1; // avoid div by zero
        
        int graphWidth = w - 2 * padding - labelPadding;
        int graphHeight = h - 2 * padding;
        
        // Cache geometry for mouse click detection
        lastMin = min;
        lastMax = max;
        lastRange = range;
        lastGraphWidth = graphWidth;
        lastGraphHeight = graphHeight;

        // Draw axes 
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(padding + labelPadding, h - padding, padding + labelPadding, padding);
        g2.drawLine(padding + labelPadding, h - padding, w - padding, h - padding);

        // Draw the polyline connecting close values
        g2.setColor(new Color(33, 150, 243)); // blue
        g2.setStroke(new BasicStroke(2f));

        int n = values.size();
        for (int i = 0; i < n - 1; i++) {
            Double v1 = values.get(i);
            Double v2 = values.get(i+1);
            if (v1 == null || v2 == null) continue;
            int x1 = padding + labelPadding + (int) ((double) i / (n - 1) * graphWidth);
            int x2 = padding + labelPadding + (int) ((double) (i+1) / (n - 1) * graphWidth);
            int y1 = padding + (int) ((max - v1) / range * graphHeight);
            int y2 = padding + (int) ((max - v2) / range * graphHeight);
            g2.drawLine(x1, y1, x2, y2);
        }

        // Draw data points on top of the line
        g2.setColor(Color.DARK_GRAY);
        for (int i = 0; i < n; i++) {
            Double v = values.get(i);
            if (v == null) continue;
            int x = padding + labelPadding + (int) ((double) i / (n - 1) * graphWidth);
            int y = padding + (int) ((max - v) / range * graphHeight);
            g2.fillOval(x - 3, y - 3, 6, 6);
        }

        // Draw min/max numeric labels on the left side
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(String.format("%.2f", max), 5, padding + 10);
        g2.drawString(String.format("%.2f", min), 5, h - padding);
        
        // Draw intermediate y-axis labels (quarters between min and max)
        int labelSteps = 3; // divide range into quarters
        for (int step = 1; step < labelSteps; step++) {
            double value = min + (range / labelSteps) * step;
            int y = padding + (int) ((max - value) / range * graphHeight);
            g2.drawString(String.format("%.2f", value), 5, y + 5);
        }

        // Draw a few sparse x-axis labels (up to 6) to avoid clutter
        int labelCount = Math.min(6, labels.size());
        if (labelCount > 0) {
            for (int i = 0; i < labelCount; i++) {
                int idx;
                if (labelCount == 1) idx = 0;
                else idx = (int) Math.round(((double) i / (labelCount - 1)) * (n - 1));
                if (idx < 0) idx = 0;
                if (idx >= n) idx = n - 1;
                String s = labels.get(idx);
                String display = s;
                // If label looks like yyyy-mm-dd, show month abbreviation (e.g., "Dec")
                try {
                    if (s != null) {
                        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            java.time.LocalDate d = java.time.LocalDate.parse(s);
                            display = d.format(java.time.format.DateTimeFormatter.ofPattern("MMM"));
                        } else if (s.matches("\\d{4}-\\d{2}-\\d{2} .*")) {
                            // intraday key like "2025-12-19 16:00:00"
                            java.time.format.DateTimeFormatter inFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(s, inFmt);
                            display = dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
                        } else if (s.length() >= 5) {
                            // fallback: show last 5 chars
                            display = s.substring(Math.max(0, s.length() - 5));
                        }
                    }
                } catch (Exception ex) {
                    display = s; // parsing fallback
                }
                int x = padding + labelPadding + (int) ((double) idx / (n - 1) * graphWidth);
                // ensure label is visible: draw with small font and clamp x
                int tx = Math.max(padding + labelPadding, Math.min(x - 20, w - padding - 40));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(display, tx, h - padding + 15);
            }
        }
    }
    
    /**
     * Handle mouse clicks on data points. Shows a popup with date and price.
     */
    private void handlePointClick(int mouseX, int mouseY) {
        int w = getWidth();
        int h = getHeight();
        int padding = lastPadding;
        int labelPadding = lastLabelPadding;
        
        if (values.isEmpty() || lastRange == 0) return;
        
        int n = values.size();
        // Check each point to see if the click is within 8 pixels
        for (int i = 0; i < n; i++) {
            Double v = values.get(i);
            if (v == null) continue;
            int x = padding + labelPadding + (int) ((double) i / (n - 1) * lastGraphWidth);
            int y = padding + (int) ((lastMax - v) / lastRange * lastGraphHeight);
            
            // Check if click is within 8 pixels of the point
            if (Math.abs(mouseX - x) <= 8 && Math.abs(mouseY - y) <= 8) {
                String date = i < labels.size() ? labels.get(i) : "?";
                String message = String.format("Date: %s\nPrice: $%.2f", date, v);
                
                // Show a simple tooltip-like label popup
                javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
                javax.swing.JLabel label = new javax.swing.JLabel(message);
                label.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
                popup.add(label);
                popup.show(this, mouseX, mouseY);
                
                return;
            }
        }
    }
}
