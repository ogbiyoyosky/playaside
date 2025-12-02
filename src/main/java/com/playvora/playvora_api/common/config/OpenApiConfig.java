package com.playvora.playvora_api.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Value("${spring.profiles.active}")
    private String appEnv;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Playaside API")
                        .version("1.0.0")
                        .description("Playaside helps communities organize matches effortlessly—mark availability, generate teams, and manage captains in real time."))
                .servers(getServerList())
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    private List<Server> getServerList() {
        List<Server> servers = new ArrayList<>();

        if ("production".equals(appEnv) ) {
            servers.add(new Server().url("https://api.playaside.com"));

        } else {
            if ("staging".equals(appEnv)) {
                servers.add(new Server().url("https://devapi.playaside.com"));
            } else {
                servers.add(new Server().url("https://devapi.playaside.com"));
            }
            servers.add(new Server().url("http://localhost:8080"));
        }

        return servers;
    }

    /**
     * Add X-User-Role-Id header parameter to all Swagger endpoints
     * This allows users to specify which role they're acting under in authenticated requests
     */
    @Bean
    public OperationCustomizer customizeOperations() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Parameter userRoleIdParam = new Parameter()
                    .in("header")
                    .name("X-User-Role-Id")
                    .description("Optional. The ID of the user role to use for this request. " +
                            "Get available roles from GET /api/v1/user-roles/me. " +
                            "Required for community-specific operations.")
                    .required(false)
                    .schema(new StringSchema())
                    .example("1");

            String className = handlerMethod.getMethod().getDeclaringClass().getName();
            // Exclude only auth controllers (controllers in the auth package)
            // Include all other controllers, including UserRoleController
            boolean isAuthController = className.contains(".auth.") || 
                                      className.contains(".auth.controller") ||
                                      className.endsWith(".auth.controllers.AuthenticationController");
            
            // Add header parameter and ensure security requirement for all endpoints except auth controllers
            if (!isAuthController) {
                operation.addParametersItem(userRoleIdParam);
                // Add security requirement if not already present (methods with @SecurityRequirement already have it)
                boolean hasSecurity = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
                if (!hasSecurity) {
                    operation.addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
                }
            }

            return operation;
        };
    }
}