# AGENTS.md - AI Coding Agent Guide for Buy-01 E-Commerce Platform

## Architecture Overview

**Buy-01** is a full-stack microservices e-commerce platform:
- **Backend**: 5 Spring Boot services on Java 17 (user, product, media, order, api-gateway)
- **Frontend**: Angular 21 with SSL/TLS
- **Database**: MongoDB (port 27017 for tests, 27018 for Docker)
- **Orchestration**: Docker Compose + Jenkins CI/CD

### Key Service Boundaries

| Service | Port | Purpose | Key Dependencies |
|---------|------|---------|------------------|
| **api-gateway** | 8080 | Routes requests to services; handles CORS | All backend services |
| **user-service** | 8081 | Auth, registration, user profiles; generates JWT | MongoDB, media-service |
| **product-service** | 8082 | Product CRUD, stock management | MongoDB, media-service |
| **media-service** | 8083 | File uploads/downloads; stores to `/app/uploads` | MongoDB |
| **order-service** | 8084 | Order creation and management | MongoDB, product-service, user-service |

**Critical**: All services communicate via HTTP using `RestTemplate`. Services use environment variables for inter-service URLs (see docker-compose.yml).

---

## Critical Developer Workflows

### Build & Test (Backend)

```bash
# Build all microservices
cd Backend && mvn clean install

# Build single service
cd Backend/user-service && mvn clean install

# Run tests with coverage
mvn test

# Run specific test class
mvn test -Dtest=UserControllerTest
```

**Key Files**: 
- Parent POM: `Backend/pom.xml` (manages Java 17, SonarCloud plugins)
- Each service has own `pom.xml` with Spring Boot 3.5.6

### Development Mode (Hot Reload)

1. **Start MongoDB locally**:
   ```bash
   ./start-dev-db.sh
   ```

2. **Start each backend service in separate terminal**:
   ```bash
   cd Backend/{service-name} && ./mvnw spring-boot:run
   ```

3. **Start frontend**:
   ```bash
   cd Frontend && npm install && npm start  # Runs on https://localhost:4443
   ```

**Note**: Frontend redirects HTTP (4200) → HTTPS (4443). Self-signed certs in `Frontend/certs/`.

### Docker Deployment

```bash
# Start entire application
./docker-start.sh  # Waits for MongoDB health check, then starts services in order

# Stop services
./docker-stop.sh

# Clean & rebuild Docker images
./docker-cleanup-rebuild.sh
```

**Important Port Mappings**:
- MongoDB: Host 27018 → Container 27017 (avoids conflict with local MongoDB on 27017)
- Services reach MongoDB internally via `mongodb://mongodb:27017` (Docker network)
- Frontend SSL: 4443, HTTP redirect: 4200

---

## Authentication & Authorization Pattern

### JWT Implementation (Custom, Not Standard OAuth)

**Token Format**: `userId:email:role:expirationTimestamp:signature` (Base64 encoded)

**Generation** (user-service):
- Created on login/registration with 3600000ms (1 hour) expiration
- Uses `JWT_SECRET` environment variable + HMAC-SHA256

**Validation Pattern** (all services):
- Every service includes `JwtAuthenticationFilter` extending `OncePerRequestFilter`
- Extracts token from `Authorization: Bearer {token}` header
- Validates signature and expiration using `JwtService.extractUserId()` / `extractRole()`
- Sets Spring `SecurityContext` with `UsernamePasswordAuthenticationToken`

**Files to Reference**:
- `Backend/user-service/src/main/java/service/user/security/JwtService.java`
- `Backend/product-service/src/main/java/service/product/security/JwtAuthenticationFilter.java`

**Frontend** (Angular):
- Interceptor (`Frontend/src/app/interceptors/auth.interceptor.ts`) adds `Authorization` header to all requests
- Token stored in `localStorage` as `auth_token`
- User roles: `ROLE_BUYER`, `ROLE_SELLER` (see `extractRole()` in JwtService)

---

## MongoDB Configuration & Data Isolation

### Critical Detail: Two MongoDB Instances

| Scenario | Host:Port | Container:Port | Used By |
|----------|-----------|------------------|---------|
| **Unit/Integration Tests** | localhost:27017 | N/A | `src/test/resources/application-test.properties` |
| **Docker Deployment** | localhost:27018 | mongodb:27017 | Services via docker-compose.yml |

**Database Naming**: Each service has dedicated database:
- `buy01_users`, `buy01_products`, `buy01_orders`, `buy01_media` (production)
- `buy01_users_test`, `buy01_products_test`, etc. (tests)

**Key Pattern**: Services have `@EnableMongoRepositories(basePackages = "service.{service}.mongo_repo")` to isolate repository scanning.

---

## Cross-Service Communication Pattern

Services communicate via HTTP + JWT for internal calls:

