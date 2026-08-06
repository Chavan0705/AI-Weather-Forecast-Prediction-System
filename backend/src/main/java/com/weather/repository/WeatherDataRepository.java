package com.weather.repository;

import com.weather.model.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    Optional<WeatherData> findTopByCityIgnoreCaseOrderByTimestampDesc(String city);

    Optional<WeatherData> findTopByOrderByTimestampDesc();

    List<WeatherData> findAllByOrderByTimestampDesc();

    List<WeatherData> findTop50ByOrderByTimestampDesc();
}
