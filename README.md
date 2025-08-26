# Java Vault - HashiCorp Vault-like Secrets Management Service

Java Vault is a Spring Boot-based secrets management service that provides functionality similar to HashiCorp Vault. It offers identity-based access control, hierarchical secrets storage, encryption, and multi-instance replication capabilities.

## Table of Contents

- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [Build Instructions](#build-instructions)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Examples](#examples)
- [Security](#security)
- [Development](#development)
- [Testing](#testing)

## Features

- **Identity-based Authentication**: JWT-based authentication with user/service/admin identity types
- **Policy-based Authorization**: Fine-grained access control using path-based policies
- **Hierarchical Secret Storage**: Organize secrets in a hierarchical path structure
- **Encryption at Rest**: AES-256-GCM encryption for all stored secrets
- **Comprehensive Versioning**: Full version management with automatic versioning, version history, specific version retrieval, and version deletion/restoration
- **Multi-instance Replication**: Database-level replication for high availability
- **RESTful API**: Complete REST API compatible with standard HTTP clients
- **Health Monitoring**: Built-in health checks and system status endpoints
- **Comprehensive Testing**: Unit tests and integration tests included

## Architecture Overview

### Core Components

1. **Authentication Service**: Handles JWT-based authentication and token validation
2. **Policy Service**: Manages access control policies and permission evaluation
3. **Secret Service**: Handles CRUD operations for secrets with encryption
4. **Encryption Service**: Provides AES-256-GCM encryption/decryption
5. **Replication Service**: Manages multi-instance synchronization

### Data Model

```
Identity (Users/Services)
├── Policies (Many-to-Many)
│   └── Rules (Path-based permissions)
└── Secrets (Created/Updated by)
    ├── Path (Hierarchical)
    ├── Key
    ├── Encrypted Value
    ├── Version
    └── Metadata
```

### Security Architecture

- **Authentication**: JWT tokens with configurable expiration
- **Authorization**: Policy-based access control with glob pattern matching
- **Encryption**: AES-256-GCM with random IV per secret
- **Transport Security**: HTTPS recommended for production

## Getting Started

### Prerequisites

- **Java 21** or higher (as configured in rules)
- **Maven 3.6+**
- **Database**: H2 (default), PostgreSQL (production)

### Quick Start

1. Clone the repository:
```bash
git clone <repository-url>
cd java-vault
```

2. Build the project:
```bash
mvn clean package
```

3. Run the application:
```bash
java -jar target/java-vault-1.0.0.jar
```

The service will start on port 8200 by default.

## Build Instructions

### Development Build

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package the application
mvn package

# Generate test coverage report
mvn jacoco:report
```

### Production Build

```bash
# Create production-ready JAR
mvn clean package -DskipTests

# Build with specific profile
mvn clean package -Pproduction
```

### Docker Build (Optional)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/java-vault-1.0.0.jar app.jar
EXPOSE 8200
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
server:
  port: 8200

vault:
  jwt:
    secret: your-jwt-secret-key-here
    expiration: 86400  # 24 hours
  
  encryption:
    key: your-32-character-encryption-key
  
  replication:
    enabled: true
    sync-interval: 30000
    cleanup-days: 7
```

### Environment Variables

- `VAULT_JWT_SECRET`: JWT signing secret
- `VAULT_ENCRYPTION_KEY`: Encryption key for secrets
- `DATABASE_URL`: Database connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password

## API Documentation

### Authentication

#### Login
```http
POST /v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer"
}
```

#### Validate Token
```http
POST /v1/auth/validate
Authorization: Bearer <token>
```

### User Management (Admin Only)

The system provides comprehensive user management capabilities accessible only to administrators with the `admin` policy.

#### Create New User
```http
POST /v1/admin/users
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "username": "developer1",
  "password": "SecurePassword123!",
  "type": "USER",
  "policies": ["developer"],
  "enabled": true,
  "description": "Frontend developer - Team Alpha"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User created successfully",
  "timestamp": "2024-01-15T10:30:00",
  "data": {
    "id": 123,
    "username": "developer1",
    "type": "USER",
    "enabled": true,
    "policies": ["developer"],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "lastLoginAt": null
  }
}
```

#### Get User Details
```http
GET /v1/admin/users/{username}
Authorization: Bearer <admin-token>
```

#### List All Users
```http
GET /v1/admin/users
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "timestamp": "2024-01-15T10:30:00",
  "data": [
    {
      "id": 1,
      "username": "admin",
      "type": "ADMIN",
      "enabled": true,
      "policies": ["admin"]
    },
    {
      "id": 2,
      "username": "developer1",
      "type": "USER", 
      "enabled": true,
      "policies": ["developer"]
    }
  ]
}
```

#### Update User
```http
PUT /v1/admin/users/{username}
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "password": "NewSecurePassword456!",
  "policies": ["developer", "readonly"],
  "enabled": true
}
```

#### Delete User
```http
DELETE /v1/admin/users/{username}
Authorization: Bearer <admin-token>
```

#### Enable/Disable User
```http
PATCH /v1/admin/users/{username}/status?enabled=false
Authorization: Bearer <admin-token>
```

#### List Available Policies
```http
GET /v1/admin/policies
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "success": true,
  "message": "Policies retrieved successfully",
  "timestamp": "2024-01-15T10:30:00",
  "data": [
    {
      "id": 1,
      "name": "admin",
      "description": "Full administrative access",
      "rules": ["*:*"]
    },
    {
      "id": 2,
      "name": "developer",
      "description": "Developer access to development secrets",
      "rules": [
        "create:secret/dev/*",
        "read:secret/dev/*",
        "update:secret/dev/*",
        "delete:secret/dev/*",
        "list:secret/dev/*",
        "read:secret/shared/*"
      ]
    },
    {
      "id": 3,
      "name": "readonly",
      "description": "Read-only access to all secrets",
      "rules": [
        "read:secret/*",
        "list:secret/*"
      ]
    }
  ]
}
```

### Secrets Management

#### Create Secret
```http
POST /v1/secret/myapp/production?key=database-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "value": "super-secret-password",
  "metadata": {
    "environment": "production",
    "service": "api"
  }
}
```

#### Read Secret (Latest Version)
```http
GET /v1/secret/myapp/production?key=database-password
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "value": "super-secret-password",
    "version": 3,
    "created_at": "2023-12-01T10:00:00",
    "updated_at": "2023-12-01T15:30:00",
    "metadata": {
      "environment": "production",
      "service": "api"
    }
  }
}
```

#### Read Specific Secret Version
```http
GET /v1/secret/myapp/production?key=database-password&version=2
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "value": "previous-secret-password",
    "version": 2,
    "created_at": "2023-12-01T10:00:00",
    "updated_at": "2023-12-01T12:15:00",
    "metadata": {
      "environment": "production",
      "service": "api"
    }
  }
}
```

#### Update Secret
```http
PUT /v1/secret/myapp/production?key=database-password
Authorization: Bearer <token>
Content-Type: application/json

{
  "value": "new-secret-password",
  "metadata": {
    "environment": "production",
    "service": "api",
    "updated_reason": "rotation"
  }
}
```

#### Delete Secret
```http
DELETE /v1/secret/myapp/production?key=database-password
Authorization: Bearer <token>
```

#### List Secrets
```http
GET /v1/secret/list/myapp/production
Authorization: Bearer <token>
```

### Secret Versioning

Java Vault provides comprehensive secret versioning capabilities:

#### List All Versions of a Secret
```http
GET /v1/secret/versions/myapp/production?key=database-password
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "path": "myapp/production/database-password",
  "total_versions": 3,
  "versions": [
    {
      "version": 3,
      "created_at": "2023-12-01T15:30:00",
      "updated_at": "2023-12-01T15:30:00",
      "created_by": "admin",
      "updated_by": "admin",
      "metadata": {"environment": "production", "updated_reason": "rotation"},
      "deleted": false,
      "deleted_at": null
    },
    {
      "version": 2,
      "created_at": "2023-12-01T12:15:00",
      "updated_at": "2023-12-01T12:15:00",
      "created_by": "admin",
      "updated_by": "admin",
      "metadata": {"environment": "production"},
      "deleted": false,
      "deleted_at": null
    },
    {
      "version": 1,
      "created_at": "2023-12-01T10:00:00",
      "updated_at": "2023-12-01T10:00:00",
      "created_by": "admin",
      "updated_by": null,
      "metadata": {"environment": "production"},
      "deleted": false,
      "deleted_at": null
    }
  ]
}
```

#### Get Version Information Summary
```http
GET /v1/secret/version-info/myapp/production?key=database-password
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "data": {
    "path": "myapp/production/database-password",
    "total_versions": 3,
    "earliest_version": 1,
    "latest_version": 3
  }
}
```

#### Get Version Range
```http
GET /v1/secret/version-range/myapp/production?key=database-password&startVersion=2&endVersion=3
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "path": "myapp/production/database-password",
  "start_version": 2,
  "end_version": 3,
  "count": 2,
  "versions": [
    {
      "value": "current-secret-password",
      "version": 3,
      "created_at": "2023-12-01T15:30:00",
      "updated_at": "2023-12-01T15:30:00",
      "created_by": "admin",
      "updated_by": "admin",
      "metadata": {"environment": "production", "updated_reason": "rotation"}
    },
    {
      "value": "previous-secret-password",
      "version": 2,
      "created_at": "2023-12-01T12:15:00",
      "updated_at": "2023-12-01T12:15:00",
      "created_by": "admin",
      "updated_by": "admin",
      "metadata": {"environment": "production"}
    }
  ]
}
```

#### Delete Specific Version
```http
DELETE /v1/secret/version/myapp/production?key=database-password&version=2
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "message": "Secret version deleted successfully",
  "path": "myapp/production/database-password",
  "version": 2
}
```

#### Restore Deleted Version
```http
POST /v1/secret/restore-version/myapp/production?key=database-password&version=2
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "message": "Secret version restored successfully",
  "path": "myapp/production/database-password",
  "version": 2
}
```

### System Operations

#### Health Check
```http
GET /v1/sys/health
```

#### System Status
```http
GET /v1/sys/status
```

## Examples

### Using cURL

#### Authentication

**Login and get JWT token:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  jq -r '.token')

# If you don't have jq installed, you can use this alternative approach:
TOKEN=$(curl -s -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  grep -o '"token":"[^"]*"' | cut -d':' -f2 | tr -d '"')

# For Windows PowerShell users:
# $response = Invoke-RestMethod -Uri "http://localhost:8200/v1/auth/login" -Method Post -Body '{"username":"admin","password":"password"}' -ContentType "application/json"
# $TOKEN = $response.token
```

**Validate token:**
```bash
curl -s -X POST http://localhost:8200/v1/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

#### User Management (Admin Only)

**Create a new user:**
```bash
# Get admin token first
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  jq -r '.token')

# Create new developer user
curl -s -X POST http://localhost:8200/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer1",
    "password": "SecurePassword123!",
    "type": "USER",
    "policies": ["developer"],
    "enabled": true,
    "description": "Frontend developer - Team Alpha"
  }'
```

**List all users:**
```bash
curl -s -X GET http://localhost:8200/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Get user details:**
```bash
curl -s -X GET http://localhost:8200/v1/admin/users/developer1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Update user:**
```bash
curl -s -X PUT http://localhost:8200/v1/admin/users/developer1 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "NewSecurePassword456!",
    "policies": ["developer", "readonly"],
    "enabled": true
  }'
```

**Disable user:**
```bash
curl -s -X PATCH "http://localhost:8200/v1/admin/users/developer1/status?enabled=false" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Delete user:**
```bash
curl -s -X DELETE http://localhost:8200/v1/admin/users/developer1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**List available policies:**
```bash
curl -s -X GET http://localhost:8200/v1/admin/policies \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### Secrets Management

**Create a new secret:**
```bash
curl -s -X POST "http://localhost:8200/v1/secret/myapp/prod?key=db-pass" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": "my-secret-password",
    "metadata": {"env": "production", "description": "Database password"}
  }'
```

**Read a secret:**
```bash
curl -s -X GET "http://localhost:8200/v1/secret/myapp/prod?key=db-pass" \
  -H "Authorization: Bearer $TOKEN"
```

**Update an existing secret:**
```bash
curl -s -X PUT "http://localhost:8200/v1/secret/myapp/prod?key=db-pass" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": "new-rotated-password",
    "metadata": {"env": "production", "description": "Rotated database password", "rotated": "true"}
  }'
```

**List secrets in a path:**
```bash
curl -s -X GET "http://localhost:8200/v1/secret/list/myapp/prod" \
  -H "Authorization: Bearer $TOKEN"
```

**Delete a secret:**
```bash
curl -s -X DELETE "http://localhost:8200/v1/secret/myapp/prod?key=db-pass" \
  -H "Authorization: Bearer $TOKEN"
```

#### System Operations

**Check system health:**
```bash
curl -s -X GET http://localhost:8200/v1/sys/health
```

**Get system status:**
```bash
curl -s -X GET http://localhost:8200/v1/sys/status
```

#### Complete Secret Management Workflow

```bash
# 1. Get authentication token
TOKEN=$(curl -s -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  jq -r '.token')
echo "Authenticated successfully, received token"

# 2. Create multiple secrets for an application
curl -s -X POST "http://localhost:8200/v1/secret/myapp/prod?key=db-password" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "complex-db-password", "metadata": {"env": "production"}}'
echo "Created database password secret"

