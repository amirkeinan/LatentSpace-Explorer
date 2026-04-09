package ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.Box;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.geometry.Point3D;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.DataManager;
import model.WordEmbedding;
import model.Vector;
import math.CosineSimilarity;

/**
 * A 3D scatter-plot panel for visualizing word embeddings using JavaFX 3D.
 *
 * This panel is our Stage C extension for the LatentSpace Explorer. We use a
 * JFXPanel to embed a JavaFX 3D scene inside our existing Swing app. This
 * shows that our Object-Oriented architecture is extensible – we added a whole
 * 3D view without needing to change anything in the Model or Math classes!
 *
 * <h3>Design Patterns we used</h3>
 * <ul>
 * <li><b>Composite Pattern</b> – The 3D scene is built as a tree of JavaFX
 * Group nodes. We have a root group that holds the axes and the points.
 * When we rotate the root group with the mouse, everything inside it
 * rotates together nicely.</li>
 * <li><b>Observer Pattern / Listeners</b> – When the user changes an axis
 * in the ControlPanel, we rebuild the scene automatically on the JavaFX
 * thread using Platform.runLater().</li>
 * </ul>
 *
 * <h3>Performance & UI Fixes</h3>
 * - Created spheres with fewer polygons (8 instead of 64) so it doesn't lag
 * when drawing 5000 words.
 * - When clicking a word, we just change the color (material) instead of
 * rebuilding the whole scene, making it very fast.
 * - Text labels for words are drawn in 2D on top of the 3D scene. This solves
 * the problem of text being mirrored or upside-down when rotating the 3D space.
 *
 * <h3>Mouse Controls</h3>
 * - Mouse drag → rotate the 3D scene
 * - Mouse scroll → zoom in and out
 * - Click on a sphere → highlight word and its nearest neighbors
 */
public class GraphPanel3D extends JPanel {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Scaling factor to convert PCA values to 3D world coordinates. */
    private static final double COORDINATE_SCALE = 800.0;

    /** Default radius for word-point spheres. */
    private static final double SPHERE_RADIUS = 3.0;

    /** Radius for the selected word sphere (larger for visibility). */
    private static final double SELECTED_SPHERE_RADIUS = 6.0;

    /** Radius for nearest-neighbor spheres. */
    private static final double NEIGHBOR_SPHERE_RADIUS = 5.0;

    /**
     * Number of polygon divisions for each sphere.
     * Default JavaFX value is 64, but for 5000 points that creates millions
     * of polygons. 8 divisions gives a recognizable sphere shape while
     * being ~64x faster to render.
     */
    private static final int SPHERE_DIVISIONS = 8;

    /** Number of nearest neighbors to highlight (including self). */
    private static final int K_NEIGHBORS = 6;

    /** Length of each 3D reference axis line. */
    private static final double AXIS_LENGTH = 500.0;

    // ========================================================================
    // PCA AXIS CONFIGURATION
    // ========================================================================

    /** Index of the PCA component mapped to the 3D X-axis (0-49). */
    private int xAxisIndex = 0;

    /** Index of the PCA component mapped to the 3D Y-axis (0-49). */
    private int yAxisIndex = 1;

    /** Index of the PCA component mapped to the 3D Z-axis (0-49). */
    private int zAxisIndex = 2;

    // ========================================================================
    // JAVAFX SCENE COMPONENTS (Composite Pattern hierarchy)
    // ========================================================================

    /** The bridge panel embedding JavaFX content inside Swing. */
    private JFXPanel jfxPanel;

    /**
     * Root group of the 3D scene – rotations applied here cascade to ALL
     * children (Composite Pattern: parent transform affects all descendants).
     */
    private Group rootGroup;

    /**
     * Sub-group holding all word-point spheres.
     * Organized separately so we can clear / rebuild points without touching
     * the axis geometry.
     */
    private Group pointsGroup;

