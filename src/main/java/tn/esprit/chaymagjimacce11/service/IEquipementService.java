package tn.esprit.chaymagjimacce11.service;

import tn.esprit.chaymagjimacce11.domain.Equipement;
import java.util.List;

public interface IEquipementService {
    Equipement create(Equipement equipement);
    Equipement findById(Long id);
    List<Equipement> findAll();
    Equipement update(Long id, Equipement equipement);
    void deleteById(Long id);
}
