package com.playvora.playvora_api.component;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedirectUriValidator {

    private final List<String> authorizedRedirectUris;

    public RedirectUriValidator(
            @Value("${application.oauth2.allowed-redirect-uris}") List<String> authorizedRedirectUris) {
        this.authorizedRedirectUris = authorizedRedirectUris;
    }

    public boolean isAuthorizedRedirectUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }

        String candidate = uri.trim();

        return authorizedRedirectUris.stream()
                .filter(allowed -> allowed != null && !allowed.isBlank())
                .map(String::trim)
                .anyMatch(allowed -> {
                    if (allowed.endsWith("*")) {
                        String prefix = allowed.substring(0, allowed.length() - 1);
                        return candidate.startsWith(prefix);
                    }
                    return candidate.equals(allowed);
                });
    }
}


