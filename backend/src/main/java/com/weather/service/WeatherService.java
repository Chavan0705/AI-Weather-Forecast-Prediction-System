package com.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.model.WeatherData;
import com.weather.repository.WeatherDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WeatherService {

    private final WeatherDataRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${weather.api.key:YOUR_API_KEY}")
    private String apiKey;

    @Value("${weather.api.city:Pune}")
    private String defaultCity;

    public WeatherService(WeatherDataRepository repository) {
        this.repository = repository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initData() {
        if (repository.count() == 0) {
            seedHistoricalData(defaultCity);
        } else {
            fetchAndSaveWeather(defaultCity);
        }
    }

    public WeatherData fetchAndSaveWeather(String cityName) {
        String city = (cityName == null || cityName.isBlank()) ? defaultCity : cityName;
        WeatherData data = null;

        if (!"YOUR_API_KEY".equalsIgnoreCase(apiKey) && !apiKey.isBlank()) {
            data = fetchFromOpenWeatherMap(city);
        }

        if (data == null) {
            data = fetchFromOpenMeteo(city);
        }

        if (data != null) {
            data.setTimestamp(LocalDateTime.now());
            return repository.save(data);
        } else {
            data = new WeatherData(null, city, 28.5, 62.0, 1012.0, 9.5, "Partly Cloudy", LocalDateTime.now());
            return repository.save(data);
        }
    }

    private WeatherData fetchFromOpenWeatherMap(String city) {
        try {
            String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, apiKey);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            WeatherData data = new WeatherData();
            data.setCity(city);
            data.setTemperature(root.path("main").path("temp").asDouble());
            data.setHumidity(root.path("main").path("humidity").asDouble());
            data.setPressure(root.path("main").path("pressure").asDouble());
            data.setWindSpeed(root.path("wind").path("speed").asDouble());
            data.setWeather(root.path("weather").get(0).path("description").asText());
            return data;
        } catch (Exception e) {
            System.err.println("OpenWeatherMap fetch failed: " + e.getMessage() + ". Falling back to Open-Meteo API.");
            return null;
        }
    }

    private double[] getCoordinates(String city) {
        double lat = 18.5204;
        double lon = 73.8567;
        try {
            String geoUrl = String.format("https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json", city);
            String geoResponse = restTemplate.getForObject(geoUrl, String.class);
            JsonNode geoRoot = objectMapper.readTree(geoResponse);

            if (geoRoot.has("results") && geoRoot.path("results").isArray() && geoRoot.path("results").size() > 0) {
                JsonNode firstResult = geoRoot.path("results").get(0);
                lat = firstResult.path("latitude").asDouble();
                lon = firstResult.path("longitude").asDouble();
            }
        } catch (Exception ignored) {}
        return new double[]{lat, lon};
    }

    private WeatherData fetchFromOpenMeteo(String city) {
        try {
            double[] coords = getCoordinates(city);
            String weatherUrl = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code",
                    coords[0], coords[1]);
            String weatherResponse = restTemplate.getForObject(weatherUrl, String.class);
            JsonNode weatherRoot = objectMapper.readTree(weatherResponse);
            JsonNode current = weatherRoot.path("current");

            double temp = current.path("temperature_2m").asDouble();
            double humidity = current.path("relative_humidity_2m").asDouble();
            double pressure = current.path("surface_pressure").asDouble();
            double windSpeed = current.path("wind_speed_10m").asDouble();
            int weatherCode = current.path("weather_code").asInt();

            String weatherDesc = parseWeatherCode(weatherCode);

            WeatherData data = new WeatherData();
            data.setCity(city);
            data.setTemperature(temp);
            data.setHumidity(humidity);
            data.setPressure(pressure);
            data.setWindSpeed(windSpeed);
            data.setWeather(weatherDesc);
            return data;
        } catch (Exception e) {
            System.err.println("Open-Meteo API fetch failed: " + e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getForecast(String city) {
        List<Map<String, Object>> forecastList = new ArrayList<>();
        try {
            double[] coords = getCoordinates(city);
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=7&timezone=auto",
                    coords[0], coords[1]);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Parse Daily Forecast (7 Days)
            JsonNode daily = root.path("daily");
            JsonNode dailyTimes = daily.path("time");
            JsonNode dailyMax = daily.path("temperature_2m_max");
            JsonNode dailyMin = daily.path("temperature_2m_min");
            JsonNode dailyCodes = daily.path("weather_code");

            // Parse Hourly Forecast (Full 7 days hourly: 168 hours)
            JsonNode hourly = root.path("hourly");
            JsonNode hourlyTimes = hourly.path("time");
            JsonNode hourlyTemps = hourly.path("temperature_2m");
            JsonNode hourlyHumidities = hourly.path("relative_humidity_2m");
            JsonNode hourlyPressures = hourly.path("surface_pressure");
            JsonNode hourlyWinds = hourly.path("wind_speed_10m");
            JsonNode hourlyCodes = hourly.path("weather_code");

            for (int d = 0; d < dailyTimes.size(); d++) {
                Map<String, Object> dayMap = new HashMap<>();
                String dateStr = dailyTimes.get(d).asText();
                dayMap.put("date", dateStr);
                dayMap.put("tempMax", dailyMax.get(d).asDouble());
                dayMap.put("tempMin", dailyMin.get(d).asDouble());
                dayMap.put("weatherCode", dailyCodes.get(d).asInt());
                dayMap.put("weather", parseWeatherCode(dailyCodes.get(d).asInt()));

                List<Map<String, Object>> dayHourlyList = new ArrayList<>();
                // 24 hours per day
                for (int h = d * 24; h < (d + 1) * 24 && h < hourlyTimes.size(); h++) {
                    Map<String, Object> hourMap = new HashMap<>();
                    hourMap.put("time", hourlyTimes.get(h).asText());
                    hourMap.put("temperature", hourlyTemps.get(h).asDouble());
                    hourMap.put("humidity", hourlyHumidities.get(h).asDouble());
                    hourMap.put("pressure", hourlyPressures.get(h).asDouble());
                    hourMap.put("windSpeed", hourlyWinds.get(h).asDouble());
                    hourMap.put("weatherCode", hourlyCodes.get(h).asInt());
                    hourMap.put("weather", parseWeatherCode(hourlyCodes.get(h).asInt()));
                    dayHourlyList.add(hourMap);
                }
                dayMap.put("hourly", dayHourlyList);
                forecastList.add(dayMap);
            }
        } catch (Exception e) {
            System.err.println("Forecast fetch error: " + e.getMessage());
        }
        return forecastList;
    }

    private String parseWeatherCode(int code) {
        if (code == 0) return "Clear Sky";
        if (code >= 1 && code <= 3) return "Partly Cloudy";
        if (code >= 45 && code <= 48) return "Foggy";
        if (code >= 51 && code <= 67) return "Rain Showers";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Heavy Rain";
        if (code >= 95) return "Thunderstorm";
        return "Overcast";
    }

    private void seedHistoricalData(String city) {
        List<WeatherData> seedList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        double baseTemp = 28.0;
        double baseHumidity = 65.0;
        double basePressure = 1012.0;
        double baseWind = 8.0;

        Random rand = new Random(42);

        for (int i = 48; i >= 0; i--) {
            double tempVar = Math.sin((48 - i) * 0.26) * 4.0 + (rand.nextDouble() * 1.5 - 0.75);
            double temp = Math.round((baseTemp + tempVar) * 10.0) / 10.0;
            double hum = Math.round((baseHumidity - tempVar * 2 + (rand.nextDouble() * 4 - 2)) * 10.0) / 10.0;
            double pres = Math.round((basePressure + (rand.nextDouble() * 4 - 2)) * 10.0) / 10.0;
            double wind = Math.round((baseWind + (rand.nextDouble() * 3 - 1.5)) * 10.0) / 10.0;

            String condition = (hum > 75) ? "Rain Showers" : (temp > 30 ? "Clear Sky" : "Partly Cloudy");

            seedList.add(new WeatherData(null, city, temp, hum, pres, wind, condition, now.minusHours(i)));
        }

        repository.saveAll(seedList);
    }

    public Optional<WeatherData> getLatestWeather(String city) {
        if (city != null && !city.isBlank()) {
            return repository.findTopByCityIgnoreCaseOrderByTimestampDesc(city);
        }
        return repository.findTopByOrderByTimestampDesc();
    }

    public List<WeatherData> getAllHistory() {
        return repository.findAllByOrderByTimestampDesc();
    }

    public List<WeatherData> getRecentHistory() {
        return repository.findTop50ByOrderByTimestampDesc();
    }
}
