package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import model.DataManager;
import model.WordEmbedding;
import model.Vector;
import math.CosineSimilarity;

/**
 * A custom Swing component that renders the 2D scatter plot of word embeddings.
 * 
 * This panel supports two rendering modes:
 * 1. SCATTER MODE (default): Traditional 2D scatter plot using PCA axes
 * 2. PROJECTION MODE: Words projected onto a single semantic axis (1D line)
 * 
 * It handles the "Viewport Transformation" from abstract vector coordinates to
 * screen pixels, supports zooming and panning in BOTH modes, and implements
 * interactive features like clicking to select words and highlighting nearest
 * neighbors.
 * 
 * ZOOM/PAN CONTROLS (work in both modes):
 * - Mouse Wheel: Zoom in/out
 * - Mouse Drag: Pan the view
 */
public class GraphPanel extends JPanel {

    // === RENDERING MODE ===
    private boolean projectionMode = false; // true = show 1D projection, false = 2D scatter
    private Map<String, Double> projectionValues; // word -> projection value on semantic axis
    private String projectionAnchor1 = ""; // First anchor word (low end)
    private String projectionAnchor2 = ""; // Second anchor word (high end)

    // === PROJECTION MODE SPECIFIC STATE ===
    private double projectionZoom = 1.0; // Zoom level for projection mode
    private double projectionOffsetX = 0; // X offset for projection panning
    private double projMinProj = 0; // Cached min projection value
    private double projMaxProj = 0; // Cached max projection value

    // === PCA AXIS CONFIGURATION ===
    /** Index of the Principal Component mapped to the X-axis (0-49). */
    private int xAxisIndex = 0; // Default PC1

    /** Index of the Principal Component mapped to the Y-axis (0-49). */
    private int yAxisIndex = 1; // Default PC2

    // === VIEWPORT TRANSFORMATION ===
    /** Zoom scale factor. Higher values zoom in. */
    private double scale = 1000.0;

    /** X-offset for panning (translation). */
    private int offsetX = 400;

    /** Y-offset for panning (translation). */
    private int offsetY = 300;

    // === INTERACTION STATE ===
    private Point lastMousePt; // Last mouse position for drag calculation
    private WordEmbedding selectedWord; // Currently selected word (red highlight)
    private List<WordEmbedding> nearestNeighbors; // K-nearest neighbors (orange highlight)

    /**
     * Gets the current PC index for the X-axis.
     * 
     * @return The index (0-49).
     */
    public int getXAxisIndex() {
        return xAxisIndex;
    }

    /**
     * Gets the current PC index for the Y-axis.
     * 
     * @return The index (0-49).
     */
    public int getYAxisIndex() {
        return yAxisIndex;
    }

