package command;

/**
 * Interface for the Command Design Pattern.
 * Encapsulates a request as an object, allowing for parameterization of clients
 * with different requests,
 * queueing of requests, and most importantly, supporting Undo/Redo operations.
 */
public interface Command {
    /**
     * Executes the command logic.
     */
    void execute();

    /**
     * Reverses the command logic, restoring the state to before execution.
     */
    void undo();
}
