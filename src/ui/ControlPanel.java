package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import math.CosineSimilarity;
import math.EuclideanDistance;
import math.DistanceMetric;
import model.DataManager;
import model.WordEmbedding;
import command.CommandManager;
import command.ChangeAxisCommand;

/**
 * The side panel providing user controls for the application.
 *
 * This panel uses a JTabbedPane to organize functionality into logical groups:
 * - Navigation: Axis selection and word search
 * - Distance: Calculate semantic distance between word pairs
 * - Arithmetic: Vector math (e.g., king - man + woman = queen)
 * - Subspace: Centroid analysis for word groups
 * - Projection: Project words onto custom semantic axes
 *
 * It also houses the Undo/Redo controls connected to the CommandManager
 * following the Command Design Pattern.
 *
 *
 * For Stage C (3D Visualization), a Z-axis drop-down and a "Switch to 3D"
 * button were added to the Navigation tab. The toggle switches the center
 * panel in MainFrame between the 2D GraphPanel and the 3D GraphPanel3D.
 * Undo/Redo now supports axis changes for both 2D and 3D views.
 */
public class ControlPanel extends JPanel {
    private GraphPanel graphPanel; // Reference to 2D visualization panel
    private GraphPanel3D graphPanel3D; // Reference to 3D visualization panel
    private MainFrame mainFrame; // Reference to main frame for view switching

    private JComboBox<String> xAxisCombo; // X-axis PCA component selector
    private JComboBox<String> yAxisCombo; // Y-axis PCA component selector
    private JComboBox<String> zAxisCombo; // Z-axis PCA component selector (3D)
    private JTextField searchField; // Word search input
    private JLabel distanceResultLabel; // Shows distance calculation result
    private JButton toggleViewButton; // Button to switch between 2D and 3D

    /** Manager for handling Undo/Redo stacks (Command Pattern). */
    private CommandManager commandManager;

    /**
     * Flag to prevent ActionListener from creating commands during undo/redo sync.
     * When true, combo box changes are ignored (we're just syncing UI with model).
     */
    private boolean isSyncingUI = false;

