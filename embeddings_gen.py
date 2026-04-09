# -*- coding: utf-8 -*-
"""
LatentSpace Explorer - Data Generation Script
---------------------------------------------
This script is responsible for the "ETL" (Extract, Transform, Load) process of the project.
1. LOADS pre-trained word embeddings (GloVe) using the gensim library.
2. PERFORMS Dimensionality Reduction using PCA (Principal Component Analysis) to project
   the 300-dimensional vectors down to 50 dimensions for the Java application.
3. EXPORTS the data into two JSON files:
   - full_vectors.json: Contains original 300D vectors (for precise math).
   - pca_vectors.json: Contains reduced 50D vectors (for visualization).
"""

import json
import gensim.downloader as api
from sklearn.decomposition import PCA
import numpy as np
import sys
import os

try:
    # Configuration: Number of words to process
    # 5000 is a good balance between data richness and application performance
    limit = 5000 
    
    # Configuration: Model Name
    # 'glove-wiki-gigaword-100' provides 100D vectors.
    # We can also use 'word2vec-google-news-300' for 300D provided we have RAM.
    # For this demo, let's stick to a robust model.
    gensim_model_name = "glove-wiki-gigaword-100"
    
    print(f"--- Loading GloVe model: {gensim_model_name} (this may take a minute)... ---")
    model = api.load(gensim_model_name)

    # Extract the top 'limit' most common words
    words = model.index_to_key[:limit]
    full_vectors = [model[word] for word in words]

    print(f"--- Performing PCA (Reduction to 50D)... ---")
    # Initialize PCA to reduce data to 50 principal components
    pca = PCA(n_components=50)
    
    # Fit and Transform the data
    # fit_transform() learns the projection matrix from the data and applies it
    pca_result = pca.fit_transform(np.array(full_vectors))

    # Prepare data structures for JSON export
    full_space_data = []
    pca_space_data = []

    for i, word in enumerate(words):
        # Store full vector for distance calculations (Cosine/Euclidean)
        full_space_data.append({
            "word": word,
            "vector": full_vectors[i].tolist() # Convert numpy array to standard list
        })
        
        # Store reduced vector for 2D/3D Graph Visualization
        pca_space_data.append({
            "word": word,
            "vector": pca_result[i].tolist()
        })

    print(f"--- Saving JSON files... ---")

    # Write full vectors
    with open('full_vectors.json', 'w', encoding='utf-8') as f:
        json.dump(full_space_data, f, ensure_ascii=False)

    # Write reduced vectors
    with open('pca_vectors.json', 'w', encoding='utf-8') as f:
        json.dump(pca_space_data, f, ensure_ascii=False)

    print(f"--- Success! Created 'full_vectors.json' and 'pca_vectors.json' ---")
    print(f"--- Processed {len(words)} words. ---")

except Exception as e:
    print(f"Error occurred during generation: {e}")
