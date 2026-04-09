package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.DataManager;
import model.WordEmbedding;
import model.Vector;

/**
 * Panel for Custom Projections (Semantic Axis Analysis).
 * 
 * Users select two words (e.g., "poor" and "rich") to define a semantic axis.
 * The system then projects all words onto this specific line to explore
 * semantic scales (e.g., socioeconomic status, sentiment, gender associations).
 * 
 * This is a key feature for researchers to expose implicit biases in
 * embeddings.
 * For example, projecting professions onto a "male-female" axis can reveal
 * gender bias in the training data.
 * 
 * The projection is visualized both as text results AND on the main graph.
 */
public class CustomProjectionPanel extends JPanel {
    private JTextField word1Field; // First anchor word (e.g., "poor")
    private JTextField word2Field; // Second anchor word (e.g., "rich")
    private JTextArea resultArea; // Text display of projection results
    private JSpinner kSpinner; // Number of words to show at each end
    private JLabel statusLabel; // Operation status
    private GraphPanel graphPanel; // Reference to main graph for visualization

    /**
     * Constructs the CustomProjectionPanel.
     */
    public CustomProjectionPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === INPUT SECTION ===
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Define Semantic Axis"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Anchor Word 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Word 1 (e.g., poor):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        word1Field = new JTextField(12);
        inputPanel.add(word1Field, gbc);

        // Row 1: Anchor Word 2
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Word 2 (e.g., rich):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        word2Field = new JTextField(12);
        inputPanel.add(word2Field, gbc);

        // Row 2: K value
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Show K words per end:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(10, 5, 50, 5);
        kSpinner = new JSpinner(spinnerModel);
        inputPanel.add(kSpinner, gbc);

        // Row 3: Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton projectButton = new JButton("Project onto Axis (Updates Graph)");
        projectButton.addActionListener(e -> projectOntoAxis());
        inputPanel.add(projectButton, gbc);

        add(inputPanel, BorderLayout.NORTH);

        // === RESULTS SECTION ===
        resultArea = new JTextArea(10, 20);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Projection Results"));
        add(scrollPane, BorderLayout.CENTER);

