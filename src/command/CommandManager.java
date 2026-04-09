package command;

import java.util.Stack;

/**
 * Invoker class in the Command Pattern.
 * Manages the execution history of commands using Undo and Redo stacks.
 * This allows the user to traverse backward and forward through their action
 * history.
 */
public class CommandManager {
    /** Stack to store executed commands (History). */
    private Stack<Command> undoStack = new Stack<>();

    /** Stack to store undone commands (Future). */
    private Stack<Command> redoStack = new Stack<>();

    /**
     * Executes a new command and pushes it onto the undo stack.
     * Clears the redo stack because a new path of history has diverged.
     * 
     * @param command The command to execute.
     */
    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Clear redo stack on new command to avoid inconsistent state
    }

    /**
     * Undoes the most recent command.
     * Pops from undo stack, calls undo(), and pushes to redo stack.
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    /**
     * Redoes the most recently undone command.
     * Pops from redo stack, calls execute(), and pushes back to undo stack.
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }
}
