package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.endpoints.ParkingEndpoints;
import fr.uga.miage.l3.exceptions.ResourceNotFoundException;
import fr.uga.miage.l3.mappers.ParkingMapper;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.responses.ParkingResponsesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ParkingController implements ParkingEndpoints {

    private final ParkingRepository parkingRepository;
    private final ParkingMapper parkingMapper;

    @Override
    public ParkingResponsesDTO getParking() {
        return parkingRepository.findTopByOrderByIdDesc()
            .map(parkingMapper::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Aucun dépôt n'est configuré"));
    }
}
