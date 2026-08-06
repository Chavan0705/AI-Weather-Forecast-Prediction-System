#!/bin/bash
# ==============================================================================
# Google Cloud Platform Deployment Script for AI Weather System
# Project ID: ai-weather-system-504712
# Project Number: 1055693681747
# ==============================================================================

PROJECT_ID="ai-weather-system-504712"
REGION="us-central1"
DB_INSTANCE="weatherdb-sql"
DB_NAME="weatherdb"
DB_USER="root"
DB_PASSWORD="WeatherPassword123!"

echo "================================================================="
echo "  Deploying AI Weather System to GCP Project: $PROJECT_ID"
echo "================================================================="

# Step 1: Set Active Project & Enable Cloud APIs
echo "[1/6] Setting GCP Project and enabling Cloud Run, Cloud SQL APIs..."
gcloud config set project $PROJECT_ID

gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com

# Step 2: Create Cloud SQL Instance (MySQL 8.0)
echo "[2/6] Provisioning Google Cloud SQL (MySQL 8.0) Instance..."
gcloud sql instances create $DB_INSTANCE \
  --database-version=MYSQL_8_0 \
  --tier=db-f1-micro \
  --region=$REGION \
  --root-password=$DB_PASSWORD || echo "Database instance already exists."

# Create Database schema
gcloud sql databases create $DB_NAME --instance=$DB_INSTANCE || echo "Database $DB_NAME already exists."

# Step 3: Create Artifact Registry Repository
echo "[3/6] Creating Artifact Registry Repository..."
gcloud artifacts repositories create weather-repo \
  --repository-format=docker \
  --location=$REGION \
  --description="AI Weather System Container Images" || echo "Repository weather-repo already exists."

# Step 4: Build & Deploy Python ML Microservice to Cloud Run
echo "[4/6] Building & Deploying Python ML Microservice..."
cd ml_service
gcloud builds submit --tag $REGION-docker.pkg.dev/$PROJECT_ID/weather-repo/python-ml:latest

PYTHON_URL=$(gcloud run deploy python-ml-service \
  --image=$REGION-docker.pkg.dev/$PROJECT_ID/weather-repo/python-ml:latest \
  --platform=managed \
  --region=$REGION \
  --allow-unauthenticated \
  --format='value(status.url)')

echo "Python ML Microservice deployed at: $PYTHON_URL"
cd ..

# Step 5: Build & Deploy Spring Boot Java Backend to Cloud Run
echo "[5/6] Building & Deploying Spring Boot Backend & Dashboard..."
cd backend
gcloud builds submit --tag $REGION-docker.pkg.dev/$PROJECT_ID/weather-repo/spring-backend:latest

DB_IP=$(gcloud sql instances describe $DB_INSTANCE --format='value(ipAddresses[0].ipAddress)')

SPRING_URL=$(gcloud run deploy spring-weather-backend \
  --image=$REGION-docker.pkg.dev/$PROJECT_ID/weather-repo/spring-backend:latest \
  --platform=managed \
  --region=$REGION \
  --set-env-vars="SPRING_DATASOURCE_URL=jdbc:mysql://$DB_IP:3306/$DB_NAME?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true,SPRING_DATASOURCE_USERNAME=$DB_USER,SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD,PYTHON_ML_SERVICE_URL=$PYTHON_URL/predict,PYTHON_ML_SERVICE_TRAIN_URL=$PYTHON_URL/train" \
  --allow-unauthenticated \
  --format='value(status.url)')

cd ..

echo "================================================================="
echo " 🎉 DEPLOYMENT SUCCESSFUL!"
echo " Live Weather Dashboard URL: $SPRING_URL"
echo "================================================================="
