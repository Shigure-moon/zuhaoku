package com.zhk.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Security 工具类
 * 用于从 SecurityContext 获取当前用户信息
 *
 * @author shigure
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前认证信息
     *
     * @return Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未认证则返回 null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            // 如果 principal 是 UserDetails，尝试从 name 中获取用户ID
            String username = ((UserDetails) principal).getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.warn("无法从用户名解析用户ID: {}", username);
                return null;
            }
        } else if (principal instanceof JwtAuthenticationToken) {
            // 如果 principal 是 JwtAuthenticationToken，直接获取用户ID
            return ((JwtAuthenticationToken) principal).getUserId();
        } else if (principal instanceof String) {
            // 如果 principal 是 String，尝试解析
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                log.warn("无法从 principal 解析用户ID: {}", principal);
                return null;
            }
        }

        // 尝试从 authentication 的 details 中获取
        Object details = authentication.getDetails();
        if (details instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) details).getUserId();
        }

        return null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未认证则返回 null
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }

        return authentication.getName();
    }

    /**
     * 获取当前用户角色
     *
     * @return 角色，如果未认证则返回 null
     */
    public static String getCurrentUserRole() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) principal).getRole();
        }

        // 尝试从 authorities 中获取角色
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    /**
     * 检查当前用户是否已认证
     *
     * @return true 如果已认证，否则 false
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 检查当前用户是否具有指定角色
     *
     * @param role 角色名称
     * @return true 如果具有该角色，否则 false
     */
    public static boolean hasRole(String role) {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String rolePrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(rolePrefix));
    }
}

