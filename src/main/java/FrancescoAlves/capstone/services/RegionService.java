package FrancescoAlves.capstone.services;

import FrancescoAlves.capstone.payloads.CreateRegionRequest;
import FrancescoAlves.capstone.entities.Region;
import FrancescoAlves.capstone.exceptions.NotFoundException;
import FrancescoAlves.capstone.exceptions.ValidationException;
import FrancescoAlves.capstone.repositories.RecipeRepository;
import FrancescoAlves.capstone.repositories.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final RegionRepository regionRepository;
    private final RecipeRepository recipeRepository;

    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    public Region findById(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Region", id));
    }

    public Region findByCode(String code) {
        return regionRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Region con code " + code + " non trovata"));
    }

    public Region create(CreateRegionRequest req) {
        if (regionRepository.existsByCode(req.getCode())) {
            throw new ValidationException("Esiste gia' una regione con code " + req.getCode());
        }
        Region region = Region.builder()
                .name(req.getName())
                .code(req.getCode())
                .description(req.getDescription())
                .build();
        region = regionRepository.save(region);
        log.info("Region creata: {} ({})", region.getName(), region.getCode());
        return region;
    }

    public Region update(Long id, CreateRegionRequest req) {
        Region region = findById(id);
        // se cambia il code verifico che non sia gia' di un'altra regione
        if (!region.getCode().equals(req.getCode())
                && regionRepository.existsByCode(req.getCode())) {
            throw new ValidationException("Code gia' in uso da un'altra regione");
        }
        region.setName(req.getName());
        region.setCode(req.getCode());
        region.setDescription(req.getDescription());
        Region updated = regionRepository.save(region);
        log.info("Region {} aggiornata", id);
        return updated;
    }

    public void delete(Long id) {
        Region region = findById(id);
        // niente cascade: meglio bloccare e avvisare l'admin se ci sono ricette collegate
        long recipeCount = recipeRepository.countByRegionId(id);
        if (recipeCount > 0) {
            throw new ValidationException(
                    "Impossibile cancellare regione: " + recipeCount + " ricette ne dipendono. " +
                            "Sposta o cancella prima le ricette."
            );
        }
        regionRepository.delete(region);
        log.info("Region {} cancellata", id);
    }
}