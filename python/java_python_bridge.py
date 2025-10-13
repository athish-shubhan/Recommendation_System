
import json
import sys
from ml_integration import MLEngine

class JavaPythonBridge:

    def __init__(self):
        self.ml_engine = MLEngine()

    def process_java_request(self, request_data: dict) -> dict:

        command = request_data.get("command")

        if command == "predict_rating":
            return self._handle_rating_prediction(request_data)
        elif command == "get_recommendations": 
            return self._handle_recommendations(request_data)
        elif command == "update_feedback":
            return self._handle_feedback_update(request_data)
        elif command == "get_performance":
            return self._handle_performance_metrics(request_data)
        else:
            return {"error": f"Unknown command: {command}"}

    def _handle_rating_prediction(self, data: dict) -> dict:
        user_id = data.get("user_id")
        item_id = data.get("item_id") 
        method = data.get("method", "hybrid")

        if not user_id or not item_id:
            return {"error": "Missing user_id or item_id"}

        try:
            prediction = self.ml_engine.predict_rating(user_id, item_id, method)
            return {"status": "success", "prediction": prediction}
        except Exception as e:
            return {"error": str(e)}

    def _handle_recommendations(self, data: dict) -> dict:
        user_id = data.get("user_id")
        item_ids = data.get("item_ids", [])
        top_k = data.get("top_k", 5)

        if not user_id:
            return {"error": "Missing user_id"}

        try:
            recommendations = self.ml_engine.get_recommendations(user_id, item_ids, top_k)
            return {"status": "success", "recommendations": recommendations}
        except Exception as e:
            return {"error": str(e)}

    def _handle_feedback_update(self, data: dict) -> dict:
        user_id = data.get("user_id")
        item_id = data.get("item_id")
        rating = data.get("rating")
        context = data.get("context")

        if not all([user_id, item_id, rating is not None]):
            return {"error": "Missing required fields"}

        try:
            self.ml_engine.update_model_with_feedback(user_id, item_id, rating, context)
            return {"status": "success", "message": "Feedback updated"}
        except Exception as e:
            return {"error": str(e)}

    def _handle_performance_metrics(self, data: dict) -> dict:
        try:
            metrics = self.ml_engine.get_model_performance()
            return {"status": "success", "metrics": metrics}
        except Exception as e:
            return {"error": str(e)}

def main():
    bridge = JavaPythonBridge()

    if len(sys.argv) > 1:
        command = sys.argv[1]
        if command == "test":
            print("Testing Python ML integration...")
            test_data = {
                "command": "predict_rating",
                "user_id": "STUDENT001", 
                "item_id": "VEG004"
            }
            result = bridge.process_java_request(test_data)
            print(f"Test result: {result}")
        else:
            print(f"Unknown command: {command}")
    else:
        try:
            line = input()
            request_data = json.loads(line)
            response = bridge.process_java_request(request_data)
            print(json.dumps(response))
        except Exception as e:
            print(json.dumps({"error": str(e)}))

if __name__ == "__main__":
    main()
