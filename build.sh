#!/bin/bash

# Parar e remover containers existentes
docker-compose -f docker-compose.yml down --remove-orphans
docker-compose -f docker-compose-app.yml down --remove-orphans
docker container prune -f

# Build da aplicação
./mvnw clean package -DskipTests

# Build da imagem Docker
docker buildx build -f Dockerfile-local -t ccs1201/rinha-backend-2025:latest .

# Subir Payment Processors
docker-compose -f docker-compose.yml up -d

# Aguardar Payment Processors
#sleep 3

# Subir aplicação
docker-compose -f docker-compose-app.yml up -d

#echo "Aplicação rodando na porta 9999"