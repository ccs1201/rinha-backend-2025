#!/usr/bin/env bash
set -euo pipefail

# Exporta as variáveis de ambiente
export CLIENT_PROCESSOR_TIMEOUT=200
export DATASOURCE_MAXIMUM_POOL_SIZE=15
export DATASOURCE_MINIMUM_IDLE=15
export DATASOURCE_PASSWORD="rinha"
export DATASOURCE_TIMEOUT=5000
export DATASOURCE_URL="jdbc:postgresql://localhost:5432/rinha"
export DATASOURCE_USERNAME="rinha"
export PAYMENT_PROCESSOR_DEFAULT_URL="http://localhost:8001"
export PAYMENT_PROCESSOR_FALLBACK_URL="http://localhost:8002"
export SERVER_PORT=9999
export THREAD_POOL_SIZE=15
export THREAD_QUEUE_SIZE=100
export WARMUP=true
export WARMUP_ROUNDS=5000

# Opções da JVM (adicione mais conforme a necessidade)
JAVA_OPTS="\
-Xmx90m -Xms90m -XX:MaxMetaspaceSize=50m -XX:+UseSerialGC \
-Dclient-processor-timeout=200 \
-Ddatasource-class-name=org.postgresql.Driver \
-Ddatasource-maximum-pool-size=15 \
-Ddatasource-minimum-idle=15 \
-Ddatasource-password=rinha \
-Ddatasource-timeout=5000 \
-Ddatasource-url=jdbc:postgresql://localhost:5432/rinha \
-Ddatasource-username=rinha \
-Dpayment-processor-default-url=http://localhost:8001 \
-Dpayment-processor-fallback-url=http://localhost:8002 \
-Dserver-port=9999 \
-Dthread-pool-size=15 \
-Dthread-queue-size=100 \
-Dwarmup=true \
-Dwarmup_rounds=5000"

cd ..
ls -a
# Caminho para o JAR
APP_JAR="./target/rinha-backend-2025-0.0.1-SNAPSHOT.jar"  # substitua pelo nome correto do seu JAR

echo "## Iniciando aplicação com as seguintes configurações:"
echo "   Variáveis de ambiente:"
#env | grep -E "CLIENT_PROCESSOR_TIMEOUT|DATASOURCE_|PAYMENT_PROCESSOR_|SERVER_PORT|THREAD_"
echo "   Opções JVM: $JAVA_OPTS"
echo "   Executando: java \$JAVA_OPTS -jar \$APP_JAR"

# Executa a aplicação
exec java $JAVA_OPTS -jar "$APP_JAR"
