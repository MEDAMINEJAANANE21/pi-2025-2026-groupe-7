package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.EquipeComponent;
import fr.uga.miage.l3.endpoints.ChauffeurEndpoints;
import fr.uga.miage.l3.mappers.ChauffeurMapper;
import fr.uga.miage.l3.request.ChauffeurRequest;
import fr.uga.miage.l3.responses.ChauffeurDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChauffeurController implements ChauffeurEndpoints {

    private final EquipeComponent equipeComponent;
    private final ChauffeurMapper chauffeurMapper;

    @Override
    public List<ChauffeurDTO> getChauffeurs() {
        return equipeComponent.chauffeurs().stream().map(chauffeurMapper::toDto).toList();
    }

    @Override
    public ChauffeurDTO createChauffeur(ChauffeurRequest request) {
        return chauffeurMapper.toDto(equipeComponent.saveChauffeur(null, request));
    }

    @Override
    public ChauffeurDTO updateChauffeur(Long id, ChauffeurRequest request) {
        return chauffeurMapper.toDto(equipeComponent.saveChauffeur(id, request));
    }

    @Override
    public void deleteChauffeur(Long id) {
        equipeComponent.deleteChauffeur(id);
    }
}
