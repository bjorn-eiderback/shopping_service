# Shopping Service (Spring Boot 4, Java 25)

Microservice demo for a shopping app:
- `user-service`: customers/users (PostgreSQL)
- `catalog-service`: items, status, price (MongoDB)
- `order-service`: buying/delivery orders (PostgreSQL)
- `gateway-service`: Spring Cloud Gateway entry point
- `audit-service`: centralized audit log (PostgreSQL)
- `authorization-service`: local OAuth2 authorization server (alternative to Auth0)

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

## Testing
Purpose:
- verify business logic quickly (unit tests)
- verify HTTP/security behavior at service boundaries (integration tests)
- verify persistence against real databases (Testcontainers integration tests)

Test categories used in this repo:
- unit tests:
  - examples: `OrderControllerTest`, `CustomerControllerTest`, `ItemControllerTest`
- integration tests (Spring context/web layer):
  - examples: `GatewaySecurityIntegrationTest`, `AuthorizationServiceIntegrationTest`, `OrderControllerIntegrationTest`
- Testcontainers repository integration tests:
  - `OrderRepositoryTestcontainersIntegrationTest` (PostgreSQL)
  - `CustomerRepositoryTestcontainersIntegrationTest` (PostgreSQL)
  - `ItemRepositoryTestcontainersIntegrationTest` (MongoDB)

Run from terminal:
```bash
# all non-Testcontainers tests in all modules (default fast path)
mvn test

# one module
mvn -pl order-service test

# run Testcontainers tests (includes TestcontainersIntegrationTest classes)
mvn -Ptestcontainers test

# run Testcontainers tests for one module
mvn -Ptestcontainers -pl order-service test
mvn -Ptestcontainers -pl user-service test
mvn -Ptestcontainers -pl catalog-service test

# one Testcontainers test class
mvn -pl order-service -Dtest=OrderRepositoryTestcontainersIntegrationTest test
mvn -pl user-service -Dtest=CustomerRepositoryTestcontainersIntegrationTest test
mvn -pl catalog-service -Dtest=ItemRepositoryTestcontainersIntegrationTest test
```

Maven profile behavior:
- default (`mvn test`): excludes `*TestcontainersIntegrationTest`
- `-Ptestcontainers`: includes `*TestcontainersIntegrationTest`

Run from IntelliJ IDEA:
1. Open the Maven project and reimport if needed.
2. Run a single test class by right-clicking the class in `src/test/java`.
3. Run module tests from the Maven tool window (`test` lifecycle for that module).
4. For Testcontainers tests, ensure Docker Desktop is running before starting the test.

Recommended IntelliJ run configurations:
1. Fast default test run (no Testcontainers):
- Type: `JUnit`
- Scope: `All in package` (or module)
- Tag expression: exclude tag `testcontainers`
2. Testcontainers-only run:
- Type: `JUnit`
- Scope: `All in package` (or module)
- Tag expression: include tag `testcontainers`
3. Maven alternative (profile-based):
- Type: `Maven`
- Command line: `-Ptestcontainers test`
- Optional module: `-pl order-service` / `-pl user-service` / `-pl catalog-service`

Notes:
- Testcontainers tests are slower than unit tests because containers are started.
- If tests pass in isolation but fail when running many tests together in IntelliJ:
  - reimport Maven projects
  - rebuild the project
  - rerun the failing module/test
- Mockito warnings about dynamic agent loading on newer JDKs are expected in this setup.

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
mvn -pl audit-service spring-boot:run
mvn -pl authorization-service spring-boot:run
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
- Audit Service Swagger UI: `http://localhost:8084/swagger-ui/index.html`
- Authorization Service endpoints: `http://localhost:9000/.well-known/openid-configuration`

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

## Auth0 (Gateway-Only JWT)
Authentication is enforced at the gateway only. Services remain internal.
The gateway validates JWTs and authorizes requests based on roles.

Provider switch:
- `AUTH_PROVIDER=none` (no JWT, optional basic auth)
- `AUTH_PROVIDER=auth0` (Auth0 JWT validation)
- `AUTH_PROVIDER=local` (local `authorization-service` JWT validation)

Auth0 env vars:
- `AUTH_ENABLED=true`
- `AUTH0_ISSUER_URI=https://<tenant>.auth0.com/`
- `AUTH0_AUDIENCE=<api-identifier>`
- `AUTH0_ROLES_CLAIM=https://your-namespace/roles` (custom claim for roles)

Local OAuth2 env vars:
- `LOCAL_OAUTH2_ISSUER_URI=http://localhost:9000`
- `LOCAL_OAUTH2_AUDIENCE=shopping-api`
- `LOCAL_OAUTH2_ROLES_CLAIM=https://example.com/roles` (configured in auth service)

Local development without auth:
- Keep `AUTH_ENABLED=false` (default).
- No JWT token is required; requests are permitted by the gateway.

Local development with basic auth (no JWT):
- Set `AUTH_DEV_BASIC_ENABLED=true` on the gateway.
- Default users:
  - `adminAPI` / `admin` (role `apiAdmin`)
  - `userAPI` / `user` (role `apiUser`)

Example (basic auth):
```
AUTH_DEV_BASIC_ENABLED=true mvn -pl gateway-service spring-boot:run
```

curl example:
```
curl -u adminAPI:admin http://localhost:8080/api/orders
```

Swagger UI with Basic Auth:
- Open `http://localhost:8080/swagger-ui/index.html`
- If prompted by the browser, enter the basic auth credentials.
- If Swagger UI loads, use the browser’s basic auth cache for subsequent calls.

Role mapping:
- `apiUser` can `GET /api/**`
- `apiAdmin` can `POST/PUT/PATCH/DELETE /api/**`

