package com.weather.scheduler;

import com.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeatherScheduler {

    private final WeatherService weatherService;

    @Value("${weather.api.city:Pune}")
    private String defaultCity;

    public WeatherScheduler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    // Every hour (3600000 ms) fetch weather and save to MySQL
    @Scheduled(fixedRate = 3600000)
    public void fetchWeather() {
        System.out.println("[Spring Scheduler] Hourly task triggered: Fetching live weather data for " + defaultCity);
        weatherService.fetchAndSaveWeather(defaultCity);
    }
}
