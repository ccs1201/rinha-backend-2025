# Rinha Backend 2025
#### [CCS1201 - Cleber Souza](https://www.linkedin.com/in/ccs1201/)
## REPO -> [Rinha-Backend-2025-CCS1201](https://github.com/ccs1201/rinha-backend-2025)

## Stack Tecnológica
* **Java 24** - JVM com otimizações mais recentes
* **Undertow** - Web server de alta performance
* **PostgreSQL 15** - Storage principal com índices otimizados
* **HikariCP** - Pool de conexões otimizado
* **Nginx** - Load balancer
* **Docker** - Containerização e orquestração

## Arquitetura

### **Componentes**
```
k6 Tests → Nginx → [App1, App2] → PostgreSQL
                ↓
        Load Balancing
```

### **Endpoints**
- `POST /payments` - Processamento assíncrono de pagamentos
- `GET /payments-summary` - Agregação de dados por período
- `POST /purge-payments` - Limpeza do storage

### **Otimizações Implementadas**
- **Async Processing**: POSTs não bloqueantes com CompletableFuture
- **JDBC Puro**: Sem overhead de ORM, apenas HikariCP
- **Índices Otimizados**: Clustered e covering indexes no PostgreSQL
- **JIT Warmup**: Aquecimento automático da JVM simulando carga real
- **Memory Tuning**: Heap 90MB + Metaspace 50MB otimizado
- **CDI Container**: Weld para gerenciamento de beans e lifecycle

### **Performance**
- **Throughput**: ~1500 requests/segundo
- **Latência**: p99 < 1ms (após warmup)
- **Recursos**: 1.5 CPU + 300MB RAM total
- **Concorrência**: Até 1000 VUs simultâneos

## Como Executar

```bash
# Build e start
docker-compose up --build

# ⚠️ IMPORTANTE: Aguardar warmup completar
# Logs mostrarão: "Application warming up wait...."
# Até aparecer: "Application warmup completed"

# Executar testes (após warmup)
cd rinha-test
k6 run rinha.js
```

## [Repositório Oficial do Desafio por zanfranceschi](https://github.com/zanfranceschi/rinha-de-backend-2025)

---

# Rinha Backend 2025 - English Version

## Technology Stack
* **Java 24** - JVM with latest optimizations
* **Undertow** - High-performance web server
* **PostgreSQL 15** - Primary storage with optimized indexes
* **HikariCP** - Optimized connection pool
* **Nginx** - Load balancer
* **Docker** - Containerization and orchestration

## Architecture

### **Components**
```
k6 Tests → Nginx → [App1, App2] → PostgreSQL
                ↓
        Load Balancing
```

### **Endpoints**
- `POST /payments` - Asynchronous payment processing
- `GET /payments-summary` - Data aggregation by period
- `POST /purge-payments` - Storage cleanup

### **Implemented Optimizations**
- **Async Processing**: Non-blocking POSTs with CompletableFuture
- **Pure JDBC**: No ORM overhead, only HikariCP
- **Optimized Indexes**: Clustered and covering indexes on PostgreSQL
- **JIT Warmup**: Automatic JVM warm-up simulating real load
- **Memory Tuning**: 90MB Heap + 50MB Metaspace optimized
- **CDI Container**: Weld for bean management and lifecycle

### **Performance**
- **Throughput**: ~1500 requests/second
- **Latency**: p99 < 1ms (after warmup)
- **Resources**: 1.5 CPU + 300MB RAM total
- **Concurrency**: Up to 1000 simultaneous VUs

## How to Run

```bash
# Build and start
docker-compose up --build

# ⚠️ IMPORTANT: Wait for warmup to complete
# Logs will show: "Application warming up wait...."
# Until: "Application warmup completed"

# Run tests (after warmup)
cd rinha-test
k6 run rinha.js
```

## [Official Challenge Repository by zanfranceschi](https://github.com/zanfranceschi/rinha-de-backend-2025)