    /**
     * Group holding the 3D axes lines (X=red, Y=green, Z=blue).
     */
    private Group axisGroup;

    /** The perspective camera providing depth-based projection. */
    private PerspectiveCamera camera;

    /** The sub-scene containing the 3D world. */
    private SubScene subScene;

    /**
     * 2D overlay pane for text labels.
     * We draw labels here in 2D on top of the 3D scene. This fixes the issue
     * where text gets mirrored/reversed when you rotate the 3D world.
     */
    private Pane labelOverlay;

    /**
     * The StackPane that layers the 3D SubScene (bottom) and the 2D label
     * overlay (top).
     */
    private StackPane stackPane;

    // ========================================================================
    // ROTATION STATE
    // ========================================================================

    /** Current rotation angle around the X-axis (degrees). */
    private double rotateX = -20;

    /** Current rotation angle around the Y-axis (degrees). */
    private double rotateY = 30;

    /** Rotate transform applied to the root group for X-axis rotation. */
    private Rotate rotateXTransform;

    /** Rotate transform applied to the root group for Y-axis rotation. */
    private Rotate rotateYTransform;

    /** Camera's Z-axis translation (controls zoom distance). */
    private Translate cameraTranslate;

    // ========================================================================
    // MOUSE INTERACTION STATE
    // ========================================================================

    /** Last recorded mouse X position for drag delta calculation. */
    private double lastMouseX;

    /** Last recorded mouse Y position for drag delta calculation. */
    private double lastMouseY;

    /** Flag to distinguish drag from click. */
    private boolean isDragging = false;

    // ========================================================================
    // SELECTION STATE
    // ========================================================================

    /** The currently selected word (highlighted in red). */
    private WordEmbedding selectedWord;

    /** List of K-nearest neighbors to the selected word (highlighted orange). */
    private List<WordEmbedding> nearestNeighbors;

    /**
     * Map from word string → Sphere node, enabling efficient lookup when
     * updating highlights without rebuilding all spheres.
     */
    private Map<String, Sphere> wordToSphere = new HashMap<>();

    /**
     * Reverse map from Sphere node → word string, used for click detection
     * via JavaFX pick results.
     */
    private Map<Sphere, String> sphereToWord = new HashMap<>();

    // ========================================================================
    // PRE-ALLOCATED MATERIALS (performance: avoid creating new objects per frame)
    // ========================================================================

    /** Material for regular (unselected) word points. */
    private final PhongMaterial defaultMaterial = new PhongMaterial(Color.web("#3264C8"));

    /** Material for the selected word point. */
    private final PhongMaterial selectedMaterial = new PhongMaterial(Color.RED);

    /** Material for nearest-neighbor word points. */
    private final PhongMaterial neighborMaterial = new PhongMaterial(Color.ORANGE);

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    /**
     * Constructs the 3D visualization panel.
     *
     * Initializes the JFXPanel bridge and schedules the JavaFX scene creation
     * on the JavaFX Application Thread (required by JavaFX threading model).
     */
    public GraphPanel3D() {
        setLayout(new BorderLayout());

        // JFXPanel is the bridge between Swing and JavaFX
        // It creates a JavaFX toolkit automatically on first instantiation
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        // JavaFX scene creation MUST happen on the JavaFX Application Thread
        Platform.runLater(this::initializeScene);
    }

    // ========================================================================
    // SCENE INITIALIZATION
    // ========================================================================

