package tn.esprit.chaymagjimacce11.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.chaymagjimacce11.domain.Employe;

public interface IEmployeRepository extends JpaRepository<Employe, Long> {
}
