#!/usr/bin/env python3


from ml_integration import IceSpiceMLEngine

def main():
    print("=== Testing Integration ===")

    # Initialize ML engine
    ml_engine = IceSpiceMLEngine()

    # Test predictions
    users = ["STUDENT001", "HEALTH001", "SWEET001", "MEAT001"]
    items = ["VEG001", "VEG004", "NONVEG001", "LIVE003", "DESSERT003"]

    print("\n1. Testing rating predictions:")
    for user in users[:2]:  # Test first 2 users
        for item in items[:3]:  # Test first 3 items
            prediction = ml_engine.predict_rating(user, item)
            print(f"  {user} -> {item}: {prediction['rating']:.2f} (confidence: {prediction['confidence']:.2f})")

    print("\n2. Testing recommendations:")
    recommendations = ml_engine.get_recommendations("STUDENT001", items, top_k=3)
    for i, rec in enumerate(recommendations, 1):
        print(f"  {i}. {rec['item_id']}: {rec['predicted_rating']:.2f}")

    print("\n3. Testing feedback update:")
    ml_engine.update_model_with_feedback("STUDENT001", "VEG004", 5.0, "Loved it!")
    print("  ✓ Feedback updated")

    print("\n4. Performance metrics:")
    metrics = ml_engine.get_model_performance()
    print(f"  {metrics}")

    print("\n✅ ML integration test completed successfully!")

if __name__ == "__main__":
    main()
