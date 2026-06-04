package FrancescoAlves.capstone.payloads;


import FrancescoAlves.capstone.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

// Risposta per /register e /login: token + dati base.
// Il frontend usa role per decidere cosa mostrare.
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private String username;
    private UserRole role;
}
