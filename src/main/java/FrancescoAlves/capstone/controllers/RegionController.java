package FrancescoAlves.capstone.controllers;

import FrancescoAlves.capstone.payloads.CreateRegionRequest;
import FrancescoAlves.capstone.entities.Region;
import FrancescoAlves.capstone.services.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public List<Region> getAll() {
        return regionService.findAll();
    }

    @GetMapping("/{id}")
    public Region getById(@PathVariable Long id) {
        return regionService.findById(id);
    }

    // scrittura solo ADMIN, regola in SecurityConfig
    @PostMapping
    public ResponseEntity<Region> create(@Valid @RequestBody CreateRegionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(regionService.create(req));
    }

    @PutMapping("/{id}")
    public Region update(@PathVariable Long id, @Valid @RequestBody CreateRegionRequest req) {
        return regionService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
