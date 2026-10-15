package tn.esprit.chaymagjimacce11.service;

import tn.esprit.chaymagjimacce11.domain.Vehicule;
import java.util.List;

public interface IVehiculeService {
    Vehicule create(Vehicule vehicule);
    Vehicule findById(Long id);
    List<Vehicule> findAll();
    Vehicule update(Long id, Vehicule vehicule);
    void deleteById(Long id);
}
