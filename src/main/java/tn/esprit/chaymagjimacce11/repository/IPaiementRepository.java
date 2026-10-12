package tn.esprit.chaymagjimacce11.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.chaymagjimacce11.domain.Paiement;

public interface IPaiementRepository extends JpaRepository<Paiement, Long> {
}
