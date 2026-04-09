package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class responsible for managing the lifecycle and access of
 * WordEmbedding data.
 * It handles loading data from JSON files and provides optimized methods to
 * retrieve
 * embeddings by index or by word string.
 */
public class DataManager {
    /** The single instance of the DataManager. */
    private static DataManager instance;

    /** A list of all loaded embeddings for iteration. */
    private List<WordEmbedding> embeddings;

    /** A map for O(1) retrieval of embeddings by word. */
    private Map<String, WordEmbedding> embeddingMap;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private DataManager() {
        embeddings = new ArrayList<>();
        embeddingMap = new HashMap<>();
    }

    /**
     * Retrieves the global instance of DataManager.
     * 
     * @return The singleton DataManager instance.
     */
    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    /**
     * Loads embedding data from the specified JSON files.
     * It correlates the full vectors and PCA vectors to create unified
     * WordEmbedding objects.
     * 
     * @param fullVectorsPath Path to the JSON file containing full 300D vectors.
     * @param pcaVectorsPath  Path to the JSON file containing PCA 50D vectors.
     */
    public void loadData(String fullVectorsPath, String pcaVectorsPath) {
        try {
            // Read all bytes to string (suitable for the scale of project like mine)
            String fullJson = new String(Files.readAllBytes(Paths.get(fullVectorsPath)));
            String pcaJson = new String(Files.readAllBytes(Paths.get(pcaVectorsPath)));

            // Parse using my custom parser
            List<Map<String, Object>> fullList = SimpleJsonParser.parseJsonArray(fullJson);
            List<Map<String, Object>> pcaList = SimpleJsonParser.parseJsonArray(pcaJson);

            // Index full vectors by word for quick lookup during merge
            Map<String, double[]> fullMap = new HashMap<>();
            for (Map<String, Object> item : fullList) {
                String word = (String) item.get("word");
                double[] vec = (double[]) item.get("vector");
                fullMap.put(word, vec);
            }

            embeddings.clear();
            embeddingMap.clear();

            // Create WordEmbedding objects combining both vector types
            for (Map<String, Object> item : pcaList) {
                String word = (String) item.get("word");
                double[] pcaVec = (double[]) item.get("vector");
                double[] fullVec = fullMap.get(word);

                if (fullVec != null) {
                    WordEmbedding we = new WordEmbedding(word, new Vector(fullVec), new Vector(pcaVec));
                    embeddings.add(we);
                    embeddingMap.put(word, we);
                }
            }

            System.out.println("Loaded " + embeddings.size() + " embeddings.");

        } catch (IOException e) {
            System.err.println("Error loading data files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Returns the list of all loaded embeddings.
     * 
     * @return A list of WordEmbedding objects.
     */
    public List<WordEmbedding> getEmbeddings() {
        return embeddings;
    }

    /**
     * Retrieves a specific embedding by its word.
     * 
     * @param word The word to look up (case-sensitive).
     * @return The WordEmbedding object, or null if not found.
     */
    public WordEmbedding getEmbedding(String word) {
        return embeddingMap.get(word);
    }
}