        // === STATUS SECTION ===
        statusLabel = new JLabel("Enter two words to define a semantic axis.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Sets the reference to the GraphPanel for visualization.
     * This allows the panel to update the main graph when projecting.
     * 
     * @param graphPanel The main graph panel reference.
     */
    public void setGraphPanel(GraphPanel graphPanel) {
        this.graphPanel = graphPanel;
    }

    /**
     * Projects all words onto the semantic axis defined by word1 -> word2.
     * 
     * The projection uses scalar projection formula:
     * proj = (v - v1) · (v2 - v1) / ||v2 - v1||
     * 
     * This gives a scalar value indicating where each word falls on the
     * line from word1 to word2. Negative values are "beyond" word1,
     * values between 0 and ||v2-v1|| are between the anchors,
     * and values beyond that are "past" word2.
     */
    private void projectOntoAxis() {
        String w1 = word1Field.getText().trim().toLowerCase();
        String w2 = word2Field.getText().trim().toLowerCase();

        // Validate input
        if (w1.isEmpty() || w2.isEmpty()) {
            statusLabel.setText("Error: Please enter both anchor words.");
            return;
        }

        WordEmbedding we1 = DataManager.getInstance().getEmbedding(w1);
        WordEmbedding we2 = DataManager.getInstance().getEmbedding(w2);

        if (we1 == null) {
            statusLabel.setText("Error: Word '" + w1 + "' not found in vocabulary.");
            return;
        }
        if (we2 == null) {
            statusLabel.setText("Error: Word '" + w2 + "' not found in vocabulary.");
            return;
        }

        // === COMPUTE AXIS VECTOR ===
        // The axis is the direction from word1 to word2 in high-dimensional space
        Vector v1 = we1.getDenseVector();
        Vector v2 = we2.getDenseVector();
        Vector axisVector = v2.subtract(v1); // Direction vector
        double axisMagnitude = axisVector.magnitude(); // Length of axis

        if (axisMagnitude == 0) {
            statusLabel.setText("Error: Anchor words have identical vectors.");
            return;
        }

        // === PROJECT ALL WORDS ===
        List<WordEmbedding> all = DataManager.getInstance().getEmbeddings();
        List<ProjectionResult> projections = new ArrayList<>();
        Map<String, Double> projectionMap = new HashMap<>(); // For GraphPanel

        for (WordEmbedding we : all) {
            Vector v = we.getDenseVector();

            // Vector from anchor1 to current word
            Vector fromAnchor = v.subtract(v1);

            // Scalar projection formula:
            // proj = (fromAnchor · axisVector) / ||axisVector||
            // This gives the signed distance along the axis
            double dotProduct = dotProduct(fromAnchor, axisVector);
            double scalarProjection = dotProduct / axisMagnitude;

            projections.add(new ProjectionResult(we.getWord(), scalarProjection));
            projectionMap.put(we.getWord(), scalarProjection);
        }

        // Sort by projection value: low values = closer to word1, high = closer to
        // word2
        projections.sort((a, b) -> Double.compare(a.projection, b.projection));

        int k = (Integer) kSpinner.getValue();
        displayResults(projections, w1, w2, k);

        // Update the main graph visualization
        if (graphPanel != null) {
            graphPanel.setProjectionMode(projectionMap, w1, w2);
        }

        statusLabel.setText("Projected " + projections.size() + " words onto [" + w1 + " → " + w2 + "]");
    }

    /**
     * Displays projection results showing words at both ends of the semantic axis.
     * Shows K words closest to each anchor.
     * 
     * @param projections Sorted list of all word projections.
     * @param w1          First anchor word (low end).
     * @param w2          Second anchor word (high end).
     * @param k           Number of words to show at each end.
     */
    private void displayResults(List<ProjectionResult> projections, String w1, String w2, int k) {
        StringBuilder sb = new StringBuilder();

        sb.append("══════════════════════════════════\n");
        sb.append("  Semantic Axis Analysis\n");
        sb.append("══════════════════════════════════\n\n");

        // Words closest to word1 (lowest projection values)
        sb.append("◀ Closest to '" + w1.toUpperCase() + "':\n");
        sb.append("─────────────────────────\n");
        for (int i = 0; i < Math.min(k, projections.size()); i++) {
            ProjectionResult pr = projections.get(i);
            sb.append(String.format("  %+8.3f  %s\n", pr.projection, pr.word));
        }

        sb.append("\n        · · ·\n\n");

        // Words closest to word2 (highest projection values)
        sb.append("▶ Closest to '" + w2.toUpperCase() + "':\n");
        sb.append("─────────────────────────\n");
        int start = Math.max(projections.size() - k, 0);
        for (int i = start; i < projections.size(); i++) {
            ProjectionResult pr = projections.get(i);
            sb.append(String.format("  %+8.3f  %s\n", pr.projection, pr.word));
        }

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0); // Scroll to top
    }

    /**
     * Calculates the dot product (inner product) of two vectors.
     * 
     * Dot product = Σ(a_i * b_i) for all dimensions i
     * 
     * This is fundamental for projection calculations:
     * - If dot product > 0: vectors point in similar directions
     * - If dot product = 0: vectors are orthogonal (perpendicular)
     * - If dot product < 0: vectors point in opposite directions
     * 
     * @param v1 First vector.
     * @param v2 Second vector.
     * @return The scalar dot product value.
     */
    private double dotProduct(Vector v1, Vector v2) {
        double sum = 0;
        for (int i = 0; i < v1.getDimension(); i++) {
            sum += v1.get(i) * v2.get(i);
        }
        return sum;
    }

    /**
     * Helper class to store projection results for sorting.
     * Associates a word with its scalar projection value.
     */
    private static class ProjectionResult {
        final String word;
        final double projection;

        ProjectionResult(String word, double projection) {
            this.word = word;
            this.projection = projection;
        }
    }
}
