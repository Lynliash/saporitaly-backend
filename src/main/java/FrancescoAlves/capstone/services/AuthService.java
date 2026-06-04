package FrancescoAlves.capstone.services;


import FrancescoAlves.capstone.payloads.AuthResponse;
import FrancescoAlves.capstone.payloads.LoginRequest;
import FrancescoAlves.capstone.payloads.RegisterRequest;
import FrancescoAlves.capstone.entities.User;
import FrancescoAlves.capstone.enums.UserRole;
import FrancescoAlves.capstone.exceptions.ValidationException;
import FrancescoAlves.capstone.repositories.UserRepository;
import FrancescoAlves.capstone.security.JwtService;
import FrancescoAlves.capstone.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ValidationException("Email gia' registrata");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ValidationException("Username gia' in uso");
        }

        String hash = passwordEncoder.encode(req.getPassword());

        // ruolo BASE forzato: mai leggere il role dal request, altrimenti uno si registra come ADMIN dal body
        User user = User.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .passwordHash(hash)
                .role(UserRole.BASE)
                .build();
        user = userRepository.save(user);
        log.info("Nuovo utente registrato: {} ({})", user.getEmail(), user.getId());

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        // authenticate() fa tutto: carica l'utente, confronta la password con l'hash
        // e se non matcha lancia BadCredentialsException (gestita dal GlobalExceptionHandler)
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        User user = userDetails.getUser();
        log.info("Login OK per {}", user.getEmail());

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
