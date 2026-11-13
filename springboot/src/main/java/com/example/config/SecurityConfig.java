package com.example.config;

import com.example.security.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(AbstractHttpConfigurer::disable)
            .securityContext(securityContext -> securityContext
                    .requireExplicitSave(false))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/login", "/config/public").permitAll()
                    .requestMatchers("/upload/**").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        if (response.isCommitted()) {
                            return;
                        }
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        Map<String, Object> result = new HashMap<>();
                        result.put("code", "401");
                        result.put("msg", "未登录或登录已过期");
                        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        if (response.isCommitted()) {
                            return;
                        }
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        Map<String, Object> result = new HashMap<>();
                        result.put("code", "403");
                        result.put("msg", "权限不足");
                        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
                    })
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
