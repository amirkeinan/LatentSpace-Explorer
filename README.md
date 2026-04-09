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

### Architecture

The project leverages a strict Model-View-Controller (MVC) architecture, paired with several GoF design patterns to ensure extensibility and solid OOP principles:
- **Command Pattern**: Powers the Undo/Redo capabilities for graph navigation.
- **Strategy Pattern**: Manages the usage of different mathematical metrics (Cosine vs. Euclidean).

## License

This project is open-source and available under the terms of the [MIT License](LICENSE).
