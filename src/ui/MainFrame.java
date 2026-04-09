package ui;

import javax.swing.*;
import java.awt.*;
import model.DataManager;

/**
 * The main application window for the LatentSpace Explorer project.
 * It sets up the UI layout and holds the visualization panels and the control
 * panel.
 *
 * For Stage C (3D Visualization), this class was updated to hold both the 2D
 * panel
 * (GraphPanel) and the 3D panel (GraphPanel3D). The user can switch between
 * them.
 * 
 * Note on Extensibility: Adding the 3D view didn't require me to change
 * the DataManager or the math logic at all, which shows the MVC architecture
 * works well.
 */
public class MainFrame extends JFrame {

    /** The panel responsible for rendering the 2D visualization of embeddings. */
    private GraphPanel graphPanel;

    /** The panel responsible for rendering the 3D visualization (JavaFX). */
    private GraphPanel3D graphPanel3D;

    /** The panel containing user controls (navigation, search, tools). */
    private ControlPanel controlPanel;

    /** Tracks which view mode is currently active. */
    private boolean is3DMode = false;

    /**
     * Constructs the MainFrame, sets up the UI, and loads data.
     */
    public MainFrame() {
        setTitle("LatentSpace Explorer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        // Initialize DataManager with paths to the generated JSON files
        DataManager.getInstance().loadData("full_vectors.json", "pca_vectors.json");

        // Create both 2D and 3D visualization panels
        graphPanel = new GraphPanel();
        graphPanel3D = new GraphPanel3D();

        // Pass both panels to ControlPanel for axis control and toggling
        controlPanel = new ControlPanel(graphPanel, graphPanel3D, this);

        // Start with 2D view by default
        add(graphPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        setVisible(true);
    }

    /**
     * Toggles between the 2D and 3D visualization.
     * We just remove the current panel from the center and add the other one.
     *
     * @param use3D true to switch to 3D view, false to switch to 2D view.
     */
    public void switchViewMode(boolean use3D) {
        if (use3D == is3DMode)
            return; // Already in requested mode

        is3DMode = use3D;

        // Remove current center panel
        if (use3D) {
            remove(graphPanel);
            add(graphPanel3D, BorderLayout.CENTER);
        } else {
            remove(graphPanel3D);
            add(graphPanel, BorderLayout.CENTER);
        }

        // Force layout recalculation and repaint
        revalidate();
        repaint();
    }

    /**
     * Returns whether the application is currently in 3D mode.
     * 
     * @return true if 3D mode is active.
     */
    public boolean is3DMode() {
        return is3DMode;
    }

    /**
     * The main entry point for the application.
     * Schedules the creation of the UI on the Event Dispatch Thread (EDT).
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame());
    }
}
