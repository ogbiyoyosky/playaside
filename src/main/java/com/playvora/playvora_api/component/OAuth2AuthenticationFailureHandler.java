package com.playvora.playvora_api.component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${application.frontend.url}")
    private String appUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // Create a redirect URL to frontend with error information
        String redirectUrl = UriComponentsBuilder.fromUriString(appUrl)
                .path("/login")
                .queryParam("message", exception.getLocalizedMessage())
                .build().toUriString();

        // Redirect to the frontend with the error information
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}