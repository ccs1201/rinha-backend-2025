@echo off

REM Setar JAVA_HOME para Java 24
set JAVA_HOME=C:\Users\cleber.constante\.jdks\temurin-24.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

REM taskkill /f /im podman.exe 2>nul & taskkill /f /im docker.exe 2>nul & taskkill /f /im dockerd.exe 2>nul
REM wsl --shutdown
REM podman machine start


podman compose -f docker-compose-payment-processor.yml down --remove-orphans
podman compose -f docker-compose.yml down --remove-orphans

REM Build da aplicação
call mvnw.cmd clean package -DskipTests

REM Build da imagem Podman
podman build -f Dockerfile-local -t ccs1201/rinha-backend-2025:latest .

REM Subir Payment Processors
podman compose -f docker-compose-payment-processor.yml up -d --remove-orphans

REM Subir aplicação
podman compose -f docker-compose.yml up -d

echo Aplicacao rodando na porta 9999