package FrancescoAlves.capstone.payloads;

import FrancescoAlves.capstone.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

// Risposta di GET /auth/me: dati utente senza token (il client ce l'ha gia').
@Data
@AllArgsConstructor
public class CurrentUserResponse {
    private UUID id;
    private String username;
    private String email;
    private UserRole role;
}
