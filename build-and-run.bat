@echo off

REM Setar JAVA_HOME para Java 24
set JAVA_HOME=C:\Users\cleber.constante\.jdks\temurin-24.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

REM Build da aplicação
call mvnw.cmd clean package -DskipTests

REM Build da imagem Podman
podman build -f Dockerfile_Local --platform linux/amd64 -t ccs1201/rinha-backend-2025:latest .

REM Subir Payment Processors
podman compose -f docker-compose.yml up -d --remove-orphans

REM Aguardar Payment Processors
timeout /t 2 /nobreak >nul

REM Subir aplicação
podman compose -f docker-compose-app.yml up -d

echo Aplicacao rodando na porta 9999