Docker/K8s wiring:
- Set `AUTH_ENABLED=true` and the Auth0 vars on the gateway container.
- See `deploy/k8s/gateway-service.yaml` for env var placeholders.

Local OAuth2 token examples (from `authorization-service`):
```
# admin token (apiAdmin + apiUser roles)
curl -u admin-client:admin-secret \
  -d grant_type=client_credentials \
  -d scope=apiAdmin \
  -d audience=shopping-api \
  http://localhost:9000/oauth2/token

# user token (apiUser role)
curl -u user-client:user-secret \
  -d grant_type=client_credentials \
  -d scope=apiUser \
  -d audience=shopping-api \
  http://localhost:9000/oauth2/token
```

Use local OAuth2 mode in gateway:
```
AUTH_PROVIDER=local \
AUTH_DEV_BASIC_ENABLED=false \
LOCAL_OAUTH2_ISSUER_URI=http://localhost:9000 \
LOCAL_OAUTH2_AUDIENCE=shopping-api \
mvn -pl gateway-service spring-boot:run
```

Use Auth0 mode in gateway:
```
AUTH_PROVIDER=auth0 \
AUTH_DEV_BASIC_ENABLED=false \
AUTH0_ISSUER_URI=https://your-tenant.auth0.com/ \
AUTH0_AUDIENCE=shopping-api \
mvn -pl gateway-service spring-boot:run
```

Use no-JWT mode in gateway:
```
AUTH_PROVIDER=none mvn -pl gateway-service spring-boot:run
```

Feature flags note:
- A flag service like Flagsmith can select the provider (`auth0` vs `local`), but apply the value at startup (env/config) rather than hot-switching auth mode at runtime.

## Audit Logging
All services emit audit events for reads and writes. Events are stored in the
`audit-service` PostgreSQL database.

Headers (forwarded by the gateway when JWT auth is enabled):
- `X-Actor-Id` (JWT subject)
- `X-Actor-Roles` (comma-separated roles)
- `X-Actor-Email` (if present in JWT)

Audit events include:
- service, entityType, entityId, action
- actor info, request path/method
- before/after JSON snapshots

## Order Service Error Handling
The order service uses custom exceptions and a controller advice to return
consistent error responses. Example error shape:
```
{
  "code": "ORDER_INVALID_STATUS_TRANSITION",
  "message": "Invalid status transition: SHIPPED -> PENDING",
  "timestamp": "2026-02-06T12:00:00Z"
}
```

Common codes:
- `ORDER_NOT_FOUND` (404)
- `ORDER_INVALID_STATUS_TRANSITION` (409)
- `ORDER_BAD_REQUEST` (400)

Example (local run):
```
AUTH_ENABLED=true \\
AUTH0_ISSUER_URI=https://your-tenant.auth0.com/ \\
AUTH0_AUDIENCE=shopping-api \\
AUTH0_ROLES_CLAIM=https://shopping.local/roles \\
mvn -pl gateway-service spring-boot:run
```

## Calling the API With Roles
All calls go through the gateway. Use a JWT that includes roles in the configured
roles claim (for example `https://shopping.local/roles`).

Role behavior:
- `apiUser` can `GET /api/**`
- `apiAdmin` can `POST/PUT/PATCH/DELETE /api/**` (and can also read)

All examples below assume local basic auth is enabled.
Replace `<USER>`/`<PASS>` with `adminAPI`/`admin` or `userAPI`/`user`.

### curl
Read (apiUser or apiAdmin):
```
curl -u <USER>:<PASS> \
  http://localhost:8080/api/orders
```

Write (apiAdmin only):
```
curl -u <USER>:<PASS> -X POST \
  -H "Content-Type: application/json" \
  -d '{"userId":"u1","itemIds":["item-1","item-2"]}' \
  http://localhost:8080/api/orders
```

### HTTPie
Read:
```
http GET :8080/api/catalog --auth <USER>:<PASS>
```

Write (admin):
```
http POST :8080/api/catalog --auth <USER>:<PASS> \
  name=Widget price:=19.99 status=ACTIVE stockQuantity:=10
```

### Postman
1. Method + URL (e.g., `GET http://localhost:8080/api/users`)
2. Authorization tab → Type `Basic Auth` → set `<USER>` / `<PASS>`
3. For POST/PUT/PATCH: add JSON body and `Content-Type: application/json`

### Java (Spring Boot 4 client)
Example using `RestClient`:
```java
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

String credentials = "adminAPI:admin";
String encodedBasic = Base64.getEncoder()
    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

RestClient client = RestClient.builder()
    .baseUrl("http://localhost:8080")
    .defaultHeader("Authorization", "Basic " + encodedBasic)
    .build();

String orders = client.get()
    .uri("/api/orders")
    .retrieve()
    .body(String.class);

String created = client.post()
    .uri("/api/orders")
    .contentType(MediaType.APPLICATION_JSON)
    .body("{\"userId\":\"u1\",\"itemIds\":[\"item-1\"]}")
    .retrieve()
    .body(String.class);
```

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
minikube image load shopping/audit-service:latest
minikube image load shopping/authorization-service:latest
```
3. Apply manifests:
```
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/users-postgres.yaml
kubectl apply -f deploy/k8s/orders-postgres.yaml
kubectl apply -f deploy/k8s/catalog-mongo.yaml
kubectl apply -f deploy/k8s/audit-postgres.yaml
kubectl apply -f deploy/k8s/user-service.yaml
kubectl apply -f deploy/k8s/catalog-service.yaml
kubectl apply -f deploy/k8s/order-service.yaml
kubectl apply -f deploy/k8s/gateway-service.yaml
kubectl apply -f deploy/k8s/audit-service.yaml
kubectl apply -f deploy/k8s/authorization-service.yaml
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