    /**
     * Constructs the GraphPanel and sets up mouse listeners for interaction.
     * Registers handlers for: click (select), drag (pan), scroll (zoom).
     * These controls work in BOTH scatter and projection modes.
     */
    public GraphPanel() {
        setBackground(Color.WHITE);

        // MouseAdapter combines MouseListener and MouseMotionListener
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePt = e.getPoint();
                if (!projectionMode) {
                    checkClick(e.getPoint());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // Calculate delta movement since last position
                int dx = e.getX() - lastMousePt.x;

                if (projectionMode) {
                    // In projection mode: horizontal pan only
                    projectionOffsetX += dx / projectionZoom;
                } else {
                    // In scatter mode: full 2D pan
                    int dy = e.getY() - lastMousePt.y;
                    offsetX += dx;
                    offsetY += dy;
                }

                lastMousePt = e.getPoint();
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Get mouse position for zoom centering
                int mouseX = e.getX();

                if (projectionMode) {
                    // Projection mode zoom
                    double oldZoom = projectionZoom;
                    if (e.getWheelRotation() < 0) {
                        projectionZoom *= 1.2; // Zoom In
                    } else {
                        projectionZoom *= 0.8; // Zoom Out
                    }
                    // Clamp zoom level
                    projectionZoom = Math.max(0.5, Math.min(projectionZoom, 20.0));

                    // Adjust offset to zoom towards mouse position
                    double zoomRatio = projectionZoom / oldZoom;
                    projectionOffsetX = mouseX - (mouseX - projectionOffsetX) * zoomRatio;
                } else {
                    // Scatter mode zoom (same as before)
                    if (e.getWheelRotation() < 0) {
                        scale *= 1.1;
                    } else {
                        scale *= 0.9;
                    }
                }
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    /**
     * Enables projection mode with the given projection values.
     * In projection mode, words are displayed on a single horizontal axis
     * based on their scalar projection onto the semantic axis defined by two anchor
     * words.
     * 
     * Users can ZOOM and PAN in this mode to explore dense areas.
     * 
     * @param values  Map of word -> projection value (scalar projection onto axis)
     * @param anchor1 First anchor word (represents low/left end of scale)
     * @param anchor2 Second anchor word (represents high/right end of scale)
     */
    public void setProjectionMode(Map<String, Double> values, String anchor1, String anchor2) {
        this.projectionMode = true;
        this.projectionValues = values;
        this.projectionAnchor1 = anchor1;
        this.projectionAnchor2 = anchor2;

        // Reset projection view
        this.projectionZoom = 1.0;
        this.projectionOffsetX = 0;

        // Pre-calculate min/max for normalization
        projMinProj = Double.MAX_VALUE;
        projMaxProj = Double.MIN_VALUE;
        for (Double val : values.values()) {
            projMinProj = Math.min(projMinProj, val);
            projMaxProj = Math.max(projMaxProj, val);
        }

        repaint();
    }

    /**
     * Disables projection mode and returns to normal 2D scatter plot.
     */
    public void clearProjectionMode() {
        this.projectionMode = false;
        this.projectionValues = null;
        this.scale = 1000.0;
        this.offsetX = 400;
        this.offsetY = 300;
        repaint();
    }

    /**
     * Checks if a user click hits a point on the graph.
     * Uses a 10-pixel "hitbox" radius for easier selection.
     * 
     * @param p The screen coordinate of the click.
     */
    private void checkClick(Point p) {
        List<WordEmbedding> embeddings = DataManager.getInstance().getEmbeddings();
        if (embeddings == null)
            return;

        double minDist = Double.MAX_VALUE;
        WordEmbedding closest = null;

        // Find the closest point within 10px of click
        for (WordEmbedding we : embeddings) {
            Point screenPt = toScreen(we.getPcaVector());
            double dist = screenPt.distance(p);

            // 10px radius tolerance - makes clicking easier
            if (dist < 10 && dist < minDist) {
                minDist = dist;
                closest = we;
            }
        }

        if (closest != null) {
            selectedWord = closest;
            findNearestNeighbors(closest);
        } else {
            // Click on empty space - deselect
            selectedWord = null;
            nearestNeighbors = null;
        }
        repaint();
    }

    /**
     * Finds and stores the K-nearest neighbors to the target word.
     * Uses Cosine Similarity in the original high-dimensional space.
     * 
     * @param target The word to analyze.
     */
    private void findNearestNeighbors(WordEmbedding target) {
        List<WordEmbedding> all = DataManager.getInstance().getEmbeddings();
        CosineSimilarity cos = new CosineSimilarity();

        // Sort all words by similarity to target
        // Time complexity: O(N * log N) where N = vocabulary size
        List<WordEmbedding> sorted = new ArrayList<>(all);
        sorted.sort((w1, w2) -> {
            double d1 = cos.calculate(target.getDenseVector(), w1.getDenseVector());
            double d2 = cos.calculate(target.getDenseVector(), w2.getDenseVector());
            return Double.compare(d2, d1); // Descending order (most similar first)
        });

        // Keep top 6 (includes self + 5 true neighbors)
        nearestNeighbors = new ArrayList<>();
        for (int i = 0; i < Math.min(6, sorted.size()); i++) {
            nearestNeighbors.add(sorted.get(i));
        }
    }

    /**
     * Transforms a vector in PCA space to 2D screen coordinates.
     * 
     * The transformation applies:
     * 1. Select X and Y values from the chosen PCA components
     * 2. Scale by zoom factor
     * 3. Flip Y axis (screen coordinates have Y increasing downward)
     * 4. Apply pan offset
     * 
     * @param pca The 50D PCA vector.
     * @return A Point object representing (x, y) pixels on screen.
     */
    private Point toScreen(Vector pca) {
        // Bounds check
        if (pca.getDimension() <= Math.max(xAxisIndex, yAxisIndex))
            return new Point(0, 0);

        // Extract values for selected axes
        double xVal = pca.get(xAxisIndex);
        double yVal = pca.get(yAxisIndex);

        // Apply transformation: scale, flip Y, add offset
        int screenX = (int) (xVal * scale) + offsetX;
        int screenY = (int) (-yVal * scale) + offsetY; // Negative because screen Y is inverted

        return new Point(screenX, screenY);
    }

    /**
     * Sets which PCA dimensions to display.
     * 
     * @param xIndex Index for X-axis (0-49).
     * @param yIndex Index for Y-axis (0-49).
     */
    public void setAxes(int xIndex, int yIndex) {
        // Exit projection mode when axes change
        if (projectionMode) {
            clearProjectionMode();
        }
        this.xAxisIndex = xIndex;
        this.yAxisIndex = yIndex;
        repaint();
    }

    /**
     * Main rendering method. Draws either 2D scatter plot or 1D projection.
     * 
     * @param g The Graphics context provided by Swing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<WordEmbedding> embeddings = DataManager.getInstance().getEmbeddings();
        if (embeddings == null || embeddings.isEmpty()) {
            g2d.drawString("No data loaded. Please run python script first.", 50, 50);
            return;
        }

        // Choose rendering mode
        if (projectionMode && projectionValues != null) {
            paintProjectionMode(g2d, embeddings);
        } else {
            paintScatterMode(g2d, embeddings);
        }
    }

    /**
     * Renders the 1D projection visualization with ZOOM and PAN support.
     * Words are drawn along a horizontal line based on their projection values.
     * 
     * CONTROLS:
     * - Mouse Wheel: Zoom in/out to see more detail
     * - Mouse Drag: Pan left/right along the axis
     * 
     * @param g2d        The Graphics2D context.
     * @param embeddings All word embeddings to display.
     */
    private void paintProjectionMode(Graphics2D g2d, List<WordEmbedding> embeddings) {
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        // Draw background
        g2d.setColor(new Color(250, 250, 255));
        g2d.fillRect(0, 0, width, height);

        // Draw title
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("Semantic Axis: [" + projectionAnchor1 + "] ←→ [" + projectionAnchor2 + "]", 20, 30);

        // Calculate the visible range based on zoom
        double range = projMaxProj - projMinProj;
        if (range == 0)
            range = 1;

        // Base width for projection (the axis length when zoom=1)
        double baseWidth = width - 120;
        double totalWidth = baseWidth * projectionZoom; // Zoomed width

        // Starting X position (accounts for pan offset)
        double startX = 60 + projectionOffsetX;

        // Draw main axis line (extended based on zoom)
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2));
        int axisStartX = Math.max(0, (int) startX);
        int axisEndX = Math.min(width, (int) (startX + totalWidth));
        g2d.drawLine(axisStartX, centerY, axisEndX, centerY);

        // Draw tick marks every 10% of visible range
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
        for (int i = 0; i <= 10; i++) {
            double projVal = projMinProj + (i / 10.0) * range;
            int tickX = (int) (startX + (projVal - projMinProj) / range * totalWidth);
            if (tickX >= 0 && tickX <= width) {
                g2d.drawLine(tickX, centerY - 5, tickX, centerY + 5);
            }
        }

        // Draw anchor labels at fixed positions
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString("◀ " + projectionAnchor1.toUpperCase(), 10, height - 50);
        g2d.drawString(projectionAnchor2.toUpperCase() + " ▶", width - 100, height - 50);

        // Use vertical offset buckets to spread overlapping words
        // Scale bucket size based on zoom (more zoom = smaller buckets = more detail)
        int bucketSize = Math.max(5, (int) (30 / projectionZoom));
        Map<Integer, Integer> xBuckets = new HashMap<>();

        g2d.setFont(
                new Font("SansSerif", Font.PLAIN, Math.max(8, Math.min(12, (int) (10 * Math.sqrt(projectionZoom))))));

        int visibleCount = 0;
        for (WordEmbedding we : embeddings) {
            Double projVal = projectionValues.get(we.getWord());
            if (projVal == null)
                continue;

            // Calculate screen X position
            double normalized = (projVal - projMinProj) / range;
            int screenX = (int) (startX + normalized * totalWidth);

            // Skip if outside visible area (culling for performance)
            if (screenX < -50 || screenX > width + 50)
                continue;

            visibleCount++;

            // Calculate vertical offset to avoid overlap
            int bucket = screenX / bucketSize;
            int offset = xBuckets.getOrDefault(bucket, 0);
            xBuckets.put(bucket, offset + 1);

            // Limit vertical spread (don't show too many stacked)
            if (offset > 15)
                continue;

            // Alternate above/below the axis with increasing distance
            int baseOffset = 25;
            int spacing = (int) (15 / Math.sqrt(projectionZoom));
            spacing = Math.max(8, spacing);
            int screenY = centerY + (offset % 2 == 0
                    ? -baseOffset - (offset / 2) * spacing
                    : baseOffset + (offset / 2) * spacing);

            // Draw point on axis
            g2d.setColor(new Color(50, 100, 200));
            int pointSize = Math.max(4, Math.min(8, (int) (6 * Math.sqrt(projectionZoom))));
            g2d.fillOval(screenX - pointSize / 2, centerY - pointSize / 2, pointSize, pointSize);

            // Draw connecting line
            g2d.setColor(new Color(200, 200, 220));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawLine(screenX, centerY, screenX, screenY);

            // Draw label
            g2d.setColor(Color.DARK_GRAY);
            String label = we.getWord();
            g2d.drawString(label, screenX - 10, screenY + (screenY < centerY ? -3 : 12));
        }

        // Draw HUD with controls info
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.drawString(String.format("Zoom: %.1fx | Visible: %d words | Scroll to zoom, drag to pan",
                projectionZoom, visibleCount), 10, height - 10);
        g2d.drawString("(Switch to Navigation tab to return to scatter plot)", 10, height - 25);
    }

