# Rinha Backend 2025
#### [CCS1201 - Cleber Souza](https://www.linkedin.com/in/ccs1201/)
## REPO -> [Rinha-Backend-2025-CCS1201](https://github.com/ccs1201/rinha-backend-2025)

## Stack Tecnológica
* **Java 24** - JVM com otimizações mais recentes
* **Spring Boot 3.5.3** - Framework web reativo
* **Undertow** - Web server de alta performance
* **Redis 7** - Storage principal com Sorted Sets
* **Nginx** - Load balancer
* **Docker** - Containerização e orquestração

## Arquitetura

### **Componentes**
```
k6 Tests → Nginx → [App1, App2] → Redis
                ↓
        Load Balancing
```

### **Endpoints**
- `POST /payments` - Processamento assíncrono de pagamentos
- `GET /payments-summary` - Agregação de dados por período
- `POST /purge-payments` - Limpeza do storage

### **Otimizações Implementadas**
- **Async Processing**: POSTs não bloqueantes
- **Redis Optimization**: Contadores para ranges amplos
- **Memory Tuning**: Heap 70MB + Metaspace 70MB
- **Connection Pooling**: Lettuce com conexões compartilhadas

### **Performance**
- **Throughput**: ~1200 requests/segundo
- **Latência**: p99 < 100ms
- **Recursos**: 1.5 CPU + 350MB RAM total
- **Concorrência**: Até 1000 VUs simultâneos

## Como Executar

```bash
# Build e start
docker-compose up --build

# Executar testes
cd rinha-test
k6 run rinha.js
```

## [Repositório Oficial do Desafio por zanfranceschi](https://github.com/zanfranceschi/rinha-de-backend-2025)

---

# Rinha Backend 2025 - English Version

## Technology Stack
* **Java 24** - JVM with latest optimizations
* **Spring Boot 3.5.3** - Reactive web framework
* **Undertow** - High-performance web server
* **Redis 7** - Primary storage with Sorted Sets
* **Nginx** - Load balancer
* **Docker** - Containerization and orchestration

## Architecture

### **Components**
```
k6 Tests → Nginx → [App1, App2] → Redis
                ↓
        Load Balancing
```

### **Endpoints**
- `POST /payments` - Asynchronous payment processing
- `GET /payments-summary` - Data aggregation by period
- `POST /purge-payments` - Storage cleanup

### **Implemented Optimizations**
- **Async Processing**: Non-blocking POSTs
- **Redis Optimization**: Counters for wide ranges
- **Memory Tuning**: 70MB Heap + 70MB Metaspace
- **Connection Pooling**: Lettuce with shared connections

### **Performance**
- **Throughput**: ~1200 requests/second
- **Latency**: p99 < 100ms
- **Resources**: 1.5 CPU + 350MB RAM total
- **Concurrency**: Up to 1000 simultaneous VUs

## How to Run

```bash
# Build and start
docker-compose up --build

# Run tests
cd rinha-test
k6 run rinha.js
```

## [Official Challenge Repository by zanfranceschi](https://github.com/zanfranceschi/rinha-de-backend-2025)