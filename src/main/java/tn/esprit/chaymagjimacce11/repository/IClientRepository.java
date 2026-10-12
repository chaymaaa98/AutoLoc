package tn.esprit.chaymagjimacce11.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.chaymagjimacce11.domain.Client;

public interface IClientRepository extends JpaRepository<Client, Long> {
}