    /**
     * Creates the full JavaFX 3D scene hierarchy (Composite Pattern).
     *
     * Scene hierarchy:
     * 
     * <pre>
     *   Scene
     *   └─ StackPane
     *      ├─ SubScene (3D, bottom layer)
     *      │  └─ rootGroup               ← rotations applied here
     *      │     ├─ axisGroup            ← X/Y/Z reference lines
     *      │     └─ pointsGroup          ← Sphere per word
     *      └─ labelOverlay (2D, top layer) ← text labels (never mirrored)
     * </pre>
     *
     * This method runs on the JavaFX Application Thread.
     */
    private void initializeScene() {
        // --- Build the Composite hierarchy ---
        rootGroup = new Group();
        pointsGroup = new Group();
        axisGroup = new Group();

        rootGroup.getChildren().addAll(axisGroup, pointsGroup);

        // --- Set up rotation transforms (applied to root → cascades to all) ---
        rotateXTransform = new Rotate(rotateX, Rotate.X_AXIS);
        rotateYTransform = new Rotate(rotateY, Rotate.Y_AXIS);
        rootGroup.getTransforms().addAll(rotateXTransform, rotateYTransform);

        // --- Build 3D reference axes ---
        buildAxisLines();

        // --- Build the SubScene with PerspectiveCamera ---
        subScene = new SubScene(rootGroup, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#F0F0F8"));

        // PerspectiveCamera provides true 3D depth perception
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(45);

        // Camera positioning: pulled back on the Z-axis to see the scene
        cameraTranslate = new Translate(0, 0, -1200);
        camera.getTransforms().add(cameraTranslate);

        subScene.setCamera(camera);

        // --- 2D Label overlay (renders ON TOP of 3D, never affected by 3D rotation)
        // ---
        // This solves the "mirrored text" problem: labels are pure 2D elements
        labelOverlay = new Pane();
        labelOverlay.setPickOnBounds(false); // Allow clicks to pass through to 3D
        labelOverlay.setMouseTransparent(true); // Don't intercept mouse events

        // --- StackPane layers the 3D scene and 2D overlay together ---
        stackPane = new StackPane(subScene, labelOverlay);

        // SubScene resize: bind to StackPane dimensions
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());

        // --- Set up mouse interaction on the SubScene ---
        setupMouseHandlers(subScene);

        // --- Wrap in a 2D scene (required by JFXPanel) ---
        Scene scene = new Scene(stackPane);
        jfxPanel.setScene(scene);

        // --- Populate with data ---
        rebuildPoints();
    }

    // ========================================================================
    // 3D REFERENCE AXES (Composite Pattern: children of axisGroup)
    // ========================================================================

    /**
     * Builds the 3D coordinate reference axes as colored Box shapes.
     *
     * Each axis is a thin elongated box:
     * <ul>
     * <li>X-axis → Red</li>
     * <li>Y-axis → Green</li>
     * <li>Z-axis → Blue</li>
     * </ul>
     *
     * These are children of {@link #axisGroup} in the Composite hierarchy.
     */
    private void buildAxisLines() {
        axisGroup.getChildren().clear();

        // X-axis (red): stretches along X
        Box xAxis = new Box(AXIS_LENGTH * 2, 1.0, 1.0);
        xAxis.setMaterial(new PhongMaterial(Color.RED));

        // Y-axis (green): stretches along Y
        Box yAxis = new Box(1.0, AXIS_LENGTH * 2, 1.0);
        yAxis.setMaterial(new PhongMaterial(Color.GREEN));

        // Z-axis (blue): stretches along Z
        Box zAxis = new Box(1.0, 1.0, AXIS_LENGTH * 2);
        zAxis.setMaterial(new PhongMaterial(Color.BLUE));

        axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
    }

    // ========================================================================
    // POINT RENDERING
    // ========================================================================

