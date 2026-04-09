# LatentSpace Explorer

A Java desktop application for exploring and visualizing word embeddings using Principal Component Analysis (PCA).
LatentSpace Explorer allows users to navigate the semantic relationships between words in both 2D and 3D space.

## Features

- **2D & 3D Visualization**: Interactive visual mapping of word embeddings using Java Swing and JavaFX.
- **PCA Axis Navigation**: Choose specific Principal Components (e.g., PC1, PC2, PC3) to explore different semantic axes.
- **Semantic Distance**: Calculate cosine similarity and Euclidean distance between word pairs.
- **Vector Arithmetic**: Perform complex semantic math (e.g., *King - Man + Woman = Queen*).
- **Subspace Analysis**: Analyze properties of word groups and compute their centroids.
- **Projection**: Project words onto custom semantic axes.
- **Undo/Redo Tracking**: Navigation history is preserved, allowing smooth and easy view adjustments using the Command Pattern.

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 8 or higher.
- (If applicable, Python environment for `embeddings_gen.py`)

### Running the Application

For Windows users, simply run the provided batch file:
```bash
./run.bat
```
This script handles the compilation of the Java source files into the `bin/` directory and executes the main program.

## Object-Oriented Design & Architecture

This project was built with a strong emphasis on clean code, extensibility, and strict Object-Oriented principles. It demonstrates practical implementations of GoF Design Patterns and SOLID principles:

- **Model-View-Controller (MVC)**: 
  The data and mathematical logic (Model) are strictly separated from the presentation layer (View). This decoupled architecture made it trivial to extend the project from a 2D view to a 3D UI without modifying a single line of the underlying data managers or math engines.
- **Command Pattern**: 
  Implemented for robust Undo/Redo functionality. Every navigation step (such as changing PCA axes) is encapsulated as a `Command` object, demonstrating a classic behavioral pattern for managing UI state history.
- **Strategy Pattern**: 
  Leveraged within the mathematics engine to seamlessly interchange distance algorithms (Cosine Similarity vs. Euclidean Distance) without altering the client code that executes the calculations.
- **SOLID Principles**:
  - *Single Responsibility Principle (SRP)*: Components are highly modular. Logic is cleanly split between `DataManager`, specialized `Math` classes, and UI controllers.
  - *Open/Closed Principle (OCP)*: Distance metrics and mathematical behaviors are designed around interfaces, making the application open for new metrics but closed for modification.

## License

This project is open-source and available under the terms of the [MIT License](LICENSE).
