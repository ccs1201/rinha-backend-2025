@echo off

REM Setar JAVA_HOME para Java 24
set JAVA_HOME=C:\Users\cleber.constante\.jdks\temurin-24.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

cd..
REM Build da aplicação
call mvnw.cmd clean package -DskipTests
cd build_scripts

REM Build da imagem Podman
docker build -f docker-compose -t ccs1201/rinha-backend-2025:latest .

REM Subir Payment Processors
docker compose -f docker-compose-payment-processor.yml up -d --remove-orphans

REM Subir aplicação
docker compose -f docker-compose.yml up -d

echo Aplicacao rodando na porta 9999