    /**
     * Rebuilds all 3D word spheres from the data.
     *
     * We use a low polygon count (SPHERE_DIVISIONS = 8) so the computer doesn't
     * lag when drawing 5000 spheres at once.
     *
     * It clears and refills pointsGroup (Composite pattern).
     */
    private void rebuildPoints() {
        if (pointsGroup == null)
            return;

        pointsGroup.getChildren().clear();
        wordToSphere.clear();
        sphereToWord.clear();

        // Also clear 2D labels
        if (labelOverlay != null) {
            labelOverlay.getChildren().clear();
        }

        List<WordEmbedding> embeddings = DataManager.getInstance().getEmbeddings();
        if (embeddings == null || embeddings.isEmpty())
            return;

        for (WordEmbedding we : embeddings) {
            Vector pca = we.getPcaVector();
            if (pca.getDimension() <= Math.max(xAxisIndex, Math.max(yAxisIndex, zAxisIndex))) {
                continue; // Skip if PCA vector doesn't have enough dimensions
            }

            // Extract PCA components for the selected axes
            double x = pca.get(xAxisIndex) * COORDINATE_SCALE;
            double y = -pca.get(yAxisIndex) * COORDINATE_SCALE; // Negate Y (screen convention)
            double z = pca.get(zAxisIndex) * COORDINATE_SCALE;

            // Create sphere with LOW polygon count for performance
            Sphere sphere = new Sphere(SPHERE_RADIUS, SPHERE_DIVISIONS);
            sphere.setMaterial(defaultMaterial);

            // Position the sphere in 3D space
            sphere.setTranslateX(x);
            sphere.setTranslateY(y);
            sphere.setTranslateZ(z);

            // Store mappings for click detection and highlight updates
            wordToSphere.put(we.getWord(), sphere);
            sphereToWord.put(sphere, we.getWord());

            pointsGroup.getChildren().add(sphere);
        }

        // Reapply highlights if a word is currently selected
        if (selectedWord != null) {
            updateHighlights();
        }
    }

    /**
     * Updates sphere colors and 2D labels for the selected word and its neighbors.
     *
     * We do this instead of rebuilding the whole scene from scratch because it's
     * much faster to just change the material of existing spheres.
     *
     * Labels are put on the 2D overlay so they don't get reverse-mirrored
     * by the 3D rotation.
     */
    private void updateHighlights() {
        // Clear old labels
        if (labelOverlay != null) {
            labelOverlay.getChildren().clear();
        }

        // Reset all spheres to default material
        for (Sphere sphere : sphereToWord.keySet()) {
            sphere.setRadius(SPHERE_RADIUS);
            sphere.setMaterial(defaultMaterial);
        }

        // Highlight nearest neighbors (orange, medium size)
        if (nearestNeighbors != null) {
            for (WordEmbedding neighbor : nearestNeighbors) {
                Sphere sphere = wordToSphere.get(neighbor.getWord());
                if (sphere != null && neighbor != selectedWord) {
                    sphere.setRadius(NEIGHBOR_SPHERE_RADIUS);
                    sphere.setMaterial(neighborMaterial);
                    // Add 2D label for neighbor
                    add2DLabel(neighbor.getWord(), sphere, Color.ORANGE, false);
                }
            }
        }

        // Highlight selected word (red, large) — drawn last to overlay neighbors
        if (selectedWord != null) {
            Sphere sphere = wordToSphere.get(selectedWord.getWord());
            if (sphere != null) {
                sphere.setRadius(SELECTED_SPHERE_RADIUS);
                sphere.setMaterial(selectedMaterial);
                // Add 2D label for selected word
                add2DLabel(selectedWord.getWord(), sphere, Color.RED, true);
            }
        }
    }

