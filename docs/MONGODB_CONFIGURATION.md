# MongoDB Configuration

## Overview

The Buy-01 application uses **two separate MongoDB instances** to avoid port conflicts between testing and deployment:

- **Local MongoDB** (port 27017): Used for running tests
- **Docker MongoDB** (port 27018): Used for Docker deployment

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Development Machine                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────┐        ┌─────────────────────┐    │
│  │  Local MongoDB      │        │  Docker MongoDB     │    │
│  │  Port: 27017        │        │  Port: 27018        │    │
│  │                     │        │  (Container)        │    │
│  │  Used by:           │        │                     │    │
│  │  - Unit Tests       │        │  Used by:           │    │
│  │  - Integration Tests│        │  - Deployed Services│    │
│  └─────────────────────┘        └─────────────────────┘    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Configuration Details

### Test Environment

**Location**: `src/test/resources/application-test.properties`

```properties
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=buy01_<service>_test
```

- Uses local MongoDB instance on port **27017**
- Each service has its own test database (e.g., `buy01_users_test`, `buy01_products_test`)
- Databases are cleaned before each test to ensure isolation

### Docker Deployment

**Location**: `docker-compose.yml`

```yaml
mongodb:
  ports:
    - "27018:27017"  # Host:Container
```

- Docker MongoDB container runs internally on port 27017
- Mapped to host port **27018** to avoid conflicts with local MongoDB
- Services inside Docker network connect to `mongodb:27017`
- External access (from host) uses `localhost:27018`

## Benefits

1. **No Port Conflicts**: Tests and deployment can run simultaneously
2. **Test Isolation**: Test databases are separate from deployment databases
3. **Continuous Testing**: Developers can run tests while services are deployed
4. **CI/CD Compatibility**: Jenkins can run tests without stopping deployed services

## Running Tests

```bash
# Tests automatically use local MongoDB on port 27017
cd Backend/user-service
./mvnw test
```

## Running Deployment

```bash
# Docker uses MongoDB on port 27018
./docker-start.sh
```

## Accessing MongoDB

### Local MongoDB (Tests)
```bash
mongosh mongodb://localhost:27017
```

### Docker MongoDB (Deployment)
```bash
mongosh mongodb://localhost:27018
```

Or connect from inside Docker network:
```bash
docker exec -it buy01-mongodb mongosh
```

## Troubleshooting

### Tests Fail to Connect to MongoDB
- Ensure local MongoDB is running: `sudo systemctl status mongod`
- Start local MongoDB: `sudo systemctl start mongod`

### Docker Services Fail to Connect to MongoDB
- Check if port 27018 is available: `netstat -tulpn | grep 27018`
- Check Docker MongoDB health: `docker compose ps`
- View MongoDB logs: `docker compose logs mongodb`

### Port Already in Use
```bash
# Check what's using the ports
lsof -i :27017  # Local MongoDB
lsof -i :27018  # Docker MongoDB

# Stop conflicting services
sudo systemctl stop mongod  # Stop local MongoDB if needed
docker compose down          # Stop Docker MongoDB if needed
```

## Database Names

| Environment | Service | Database Name |
|-------------|---------|---------------|
| Test | User Service | `buy01_users_test` |
| Test | Product Service | `buy01_products_test` |
| Test | Media Service | `buy01_media_test` |
| Deployment | All Services | `buy01` |

## Security Considerations

- Test databases use no authentication (local only)
- Production deployments should configure MongoDB authentication
- Add `MONGODB_USERNAME` and `MONGODB_PASSWORD` environment variables for production
- Use secrets management for credentials in production

## Future Enhancements

- Add MongoDB authentication
- Implement database backup strategy
- Configure replica sets for high availability
- Add monitoring and alerting

