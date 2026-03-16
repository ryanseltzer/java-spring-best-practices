package learn.spring_best_practices.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permits the H2 console and Actuator health endpoints without authentication.
 * This filter chain is scoped to the {@code dev} profile only — these paths are
 * fully blocked by {@link SecurityConfig} in all other environments.
 *
 * {@code @Order(1)} ensures this chain is evaluated before the main chain so
 * that the H2 console and health matchers take precedence.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain devFilterChain(HttpSecurity http) {
        http
                .securityMatcher("/h2-console/**", "/actuator/health/**")
                .csrf(csrf -> csrf.disable())
                // Allow same-origin frames so the H2 console iframe renders correctly
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
