package com.edulearn.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder googleJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
            .authorizeHttpRequests(authz -> authz
                // Permit Swagger UI and OpenAPI documentation endpoints
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources",
                    "/swagger-resources/**",
                    "/actuator/**",
                    "/webjars/**",
                    "/auth/all",        // Use this if your URL is /auth/all
                    "/auth/role/**",
                    "/auth/users/**"
                     
                     
                    
                ).permitAll()
                // Permit auth endpoints
                .requestMatchers("/auth/test","/auth/register", "/auth/login", "/auth/google", "/auth/forgot-password", "/auth/forgot-password/**", "/auth/validate", "/auth/refresh").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
