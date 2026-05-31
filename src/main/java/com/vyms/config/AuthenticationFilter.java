package com.vyms.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Global authentication and authorisation filter.
 *
 * <p>This filter runs before every request and enforces two rules:
 * <ol>
 *   <li><b>Authentication</b> – the user must have a valid session with
 *       {@code currentUserId} set. If not, the request is redirected to
 *       {@code /login}.</li>
 *   <li><b>Authorisation</b> – the user's role stored in
 *       {@code currentUserRole} must match the URL path prefix:
 *       {@code /admin/**} requires {@code ADMIN}, and
 *       {@code /manager/**} requires {@code MANAGER}.</li>
 * </ol>
 *
 * <p>Public paths (login page, static resources, etc.) are whitelisted and
 * always allowed through without a session.
 */
@Component
@Order(1)
public class AuthenticationFilter implements Filter {

    /**
     * Checks whether the requested URI is public and does not need
     * authentication. This covers the login/logout pages, CSS, JS, fonts,
     * images, uploaded files, and the root redirect.
     */
    private boolean isPublicPath(String uri) {
        return uri.equals("/")
                || uri.equals("/login")
                || uri.equals("/logout")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/fonts/")
                || uri.startsWith("/uploads/")
                || uri.startsWith("/images/")
                || uri.equals("/favicon.ico")
                || uri.startsWith("/error");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String uri = httpReq.getRequestURI();

        // ── 1. Allow public paths without any session check ──
        if (isPublicPath(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // ── 2. Authentication check ──
        HttpSession session = httpReq.getSession(false);
        if (session == null || session.getAttribute("currentUserId") == null) {
            httpResp.sendRedirect("/login");
            return;
        }

        // ── 3. Role-based authorisation ──
        String role = (String) session.getAttribute("currentUserRole");

        if (uri.startsWith("/admin")) {
            // Only ADMIN may access /admin/**
            if (!"ADMIN".equalsIgnoreCase(role)) {
                httpResp.sendRedirect("/manager");
                return;
            }
        } else if (uri.startsWith("/manager")) {
            // Only MANAGER may access /manager/**
            if (!"MANAGER".equalsIgnoreCase(role)) {
                httpResp.sendRedirect("/admin");
                return;
            }
        }
        // /profile, /sales/*, /vehicles/*, etc. → any authenticated user is OK

        // ── 4. Request is authenticated and authorised — continue ──
        chain.doFilter(request, response);
    }
}
