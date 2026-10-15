package tn.esprit.chaymagjimacce11.service;

import tn.esprit.chaymagjimacce11.domain.Employe;
import java.util.List;

public interface IEmployeService {
    Employe create(Employe employe);
    Employe findById(Long id);
    List<Employe> findAll();
    Employe update(Long id, Employe employe);
    void deleteById(Long id);
}
