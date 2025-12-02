package com.playvora.playvora_api.app;

import com.playvora.playvora_api.user.entities.User;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class AppUserDetail implements UserDetails {
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
       return getGlobalRoles().stream()
       .map(SimpleGrantedAuthority::new)
       .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public  String getPassword() {
        return user.getPassword();
    }

    public Set<String> getGlobalRoles() {
        return user.getUserRoles().stream()
        .filter(userRole -> userRole.isActive() && userRole.getCommunity() == null)
        .map(userRole -> userRole.getRole().getName())
        .collect(Collectors.toSet());
    }
    public Set<String> getCommunityRoles() {
        return user.getUserRoles().stream()
        .filter(userRole -> userRole.isActive() && userRole.getCommunity() != null)
        .map(userRole -> userRole.getRole().getName())
        .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User getUser() {
        return user;
    }
}
