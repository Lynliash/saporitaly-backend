package FrancescoAlves.capstone.payloads;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

// Input dello svuotafrigo: ingredienti dell'utente + parametri di tuning.
@Data
public class PantryMatchRequest {

    // Ingredienti in formato libero ("pomodorini", "mozzarella")
    @NotEmpty(message = "fornire almeno un ingrediente")
    private List<String> ingredients;

    // Soglia minima di match per finire nei risultati
    private Double minMatchPercent = 50.0;

    // Abilita i sostituti (es. stracciatella al posto di burrata)
    private Boolean allowSubstitutions = true;

    // Score minimo perche' un sostituto valga
    private Double minSubstitutionScore = 0.7;

    private Integer limit = 20;
}
