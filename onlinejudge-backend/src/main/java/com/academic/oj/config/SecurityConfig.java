package com.academic.oj.config;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, ex) ->
                            writeErrorResponse(response, objectMapper, ResultCode.UNAUTHORIZED))
                    .accessDeniedHandler((request, response, ex) ->
                            writeErrorResponse(response, objectMapper, ResultCode.FORBIDDEN)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/system/public-configs").permitAll()
                    .requestMatchers(HttpMethod.GET, "/problem/list", "/problem/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/contest/list", "/contest/*", "/contest/*/ranking").permitAll()
                    .requestMatchers(HttpMethod.GET, "/discussion/list", "/discussion/*", "/discussion/*/comments").permitAll()
                    .requestMatchers(HttpMethod.GET, "/submission/points/ranking").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeErrorResponse(HttpServletResponse response, ObjectMapper objectMapper, ResultCode resultCode) {
        response.setStatus(resultCode.getCode());
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(resultCode)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write security response", e);
        }
    }
}

