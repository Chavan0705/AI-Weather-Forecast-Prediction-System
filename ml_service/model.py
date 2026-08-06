import os
import joblib
import pandas as pd
import numpy as np

try:
    import pymysql
except ImportError:
    pymysql = None

try:
    import requests
except ImportError:
    requests = None

from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier

MODEL_FILE = os.path.join(os.path.dirname(__file__), "weather_model.pkl")
CONDITION_MODEL_FILE = os.path.join(os.path.dirname(__file__), "condition_model.pkl")

def fetch_data_from_mysql():
    """Fetch historical weather data directly from MySQL database."""
    if not pymysql:
        return None
    try:
        conn = pymysql.connect(
            host="localhost",
            user="root",
            password="root123",
            database="weatherdb",
            cursorclass=pymysql.cursors.DictCursor
        )
        query = """
        SELECT temperature, humidity, pressure, wind_speed, weather, timestamp
        FROM weather_data
        ORDER BY timestamp ASC
        """
        data = pd.read_sql(query, conn)
        conn.close()
        return data
    except Exception as e:
        print(f"[Python ML] Could not connect directly to MySQL ({e}). Will try Spring Boot API fallback.")
        return None

def fetch_data_from_java_api():
    """Fetch historical weather data from Spring Boot REST API endpoint."""
    if not requests:
        return None
    try:
        response = requests.get("http://localhost:8080/api/weather/history", timeout=5)
        if response.status_code == 200:
            data = pd.DataFrame(response.json())
            if not data.empty and "windSpeed" in data.columns:
                data = data.rename(columns={"windSpeed": "wind_speed"})
            return data
    except Exception as e:
        print(f"[Python ML] Spring Boot API fetch fallback unavailable ({e}).")
    return None

def generate_synthetic_history():
    """Fallback generator to ensure robust model training even without external DB data."""
    np.random.seed(42)
    rows = 100
    base_temp = 28.0
    
    temps, humidities, pressures, winds, conditions = [], [], [], [], []
    
    for i in range(rows):
        t_var = np.sin(i * 0.2) * 5.0 + np.random.normal(0, 1.0)
        t = round(base_temp + t_var, 1)
        h = round(max(30, min(95, 65 - t_var * 2 + np.random.normal(0, 3))), 1)
        p = round(1012.0 + np.random.normal(0, 2), 1)
        w = round(max(1, 8.0 + np.random.normal(0, 2)), 1)
        
        cond = "Rain Showers" if h > 75 else ("Clear Sky" if t > 30 else "Partly Cloudy")
        
        temps.append(t)
        humidities.append(h)
        pressures.append(p)
        winds.append(w)
        conditions.append(cond)

    return pd.DataFrame({
        "temperature": temps,
        "humidity": humidities,
        "pressure": pressures,
        "wind_speed": winds,
        "weather": conditions
    })

def train_and_save_model():
    """Train RandomForest models for temperature prediction & condition classification."""
    print("[Python ML] Initiating model training workflow...")
    
    data = fetch_data_from_mysql()
    if data is None or len(data) < 5:
        data = fetch_data_from_java_api()
        
    if data is None or len(data) < 5:
        print("[Python ML] Using synthetic dataset for initialization...")
        data = generate_synthetic_history()

    # Feature matrix X
    X = data[["temperature", "humidity", "pressure", "wind_speed"]]
    
    # Target y is next step temperature
    y_temp = data["temperature"].shift(-1)
    
    # Clean last row where shift produces NaN
    X_clean = X[:-1]
    y_temp_clean = y_temp[:-1]
    
    # Train Temperature Regression Model
    model = RandomForestRegressor(n_estimators=100, random_state=42)
    model.fit(X_clean, y_temp_clean)
    joblib.dump(model, MODEL_FILE)
    
    # Train Weather Condition Classification Model
    if "weather" in data.columns:
        y_cond = data["weather"].shift(-1)[:-1]
        cond_model = RandomForestClassifier(n_estimators=50, random_state=42)
        cond_model.fit(X_clean, y_cond)
        joblib.dump(cond_model, CONDITION_MODEL_FILE)

    print(f"[Python ML] Model trained successfully on {len(X_clean)} samples and saved to {MODEL_FILE}")
    return {
        "status": "success",
        "sampleCount": len(X_clean),
        "modelPath": MODEL_FILE
    }

def load_or_train_model():
    if not os.path.exists(MODEL_FILE):
        train_and_save_model()
    return joblib.load(MODEL_FILE)

def predict_weather(temp, humidity, pressure, wind_speed):
    """Predict next hour temperature using the serialized RandomForest model."""
    model = load_or_train_model()
    features = np.array([[temp, humidity, pressure, wind_speed]])
    predicted_temp = float(model.predict(features)[0])
    
    predicted_cond = "Partly Cloudy"
    if os.path.exists(CONDITION_MODEL_FILE):
        try:
            cond_model = joblib.load(CONDITION_MODEL_FILE)
            predicted_cond = str(cond_model.predict(features)[0])
        except Exception:
            pass
            
    return {
        "predictedTemperature": round(predicted_temp, 1),
        "predictedCondition": predicted_cond
    }

if __name__ == "__main__":
    train_and_save_model()
