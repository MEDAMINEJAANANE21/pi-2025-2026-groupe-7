package fr.uga.miage.l3.controllers;

import fr.uga.miage.l3.endpoints.ParkingEndpoints;
import fr.uga.miage.l3.exceptions.rest.BadRequestRestException;
import fr.uga.miage.l3.exceptions.rest.NotFoundElementRestException;
import fr.uga.miage.l3.mappers.AdresseMapper;
import fr.uga.miage.l3.models.entities.AdresseEntity;
import fr.uga.miage.l3.models.entities.ParkingEntity;
import fr.uga.miage.l3.repository.AdresseRepository;
import fr.uga.miage.l3.repository.ParkingRepository;
import fr.uga.miage.l3.request.ParkingCreationRequest;
import fr.uga.miage.l3.responses.ParkingResponsesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ParkingController implements ParkingEndpoints {

  private final ParkingRepository parkingRepository;
  private final AdresseRepository adresseRepository;
  private final AdresseMapper adresseMapper;

  @Override
  public ParkingResponsesDTO getParking() {
    ParkingEntity parking = parkingRepository.findTopByOrderByIdDesc()
      .orElseThrow(() -> new NotFoundElementRestException("No parking configured"));
    return new ParkingResponsesDTO(adresseMapper.toDTO(parking.getAdresse()));
  }
}
