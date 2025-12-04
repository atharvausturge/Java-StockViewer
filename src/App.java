import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class App {
    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();
        frame.setTitle("Stock Viewer");
        frame.setSize(1080,720);
        
        // Create panel to hold input components
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        
        // Label
        JLabel label = new JLabel("Enter Stock Symbol: ");
        inputPanel.add(label, BorderLayout.WEST);
        
        // Create and add a text box
        JTextField textBox = new JTextField(20);
        inputPanel.add(textBox, BorderLayout.CENTER);
        
        // Key Press to get Stock Symbol
        textBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stockSymbol = textBox.getText();
                System.out.println("Stock symbol entered: " + stockSymbol);
            }
        });
        
        // Add the input panel to the frame
        frame.add(inputPanel, BorderLayout.NORTH);
        
        frame.setVisible(true);
    }
}
