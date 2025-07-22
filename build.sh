#!/bin/bash

export JAVA_HOME=/home/csouza/.jdks/graalvm-jdk-24.0.1
export PATH=$JAVA_HOME/bin:$PATH
source ~/.bashrc

# Parar Payment Processors
docker-compose -f docker-compose-payment-processor.yml down

# Subir aplicação
docker-compose -f docker-compose.yml down

# Build da aplicação
./mvnw clean package -Pnative spring-boot:build-image-no-fork

# Build da imagem Docker
#docker buildx build -f Dockerfile -t csouzadocker/rinha-2025-redis:latest .

# Subir Payment Processors
#docker-compose -f docker-compose-payment-processor.yml up -d

# Aguardar Payment Processors
#sleep 3

# Subir aplicação
#docker-compose -f docker-compose.yml up -d

#echo "Aplicação rodando na porta 9999"