@echo off
REM ==============================================================================
REM Google Cloud Platform Deployment Script for AI Weather System
REM Project ID: ai-weather-system-504712
REM Project Number: 1055693681747
REM ==============================================================================

SET PROJECT_ID=ai-weather-system-504712
SET REGION=us-central1
SET DB_INSTANCE=weatherdb-sql
SET DB_NAME=weatherdb
SET DB_USER=root
SET DB_PASSWORD=WeatherPassword123!

echo =================================================================
echo   Deploying AI Weather System to GCP Project: %PROJECT_ID%
echo =================================================================

echo [1/6] Setting GCP Project and enabling APIs...
call gcloud config set project %PROJECT_ID%
call gcloud services enable run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com

echo [2/6] Provisioning Google Cloud SQL (MySQL 8.0)...
call gcloud sql instances create %DB_INSTANCE% --database-version=MYSQL_8_0 --tier=db-f1-micro --region=%REGION% --root-password=%DB_PASSWORD%
call gcloud sql databases create %DB_NAME% --instance=%DB_INSTANCE%

echo [3/6] Creating Artifact Registry Repository...
call gcloud artifacts repositories create weather-repo --repository-format=docker --location=%REGION% --description="AI Weather System Container Images"

echo [4/6] Building & Deploying Python ML Microservice...
cd ml_service
call gcloud builds submit --tag %REGION%-docker.pkg.dev/%PROJECT_ID%/weather-repo/python-ml:latest
call gcloud run deploy python-ml-service --image=%REGION%-docker.pkg.dev/%PROJECT_ID%/weather-repo/python-ml:latest --platform=managed --region=%REGION% --allow-unauthenticated
cd ..

echo [5/6] Building & Deploying Spring Boot Backend & Dashboard...
cd backend
call gcloud builds submit --tag %REGION%-docker.pkg.dev/%PROJECT_ID%/weather-repo/spring-backend:latest
cd ..

echo =================================================================
echo 🎉 DEPLOYMENT CONFIGURATION COMPLETE!
echo Check your Cloud Run console at: https://console.cloud.google.com/run?project=%PROJECT_ID%
echo =================================================================
