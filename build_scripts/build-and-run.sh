#!/bin/bash

docker-compose -f docker-compose-payment-processor.yml down --remove-orphans
docker-compose -f docker-compose.yml down --remove-orphans
docker container prune -f

# Build da aplicação
#./mvnw clean package -DskipTests

# Build da imagem Docker
docker buildx build -f Dockerfile -t ccs1201/rinha-backend-2025-jdbc:latest --build

# Subir Payment Processors
docker-compose -f docker-compose-payment-processor.yml up -d

# Aguardar Payment Processors
#sleep 10

# Subir aplicação
docker-compose -f docker-compose.yml up -d

echo ">>>> Wait for backend warm up (follow through console) then it will be available on port 9999"