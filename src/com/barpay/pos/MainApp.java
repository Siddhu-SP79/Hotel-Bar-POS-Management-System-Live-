package com.barpay.pos;

import javax.swing.SwingUtilities;
import com.barpay.pos.ui.POSPanel;
import com.barpay.pos.util.Logger;

/**
 * Main application launcher for the POS system.
 */
public class MainApp {

    public static void main(String[] args) {
       
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Create and display the main POS window
                    POSPanel mainFrame = new POSPanel();
                    mainFrame.setLocationRelativeTo(null); // Center the window
                    mainFrame.setVisible(true);
                } catch (Exception e) {
                    Logger.error("App", "Failed to launch application: " + e.getMessage());
                    e.printStackTrace();
                    // Show a critical error message if the app fails to start
                    javax.swing.JOptionPane.showMessageDialog(null, 
                        "A critical error occurred on startup: \n" + e.getMessage() + 
                        "\nSee logs for details.", 
                        "Startup Failed", 
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}