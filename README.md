# Shopping Service (Spring Boot 4, Java 25)

Microservice demo for a shopping app:
- `user-service`: customers/users (PostgreSQL)
- `catalog-service`: items, status, price (MongoDB)
- `order-service`: buying/delivery orders (PostgreSQL)
- `gateway-service`: Spring Cloud Gateway entry point

## Prerequisites
- Java 25
- Maven 3.9+
- Docker (optional for local DBs)
- Kubernetes (minikube) for k8s deployment

## Build
```
cd /Users/bjorne/gitrepos/SHOP/shopping_service
mvn -DskipTests package
```

## Local databases (Docker)
```
cd /Users/bjorne/gitrepos/SHOP/shopping_service
docker compose -f deploy/docker/docker-compose.yaml up
```

## Run locally
```
mvn -pl user-service spring-boot:run
mvn -pl catalog-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl gateway-service spring-boot:run
```

Gateway routes:
- `GET/POST /api/users/**` -> `user-service`
- `GET/POST /api/catalog/**` -> `catalog-service`
- `GET/POST /api/orders/**` -> `order-service`

## OpenAPI / Swagger UI
Each service exposes OpenAPI docs and Swagger UI:
- OpenAPI JSON: `http://<service-host>/v3/api-docs`
- Swagger UI: `http://<service-host>/swagger-ui/index.html`

Examples (local):
- Gateway Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- User Service Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- Catalog Service Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- Order Service Swagger UI: `http://localhost:8083/swagger-ui/index.html`

Gateway aggregation config (runtime):
- Default (local) uses `http://localhost:8081/8082/8083`.
- Profiles `stage` and `prod` default to service DNS names (e.g., `http://user-service:8081`).
- Override any URL with env vars:
  - `OPENAPI_USER_URL`
  - `OPENAPI_CATALOG_URL`
  - `OPENAPI_ORDER_URL`

Select profile at runtime (no build-time changes required):
```
SPRING_PROFILES_ACTIVE=stage mvn -pl gateway-service spring-boot:run
```

Available profiles:
- `default` (no profile): local URLs (localhost).
- `stage`: service DNS defaults (e.g., `http://user-service:8081`).
- `prod`: service DNS defaults (e.g., `http://user-service:8081`).

## Lombok and Records
- Lombok is used for entity boilerplate (getters/setters/constructors).
- Java records are used for simple immutable DTOs.
- Requires annotation processing enabled in the IDE.

## Kubernetes (minikube)
1. Build images locally (one per service):
```
# Example for user-service
docker build -f user-service/Dockerfile --build-arg SERVICE=user-service -t shopping/user-service:latest .
```
2. Load images into minikube (if using docker driver, this may be automatic):
```
minikube image load shopping/user-service:latest
minikube image load shopping/catalog-service:latest
minikube image load shopping/order-service:latest
minikube image load shopping/gateway-service:latest
```
3. Apply manifests:
```
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/users-postgres.yaml
kubectl apply -f deploy/k8s/orders-postgres.yaml
kubectl apply -f deploy/k8s/catalog-mongo.yaml
kubectl apply -f deploy/k8s/user-service.yaml
kubectl apply -f deploy/k8s/catalog-service.yaml
kubectl apply -f deploy/k8s/order-service.yaml
kubectl apply -f deploy/k8s/gateway-service.yaml
```
4. Optional ingress:
```
minikube addons enable ingress
kubectl apply -f deploy/k8s/ingress.yaml
```
Add `shopping.local` to `/etc/hosts` pointing to the minikube IP:
```
minikube ip
```

## Notes
- This project assumes Spring Boot `4.0.2` and Spring Cloud `2025.1.1`. If you want pinned versions, update `pom.xml` in the repo root.
- Order total is computed by calling `catalog-service` for each item id.
