package com.weather.service;

import com.weather.model.PredictionRequest;
import com.weather.model.PredictionResponse;
import com.weather.model.WeatherData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PredictionService {

    private final RestTemplate restTemplate;

    @Value("${python.ml.service.url:http://localhost:5000/predict}")
    private String predictUrl;

    @Value("${python.ml.service.train-url:http://localhost:5000/train}")
    private String trainUrl;

    public PredictionService() {
        this.restTemplate = new RestTemplate();
    }

    public PredictionResponse getPrediction(WeatherData currentData) {
        if (currentData == null) {
            return new PredictionResponse(25.0, "Partly Cloudy", 0.0, "No Input Data");
        }

        try {
            PredictionRequest request = new PredictionRequest(
                    currentData.getTemperature(),
                    currentData.getHumidity(),
                    currentData.getPressure(),
                    currentData.getWindSpeed()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PredictionRequest> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(predictUrl, entity, Map.class);

            if (response != null && response.containsKey("predictedTemperature")) {
                Double predictedTemp = Double.parseDouble(response.get("predictedTemperature").toString());
                String condition = (String) response.getOrDefault("predictedCondition", currentData.getWeather());
                Double delta = Math.round((predictedTemp - currentData.getTemperature()) * 10.0) / 10.0;

                return new PredictionResponse(
                        Math.round(predictedTemp * 10.0) / 10.0,
                        condition,
                        delta,
                        "AI Random Forest Model Active"
                );
            }
        } catch (Exception e) {
            System.err.println("Python ML Service call failed: " + e.getMessage() + ". Using intelligent heuristic estimator.");
        }

        // Fallback ML Heuristic estimator if Python microservice is starting up
        double currentTemp = currentData.getTemperature();
        double tempDelta = (currentData.getHumidity() > 70 ? -0.8 : 0.6) + (currentData.getPressure() < 1010 ? -0.4 : 0.2);
        double estTemp = Math.round((currentTemp + tempDelta) * 10.0) / 10.0;
        double delta = Math.round((estTemp - currentTemp) * 10.0) / 10.0;

        return new PredictionResponse(
                estTemp,
                currentData.getWeather(),
                delta,
                "Python ML Microservice Connecting..."
        );
    }

    public Map<String, Object> retrainModel() {
        try {
            return restTemplate.postForObject(trainUrl, null, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("status", "error");
            fallback.put("message", "Python ML service unavailable at " + trainUrl);
            return fallback;
        }
    }
}
