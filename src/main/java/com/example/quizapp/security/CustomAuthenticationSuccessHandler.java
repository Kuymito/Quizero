package com.example.quizapp.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Collection;

/**
 * This handler redirects the user to a specific dashboard based on their role
 * after a successful login.
 */
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();

            if (authorityName.equals("ROLE_ADMIN")) {
                redirectStrategy.sendRedirect(request, response, "/templates/admin/dashboard");
                return;
            } else if (authorityName.equals("ROLE_TEACHER")) {
                redirectStrategy.sendRedirect(request, response, "/teacher/dashboard");
                return;
            } else if (authorityName.equals("ROLE_STUDENT")) {
                redirectStrategy.sendRedirect(request, response, "/templates/student/dashboard");
                return;
            }
        }

        // Fallback for any other case (shouldn't happen with our setup)
        throw new IllegalStateException("User has no recognizable role.");
    }
}