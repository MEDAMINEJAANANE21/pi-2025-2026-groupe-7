package fr.uga.miage.l3.component;

import fr.uga.miage.l3.models.entities.JourneeEntity;
import fr.uga.miage.l3.repository.JourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class JourneeComponent {
  private final JourneeRepository journeeRepository;

  public JourneeEntity getOrCreate(LocalDate date) {
      return journeeRepository.findById(date)
          .orElseGet(() -> journeeRepository.save(
              JourneeEntity
                  .builder()
                  .date(date)
                  .build()
          ));
  }
}
