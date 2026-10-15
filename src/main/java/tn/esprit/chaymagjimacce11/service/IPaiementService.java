package tn.esprit.chaymagjimacce11.service;

import tn.esprit.chaymagjimacce11.domain.Paiement;
import java.util.List;

public interface IPaiementService {
    Paiement findById(Long id);
    List<Paiement> findAll();
}
