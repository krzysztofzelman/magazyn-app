package com.example.magazyn.config;

import com.example.magazyn.service.CustomUserDetailsService;
import com.example.magazyn.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public AuditLogFilter auditLogFilter() {
        return new AuditLogFilter();
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowCredentials(true);
                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                config.setAllowedHeaders(List.of("*"));
                config.setMaxAge(3600L);
                source.registerCorsConfiguration("/**", config);
                return config;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/logout").authenticated()
                .requestMatchers("/api/auth/register").hasRole("ADMIN")
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui/**", "/api-docs/**").hasRole("ADMIN")

                // User management — ADMIN only
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Inventory — ADMIN and MANAGER
                .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "MANAGER")

                // Seed data — ADMIN only
                .requestMatchers("/api/seed/**").hasRole("ADMIN")

                // Audit — ADMIN only
                .requestMatchers("/api/audit/**").hasRole("ADMIN")

                // PZ/WZ documents — ADMIN, MANAGER, WAREHOUSE
                .requestMatchers("/api/pz-documents/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")
                .requestMatchers("/api/wz-documents/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")
                .requestMatchers("/api/documents/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")

                // Locations — all roles including VIEWER (read-only)
                .requestMatchers("/api/locations/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE", "VIEWER")

                // Products — all roles including VIEWER (read-only)
                .requestMatchers("/api/products/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE", "VIEWER")

                // Stock — ADMIN, MANAGER, WAREHOUSE
                .requestMatchers("/api/stock/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")

                // Stats/Reports — ADMIN, MANAGER, VIEWER
                .requestMatchers("/api/stats/**").hasAnyRole("ADMIN", "MANAGER", "VIEWER")

                // Batches — ADMIN, MANAGER, WAREHOUSE
                .requestMatchers("/api/batches/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")

                // Contractors — ADMIN, MANAGER, WAREHOUSE
                .requestMatchers("/api/contractors/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")

                // Reservations — ADMIN, MANAGER
                .requestMatchers("/api/reservations/**").hasAnyRole("ADMIN", "MANAGER")

                // Scanner - ADMIN, MANAGER, WAREHOUSE
                .requestMatchers("/api/scanner/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")

                .requestMatchers("/", "/index.html", "/favicon.svg", "/icons.svg", "/assets/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(auditLogFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    byte[] body = ("{\"status\":401,\"message\":\"Token niewa\u017cny lub wygas\u0142\",\"timestamp\":\""
                        + java.time.LocalDateTime.now() + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setContentLength(body.length);
                    response.getOutputStream().write(body);
                    response.getOutputStream().flush();
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    byte[] body = ("{\"status\":403,\"message\":\"Brak uprawnie\u0144\",\"timestamp\":\""
                        + java.time.LocalDateTime.now() + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setContentLength(body.length);
                    response.getOutputStream().write(body);
                    response.getOutputStream().flush();
                })
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
