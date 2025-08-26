package com.example.vault.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8200" + contextPath)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.vault.example.com")
                                .description("Production server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /v1/auth/login endpoint")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Java Vault API")
                .description("""
                        HashiCorp Vault-like secrets management service built with Spring Boot.
                        
                        This API provides secure secret storage, versioning, access control, and audit logging capabilities.
                        
                        ## Authentication
                        Most endpoints require a Bearer token obtained from the `/v1/auth/login` endpoint.
                        Include the token in the Authorization header: `Authorization: Bearer <your-token>`
                        
                        ## Key Features
                        - Secure secret storage with encryption at rest
                        - Secret versioning and rollback capabilities  
                        - Role-based access control with policies
                        - Comprehensive audit logging
                        - Real-time replication support
                        
                        ## Getting Started
                        1. Authenticate using `/v1/auth/login`
                        2. Store secrets using `/v1/secret/{path}`
                        3. Retrieve secrets using GET `/v1/secret/{path}`
                        4. Manage versions using the versioning endpoints
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Vault API Support")
                        .email("support@example.com")
                        .url("https://github.com/example/java-vault"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}