    /**
     * Creates a 2D label positioned near a 3D sphere using coordinate projection.
     *
     * The label is placed in the {@link #labelOverlay} pane, which sits on top
     * of the 3D SubScene. The 3D-to-2D projection uses
     * {@link Node#localToScene(Point3D)} to convert the sphere's 3D position
     * to screen coordinates.
     *
     * @param word   The text to display.
     * @param sphere The 3D sphere to position the label near.
     * @param color  Label text color.
     * @param isBold Whether to use bold font.
     */
    private void add2DLabel(String word, Sphere sphere, Color color, boolean isBold) {
        if (labelOverlay == null || subScene == null)
            return;

        Label label = new Label(word);
        label.setTextFill(color);
        label.setFont(Font.font("SansSerif",
                isBold ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        label.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-padding: 1 3;");

        // Project 3D sphere position to 2D screen coordinates
        // This uses the SubScene's camera automatically
        Point3D sceneCoords = sphere.localToScene(Point3D.ZERO, true);

        // Position label slightly offset from the projected point
        label.setLayoutX(sceneCoords.getX() + 8);
        label.setLayoutY(sceneCoords.getY() - 10);

        labelOverlay.getChildren().add(label);
    }

    /**
     * Refreshes the 2D label positions after scene rotation/zoom.
     * Called after every drag or scroll to keep labels aligned with spheres.
     */
    private void refreshLabelPositions() {
        if (labelOverlay == null || selectedWord == null)
            return;

        // Faster approach: clear and re-add only the active labels
        labelOverlay.getChildren().clear();

        if (nearestNeighbors != null) {
            for (WordEmbedding neighbor : nearestNeighbors) {
                if (neighbor != selectedWord) {
                    Sphere sphere = wordToSphere.get(neighbor.getWord());
                    if (sphere != null) {
                        add2DLabel(neighbor.getWord(), sphere, Color.ORANGE, false);
                    }
                }
            }
        }

        if (selectedWord != null) {
            Sphere sphere = wordToSphere.get(selectedWord.getWord());
            if (sphere != null) {
                add2DLabel(selectedWord.getWord(), sphere, Color.RED, true);
            }
        }
    }

    // ========================================================================
    // MOUSE INTERACTION
    // ========================================================================

    /**
     * Sets up mouse event handlers for rotation, zoom, and click selection.
     *
     * <ul>
     * <li><b>Drag</b>: Rotates the root group around X and Y axes. Since
     * the rotation is applied to the root of the Composite hierarchy,
     * ALL children (axes, points) rotate together.</li>
     * <li><b>Scroll</b>: Translates the camera along Z (zoom).</li>
     * <li><b>Click</b>: Uses JavaFX pick-result to identify which Sphere
     * was clicked, then selects the corresponding word.</li>
     * </ul>
     *
     * @param scene The SubScene to attach handlers to.
     */
    private void setupMouseHandlers(SubScene scene) {
        // --- Record mouse position on press ---
        scene.setOnMousePressed((MouseEvent event) -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
            isDragging = false;
        });

        // --- Drag to rotate ---
        scene.setOnMouseDragged((MouseEvent event) -> {
            isDragging = true;
            double deltaX = event.getSceneX() - lastMouseX;
            double deltaY = event.getSceneY() - lastMouseY;

            // Horizontal drag → rotate around Y-axis
            rotateY += deltaX * 0.5;
            rotateYTransform.setAngle(rotateY);

            // Vertical drag → rotate around X-axis
            rotateX -= deltaY * 0.5;
            rotateXTransform.setAngle(rotateX);

            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();

            // Update label positions to follow rotated spheres
            refreshLabelPositions();
        });

        // --- Scroll to zoom ---
        scene.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            // Move camera closer / farther on Z
            double currentZ = cameraTranslate.getZ();
            cameraTranslate.setZ(currentZ + delta * 1.5);

            // Update label positions after zoom
            refreshLabelPositions();
        });

