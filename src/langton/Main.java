package langton;

import langton.ui.MainUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// App entry point.
public class Main {

    // Starts the program.
    public static void main(String[] args) {
        System.out.println("Starting Langton's Ant - Parallel Simulation");
        System.out.println("CMP6011 - Parallel and Distributed Systems");
        System.out.printf("Java version:    %s%n", System.getProperty("java.version"));
        System.out.printf("Available cores: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.println();

        // Launches the UI window.
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            MainUI ui = new MainUI();
            ui.show();
        });
    }
}
