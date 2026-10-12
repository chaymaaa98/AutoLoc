package tn.esprit.chaymagjimacce11.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.chaymagjimacce11.domain.Reservation;

public interface IReservationRepository extends JpaRepository<Reservation, Long> {
}
