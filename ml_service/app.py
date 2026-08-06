import os
import sys
from flask import Flask, request, jsonify
from flask_cors import CORS

sys.path.append(os.path.dirname(__file__))
from model import predict_weather, train_and_save_model, MODEL_FILE

app = Flask(__name__)
CORS(app)

@app.route("/health", methods=["GET"])
def health():
    model_status = "loaded" if os.path.exists(MODEL_FILE) else "not_found"
    return jsonify({
        "status": "healthy",
        "service": "Python Weather ML Microservice",
        "modelStatus": model_status
    })

@app.route("/predict", methods=["POST"])
def predict():
    try:
        data = request.get_json(force=True)
        temp = float(data.get("temperature", 25.0))
        humidity = float(data.get("humidity", 60.0))
        pressure = float(data.get("pressure", 1013.0))
        wind = float(data.get("windSpeed", data.get("wind_speed", 8.0)))

        result = predict_weather(temp, humidity, pressure, wind)
        
        return jsonify({
            "predictedTemperature": result["predictedTemperature"],
            "predictedCondition": result["predictedCondition"],
            "status": "success"
        })
    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 400

@app.route("/train", methods=["POST"])
def train():
    try:
        res = train_and_save_model()
        return jsonify(res)
    except Exception as e:
        return jsonify({
            "status": "error",
            "message": str(e)
        }), 500

if __name__ == "__main__":
    print("[Python ML Microservice] Starting server on port 5000...")
    # Train initial model if missing
    if not os.path.exists(MODEL_FILE):
        try:
            train_and_save_model()
        except Exception as e:
            print(f"[Python ML Microservice] Initial training deferred: {e}")
            
    app.run(host="0.0.0.0", port=5000, debug=True)
