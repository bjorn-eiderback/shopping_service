# Future Considerations: Split Into Separate Repos

This document outlines what it would take to split the current multi-module
Spring Boot project into separate repositories per service, and how to keep
the system coherent afterward.

## Summary: When to Split
- Stay monorepo if you want fast local iteration and a single CI pipeline.
- Split repos when teams, release cadence, or security boundaries diverge.

## Benefits
- Independent releases and deployments per service.
- Smaller codebases and clearer ownership boundaries.
- Per-service CI/CD and access control.

## Drawbacks
- Cross-service changes require coordination across repos.
- More pipelines and operational overhead.
- Higher risk of API/version drift.
- Local development becomes more complex.

## What Changes in Code/Build
Each service becomes a standalone Spring Boot repo:
- Each repo gets its own `pom.xml` (use `spring-boot-starter-parent`).
- Remove the root multi-module `pom.xml`.
- Keep `application.yaml` and service-specific configs local to that repo.

Shared code becomes a library repo:
- Move shared DTOs/utilities into a `shopping-contracts` (or similar) repo.
- Publish that library to an artifact repository.

## Artifact Strategy
Use both of these:
- **Nexus/Artifactory** for Java libraries (shared DTOs, generated clients).
- **Container registry** (ECR/GCR/GHCR) for service images.

This keeps library versions explicit and reproducible while images remain
deployable across environments.

## Spring Cloud: “Tying It Together”
Spring Cloud doesn’t couple code; it coordinates runtime behavior:
- Service discovery: Kubernetes DNS or Spring Cloud Kubernetes.
- Central config: Spring Cloud Config or K8s ConfigMaps/Secrets.
- Gateway: single public entry point (already in place).
- Observability: Actuator + metrics + tracing (OpenTelemetry).
- OpenAPI aggregation: keep gateway Swagger UI pointing to each service.

## Migration Plan (Practical Checklist)
1. Pick one service (start with `user-service`) and extract it.
2. Create a shared library repo if needed (DTOs, common error model).
3. Set up Nexus/Artifactory and publish the shared library.
4. Introduce semantic versioning for shared artifacts.
5. Extract remaining services one by one.
6. Create a lightweight “integration” repo for:
   - Docker Compose
   - Kubernetes manifests
   - Any cross-service runtime configuration

## Standalone POM Template (Example)
Use this as a starting point per service repo:

```xml
<project xmlns=\"http://maven.apache.org/POM/4.0.0\"
         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.2</version>
    <relativePath/>
  </parent>

  <groupId>com.example.shopping</groupId>
  <artifactId>user-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <java.version>25</java.version>
    <spring-cloud.version>2025.1.1</spring-cloud.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <!-- service dependencies here -->
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

## Contract/DTO Strategy
Two workable options:

Option A: Shared library
- Create `shopping-contracts` module/repo.
- Publish with semver to Nexus/Artifactory.
- Services depend on it using versions.

Option B: OpenAPI-driven contracts
- Define OpenAPI in each service.
- Generate clients for other services.
- Avoids shared binary dependencies but needs a generator pipeline.

## Integration Repo (Recommended)
Create a separate repo called `shopping-platform`:
- `deploy/docker/` with docker-compose to run all services.
- `deploy/k8s/` for manifests and overlays.
- `README.md` with environment setup.

This keeps deployment coordination centralized even as code is split.

