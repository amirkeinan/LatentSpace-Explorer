package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.DataManager;
import model.WordEmbedding;
import model.Vector;
import math.CosineSimilarity;

/**
 * Panel for performing Vector Arithmetic (Word Analogies).
 * 
 * This implements the famous "King - Man + Woman = Queen" demonstration
 * that showcases how word embeddings capture semantic relationships.
 * 
 * The user enters an equation like "king - man + woman" and the system:
 * 1. Parses the equation into words and operators
 * 2. Retrieves the vector for each word
 * 3. Performs the arithmetic: V(king) - V(man) + V(woman)
 * 4. Finds words whose vectors are most similar to the result
 * 
 * IMPORTANT: Spaces are optional! Both "king-man+woman" and "king - man +
 * woman"
 * are valid input formats.
 */
public class VectorArithmeticPanel extends JPanel {
    private JTextField inputField; // User input: e.g., "king - man + woman"
    private JLabel resultLabel; // Status/error display
    private JTextArea resultArea; // Shows top matching words

    /**
     * Constructs the VectorArithmeticPanel with input field and results display.
     */
    public VectorArithmeticPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // === INPUT SECTION ===
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Vector Arithmetic"));

        JLabel instructionLabel = new JLabel("Equation (e.g., king - man + woman):");
        inputField = new JTextField();
        JButton calcButton = new JButton("Calculate");
        calcButton.addActionListener(e -> calculate());

        inputPanel.add(instructionLabel, BorderLayout.NORTH);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(calcButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.NORTH);

        // === RESULTS SECTION ===
        resultLabel = new JLabel("Enter an equation and click Calculate.");
        add(resultLabel, BorderLayout.CENTER);

        resultArea = new JTextArea(8, 20);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Top Matches"));
        add(scrollPane, BorderLayout.SOUTH);
    }

    /**
     * Parses the equation string and performs vector arithmetic.
     * 
     * The parsing uses regex to extract words and operators, making spaces
     * optional.
     * Regex pattern: ([a-zA-Z0-9_]+)|([+\-])
     * - Group 1: Matches word tokens (alphanumeric + underscore)
     * - Group 2: Matches operators (+ or -)
     * 
     * The algorithm maintains a "current vector" and applies operations
     * sequentially:
     * 1. First word initializes the current vector
     * 2. Subsequent words are added or subtracted based on preceding operator
     */
    private void calculate() {
        String equation = inputField.getText().trim();
        if (equation.isEmpty()) {
            resultLabel.setText("Please enter an equation.");
            return;
        }

        // === REGEX-BASED TOKENIZATION ===
        // This pattern matches either:
        // - A word (sequence of letters/numbers/underscores)
        // - An operator (+ or -)
        // The regex handles both spaced ("king - man") and unspaced ("king-man") input
        Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+)|([+\\-])");
        Matcher matcher = pattern.matcher(equation);

        Vector currentVec = null;
        String lastOp = "+"; // Default: first word is "added" (no preceding operator)
        List<String> processedTokens = new ArrayList<>();

        // Process each token found by the regex
        while (matcher.find()) {
            String word = matcher.group(1); // Captured word (or null)
            String op = matcher.group(2); // Captured operator (or null)

            if (op != null) {
                // Token is an operator - save it for the next word
                lastOp = op;
            } else if (word != null) {
                // Token is a word - look up its vector
                WordEmbedding we = DataManager.getInstance().getEmbedding(word.toLowerCase());
                if (we == null) {
                    resultLabel.setText("Error: Word '" + word + "' not found in vocabulary.");
                    resultArea.setText("");
                    return;
                }

                Vector v = we.getDenseVector();

                // Apply the operation
                if (currentVec == null) {
                    // First word: initialize the accumulator
                    currentVec = v;
                } else {
                    // Subsequent words: add or subtract
                    if (lastOp.equals("+")) {
                        currentVec = currentVec.add(v);
                    } else if (lastOp.equals("-")) {
                        currentVec = currentVec.subtract(v);
                    }
                }

                // Track for debug/display
                processedTokens.add((lastOp.equals("-") ? "-" : "+") + word);
            }
        }

        // Find words closest to the resulting vector
        if (currentVec != null) {
            resultLabel.setText("Result for: " + String.join(" ", processedTokens));
            findClosest(currentVec);
        } else {
            resultLabel.setText("Error: Could not parse equation. Use format: word1 - word2 + word3");
            resultArea.setText("");
        }
    }

    /**
     * Finds the K words whose vectors are most similar to the target vector.
     * Uses Cosine Similarity as the metric (standard for word embeddings).
     * 
     * The algorithm:
     * 1. Calculates cosine similarity between target and ALL vocabulary words
     * 2. Sorts by similarity (descending)
     * 3. Returns the top 5 matches
     * 
     * Note: The input words themselves may appear in results (they're often
     * the closest matches). In production, you might want to filter them out.
     *
     * @param target The calculated result vector from the arithmetic operation.
     */
    private void findClosest(Vector target) {
        List<WordEmbedding> all = DataManager.getInstance().getEmbeddings();
        CosineSimilarity cos = new CosineSimilarity();

        // Create sorted copy - don't modify original!
        // Time: O(N log N) where N = vocabulary size
        List<WordEmbedding> sorted = new ArrayList<>(all);
        sorted.sort((w1, w2) -> {
            double sim1 = cos.calculate(target, w1.getDenseVector());
            double sim2 = cos.calculate(target, w2.getDenseVector());
            return Double.compare(sim2, sim1); // Descending (highest similarity first)
        });

        // Build results string
        StringBuilder sb = new StringBuilder();
        sb.append("Rank  Word            Similarity\n");
        sb.append("────────────────────────────────\n");

        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            WordEmbedding we = sorted.get(i);
            double score = cos.calculate(target, we.getDenseVector());
            sb.append(String.format("%2d.   %-15s %.4f\n", i + 1, we.getWord(), score));
        }

        resultArea.setText(sb.toString());
    }
}