    /**
     * Constructs the ControlPanel and initializes all tabs.
     * This constructor now accepts references to both the 2D and 3D panels,
     * as well as the MainFrame for view switching.
     *
     * @param graphPanel   Reference to the 2D GraphPanel.
     * @param graphPanel3D Reference to the 3D GraphPanel3D.
     * @param mainFrame    Reference to the MainFrame for switching views.
     */
    public ControlPanel(GraphPanel graphPanel, GraphPanel3D graphPanel3D, MainFrame mainFrame) {
        this.graphPanel = graphPanel;
        this.graphPanel3D = graphPanel3D;
        this.mainFrame = mainFrame;
        this.commandManager = new CommandManager();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 0)); // Fixed width, flexible height

        // Create tabbed pane for organizing features
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Navigation", createNavigationPanel());
        tabbedPane.addTab("Distance", createDistancePanel());
        tabbedPane.addTab("Arithmetic", new VectorArithmeticPanel());
        tabbedPane.addTab("Subspace", new SubspacePanel());

        // Create projection panel with reference to graph
        CustomProjectionPanel projPanel = new CustomProjectionPanel();
        projPanel.setGraphPanel(graphPanel); // Enable graph visualization
        tabbedPane.addTab("Projection", projPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Undo/Redo buttons at bottom
        add(createUndoRedoPanel(), BorderLayout.SOUTH);
    }

    /**
     * Creates the panel containing Undo and Redo buttons.
     * These buttons interact with the CommandManager to traverse action history.
     *
     * @return The configured JPanel.
     */
    private JPanel createUndoRedoPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        JButton undoBtn = new JButton("Undo");
        JButton redoBtn = new JButton("Redo");

        undoBtn.addActionListener(e -> {
            commandManager.undo();
            // Sync combo boxes with actual graph state after undo
            // Use the flag to prevent ActionListener from creating new commands
            syncUIWithGraphState();
        });

        redoBtn.addActionListener(e -> {
            commandManager.redo();
            // Sync combo boxes with actual graph state after redo
            syncUIWithGraphState();
        });

        panel.add(undoBtn);
        panel.add(redoBtn);
        return panel;
    }

    /**
     * Synchronizes the UI combo boxes with the actual graph state.
     * Uses a flag to prevent the ActionListener from creating new commands
     * during this synchronization (which would corrupt the undo/redo history).
     *
     * Stage C: Now also syncs the Z-axis combo and the 3D panel state.
     */
    private void syncUIWithGraphState() {
        // Set flag to indicate we're syncing (ignore combo box changes)
        isSyncingUI = true;
        try {
            if (mainFrame.is3DMode()) {
                // Sync from 3D panel state
                xAxisCombo.setSelectedIndex(graphPanel3D.getXAxisIndex());
                yAxisCombo.setSelectedIndex(graphPanel3D.getYAxisIndex());
                zAxisCombo.setSelectedIndex(graphPanel3D.getZAxisIndex());
            } else {
                // Sync from 2D panel state
                xAxisCombo.setSelectedIndex(graphPanel.getXAxisIndex());
                yAxisCombo.setSelectedIndex(graphPanel.getYAxisIndex());
            }
        } finally {
            // Always reset flag, even if exception occurs
            isSyncingUI = false;
        }
    }

    /**
     * Creates the Navigation panel for manual axis control and word search.
     * Allows users to select which PCA components to visualize.
     *
     * For Stage C I added a Z-axis drop-down and a 3D toggle button here.
     *
     * @return The configured JPanel.
     */
    private JPanel createNavigationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === AXIS SELECTION ===
        JPanel axisPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        axisPanel.setBorder(BorderFactory.createTitledBorder("Axis Selection"));

        // Create options for all 50 PCA components
        String[] axes = new String[50];
        for (int i = 0; i < 50; i++)
            axes[i] = "PC" + (i + 1);

        xAxisCombo = new JComboBox<>(axes);
        yAxisCombo = new JComboBox<>(axes);
        zAxisCombo = new JComboBox<>(axes);
        yAxisCombo.setSelectedIndex(1); // Default: PC2
        zAxisCombo.setSelectedIndex(2); // Default: PC3

        // Z-axis starts disabled (only active in 3D mode)
        zAxisCombo.setEnabled(false);

        // Add listener to update graph when selection changes, it means th UI tells us when the user wants to change the axes
        // This listener respects the isSyncingUI flag
        ActionListener axisListener = e -> updateAxes();
        xAxisCombo.addActionListener(axisListener);
        yAxisCombo.addActionListener(axisListener);
        zAxisCombo.addActionListener(axisListener);

        axisPanel.add(new JLabel("X Axis:"));
        axisPanel.add(xAxisCombo);
        axisPanel.add(new JLabel("Y Axis:"));
        axisPanel.add(yAxisCombo);
        axisPanel.add(new JLabel("Z Axis:"));
        axisPanel.add(zAxisCombo);

        panel.add(axisPanel);
        panel.add(Box.createVerticalStrut(10));

        // === 2D / 3D TOGGLE ===
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.setBorder(BorderFactory.createTitledBorder("View Mode"));
        toggleViewButton = new JButton("Switch to 3D");
        toggleViewButton.addActionListener(e -> toggleViewMode());
        viewPanel.add(toggleViewButton, BorderLayout.CENTER);

        panel.add(viewPanel);
        panel.add(Box.createVerticalStrut(10));

        // === WORD SEARCH ===
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Word"));
        searchField = new JTextField();
        JButton searchButton = new JButton("Go");
        searchButton.addActionListener(e -> {
            String word = searchField.getText().trim();
            if (!word.isEmpty()) {
                if (mainFrame.is3DMode()) {
                    graphPanel3D.centerOnWord(word);
                } else {
                    graphPanel.centerOnWord(word);
                }
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        panel.add(searchPanel);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Toggles between 2D and 3D view modes.
     *
     * When switching to 3D mode:
     * - Enables the Z-axis combo box
     * - Syncs the 3D panel's axes with current combo selections
     * - Swaps the center panel in MainFrame
     *
     * When switching back to 2D:
     * - Disables the Z-axis combo box
     * - Swaps back to the 2D panel
     */
    private void toggleViewMode() {
        boolean goTo3D = !mainFrame.is3DMode();

        if (goTo3D) {
            // Enable Z-axis selector
            zAxisCombo.setEnabled(true);

            // Sync 3D panel axes with current combo selections
            int x = xAxisCombo.getSelectedIndex();
            int y = yAxisCombo.getSelectedIndex();
            int z = zAxisCombo.getSelectedIndex();
            graphPanel3D.setAxes(x, y, z);

            toggleViewButton.setText("Switch to 2D");
        } else {
            // Disable Z-axis selector
            zAxisCombo.setEnabled(false);

            // Sync 2D panel axes with current X/Y selections
            int x = xAxisCombo.getSelectedIndex();
            int y = yAxisCombo.getSelectedIndex();
            graphPanel.setAxes(x, y);

            toggleViewButton.setText("Switch to 3D");
        }

        mainFrame.switchViewMode(goTo3D);
    }

    /**
     * Creates the Distance Calculation panel.
     * Allows users to measure semantic distance between two words.
     *
     * @return The configured JPanel.
     */
    private JPanel createDistancePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel distancePanel = new JPanel(new GridLayout(4, 1, 5, 5));
        distancePanel.setBorder(BorderFactory.createTitledBorder("Calculate Distance"));
        JTextField word1Field = new JTextField();
        JTextField word2Field = new JTextField();

        // Strategy Pattern: user selects which metric to use
        JComboBox<String> metricCombo = new JComboBox<>(new String[] { "Cosine", "Euclidean" });
        JButton calcButton = new JButton("Calculate");
        distanceResultLabel = new JLabel("Result: -");

        calcButton.addActionListener(e -> {
            calculateDistance(word1Field.getText(), word2Field.getText(), (String) metricCombo.getSelectedItem());
        });

        distancePanel.add(word1Field);
        distancePanel.add(word2Field);
        distancePanel.add(metricCombo);
        distancePanel.add(calcButton);

        panel.add(distancePanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(distanceResultLabel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Updates the graph axes via a Command object (supporting Undo/Redo).
     *
     * This method wraps the axis change in a ChangeAxisCommand object,
     * which stores both the old and new state to enable reversal.
     *
     * IMPORTANT: This method checks the isSyncingUI flag and returns early
     * if we're just syncing the UI after an undo/redo operation.
     *
     *
     * For Stage C, this supports both 2D (X,Y) and 3D (X,Y,Z) axis changes.
     * In 3D mode, the command stores and restores all three axes.
     */
    private void updateAxes() {
        // Skip if we're syncing UI after undo/redo (don't create new commands)
        if (isSyncingUI) {
            return;
        }

        int x = xAxisCombo.getSelectedIndex();
        int y = yAxisCombo.getSelectedIndex();
        int z = zAxisCombo.getSelectedIndex();

        if (mainFrame.is3DMode()) {
            // 3D mode: update all three axes
            int oldX = graphPanel3D.getXAxisIndex();
            int oldY = graphPanel3D.getYAxisIndex();
            int oldZ = graphPanel3D.getZAxisIndex();

            if (x != oldX || y != oldY || z != oldZ) {
                ChangeAxisCommand cmd = new ChangeAxisCommand(
                        graphPanel, graphPanel3D, oldX, oldY, oldZ, x, y, z);
                commandManager.executeCommand(cmd);
            }
        } else {
            // 2D mode: update X and Y only (Z ignored)
            int oldX = graphPanel.getXAxisIndex();
            int oldY = graphPanel.getYAxisIndex();

            if (x != oldX || y != oldY) {
                ChangeAxisCommand cmd = new ChangeAxisCommand(
                        graphPanel, null, oldX, oldY, -1, x, y, -1);
                commandManager.executeCommand(cmd);
            }
        }
    }

    /**
     * Calculates and displays the distance/similarity between two words.
     * Uses the Strategy Pattern to swap between different distance metrics.
     *
     * @param w1         First word.
     * @param w2         Second word.
     * @param metricName Name of the metric to use ("Cosine" or "Euclidean").
     */
    private void calculateDistance(String w1, String w2, String metricName) {
        WordEmbedding we1 = DataManager.getInstance().getEmbedding(w1.trim());
        WordEmbedding we2 = DataManager.getInstance().getEmbedding(w2.trim());

        if (we1 == null || we2 == null) {
            distanceResultLabel.setText("Word not found");
            return;
        }

        // Strategy Pattern in action:
        // The metric variable can hold any implementation of DistanceMetric
        DistanceMetric metric;
        if ("Cosine".equals(metricName)) {
            metric = new CosineSimilarity(); // Returns similarity (0 to 1, higher = more similar)
        } else {
            metric = new EuclideanDistance(); // Returns distance (lower = more similar)
        }

        double dist = metric.calculate(we1.getDenseVector(), we2.getDenseVector());
        distanceResultLabel.setText(String.format("Result: %.4f", dist));
    }
}
