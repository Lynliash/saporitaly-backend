package FrancescoAlves.capstone.controllers;

import FrancescoAlves.capstone.entities.User;
import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.security.UserDetailsImpl;
import FrancescoAlves.capstone.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // crea sempre utenti BASE
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    // serve al frontend dopo un refresh: ha il token in localStorage ma non i dati utente,
    // li ripesca da qui. token null nella risposta, non ne generiamo uno nuovo
    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        User u = currentUser.getUser();
        return new CurrentUserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }
}