curl -s -X POST "http://localhost:8200/v1/secret/myapp/prod?key=api-key" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "sk_live_abcdef123456", "metadata": {"env": "production"}}'
echo "Created API key secret"

# 3. List all secrets in the path
curl -s -X GET "http://localhost:8200/v1/secret/list/myapp/prod" \
  -H "Authorization: Bearer $TOKEN"

# 4. Read a specific secret
curl -s -X GET "http://localhost:8200/v1/secret/myapp/prod?key=db-password" \
  -H "Authorization: Bearer $TOKEN"

# 5. Update a secret (rotation)
curl -s -X PUT "http://localhost:8200/v1/secret/myapp/prod?key=db-password" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value": "new-rotated-password-2023", "metadata": {"env": "production", "rotated": "true"}}'
echo "Rotated database password"

# 6. Delete a secret when no longer needed
curl -s -X DELETE "http://localhost:8200/v1/secret/myapp/prod?key=api-key" \
  -H "Authorization: Bearer $TOKEN"
echo "Deleted API key secret"
```

#### Secret Versioning Examples

**List all versions of a secret:**
```bash
curl -s -X GET "http://localhost:8200/v1/secret/versions/myapp/prod?key=db-password" \
  -H "Authorization: Bearer $TOKEN"
```

**Get version information:**
```bash
curl -s -X GET "http://localhost:8200/v1/secret/version-info/myapp/prod?key=db-password" \
  -H "Authorization: Bearer $TOKEN"
