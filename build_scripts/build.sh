#!/bin/bash

# Parar e remover containers existentes
docker-compose -f docker-compose-payment-processor.yml down --remove-orphans
docker-compose -f docker-compose.yml down --remove-orphans
docker-clean run

# Build da aplicação
cd ..
./mvnw clean package -DskipTests

# Build da imagem Docker
docker buildx build -f ./build_scripts/Dockerfile-local -t ccs1201/rinha-backend-2025-puro:latest .

# Subir Payment Processors
docker-compose -f ./build_scripts/docker-compose-payment-processor.yml up -d

# Aguardar Payment Processors
sleep 3

# Subir aplicação
docker-compose -f ./build_scripts/docker-compose.yml up -d

#echo "Aplicação rodando na porta 9999"