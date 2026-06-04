package FrancescoAlves.capstone.payloads;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

// Ricette matchate + input non riconosciuti, cosi' la UI puo' avvisare
// (es. "non conosciamo 'fior di latte'").
@Data
@Builder
@AllArgsConstructor
public class PantryMatchResponse {
    private List<RecipeMatchResult> matches;
    private List<String> unrecognizedInputs;
    private Integer totalCandidatesEvaluated;
}