```

**Get a specific version:**
```bash
curl -s -X GET "http://localhost:8200/v1/secret/myapp/prod?key=db-password&version=1" \
  -H "Authorization: Bearer $TOKEN"
```

**Get a range of versions:**
```bash
curl -s -X GET "http://localhost:8200/v1/secret/version-range/myapp/prod?key=db-password&startVersion=1&endVersion=2" \
  -H "Authorization: Bearer $TOKEN"
```

**Delete a specific version:**
```bash
curl -s -X DELETE "http://localhost:8200/v1/secret/version/myapp/prod?key=db-password&version=1" \
  -H "Authorization: Bearer $TOKEN"
```

**Restore a deleted version:**
```bash
curl -s -X POST "http://localhost:8200/v1/secret/restore-version/myapp/prod?key=db-password&version=1" \
  -H "Authorization: Bearer $TOKEN"
```

### Using Java/Spring

```java
@Service
public class VaultClient {
    private final RestTemplate restTemplate;
    private final String vaultUrl = "http://localhost:8200";
    private String token;
    
    public void authenticate(String username, String password) {
        AuthRequest request = new AuthRequest(username, password);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            vaultUrl + "/v1/auth/login", request, AuthResponse.class);
        this.token = response.getBody().getToken();
    }
    
    public String getSecret(String path, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        ResponseEntity<SecretResponse> response = restTemplate.exchange(
            vaultUrl + "/v1/secret/" + path + "?key=" + key,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            SecretResponse.class
        );
        
        return response.getBody().getData().getValue();
    }
}
```

### Policy Configuration

Policies define what operations are allowed on which paths:

```json
{
  "name": "developer-policy",
  "description": "Access for developers",
  "rules": [
    "read:secret/myapp/*",
    "write:secret/myapp/dev/*",
    "list:secret/myapp/*"
  ]
}
```

Policy rule format: `operation:path_pattern`
- Operations: `create`, `read`, `update`, `delete`, `list`, `*` (all)
- Path patterns support wildcards: `*` (any), `?` (single char)

## Security

### Production Security Checklist

- [ ] Use HTTPS/TLS in production
- [ ] Generate strong, unique JWT secrets
- [ ] Use secure encryption keys (32+ characters)
- [ ] Enable database encryption at rest
- [ ] Configure network security groups
- [ ] Set up log monitoring and alerting
- [ ] Implement secret rotation policies
- [ ] Use secure database credentials
- [ ] Enable audit logging

### Key Management

- Store encryption keys securely (e.g., AWS KMS, Azure Key Vault)
- Rotate keys regularly
- Use different keys per environment
- Never commit keys to version control

## Development

### Project Structure

```
src/
├── main/java/com/example/vault/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── dto/            # Data transfer objects
│   ├── entity/         # JPA entities
│   ├── exception/      # Exception handlers
│   ├── repository/     # Data repositories
│   ├── security/       # Security components
│   └── service/        # Business logic
└── test/               # Unit and integration tests
```

### Adding New Features

1. Create entity classes in `entity/`
2. Add repository interfaces in `repository/`
3. Implement business logic in `service/`
4. Create DTOs in `dto/`
5. Add REST endpoints in `controller/`
6. Write tests in `test/`

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SecretServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Coverage

The project includes comprehensive tests:
- Unit tests for all services
- Integration tests for REST APIs
- Security tests for authentication/authorization
- Encryption/decryption tests

View coverage reports in `target/site/jacoco/index.html`

### Manual Testing

Use the provided Postman collection or test scripts:

```bash
# Health check
curl http://localhost:8200/v1/sys/health

# System status
curl http://localhost:8200/v1/sys/status
```

## Troubleshooting

### Common Issues

**Authentication fails:**
- Check JWT secret configuration
- Verify user credentials in database
- Check token expiration settings

**Secrets not decrypting:**
- Verify encryption key is consistent
- Check database data integrity
- Ensure proper character encoding

**Replication not working:**
- Check database connectivity
- Verify replication configuration
- Review application logs

### Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.example.vault: DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License. See LICENSE file for details.

---

For more information or support, please open an issue in the project repository.