        // --- Click to select (only on actual click, not drag-end) ---
        scene.setOnMouseReleased((MouseEvent event) -> {
            if (!isDragging) {
                handleClick(event);
            }
        });
    }

    /**
     * Handles a mouse click event by checking if a Sphere was hit.
     *
     * Uses JavaFX's built-in pick-result system which performs ray-casting
     * through the 3D scene to find intersected nodes.
     *
     * @param event The mouse click event.
     */
    private void handleClick(MouseEvent event) {
        PickResult pickResult = event.getPickResult();
        Node picked = pickResult.getIntersectedNode();

        if (picked instanceof Sphere && sphereToWord.containsKey(picked)) {
            // A word sphere was clicked
            String word = sphereToWord.get(picked);
            WordEmbedding we = DataManager.getInstance().getEmbedding(word);
            if (we != null) {
                selectedWord = we;
                findNearestNeighbors(we);
                updateHighlights(); // Fast: only updates materials, no rebuild
            }
        } else {
            // Clicked empty space → deselect
            selectedWord = null;
            nearestNeighbors = null;
            updateHighlights();
        }
    }

    // ========================================================================
    // NEAREST NEIGHBOR SEARCH
    // ========================================================================

    /**
     * Finds the K-nearest neighbors to the target word using Cosine Similarity
     * in the original high-dimensional space (NOT the PCA projection).
     *
     * This is identical to the algorithm in {@link GraphPanel} – the calculation
     * logic lives in the math layer and is reused without modification.
     *
     * @param target The word whose neighbors to find.
     */
    private void findNearestNeighbors(WordEmbedding target) {
        List<WordEmbedding> all = DataManager.getInstance().getEmbeddings();
        CosineSimilarity cos = new CosineSimilarity();

        // Sort by descending cosine similarity
        List<WordEmbedding> sorted = new ArrayList<>(all);
        sorted.sort((w1, w2) -> {
            double d1 = cos.calculate(target.getDenseVector(), w1.getDenseVector());
            double d2 = cos.calculate(target.getDenseVector(), w2.getDenseVector());
            return Double.compare(d2, d1);
        });

        // Keep top K (includes self + K-1 true neighbors)
        nearestNeighbors = new ArrayList<>();
        for (int i = 0; i < Math.min(K_NEIGHBORS, sorted.size()); i++) {
            nearestNeighbors.add(sorted.get(i));
        }
    }

    // ========================================================================
    // PUBLIC API (mirrors GraphPanel for interoperability)
    // ========================================================================

    /**
     * Gets the current PCA component index mapped to the X-axis.
     * 
     * @return Index (0-49).
     */
    public int getXAxisIndex() {
        return xAxisIndex;
    }

    /**
     * Gets the current PCA component index mapped to the Y-axis.
     * 
     * @return Index (0-49).
     */
    public int getYAxisIndex() {
        return yAxisIndex;
    }

    /**
     * Gets the current PCA component index mapped to the Z-axis.
     * 
     * @return Index (0-49).
     */
    public int getZAxisIndex() {
        return zAxisIndex;
    }

    /**
     * Sets which PCA dimensions to display on the 3 axes and rebuilds the
     * 3D scene. This is called by the ControlPanel when the user changes
     * axis selections, and by ChangeAxisCommand during undo/redo.
     *
     * @param xIndex PCA component for X-axis (0-49).
     * @param yIndex PCA component for Y-axis (0-49).
     * @param zIndex PCA component for Z-axis (0-49).
     */
    public void setAxes(int xIndex, int yIndex, int zIndex) {
        this.xAxisIndex = xIndex;
        this.yAxisIndex = yIndex;
        this.zAxisIndex = zIndex;

        // Rebuild on the JavaFX Application Thread
        Platform.runLater(() -> {
            buildAxisLines();
            rebuildPoints();
        });
    }

    /**
     * Centers the 3D view on a specific word and selects it.
     * Resets rotation and adjusts camera to point at the word's position.
     *
     * @param word The word string to find and center on.
     */
    public void centerOnWord(String word) {
        WordEmbedding we = DataManager.getInstance().getEmbedding(word);
        if (we != null) {
            selectedWord = we;
            findNearestNeighbors(we);

            Platform.runLater(() -> {
                // Reset rotation for a clean view
                rotateX = -20;
                rotateY = 30;
                rotateXTransform.setAngle(rotateX);
                rotateYTransform.setAngle(rotateY);

                // Reset camera zoom
                cameraTranslate.setZ(-1200);

                updateHighlights();
            });
        }
    }
}
