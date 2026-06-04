package FrancescoAlves.capstone.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// BCrypt: salt automatico e costo regolabile. Niente MD5/SHA grezzi sulle password.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 10 di default, ~70ms a hash: lento apposta contro il brute force
        return new BCryptPasswordEncoder();
    }
}