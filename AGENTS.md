# Codex Working Agreements

## Product intent
A simple shopping microservices demo built with Spring Boot, Spring Cloud, Docker, and Kubernetes.
The system exposes a REST API (via a gateway) and persists data across three databases:
- Users/customers
- Catalog items (status, price, etc.)
- Orders/purchases and delivery status

This file is the living description of the system. Update it when requirements change.

## Current architecture (source of truth)
Services:
- `user-service`: manages users/customers (PostgreSQL)
- `catalog-service`: manages items, status, price (MongoDB)
- `order-service`: creates/updates orders and delivery status (PostgreSQL)
- `gateway-service`: Spring Cloud Gateway entry point
- `audit-service`: centralized audit log store (PostgreSQL)

Gateway routes:
- `/api/users/**` -> `user-service`
- `/api/catalog/**` -> `catalog-service`
- `/api/orders/**` -> `order-service`

Inter-service behavior:
- `order-service` resolves item details/prices by calling `catalog-service` for each item id.

## Requirements (aligned with original intent)
- Keep three separate persistence stores (users, catalog, orders).
- Keep services isolated by responsibility (users, items, orders/delivery).
- All external traffic goes through `gateway-service`.
- Services communicate via REST unless explicitly changed.
- Docker and Kubernetes deployment assets must remain functional.
- Expose Actuator endpoints and OpenAPI/Swagger UI for all services.

## Technical constraints
- Java 25, Spring Boot 4.0.x, Spring Cloud 2025.0.x.
- Maven multi-module build at repo root (`/Users/bjorne/gitrepos/SHOP/shopping_service`).
- No breaking changes to existing public REST endpoints unless explicitly requested.
- Use Lombok for entity boilerplate and Java records for simple immutable DTOs.

## How Codex should work in this repo
- Prefer small, incremental changes per request.
- Preserve existing project structure and conventions.
- Update tests and docs when behavior changes.
- Keep API contracts explicit (request/response models and error codes).
- For cross-service changes, update gateway routes and deployment manifests when needed.

## When adding features
- Provide or update REST endpoints in the owning service.
- Keep database changes scoped to the owning service only.
- If new inter-service calls are needed, document them in this file.
- If new deployment settings are required, update `deploy/docker` and/or `deploy/k8s`.

## Out of scope (unless requested)
- New service discovery/registry.
- Event-driven messaging (Kafka/RabbitMQ).
- AuthN/AuthZ beyond current implementation.
