package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.component.EquipeComponent;
import fr.uga.miage.l3.endpoints.VehiculeEndpoints;
import fr.uga.miage.l3.mappers.VehiculeMapper;
import fr.uga.miage.l3.request.VehiculeRequest;
import fr.uga.miage.l3.responses.VehiculeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VehiculeController implements VehiculeEndpoints {

    private final EquipeComponent equipeComponent;
    private final VehiculeMapper vehiculeMapper;

    @Override
    public List<VehiculeDTO> getVehicules() {
        return equipeComponent.vehicules().stream().map(vehiculeMapper::toDto).toList();
    }

    @Override
    public VehiculeDTO createVehicule(VehiculeRequest request) {
        return vehiculeMapper.toDto(equipeComponent.saveVehicule(null, request));
    }

    @Override
    public VehiculeDTO updateVehicule(Long id, VehiculeRequest request) {
        return vehiculeMapper.toDto(equipeComponent.saveVehicule(id, request));
    }

    @Override
    public void deleteVehicule(Long id) {
        equipeComponent.deleteVehicule(id);
    }
}
