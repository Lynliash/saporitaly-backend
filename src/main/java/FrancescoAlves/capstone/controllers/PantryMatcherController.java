package FrancescoAlves.capstone.controllers;

import FrancescoAlves.capstone.payloads.*;
import FrancescoAlves.capstone.services.PantryMatcherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pantry")
@RequiredArgsConstructor
public class PantryMatcherController {

    private final PantryMatcherService pantryMatcherService;

    // richiede auth (anyRequest().authenticated() in SecurityConfig)
    // body es: { "ingredients": ["pomodorini","mozzarella","basilico"], "minMatchPercent": 60 }
    @PostMapping("/match")
    public PantryMatchResponse match(@Valid @RequestBody PantryMatchRequest req) {
        return pantryMatcherService.match(req);
    }
}
