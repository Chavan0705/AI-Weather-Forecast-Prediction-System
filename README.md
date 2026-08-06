# 🌤️ AI Weather Forecast & Prediction System

A production-style full-stack weather monitoring and prediction application. The system continuously fetches live meteorological metrics using **Java Spring Boot**, persists historical records into **MySQL**, trains a **Python Scikit-Learn RandomForest** machine learning model to predict next-hour temperatures, and presents live metrics, hourly timelines, and 7-day extended forecasts on an interactive **Tailwind CSS Glassmorphic Dashboard**.

---

## 🏗️ System Architecture

```text
               Open-Meteo / OpenWeatherMap API
                              │
                              ▼
                   Spring Boot (Java Backend)
                              │
               Fetch Live Weather Every Hour
                              │
                              ▼
                  MySQL Database (weatherdb)
                              │
              REST API (/weather, /history)
                              │
            ┌─────────────────┴─────────────────┐
            ▼                                   ▼
      Python ML Model                    Tailwind CSS UI
 (Train & Predict Microservice)         Interactive Dashboard
            │                                   │
            └────────────► Prediction API ◄─────┘
```

---

## 🚀 Key Features

- **Live Meteorological Ingestion**: Automatically fetches real-time temperature, humidity, pressure, wind velocity, and condition descriptions from Open-Meteo API / OpenWeatherMap API.
- **MySQL Database Persistence**: Spring Data JPA ORM persists all weather readings into the `weather_data` table.
- **Automated Spring Scheduler**: `@Scheduled(fixedRate = 3600000)` job runs hourly to fetch and save fresh weather data.
- **Python Machine Learning Microservice**:
  - Uses `RandomForestRegressor` for next-hour temperature predictions.
  - Uses `RandomForestClassifier` for weather condition forecasting.
  - Exposed via Flask REST API endpoints (`/predict`, `/train`, `/health`).
  - Serializes models into `weather_model.pkl`.
- **Interactive Glassmorphism Dashboard**:
  - Built with **Tailwind CSS** using a custom Light Sky Blue (`#8ee0fb`) and Ocean Cerulean (`#71b2e6`) palette.
  - **Quick City Selector**: 1-click weather switching for Pune, Mumbai, London, New York, Tokyo, Dubai.
  - **24-Hour Hourly Forecast Panel**: Horizontal scrollable timeline showing hourly temperatures and AI predictions (`🤖 AI Temp`).
  - **7-Day Extended Forecast Grid**: Interactive daily cards allowing users to inspect hourly breakdowns for any day.
  - **Chart.js Analytics**: Real-time trajectory plotting actual historical temperatures against AI predictions.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Frontend** | HTML5, Tailwind CSS v3, JavaScript (ES6+), Chart.js, FontAwesome |
| **Backend** | Java 17/21, Spring Boot 3.2, Spring Data JPA, Spring Web |
| **Database** | MySQL 8.0 / H2 Database |
| **Machine Learning** | Python 3.12, Scikit-Learn, Pandas, NumPy, Joblib |
| **ML Microservice** | Flask, Flask-CORS, PyMySQL, Requests |
| **External APIs** | Open-Meteo API (Free, High-Accuracy) / OpenWeatherMap API |

---

## 🗄️ MySQL Database Schema

```sql
CREATE DATABASE IF NOT EXISTS weatherdb;
USE weatherdb;

CREATE TABLE IF NOT EXISTS weather_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    city VARCHAR(100) NOT NULL,
    temperature DOUBLE NOT NULL,
    humidity DOUBLE NOT NULL,
    pressure DOUBLE NOT NULL,
    wind_speed DOUBLE NOT NULL,
    weather VARCHAR(100) NOT NULL,
    timestamp DATETIME NOT NULL
);
```

---

## 📡 REST API Endpoints

### Spring Boot Backend (`http://localhost:8080`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/weather/latest?city={name}` | Fetch latest recorded weather for specified city |
| `GET` | `/api/weather/history` | Retrieve full historical weather dataset from MySQL |
| `GET` | `/api/weather/forecast?city={name}` | Fetch 7-day daily and 24-hour hourly forecast data |
| `POST` | `/api/weather/fetch?city={name}` | Trigger immediate Open-Meteo API fetch & save to MySQL |
| `GET` | `/api/weather/predict?city={name}` | Proxy prediction request to Python ML service |
| `POST` | `/api/weather/retrain` | Trigger Python ML model retraining |

### Python ML Microservice (`http://localhost:5000`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/health` | Service health status and model load state |
| `POST` | `/predict` | Accept `{temperature, humidity, pressure, windSpeed}`, return predicted temperature |
| `POST` | `/train` | Query MySQL/API history, train RandomForest model, save `weather_model.pkl` |

---

## 💻 How to Run Locally

### Prerequisites
- Java JDK 17 or higher
- Maven 3.8+
- Python 3.10+
- MySQL Server (optional, fallback to H2/in-memory available)

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/Chavan0705/AI-Weather-Forecast-Prediction-System.git
cd AI-Weather-Forecast-Prediction-System
```

### Step 2: Initialize Database (MySQL)
```bash
mysql -u root -p < schema.sql
```

### Step 3: Install & Start Python ML Microservice
```bash
cd ml_service
pip install -r requirements.txt
python app.py
```
*The Python service will run on `http://localhost:5000`.*

### Step 4: Start Spring Boot Backend
In a new terminal tab:
```bash
cd backend
mvn spring-boot:run
```
*The Java backend will run on `http://localhost:8080`.*

### Step 5: Open Dashboard
Open your browser and navigate to:
```text
http://localhost:8080
```

---

## 📂 Directory Structure

```text
AI-Weather-Forecast-Prediction-System/
├── backend/                             # Java Spring Boot Application
│   ├── pom.xml                          # Maven dependencies
│   └── src/main/
│       ├── java/com/weather/
│       │   ├── WeatherApplication.java  # Main App entry point
│       │   ├── controller/
│       │   │   └── WeatherController.java # REST Endpoints
│       │   ├── model/
│       │   │   ├── WeatherData.java     # JPA Entity
│       │   │   ├── PredictionRequest.java
│       │   │   └── PredictionResponse.java
│       │   ├── repository/
│       │   │   └── WeatherDataRepository.java
│       │   ├── scheduler/
│       │   │   └── WeatherScheduler.java  # Hourly scheduled fetch job
│       │   └── service/
│       │       ├── WeatherService.java    # Weather API fetch & persistence
│       │       └── PredictionService.java # Python ML Service Client
│       └── resources/
│           ├── application.properties   # App configuration
│           └── static/                  # Dashboard static files
│               ├── index.html           # Tailwind CSS Dashboard HTML
│               ├── style.css            # Custom CSS animations & glows
│               └── app.js               # Frontend JavaScript controller
├── ml_service/                          # Python ML Microservice
│   ├── app.py                           # Flask REST API server
│   ├── model.py                         # Scikit-Learn RandomForest trainer
│   ├── requirements.txt                 # Python dependencies
│   └── weather_model.pkl                # Trained model artifact
├── schema.sql                           # MySQL Database DDL script
└── README.md                            # Documentation
```

---

## 👤 Author

**Chavan0705**
- GitHub: [@Chavan0705](https://github.com/Chavan0705)
