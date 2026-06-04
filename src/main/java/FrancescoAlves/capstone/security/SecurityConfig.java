package FrancescoAlves.capstone.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// @EnableMethodSecurity abilita @PreAuthorize sui metodi.
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // origini CORS separate da virgola, in prod si sovrascrive con FRONTEND_URL
    @Value("${FRONTEND_URL:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API stateless: niente cookie, niente CSRF
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // niente HttpSession, ogni request si autentica col suo JWT
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // endpoint pubblici
                        .requestMatchers("/auth/**").permitAll()
                        // l'archivio e' consultabile senza login
                        .requestMatchers(HttpMethod.GET, "/recipes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/regions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ingredients/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reviews/by-recipe/**").permitAll()
                        // serve al dettaglio ricetta; create/delete restano ADMIN sotto
                        .requestMatchers(HttpMethod.GET, "/substitutions").permitAll()

                        // scrittura archivio: solo ADMIN
                        .requestMatchers(HttpMethod.POST, "/recipes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/recipes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/recipes/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/regions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/regions/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/regions/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/ingredients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/ingredients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ingredients/**").hasRole("ADMIN")

                        .requestMatchers("/aliases/**").hasRole("ADMIN")
                        .requestMatchers("/substitutions/**").hasRole("ADMIN")

                        // tutto il resto richiede login
                        .anyRequest().authenticated())
                // il filtro JWT va prima di quello standard username/password
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // serve a AuthService per fare il login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}