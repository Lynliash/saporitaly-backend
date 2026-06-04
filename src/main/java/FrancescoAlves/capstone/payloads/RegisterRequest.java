package FrancescoAlves.capstone.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// La registrazione crea sempre utenti BASE (forzato nell'AuthService),
// non ci si puo' auto-registrare come ADMIN o PREMIUM.
@Data
public class RegisterRequest {

    @NotBlank(message = "email obbligatoria")
    @Email(message = "formato email non valido")
    private String email;

    @NotBlank(message = "username obbligatorio")
    @Size(min = 3, max = 50, message = "username tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "password obbligatoria")
    @Size(min = 8, message = "password minimo 8 caratteri")
    private String password;
}