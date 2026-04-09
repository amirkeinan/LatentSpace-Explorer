package command;

import ui.GraphPanel;
import ui.GraphPanel3D;

/**
 * A concrete Command implementation that encapsulates the action of changing
 * the graph axes.
 * It stores the "before" and "after" states of the X, Y, and Z axis indices
 * to allow stepping back and forth through navigation history.
 *
 *
 * For Stage C (3D Support), this command was updated to support an optional 
 * third axis (Z). If a GraphPanel3D is provided, undo/redo handles the Z-axis too. 
 * If it's just 2D (graphPanel3D is null), it works exactly like before.
 *
 * We use -1 for the Z-axis to mean "not applicable" (when we are in 2D mode).
 */
public class ChangeAxisCommand implements Command {

    /** The 2D graph panel (always present). */
    private GraphPanel graphPanel;

    /** The 3D graph panel (null when operating in 2D-only mode). */
    private GraphPanel3D graphPanel3D;

    /** Previous X-axis PCA component index. */
    private int oldX;

    /** Previous Y-axis PCA component index. */
    private int oldY;

    /** Previous Z-axis PCA component index (-1 if 2D mode). */
    private int oldZ;

    /** New X-axis PCA component index. */
    private int newX;

    /** New Y-axis PCA component index. */
    private int newY;

    /** New Z-axis PCA component index (-1 if 2D mode). */
    private int newZ;

    /**
     * Constructs a new ChangeAxisCommand supporting both 2D and 3D modes.
     *
     * @param graphPanel   The 2D graph panel (receiver for 2D axis changes).
     * @param graphPanel3D The 3D graph panel (receiver for 3D axis changes),
     *                     or {@code null} for 2D-only mode.
     * @param oldX         The previous X-axis index.
     * @param oldY         The previous Y-axis index.
     * @param oldZ         The previous Z-axis index (-1 for 2D mode).
     * @param newX         The new X-axis index.
     * @param newY         The new Y-axis index.
     * @param newZ         The new Z-axis index (-1 for 2D mode).
     */
    public ChangeAxisCommand(GraphPanel graphPanel, GraphPanel3D graphPanel3D,
                             int oldX, int oldY, int oldZ,
                             int newX, int newY, int newZ) {
        this.graphPanel = graphPanel;
        this.graphPanel3D = graphPanel3D;
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldZ = oldZ;
        this.newX = newX;
        this.newY = newY;
        this.newZ = newZ;
    }

    /**
     * Applies the new axis settings to the appropriate graph panel.
     *
     * If a 3D panel is available and the Z-axis is set (not -1),
     * applies all three axes to the 3D panel.
     * Otherwise, applies X and Y to the 2D panel.
     */
    @Override
    public void execute() {
        if (graphPanel3D != null && newZ >= 0) {
            graphPanel3D.setAxes(newX, newY, newZ);
        } else {
            graphPanel.setAxes(newX, newY);
        }
    }

    /**
     * Reverts the graph to the old axis settings.
     *
     * Mirrors the logic of {@link #execute()}, using the old values instead.
     */
    @Override
    public void undo() {
        if (graphPanel3D != null && oldZ >= 0) {
            graphPanel3D.setAxes(oldX, oldY, oldZ);
        } else {
            graphPanel.setAxes(oldX, oldY);
        }
    }
}
