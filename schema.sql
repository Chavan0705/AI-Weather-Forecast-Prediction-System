-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS weatherdb;
USE weatherdb;

-- Create weather_data table as per specification
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
