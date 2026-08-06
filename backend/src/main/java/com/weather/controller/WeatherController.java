package com.weather.controller;

import com.weather.model.PredictionResponse;
import com.weather.model.WeatherData;
import com.weather.service.PredictionService;
import com.weather.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*")
public class WeatherController {

    private final WeatherService weatherService;
    private final PredictionService predictionService;

    public WeatherController(WeatherService weatherService, PredictionService predictionService) {
        this.weatherService = weatherService;
        this.predictionService = predictionService;
    }

    @GetMapping("/latest")
    public ResponseEntity<WeatherData> getLatestWeather(@RequestParam(value = "city", required = false, defaultValue = "Pune") String city) {
        Optional<WeatherData> latest = weatherService.getLatestWeather(city);
        return latest.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(weatherService.fetchAndSaveWeather(city)));
    }

    @GetMapping("/history")
    public ResponseEntity<List<WeatherData>> getHistory() {
        return ResponseEntity.ok(weatherService.getAllHistory());
    }

    @GetMapping("/forecast")
    public ResponseEntity<List<Map<String, Object>>> getForecast(@RequestParam(value = "city", required = false, defaultValue = "Pune") String city) {
        return ResponseEntity.ok(weatherService.getForecast(city));
    }

    @PostMapping("/fetch")
    public ResponseEntity<WeatherData> fetchWeatherNow(@RequestParam(value = "city", required = false, defaultValue = "Pune") String city) {
        WeatherData data = weatherService.fetchAndSaveWeather(city);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/predict")
    public ResponseEntity<PredictionResponse> getPrediction(@RequestParam(value = "city", required = false, defaultValue = "Pune") String city) {
        Optional<WeatherData> latestOpt = weatherService.getLatestWeather(city);
        WeatherData latest = latestOpt.orElseGet(() -> weatherService.fetchAndSaveWeather(city));

        PredictionResponse response = predictionService.getPrediction(latest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/retrain")
    public ResponseEntity<Map<String, Object>> retrainModel() {
        Map<String, Object> result = predictionService.retrainModel();
        return ResponseEntity.ok(result);
    }
}
