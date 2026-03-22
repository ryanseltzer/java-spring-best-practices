package learn.spring_best_practices.config;

import learn.spring_best_practices.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    @SuppressWarnings("java:S4502") // Stateless JWT API — CSRF not applicable
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // A01/A07: CSRF disabled — stateless REST API authenticated via JWT (no session cookies)
                
                .csrf(csrf -> csrf.disable())

                // A07: Enforce stateless sessions — no HttpSession created or used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // All requests must carry a valid JWT.
                        // H2 console and /actuator/health/** are permitted only under the dev
                        // profile via DevSecurityConfig — they are fully blocked here.
                        .anyRequest().authenticated()
                )

                // Explicit 401 for unauthenticated requests — REST APIs must not redirect to login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")))

                // A07: Insert JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // A05: Harden HTTP response headers
                .headers(headers -> headers
                        // A05: Prevent MIME-type sniffing
                        .contentTypeOptions(Customizer.withDefaults())
                        // A02: Enforce HTTPS for future requests
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // Default frame policy is DENY — protects against clickjacking.
                        // Dev profile overrides this to sameOrigin for the H2 console iframe.
                        .frameOptions(Customizer.withDefaults())
                );

        return http.build();
    }
}
