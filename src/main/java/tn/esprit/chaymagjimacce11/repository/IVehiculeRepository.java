package tn.esprit.chaymagjimacce11.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.chaymagjimacce11.domain.Vehicule;

public interface IVehiculeRepository extends JpaRepository<Vehicule, Long> {
}
