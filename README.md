# Shopping Microservices POC (Spring Boot + Kafka)

Minimal proof-of-concept shopping backend with:
- `api-gateway`
- `auth-service`
- `order-service`
- `payment-service`
- `inventory-service`
- Kafka + Zookeeper + Kafka UI

## Architecture (minimal)

- Client calls `api-gateway` on port `8080`.
- Gateway routes to downstream services.
- `auth-service` provides register/login and returns a simple bearer token.
- `order-service` creates orders and publishes `order.created` Kafka event.
- `payment-service` consumes `order.created`, simulates payment, publishes:
  - `payment.succeeded`
  - `payment.failed`
- `order-service` consumes payment events and updates order status.
- `inventory-service` exposes reserve endpoint and also consumes `order.created` to reserve stock.

## Ports

- `api-gateway`: `8080`
- `auth-service`: `8081`
- `order-service`: `8082`
- `payment-service`: `8083`
- `inventory-service`: `8084`
- Kafka UI: `8085`
- Kafka broker: `9092`

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker Desktop

## Start infrastructure

From project root:

```powershell
docker compose up -d
```

Open Kafka UI:
- http://localhost:8085

Or use helper script:

```powershell
.\start-infra.ps1
```

## Start services (5 terminals)

```powershell
cd .\auth-service; .\mvnw.cmd spring-boot:run
cd .\order-service; .\mvnw.cmd spring-boot:run
cd .\payment-service; .\mvnw.cmd spring-boot:run
cd .\inventory-service; .\mvnw.cmd spring-boot:run
cd .\api-gateway; .\mvnw.cmd spring-boot:run
```

Or start all in separate terminals:

```powershell
.\start-all.ps1
```

Recommended startup order:

```powershell
.\start-infra.ps1
.\start-all.ps1
```

Stop all service JVMs:

```powershell
.\stop-all.ps1
```

## Quick smoke test through gateway

1) Register

```powershell
curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" -d '{"email":"user@test.com","password":"pass123"}'
```

2) Login

```powershell
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"user@test.com","password":"pass123"}'
```

Copy `token` from response.

3) Create order

```powershell
curl -X POST http://localhost:8080/orders -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"userId":"u1","productId":"product-1","quantity":2,"amount":200}'
```

4) Check order status

```powershell
curl http://localhost:8080/orders/<order-id> -H "Authorization: Bearer <token>"
```

Payment status rules in this POC:
- `amount <= 1000` => success => order becomes `CONFIRMED`
- `amount > 1000` => failed => order becomes `FAILED`

## Notes

- Persistence is in-memory for speed of setup.
- Token validation at gateway is header-based (`Bearer ...`) placeholder only.
- This is intentionally minimal and suitable for growing into DB-backed + proper JWT + saga orchestration.
