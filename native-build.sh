#!/bin/bash

set -e  # Fail fast em caso de erro

clear

echo "Limpando containers antigos..."
docker builder prune -a -f

echo "Iniciando build da imagem native..."
./mvnw clean -Pnative spring-boot:build-image

