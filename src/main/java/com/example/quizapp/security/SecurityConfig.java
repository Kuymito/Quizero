package com.example.quizapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// Removed unused import: import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // This bean is no longer needed as the logic is handled inline below
    // @Bean
    // public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
    //     return new CustomAuthenticationSuccessHandler();
    // }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // CRITICAL FIX: Match against controller URLs, not template paths
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/student/**").hasRole("STUDENT")

                        // Allow access to public pages (login, home, static resources)
                        .requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        // This inline success handler is correct and redirects to URLs
                        .successHandler((request, response, authentication) -> {
                            // Get the user's role
                            String authority = authentication.getAuthorities().stream()
                                    .findFirst()
                                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                                    .orElse("ROLE_STUDENT"); // Default

                            String redirectUrl = switch (authority) {
                                case "ROLE_ADMIN" -> "/admin/dashboard";
                                case "ROLE_TEACHER" -> "/teacher/dashboard";
                                default -> "/student/dashboard";
                            };

                            response.sendRedirect(request.getContextPath() + redirectUrl);
                        })
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }
}