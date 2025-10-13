

import numpy as np
import pandas as pd
import json
import sqlite3
import pickle
from datetime import datetime, timedelta
from typing import List, Dict, Tuple, Optional
import logging
from pathlib import Path

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class MLEngine:

    def __init__(self, db_path: str = "is.db"):
        self.db_path = db_path
        self.models = {}
        self._init_database()
        self._load_models()

    def _init_database(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        # Create tables for ML data
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS user_interactions (
                user_id TEXT,
                item_id TEXT,
                rating REAL,
                timestamp TEXT,
                context TEXT,
                PRIMARY KEY (user_id, item_id, timestamp)
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS item_features (
                item_id TEXT PRIMARY KEY,
                name TEXT,
                category TEXT,
                price REAL,
                ingredients TEXT,
                tags TEXT,
                avg_rating REAL,
                popularity_score REAL
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS user_profiles (
                user_id TEXT PRIMARY KEY,
                dietary_preference TEXT,
                spice_level INTEGER,
                price_range_min REAL,
                price_range_max REAL,
                favorite_categories TEXT,
                allergens TEXT
            )
        """)

        conn.commit()
        conn.close()
        logger.info("Database initialized successfully")

    def _load_models(self):
        """Load pre-trained ML models."""
        models_dir = Path("models")
        models_dir.mkdir(exist_ok=True)

        # Initialize models if they don't exist
        if not (models_dir / "collaborative_filter.pkl").exists():
            self._train_initial_models()

    def _train_initial_models(self):
        """Train initial models with sample data."""
        logger.info("Training initial ML models...")

        # Create sample training data for Ice & Spice
        sample_data = self._generate_sample_training_data()

        # Train collaborative filtering model
        self._train_collaborative_filtering(sample_data)

        logger.info("Initial model training completed")

    def _generate_sample_training_data(self) -> pd.DataFrame:
        """Generate sample training data for Ice & Spice menu items."""

        # Ice & Spice menu items with features
        items = [
            ("VEG001", "Veg Burger", "Vegetarian Food", 50, "vegetarian", 4.2),
            ("VEG004", "Vada Pav", "Vegetarian Food", 25, "vegetarian,spicy", 4.5),
            ("NONVEG001", "Chicken Burger", "Non-Vegetarian Food", 60, "non-vegetarian", 4.3),
            ("LIVE003", "Pav Bhaji", "Live Counter", 60, "vegetarian,spicy", 4.7),
            ("DESSERT003", "Brownie", "Desserts", 50, "vegetarian,sweet", 4.6),
        ]

        # Generate user interaction data
        users = ["STUDENT001", "HEALTH001", "SWEET001", "MEAT001"]
        interactions = []

        np.random.seed(42)  # For reproducible results

        for user in users:
            for item_id, name, category, price, tags, avg_rating in items:
                # Generate ratings based on user preferences
                if user == "STUDENT001":  # Budget conscious, likes spicy
                    if price <= 60 and "spicy" in tags:
                        rating = np.random.normal(4.5, 0.5)
                    else:
                        rating = np.random.normal(3.5, 0.7)

                elif user == "HEALTH001":  # Vegetarian, health conscious
                    if "vegetarian" in tags:
                        rating = np.random.normal(4.5, 0.4)
                    else:
                        continue  # Skip non-veg items

                elif user == "SWEET001":  # Loves desserts
                    if "sweet" in tags:
                        rating = np.random.normal(4.8, 0.3)
                    else:
                        rating = np.random.normal(3.5, 0.7)

                elif user == "MEAT001":  # Non-veg lover
                    if "non-vegetarian" in tags:
                        rating = np.random.normal(4.6, 0.4)
                    else:
                        rating = np.random.normal(3.0, 0.8)

                rating = np.clip(rating, 1.0, 5.0)

                if rating >= 3.0:
                    interactions.append({
                        'user_id': user,
                        'item_id': item_id,
                        'rating': rating,
                        'category': category,
                        'price': price,
                        'tags': tags,
                        'timestamp': datetime.now() - timedelta(days=np.random.randint(1, 90))
                    })

        return pd.DataFrame(interactions)

    def _train_collaborative_filtering(self, data: pd.DataFrame):
        """Train collaborative filtering model."""
        try:
            # Create user-item matrix
            pivot_table = data.pivot_table(index='user_id', columns='item_id', values='rating', fill_value=0)

            # Simple collaborative filtering using cosine similarity
            from sklearn.metrics.pairwise import cosine_similarity

            user_similarity = cosine_similarity(pivot_table.values)
            item_similarity = cosine_similarity(pivot_table.T.values)

            self.models['collaborative'] = {
                'user_similarity': user_similarity,
                'item_similarity': item_similarity,
                'user_index': pivot_table.index.tolist(),
                'item_index': pivot_table.columns.tolist(),
                'pivot_table': pivot_table
            }

            # Save model
            models_dir = Path("models")
            with open(models_dir / "collaborative_filter.pkl", 'wb') as f:
                pickle.dump(self.models['collaborative'], f)

            logger.info("Collaborative filtering model trained successfully")

        except ImportError:
            logger.warning("scikit-learn not available, using simple fallback model")
            self.models['collaborative'] = {'fallback': True}
        except Exception as e:
            logger.error(f"Error training collaborative filtering: {e}")

    def predict_rating(self, user_id: str, item_id: str, method: str = "hybrid") -> Dict:
        """Predict rating for a user-item pair."""

        if method in ["collaborative", "hybrid"] and "collaborative" in self.models:
            return self._predict_collaborative(user_id, item_id)

        # Fallback prediction
        return {"rating": 3.5, "confidence": 0.3, "method": "fallback"}

    def _predict_collaborative(self, user_id: str, item_id: str) -> Dict:
        """Predict using collaborative filtering."""
        try:
            model = self.models["collaborative"]

            if 'fallback' in model:
                return {"rating": 3.5, "confidence": 0.3, "method": "collaborative"}

            if user_id not in model["user_index"] or item_id not in model["item_index"]:
                return {"rating": 3.5, "confidence": 0.2, "method": "collaborative"}

            user_idx = model["user_index"].index(user_id)
            item_idx = model["item_index"].index(item_id)

            # Simple prediction based on user similarity
            user_similarities = model["user_similarity"][user_idx]
            pivot_table = model["pivot_table"]

            # Find similar users who rated this item
            item_ratings = pivot_table.iloc[:, item_idx]
            similar_users = np.where((user_similarities > 0.1) & (item_ratings > 0))[0]

            if len(similar_users) > 0:
                weights = user_similarities[similar_users]
                ratings = item_ratings.iloc[similar_users]

                if np.sum(weights) > 0:
                    predicted_rating = np.average(ratings, weights=weights)
                    confidence = min(len(similar_users) / 10.0, 0.9)

                    return {
                        "rating": float(np.clip(predicted_rating, 1.0, 5.0)),
                        "confidence": float(confidence),
                        "method": "collaborative"
                    }

            # Fallback to item average
            item_avg = np.mean(item_ratings[item_ratings > 0])
            if not np.isnan(item_avg):
                return {"rating": float(item_avg), "confidence": 0.4, "method": "collaborative"}

            return {"rating": 3.5, "confidence": 0.2, "method": "collaborative"}

        except Exception as e:
            logger.error(f"Error in collaborative prediction: {e}")
            return {"rating": 3.5, "confidence": 0.1, "method": "collaborative"}

    def get_recommendations(self, user_id: str, item_ids: List[str], top_k: int = 5) -> List[Dict]:
        """Get top-k recommendations for a user from given item list."""

        recommendations = []

        for item_id in item_ids:
            prediction = self.predict_rating(user_id, item_id, method="hybrid")
            recommendations.append({
                "item_id": item_id,
                "predicted_rating": prediction["rating"],
                "confidence": prediction["confidence"],
                "method": prediction["method"]
            })

        # Sort by predicted rating and confidence
        recommendations.sort(
            key=lambda x: (x["predicted_rating"] * x["confidence"]), 
            reverse=True
        )

        return recommendations[:top_k]

    def update_model_with_feedback(self, user_id: str, item_id: str, rating: float, context: str = None):
        """Update models with new user feedback."""

        # Store interaction in database
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            INSERT OR REPLACE INTO user_interactions 
            (user_id, item_id, rating, timestamp, context)
            VALUES (?, ?, ?, ?, ?)
        """, (user_id, item_id, rating, datetime.now().isoformat(), context or ""))

        conn.commit()
        conn.close()

        logger.info(f"Updated feedback for user {user_id}, item {item_id}, rating {rating}")

    def get_model_performance(self) -> Dict:
        """Get performance metrics for all models."""

        conn = sqlite3.connect(self.db_path)

        # Get interaction count
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM user_interactions")
        interaction_count = cursor.fetchone()[0]

        conn.close()

        return {
            "total_interactions": interaction_count,
            "models_available": list(self.models.keys()),
            "status": "operational"
        }

if __name__ == "__main__":
    # Demo the ML engine
    print("=== ML Engine Demo ===")

    ml_engine = IceSpiceMLEngine()

    # Test prediction
    prediction = ml_engine.predict_rating("STUDENT001", "VEG004")
    print(f"Prediction for STUDENT001 -> VEG004: {prediction}")

    # Test recommendations
    item_list = ["VEG001", "VEG004", "NONVEG001", "LIVE003", "DESSERT003"]
    recommendations = ml_engine.get_recommendations("STUDENT001", item_list, top_k=3)

    print("\nTop 3 recommendations for STUDENT001:")
    for i, rec in enumerate(recommendations, 1):
        print(f"{i}. {rec['item_id']}: Rating {rec['predicted_rating']:.2f} "
              f"(Confidence: {rec['confidence']:.2f})")
