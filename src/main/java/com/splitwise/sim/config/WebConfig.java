package com.splitwise.sim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    /**
     * CORS Configuration for WebMVC
     * This handles CORS for @Controller endpoints
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")  // Allow all origins (for development)
                        .allowedOrigins(
                                "http://localhost:5173",      // Vite default
                                "http://localhost:5174",      // Vite alternative
                                "http://localhost:3000",      // React/Next.js
                                "http://localhost:4173",      // Vite preview
                                "http://127.0.0.1:5173",      // Localhost alternative
                                "https://splitwise-sim.onrender.com",
                                "https://*.vercel.app",       // Vercel deployments
                                "https://*.netlify.app"       // Netlify deployments
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Content-Disposition")
                        .allowCredentials(true)
                        .maxAge(3600); // Cache preflight for 1 hour
            }
        };
    }

    /**
     * CORS Configuration Source
     * This provides fine-grained CORS control
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins for development (remove in production)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Or specify exact origins for production
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3000",
                "http://localhost:4173",
                "http://127.0.0.1:5173",
                "https://splitwise-sim.onrender.com"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * CORS Filter Bean
     * Ensures CORS is applied to all requests
     */
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}