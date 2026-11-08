# Ice & Spice Restaurant Recommendation System

A Java console demo of a menu recommendation engine for a fictional restaurant ("Ice & Spice"), built around an object-oriented design: abstract preference and feedback analyzers, a recommendation engine that composes them, user profiles, menu categories, inventory, offers and a popularity tracker. An optional Python component trains a small collaborative-filtering model and can be called from Java.

This started as a group university project. `Inventory` was a stub left for another team member and has been filled in with a simple per-item stock map.

## Run the demo

Needs JDK 11 or later.

```bash
./build.sh
./run.sh
```

`Main` builds a 15-item menu, four sample user profiles and two offers, then prints the top three recommendations per user and processes one piece of feedback.

## Python ML component (optional)

```bash
cd python
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python test_ml.py
```

`ml_integration.py` generates sample interaction data on first use and fits an item/user similarity model. It writes `is.db` and `models/` into the current directory (both git-ignored). `java_python_bridge.py` reads one JSON request from stdin and prints one JSON response; `src/ml/MLPredictionService.java` calls it from Java. That Java class needs the Jackson databind jars in `lib/` (Jackson is not bundled), and the Python script path is relative to the repository root. The demo in `Main` does not use it.

## Layout

```
src/Main.java          demo entry point
src/engine/            RecommendationEngine, UserProfileManager
src/analyzer/          preference analyzers (content-based, collaborative) and feedback analyzers
src/model/             MenuItem, MenuCategory, Offer, Order, Inventory, UserPreferences, ...
src/filter/            RecommendationFilter
src/tracker/           PopularityTracker
src/ml/                Java client for the Python component
python/                collaborative-filtering model and bridge
```

## Limitations

- There are no automated unit tests; `python/test_ml.py` is a smoke script.
- With no order history, every user goes through the "cold start" path, so the demo's top three are the same for all four profiles.
- The collaborative filter is trained on generated sample data, not real orders.
