package com.zhk.common.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证 Token
 * 用于在 SecurityContext 中存储 JWT 认证信息
 *
 * @author shigure
 */
@Getter
public class JwtAuthenticationToken implements Authentication {

    private final Long userId;
    private final String username;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;
    private boolean authenticated = true;

    public JwtAuthenticationToken(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        // 将角色转换为 GrantedAuthority
        this.authorities = role != null 
            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
            : Collections.emptyList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return this;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return username;
    }
}