    /**
     * Renders the standard 2D scatter plot visualization.
     * 
     * @param g2d        The Graphics2D context.
     * @param embeddings All word embeddings to display.
     */
    private void paintScatterMode(Graphics2D g2d, List<WordEmbedding> embeddings) {
        int radius = 4;

        // Draw all points
        for (WordEmbedding we : embeddings) {
            Point pt = toScreen(we.getPcaVector());

            // Viewport Culling: skip points outside visible area
            // This improves performance by not rendering off-screen points
            if (pt.x > -10 && pt.x < getWidth() + 10 && pt.y > -10 && pt.y < getHeight() + 10) {

                if (we == selectedWord) {
                    // Selected word: large red circle with label
                    g2d.setColor(Color.RED);
                    g2d.fillOval(pt.x - 6, pt.y - 6, 12, 12);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(we.getWord(), pt.x + 8, pt.y - 8);

                } else if (nearestNeighbors != null && nearestNeighbors.contains(we)) {
                    // Neighbor: medium orange circle with label
                    g2d.setColor(Color.ORANGE);
                    g2d.fillOval(pt.x - 5, pt.y - 5, 10, 10);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(we.getWord(), pt.x + 6, pt.y - 6);

                } else {
                    // Regular word: small blue dot, no label (to reduce clutter)
                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(pt.x - radius / 2, pt.y - radius / 2, radius, radius);
                }
            }
        }

        // Draw HUD (Heads-Up Display) with current state info
        g2d.setColor(Color.BLACK);
        g2d.drawString("X-Axis: PC" + (xAxisIndex + 1), 10, 20);
        g2d.drawString("Y-Axis: PC" + (yAxisIndex + 1), 10, 35);
        g2d.drawString("Words: " + embeddings.size(), 10, 50);
        if (selectedWord != null) {
            g2d.drawString("Selected: " + selectedWord.getWord(), 10, 65);
        }
    }

    /**
     * Centers the view on a specific word and selects it.
     * 
     * @param word The word string to find and center on.
     */
    public void centerOnWord(String word) {
        // Exit projection mode when centering
        if (projectionMode) {
            clearProjectionMode();
        }

        WordEmbedding we = DataManager.getInstance().getEmbedding(word);
        if (we != null) {
            Vector pca = we.getPcaVector();
            double xVal = pca.get(xAxisIndex);
            double yVal = pca.get(yAxisIndex);

            // Calculate offset needed to center this point
            // Formula: screenCenter = val * scale + offset
            // Therefore: offset = screenCenter - val * scale
            offsetX = (int) (getWidth() / 2.0 - xVal * scale);
            offsetY = (int) (getHeight() / 2.0 - (-yVal * scale));

            selectedWord = we;
            findNearestNeighbors(we);

            repaint();
        }
    }
}
