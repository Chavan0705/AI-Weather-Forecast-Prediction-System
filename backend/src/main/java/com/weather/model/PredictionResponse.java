package com.weather.model;

public class PredictionResponse {
    private Double predictedTemperature;
    private String predictedCondition;
    private Double delta;
    private String status;

    public PredictionResponse() {
    }

    public PredictionResponse(Double predictedTemperature, String predictedCondition, Double delta, String status) {
        this.predictedTemperature = predictedTemperature;
        this.predictedCondition = predictedCondition;
        this.delta = delta;
        this.status = status;
    }

    public Double getPredictedTemperature() {
        return predictedTemperature;
    }

    public void setPredictedTemperature(Double predictedTemperature) {
        this.predictedTemperature = predictedTemperature;
    }

    public String getPredictedCondition() {
        return predictedCondition;
    }

    public void setPredictedCondition(String predictedCondition) {
        this.predictedCondition = predictedCondition;
    }

    public Double getDelta() {
        return delta;
    }

    public void setDelta(Double delta) {
        this.delta = delta;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
