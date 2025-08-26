# Java Vault - User Management & Secret Organization Instructions

## Overview

This document provides step-by-step instructions for managing users and organizing secrets in your Java Vault system. The system uses a hierarchical path structure to prevent secret collisions between users and provides role-based access control through policies.

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Default Setup](#default-setup)
3. [Administrator Tasks](#administrator-tasks)
4. [User Secret Management](#user-secret-management)
5. [Secret Path Organization](#secret-path-organization)
6. [Complete Workflow Examples](#complete-workflow-examples)

## System Architecture

### Identity & Policy Model
```
├── Identities (Users/Services)
│   ├── admin (ADMIN type) → admin policy
│   ├── demo (USER type) → developer policy
│   └── [new users] → assigned policies
├── Policies
│   ├── admin: "*:*" (full access)
│   ├── developer: access to "dev/*" paths
│   └── readonly: read access to all secrets
└── Secrets (organized by paths)
    ├── shared/ (shared secrets)
    ├── dev/ (development environment)
    ├── prod/ (production environment)
    └── users/{username}/ (user-specific secrets)
```

### Authentication Flow
1. User logs in with username/password
2. System validates credentials against Identity table
3. JWT token generated with embedded policies
4. Token used for subsequent API calls
5. Policies evaluated for each secret access request

## Default Setup

### Built-in Accounts
Your system starts with these default accounts:

| Username | Password | Type | Policies | Access |
|----------|----------|------|----------|---------|
| admin | admin123 | ADMIN | admin | Full access to everything |
| demo | demo123 | USER | developer | Access to dev/* and shared/* |

⚠️ **IMPORTANT**: Change these default passwords immediately in production!

### Built-in Policies
- **admin**: `*:*` - Full access to all operations and paths
- **developer**: Access to `dev/*` paths and read access to `shared/*`
- **readonly**: Read and list access to all `secret/*` paths

## Administrator Tasks

### Step 1: Login as Administrator

```bash
# Get admin token
curl -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.token'

# Store token for subsequent use
export ADMIN_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

**Annotated Steps:**
- **Purpose**: Authenticate as administrator to perform user management tasks
- **Result**: Receives JWT token with admin privileges
- **Token Contents**: Username, admin policy, and expiration time

### Step 2: Create Custom Policies (Optional)

**Note**: Currently your system doesn't have policy management endpoints. You would need to add these endpoints or create policies directly in the database. For now, use the existing policies.

The system includes these policy patterns:
- `create:path_pattern` - Allow creating secrets
- `read:path_pattern` - Allow reading secrets
- `update:path_pattern` - Allow updating secrets
- `delete:path_pattern` - Allow deleting secrets
- `list:path_pattern` - Allow listing secrets
- `*:path_pattern` - Allow all operations

### Step 3: Create New User Account

**Now Available**: The system provides full user creation capabilities through admin endpoints.

**Create New User:**
```bash
# Create a new developer user
curl -X POST http://localhost:8200/v1/admin/users \
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

**Expected Response:**
```json
{
  "success": true,
  "message": "User created successfully",
  "user": {
    "id": 123,
    "username": "developer1",
    "type": "USER",
    "enabled": true,
    "policies": ["developer"],
    "created_at": "2024-01-15T10:00:00"
  }
}
```

**Database Implementation** (what happens behind the scenes):
```sql
-- The system would execute something like:
INSERT INTO identity (name, password_hash, type, enabled, created_at) 
VALUES ('developer1', '$2a$10$...', 'USER', true, NOW());

INSERT INTO identity_policies (identity_id, policy_id)
SELECT i.id, p.id FROM identity i, policy p 
WHERE i.name = 'developer1' AND p.name = 'developer';
```

### Step 4: User Path Assignment Strategy

To prevent secret collisions, implement this path structure:

```
secret/
├── shared/           # Shared secrets (readable by all users)
│   ├── database/     # Shared database configs
│   └── external-apis/ # Shared API keys
├── dev/              # Development environment
│   └── {any-structure}/
├── staging/          # Staging environment  
│   └── {any-structure}/
├── prod/             # Production environment (restricted)
│   └── {any-structure}/
└── users/            # User-specific secrets
    ├── developer1/   # Secrets only for developer1
    ├── developer2/   # Secrets only for developer2
    └── service-account1/ # Secrets for service accounts
```

## User Secret Management

### Step 1: User Authentication

```bash
# User logs in with their credentials
curl -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"developer1","password":"temp_password_123"}'

# Extract token
export USER_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

**Annotated Steps:**
- **Purpose**: User authenticates to get their specific access token
- **Result**: Token contains user's assigned policies
- **Access Level**: Determined by policies attached to the user account

### Step 2: Create User-Specific Secrets

```bash
# Create secret in user's personal namespace
curl -X POST "http://localhost:8200/v1/secret/users/developer1/personal?key=my-api-key" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": "sk_live_abc123xyz789",
    "metadata": {
      "service": "payment-gateway",
      "environment": "personal",
      "created_by": "developer1"
    }
  }'
```

**Annotated Steps:**
- **Path Structure**: `users/{username}/` ensures no collision with other users
- **Access Control**: Only the user (or admin) can access this path
- **Metadata**: Helps organize and track secret usage

### Step 3: Access Shared Resources

```bash
# Read shared database configuration
curl -X GET "http://localhost:8200/v1/secret/shared/database?key=dev-connection" \
  -H "Authorization: Bearer $USER_TOKEN"

# List available shared secrets
curl -X GET "http://localhost:8200/v1/secret/list/shared" \
  -H "Authorization: Bearer $USER_TOKEN"
```

**Annotated Steps:**
- **Shared Access**: Multiple users can read shared secrets
- **Environment Separation**: Different configs for dev/staging/prod

## Secret Path Organization

### Recommended Path Structure

```bash
# Environment-based organization
secret/dev/myapp/database-password
secret/staging/myapp/database-password  
secret/prod/myapp/database-password

# Service-based organization
secret/services/api-gateway/jwt-secret
secret/services/payment/stripe-key
secret/services/email/sendgrid-key

# User-specific secrets
secret/users/developer1/personal-github-token
secret/users/developer1/test-api-keys
secret/users/service-account1/automation-keys

# Shared resources
secret/shared/databases/postgres-readonly
secret/shared/external-apis/weather-service
secret/shared/certificates/wildcard-ssl
```

### Path Collision Prevention

The system prevents collisions through:

1. **Policy-based access control**: Users can only access paths allowed by their policies
2. **Hierarchical structure**: Different teams/users use different path prefixes
3. **Naming conventions**: Standardized path patterns prevent conflicts

**Example Policy for User Isolation:**
```json
{
  "name": "user-developer1-policy",
  "rules": [
    "create:secret/users/developer1/*",
    "read:secret/users/developer1/*", 
    "update:secret/users/developer1/*",
    "delete:secret/users/developer1/*",
    "list:secret/users/developer1/*",
    "read:secret/shared/*",
    "list:secret/shared/*",
    "read:secret/dev/*",
    "list:secret/dev/*"
  ]
}
```

## Complete Workflow Examples

### Example 1: Developer Onboarding

```bash
# 1. Admin creates new developer account
curl -X POST http://localhost:8200/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_developer",
    "password": "SecurePassword456!",
    "type": "USER", 
    "policies": ["developer"],
    "enabled": true,
    "description": "Backend developer - Team Beta"
  }'

# 2. Developer logs in and changes password (when endpoint is implemented)
curl -X POST http://localhost:8200/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jane_developer","password":"temp_pass_456"}'

export JANE_TOKEN="..."

curl -X POST http://localhost:8200/v1/auth/change-password \
  -H "Authorization: Bearer $JANE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "current_password": "temp_pass_456",
    "new_password": "secure_new_password_789"
  }'

# 3. Developer creates personal secrets
curl -X POST "http://localhost:8200/v1/secret/users/jane_developer/tools?key=github-token" \
  -H "Authorization: Bearer $JANE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": "ghp_personal_token_xyz",
    "metadata": {
      "service": "github",
      "scope": "repo,user"
    }
  }'

# 4. Developer accesses shared development resources
curl -X GET "http://localhost:8200/v1/secret/shared/database?key=dev-postgres" \
  -H "Authorization: Bearer $JANE_TOKEN"
```

### Example 2: Service Account Setup

```bash
# 1. Admin creates service account
curl -X POST http://localhost:8200/v1/auth/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ci_cd_service",
    "password": "auto_generated_secure_password",
    "type": "SERVICE",
    "policies": ["deployment-service"],
    "enabled": true
  }'

# 2. Service account stores deployment keys
export SERVICE_TOKEN="..."

curl -X POST "http://localhost:8200/v1/secret/users/ci_cd_service/deployment?key=aws-access-key" \
  -H "Authorization: Bearer $SERVICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "value": "AKIA...",
    "metadata": {
      "service": "aws",
      "environment": "production",
      "permissions": "deployment-only"
    }
  }'
```

### Example 3: Multi-Environment Secret Management

```bash
# Create same secret across environments with different values
environments=("dev" "staging" "prod")
values=("dev_db_password" "staging_db_password" "super_secure_prod_password")

for i in "${!environments[@]}"; do
  env="${environments[$i]}"
  value="${values[$i]}"
  
  curl -X POST "http://localhost:8200/v1/secret/${env}/myapp?key=database-password" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"value\": \"${value}\",
      \"metadata\": {
        \"environment\": \"${env}\",
        \"service\": \"myapp\",
        \"type\": \"database\"
      }
    }"
done

# Application can access appropriate secret based on environment
curl -X GET "http://localhost:8200/v1/secret/prod/myapp?key=database-password" \
  -H "Authorization: Bearer $PROD_TOKEN"
```

## Security Best Practices

### Path Organization Rules
1. **Use environment prefixes**: `dev/`, `staging/`, `prod/`
2. **Isolate user secrets**: `users/{username}/`
3. **Group shared resources**: `shared/{category}/`
4. **Service-specific paths**: `services/{service-name}/`

### Access Control Guidelines
1. **Principle of least privilege**: Users only get access to what they need
2. **Environment separation**: Production secrets need special policies
3. **Audit trails**: All secret access is logged with user information
4. **Token expiration**: JWT tokens expire and must be renewed

### Available Features ✅

**Implemented User Management Endpoints**:
- `POST /v1/admin/users` - Create new user accounts ✅
- `GET /v1/admin/users` - List all users ✅  
- `GET /v1/admin/users/{username}` - Get user details ✅
- `PUT /v1/admin/users/{username}` - Update user accounts ✅
- `DELETE /v1/admin/users/{username}` - Delete user accounts ✅
- `PATCH /v1/admin/users/{username}/status` - Enable/disable users ✅
- `GET /v1/admin/policies` - List available policies ✅

### Remaining Limitations

**Missing Endpoints** (still need to be implemented):
- `POST /v1/auth/change-password` - Allow users to change passwords
- `POST /v1/admin/policies` - Create custom policies
- `PUT /v1/admin/policies/{name}` - Update existing policies
- `DELETE /v1/admin/policies/{name}` - Delete policies

**Current Status**: User management is fully functional! Users can now be created, updated, and managed through the admin API endpoints.

## Next Steps

1. **Implement missing endpoints** for complete user management
2. **Add policy management APIs** for flexible access control
3. **Set up proper secret path conventions** for your organization
4. **Create automated user onboarding scripts**
5. **Implement audit logging** for compliance requirements

This completes the comprehensive guide for managing users and secrets in your Java Vault system!
