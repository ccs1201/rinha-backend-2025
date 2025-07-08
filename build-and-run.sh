#!/bin/bash

# Build da aplicação
./mvnw clean package -DskipTests

# Build da imagem Docker
docker buildx build --platform linux/amd64 -t ccs1201/rinha-backend-2025:latest .

# Subir Payment Processors
docker-compose -f docker-compose.yml up -d --remove-orphans

# Aguardar Payment Processors
sleep 10

# Subir aplicação
docker-compose -f docker-compose-app.yml up -d --remove-orphans

echo "Aplicação rodando na porta 9999"