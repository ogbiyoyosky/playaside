package com.playvora.playvora_api.component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.playvora.playvora_api.common.exception.UserRoleHeaderException;
import com.playvora.playvora_api.common.utils.UserRoleContext;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.entities.UserRole;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.user.repo.UserRoleRepository;
import com.playvora.playvora_api.user.services.IJwtService;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playvora.playvora_api.common.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final IJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ObjectMapper objectMapper;
    
    private static final String USER_ROLE_ID_HEADER = "X-User-Role-Id";

    @Override
    public void doFilterInternal(@NonNull HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain chain) throws IOException, ServletException {
        try {
            // Skip JWT validation for WebSocket endpoints and static resources.
            // IMPORTANT: Do NOT treat API routes (e.g. /api/v1/files/**) as static
            // even if they end with an asset extension like .jpg.
            String requestPath = request.getRequestURI();
            boolean isStaticPath =
                    requestPath.startsWith("/ws") ||
                    requestPath.startsWith("/static") ||
                    requestPath.startsWith("/public") ||
                    requestPath.startsWith("/resources");

            boolean hasStaticExtension =
                    !requestPath.startsWith("/api/") && (
                        requestPath.endsWith(".html") ||
                        requestPath.endsWith(".css") ||
                        requestPath.endsWith(".js") ||
                        requestPath.endsWith(".png") ||
                        requestPath.endsWith(".jpg") ||
                        requestPath.endsWith(".jpeg") ||
                        requestPath.endsWith(".gif") ||
                        requestPath.endsWith(".svg") ||
                        requestPath.endsWith(".ico") ||
                        requestPath.endsWith(".woff") ||
                        requestPath.endsWith(".woff2") ||
                        requestPath.endsWith(".ttf") ||
                        requestPath.endsWith(".eot")
                    );

            if (isStaticPath || hasStaticExtension) {
                chain.doFilter(request, response);
                return;
            }

            // Extract JWT from Authorization header
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                chain.doFilter(request, response); // No token, let Spring handle 401
                return;
            }
            String jwt = authorizationHeader.substring(7);

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                // If already authenticated, process user role header
                processUserRoleHeader(request);
                chain.doFilter(request, response);
                return;
            }

            // Validate JWT
            String email = jwtService.extractUsername(jwt);
            if (email == null) {
                chain.doFilter(request, response); // No email, let Spring handle 401
                return;
            }

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (userDetails == null) {
                chain.doFilter(request, response); // No user details, let Spring handle 401
                return;
            }

            // Validate JWT
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                
                // After authentication is set, process user role header
                processUserRoleHeader(request);
            }
            chain.doFilter(request, response);
        } catch (UserRoleHeaderException ex) {
            handleUserRoleHeaderException(ex, response);
        } finally {
            // Always clear the UserRoleContext at the end of the request
            UserRoleContext.clear();
        }
    }
    
    /**
     * Handle UserRoleHeaderException by writing error response
     */
    private void handleUserRoleHeaderException(UserRoleHeaderException ex, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        ErrorResponse errorResponse = new ErrorResponse("USER_ROLE_HEADER_ERROR", ex.getMessage());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }


    /**
     * Process the X-User-Role-Id header and validate it belongs to the authenticated user.
     * If valid, set it in the UserRoleContext for use in service methods.
     * Throws UserRoleHeaderException if header is missing or invalid for authenticated routes.
     * 
     * @param request HTTP request
     * @throws UserRoleHeaderException if header is missing or invalid
     */
    private void processUserRoleHeader(HttpServletRequest request) {
        // Check if this is an authenticated route
        boolean isAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null 
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser");
        
        // Skip validation for public routes
        String requestPath = request.getRequestURI();
        boolean isPublicRoute = isPublicRoute(requestPath);

        log.info("requestPath: {}", requestPath);
        log.info("isAuthenticated: {}", isAuthenticated);
        log.info("isPublicRoute: {}", isPublicRoute);
        
        if (!isAuthenticated || isPublicRoute) {
            log.debug("Skipping user role header validation for public or unauthenticated route: {}", requestPath);
            return;
        }
        
        String userRoleIdHeader = request.getHeader(USER_ROLE_ID_HEADER);
        
        if (userRoleIdHeader == null || userRoleIdHeader.trim().isEmpty()) {
            log.warn("X-User-Role-Id header is required for authenticated routes but was not provided");
            throw new UserRoleHeaderException("X-User-Role-Id header is required for authenticated routes");
        }
        
        String userRoleIdStr = userRoleIdHeader.trim();
        UUID userRoleId;
        try {
            userRoleId = UUID.fromString(userRoleIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user role ID format: {}", userRoleIdStr);
            throw new UserRoleHeaderException("Invalid X-User-Role-Id format. Expected a valid UUID");
        }
        
        // Get the authenticated user's email
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String userEmail = userDetails.getUsername();
        
        // Get the user from the database
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.warn("User not found for email: {}", userEmail);
            throw new UserRoleHeaderException("User not found");
        }
        
        User user = userOpt.get();
        
        // Validate the user role ID belongs to this user
        Optional<UserRole> userRoleOpt = userRoleRepository.findActiveUserRoleById(userRoleId);
        
        if (userRoleOpt.isEmpty()) {
            log.warn("User role ID {} not found or inactive", userRoleId);
            throw new UserRoleHeaderException("User role ID not found or inactive");
        }
        
        UserRole userRole = userRoleOpt.get();
        
        // Verify the user role belongs to the authenticated user
        if (!userRole.getUser().getId().equals(user.getId())) {
            log.warn("User role ID {} does not belong to user {}", userRoleId, user.getId());
            throw new UserRoleHeaderException("User role ID does not belong to the authenticated user");
        }
        
        // Set the validated user role in the context
        UserRoleContext.setCurrentUserRole(userRole);
        log.debug("Set user role context: roleId={}, roleName={}, communityId={}", 
                userRole.getId(), 
                userRole.getRole().getName(),
                userRole.getCommunity() != null ? userRole.getCommunity().getId() : "global");
    }
    
    /**
     * Check if the request path is a public route that doesn't require X-User-Role-Id header
     */
    private boolean isPublicRoute(String requestPath) {
        return requestPath.startsWith("/api/v1/auth/") ||
               requestPath.equals("/") ||
               requestPath.equals("/api/v1") ||
               requestPath.startsWith("/actuator/") ||
               requestPath.startsWith("/swagger-ui/") ||
               requestPath.startsWith("/v3/api-docs/") ||
               requestPath.startsWith("/api-docs/") ||
               requestPath.startsWith("/api/v1/users/me") ||
               requestPath.startsWith("/api/v1/users") ||
               requestPath.startsWith("/api/v1/files");
    }
}
