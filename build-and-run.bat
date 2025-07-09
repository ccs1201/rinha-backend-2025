@echo off

REM Setar JAVA_HOME para Java 24
set JAVA_HOME=C:\Users\cleber.constante\.jdks\temurin-24.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

taskkill /f /im podman.exe 2>nul & taskkill /f /im docker.exe 2>nul & taskkill /f /im dockerd.exe 2>nul
wsl --shutdown
podman machine start

REM Build da aplicação
call mvnw.cmd clean package -DskipTests

REM Build da imagem Podman
podman build -f PodmanFile -t ccs1201/rinha-backend-2025:latest .

REM Subir Payment Processors
podman compose -f docker-compose.yml up -d --remove-orphans

REM Subir aplicação
podman compose -f docker-compose-app.yml up -d

echo Aplicacao rodando na porta 9999