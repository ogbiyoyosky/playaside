package com.playvora.playvora_api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import com.playvora.playvora_api.component.DynamicCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.playvora.playvora_api.component.CustomOAuth2AuthorizationRequestResolver;
import com.playvora.playvora_api.component.JwtFilter;   
import com.playvora.playvora_api.component.OAuth2AuthenticationFailureHandler;
import com.playvora.playvora_api.component.OAuth2AuthenticationSuccessHandler;
import com.playvora.playvora_api.component.RestAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {
    private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtFilter jwtFilter;
    private final DynamicCorsConfigurationSource dynamicCorsConfigurationSource;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    
    public SecurityConfig(
            CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver,
            @Lazy OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            @Lazy OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
            @Lazy JwtFilter jwtFilter,
            @Lazy DynamicCorsConfigurationSource dynamicCorsConfigurationSource,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.customOAuth2AuthorizationRequestResolver = customOAuth2AuthorizationRequestResolver;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.jwtFilter = jwtFilter;
        this.dynamicCorsConfigurationSource = dynamicCorsConfigurationSource;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Allow static resources first (HTML, CSS, JS, images, etc.)
                        .requestMatchers("/*.html", "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.ico", "/*.woff", "/*.woff2", "/*.ttf", "/*.eot").permitAll()
                        .requestMatchers("/static/**", "/public/**", "/resources/**", "/META-INF/resources/**").permitAll()
                        .requestMatchers("/draft-selection.html", "/websocket-test.html").permitAll()
                        // Allow public endpoints
                        .requestMatchers("/").permitAll()
                        // Public metadata endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/match-events/metadata").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/communities/metadata").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/match-events/{id}/metadata").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/communities/{id}/metadata").permitAll()
                        .requestMatchers("/api/v1").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/password-reset/**").permitAll()
                        .requestMatchers("/api/v1/auth/authorize/**").permitAll()
                        .requestMatchers("/api/v1/users/register").permitAll()
                        // Allow public read access to match events (for viewing draft)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/websocket-test").permitAll()
                        .requestMatchers("/wallet-topup-test").permitAll()
                        .requestMatchers("/wallet-checkout-test").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/files/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**","/api-docs/swagger-config","/api-docs", "/swagger-ui.html").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/v1/payments/apple-pay/domains").permitAll()
                        .requestMatchers("/api/v1/payments/apple-pay/domains/**").permitAll()
                        .requestMatchers("/api/v1/payments/webhooks/stripe").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(dynamicCorsConfigurationSource))
                .oauth2Login(oauth2Login -> oauth2Login
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customOAuth2AuthorizationRequestResolver)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                        .redirectionEndpoint(redirect -> redirect
                                .baseUri("/api/v1/auth/callback/*")
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