```java
// Example: Order Service → Product Service
@Service
public class ProductServiceClient {
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${product.service.url:http://localhost:8082}")  // Docker: product-service:8082
    private String productServiceUrl;
    
    public Product getProduct(String productId) {
        return restTemplate.getForObject(
            productServiceUrl + "/api/products/" + productId, 
            Product.class
        );
    }
}
```

**Internal Endpoints** (for service-to-service only):
- `PATCH /internal/stock/{productId}` - Adjust inventory (order-service → product-service)
- Other internal endpoints use same `/internal/` prefix pattern

**Dependencies in docker-compose.yml**:
- `depends_on: service_healthy` ensures startup order
- Services use internal hostnames: `http://buy01-product-service:8082`

---

## API Gateway Routing

**Routes** (from `GatewayRoutingTest.java`):
- `/api/users/**` → user-service:8081
- `/api/products/**` → product-service:8082
- `/api/media/**` → media-service:8083
- `/api/orders/**` → order-service:8084

**CORS Configuration**:
- Allowed origins: `http://localhost:4200`, `https://localhost:4443`, `http://localhost:4201`
- Security headers configured: `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`

---

## Project Conventions & Patterns

### Package Structure (Each Service)
```
service/{service}/
  ├── controllers/        # REST endpoints
  ├── services/          # Business logic
  ├── mongo_repo/        # MongoRepository interfaces
  ├── models/            # @Document POJOs
  ├── security/          # JwtService, JwtAuthenticationFilter
  ├── clients/           # RestTemplate clients for other services
  ├── exception/         # Custom exceptions
  └── {Service}Application.java  # @SpringBootApplication entry point
```

### Test Configuration
- Tests use `@TestPropertySource(locations = "classpath:application-test.properties")`
- `@SpringBootTest` used for integration tests
- Each service has dedicated test database

### Frontend (Angular Standalone Components)
- Uses Angular 21 with standalone components (no NgModules)
- Interceptors in `src/app/interceptors/`
- Services in `src/app/services/` (inject via `inject()`)
- Routes defined in `app.routes.ts`
- SSL certs generated via `cert-generator.mjs` for local development

### Frontend Build Commands
```json
{
  "start": "node server.mjs",              // Production server with SSL redirect
  "start:dev": "ng serve",                 // Dev server (ng serve)
  "test": "ng test",                       // Karma tests
  "test:coverage": "ng test --code-coverage --browsers=ChromeHeadless"
}
```

---

## CI/CD Conventions

### Jenkins Pipeline (Branch Strategy)

- **main/master**: Build → Manual Approval Required → Deploy
- **dev**: Build → Auto-Deploy
- **jenkins/feature branches**: Build Only

**Key Features**:
- Parallel builds for all 5 microservices
- Docker image creation with tags: `{BUILD_NUMBER}-{GIT_COMMIT_SHORT}`
- Health checks after deployment (MongoDB, all services, frontend)
- Auto-rollback on failure
- Email notifications to configured recipients

### Environment Variables & Secrets

**Never in Code** (in `.gitignore`):
- `.env` file contains: `JWT_SECRET`, `JWT_EXPIRATION`, MongoDB config
- `JWT_SECRET` must be 512-bit (64 bytes Base64) minimum
- Generated via: `openssl rand -base64 64`

**Jenkins Credentials**:
- `JWT_SECRET` stored in Jenkins credentials (ID: `JWT_SECRET`)
- Injected via `withCredentials` in Jenkinsfile
- SonarCloud token in GitHub secrets (for GitHub Actions workflow)

---

## Common Debugging Points

### If Tests Fail

1. **Ensure local MongoDB on 27017**:
   ```bash
   ./start-dev-db.sh
   ```

2. **Test database isolation**: Tests use separate databases (e.g., `buy01_users_test`)
   - If data persists, manually drop test database: `db.dropDatabase()`

### If Services Don't Connect

1. **Check docker-compose.yml** for inter-service URLs
2. **Verify JWT_SECRET** is set (same across all services)
3. **Monitor logs**: `docker compose logs {service-name}`

### If Frontend Shows CORS Errors

1. Verify gateway CORS config allows origin
2. Check `Authorization` header is set via `auth.interceptor.ts`
3. Ensure token hasn't expired (1 hour default)

---

## File Location Quick Reference

| Need | File |
|------|------|
| Build config (all services) | `Backend/pom.xml` |
| JWT token format & validation | `Backend/*/src/main/java/service/*/security/JwtService.java` |
| API Gateway routing | `Backend/api-gateway/src/main/java/api/gateway/` |
| Docker setup | `docker-compose.yml` |
| Deployment scripts | `./docker-*.sh`, `./jenkins-deploy.sh` |
| Frontend entry point | `Frontend/src/app/app.ts` |
| Frontend auth | `Frontend/src/app/interceptors/auth.interceptor.ts` |
| MongoDB docs | `docs/MONGODB_CONFIGURATION.md` |
| JWT docs | `docs/JWT_SECURITY.md` |
| Jenkins docs | `docs/JENKINS_SETUP.md`, `docs/JENKINS_QUICK_START.md` |

