package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import model.DataManager;
import model.WordEmbedding;
import model.Vector;
import math.CosineSimilarity;

/**
 * Panel for performing Subspace/Cluster analysis.
 * Users input a group of related words (e.g., "apple, pear, banana"),
 * and the system calculates their "Centroid" (average vector) to identify
 * the central concept and find other words belonging to this cluster.
 * The user can control the value of K (number of neighbors to display).
 */
public class SubspacePanel extends JPanel {
    private JTextArea inputArea; // Words separated by comma or new line
    private JTextArea resultArea; // Displays the K nearest words to centroid
    private JLabel statusLabel; // Shows current operation status
    private JSpinner kSpinner; // User-controlled K value for neighbor count

    /**
     * Constructs the SubspacePanel with proper layout.
     * Uses BoxLayout for vertical stacking of components.
     */
    public SubspacePanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === TOP SECTION: Input Area ===
        JPanel topSection = new JPanel(new BorderLayout(5, 5));
        topSection.setBorder(BorderFactory.createTitledBorder("Subspace & Centroid Analysis"));

        // Label for input
        JLabel inputLabel = new JLabel("Enter words (comma/newline separated):");
        topSection.add(inputLabel, BorderLayout.NORTH);

        // Text area for word input
        inputArea = new JTextArea(4, 20);
        inputArea.setToolTipText("Enter words separated by commas or new lines");
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane scrollInput = new JScrollPane(inputArea);
        topSection.add(scrollInput, BorderLayout.CENTER);

        // === CONTROL SECTION: K Spinner + Button ===
        JPanel controlPanel = new JPanel(new BorderLayout(10, 5));

        // K selection panel (left side)
        JPanel kPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        kPanel.add(new JLabel("K (neighbors):"));

        // Spinner: default=10, min=1, max=100, step=1
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(10, 1, 100, 1);
        kSpinner = new JSpinner(spinnerModel);
        kSpinner.setPreferredSize(new Dimension(60, 25));
        kPanel.add(kSpinner);
        controlPanel.add(kPanel, BorderLayout.WEST);

        // Calculate button (right side) - prominent placement
        JButton calcButton = new JButton("Calculate Centroid");
        calcButton.addActionListener(e -> calculateCentroid());
        controlPanel.add(calcButton, BorderLayout.EAST);

        topSection.add(controlPanel, BorderLayout.SOUTH);

        add(topSection, BorderLayout.NORTH);

        // === CENTER SECTION: Results Area ===
        resultArea = new JTextArea(8, 20);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // === BOTTOM SECTION: Status Label ===
        statusLabel = new JLabel("Status: Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Parses the input list, retrieves vectors, and computes the geometric
     * centroid.
     * 
     * The centroid is the "average" position of all input word vectors,
     * representing the conceptual center of the word group.
     */
    private void calculateCentroid() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("Status: Please enter some words.");
            return;
        }

        // Split input by comma, newline, or whitespace
        // Regex: [,\n\s]+ matches one or more of comma, newline, or space
        String[] tokens = text.split("[,\\n\\s]+");
        List<Vector> vectors = new ArrayList<>();
        List<String> validWordsList = new ArrayList<>();

        // Validate each token and collect valid word vectors
        for (String token : tokens) {
            token = token.trim().toLowerCase();
            if (token.isEmpty())
                continue;

            WordEmbedding we = DataManager.getInstance().getEmbedding(token);
            if (we != null) {
                vectors.add(we.getDenseVector());
                validWordsList.add(token);
            }
        }

        if (vectors.isEmpty()) {
            statusLabel.setText("Status: No valid words found in vocabulary.");
            return;
        }

        // === CENTROID CALCULATION ===
        // Centroid = (1/N) * Σ(all vectors)
        // This gives us the geometric center of the word cluster
        int dim = vectors.get(0).getDimension();
        double[] sum = new double[dim];

        // Sum all vectors element-wise
        for (Vector v : vectors) {
            for (int i = 0; i < dim; i++) {
                sum[i] += v.get(i);
            }
        }

        // Divide by count to get average
        double[] avg = new double[dim];
        for (int i = 0; i < dim; i++) {
            avg[i] = sum[i] / vectors.size();
        }
        Vector centroid = new Vector(avg);

        // Update status with processed words
        String wordSummary = String.join(", ", validWordsList);
        if (wordSummary.length() > 50) {
            wordSummary = wordSummary.substring(0, 47) + "...";
        }
        statusLabel.setText("Centroid: " + vectors.size() + " words (" + wordSummary + ")");

        findClosestToCentroid(centroid);
    }

    /**
     * Finds K words closest to the abstract centroid vector.
     * Uses Cosine Similarity as the distance metric.
     *
     * @param centroid The calculated average vector representing the cluster
     *                 center.
     */
    private void findClosestToCentroid(Vector centroid) {
        List<WordEmbedding> all = DataManager.getInstance().getEmbeddings();
        CosineSimilarity cos = new CosineSimilarity();
        int k = (Integer) kSpinner.getValue();

        // Create a copy and sort by descending cosine similarity
        // Higher cosine = more similar (range: -1 to 1)
        List<WordEmbedding> sorted = new ArrayList<>(all);
        sorted.sort((w1, w2) -> {
            double sim1 = cos.calculate(centroid, w1.getDenseVector());
            double sim2 = cos.calculate(centroid, w2.getDenseVector());
            return Double.compare(sim2, sim1); // Descending order
        });

        // Build result string
        StringBuilder sb = new StringBuilder();
        sb.append("=== Top " + k + " Words Near Centroid ===\n");
        sb.append("────────────────────────────────\n");

        for (int i = 0; i < Math.min(k, sorted.size()); i++) {
            WordEmbedding we = sorted.get(i);
            double score = cos.calculate(centroid, we.getDenseVector());
            // Format: rank, word, similarity score
            sb.append(String.format("%3d. %-15s (%.4f)\n", i + 1, we.getWord(), score));
        }

        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0); // Scroll to top
    }
}
