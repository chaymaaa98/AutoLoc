package tn.esprit.chaymagjimacce11.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.chaymagjimacce11.domain.Equipement;
import tn.esprit.chaymagjimacce11.exception.ResourceNotFoundException;
import tn.esprit.chaymagjimacce11.repository.IEquipementRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipementServiceImpl implements IEquipementService {

    private final IEquipementRepository equipementRepository;

    private void validerEquipement(Equipement equipement) {
        if (equipement.getLibelle() == null || equipement.getLibelle().trim().isEmpty()) {
            throw new IllegalArgumentException("Le libellé de l'équipement ne peut pas être vide");
        }
    }

    @Override
    public Equipement create(Equipement equipement) {
        if (equipement.getIdEquipement() != null) {
            throw new IllegalArgumentException("Un nouvel équipement ne doit pas avoir d'identifiant");
        }
        validerEquipement(equipement);
        return equipementRepository.save(equipement);
    }

    @Override
    public Equipement findById(Long id) {
        return equipementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipement", id));
    }

    @Override
    public List<Equipement> findAll() {
        return equipementRepository.findAll();
    }

    @Override
    public Equipement update(Long id, Equipement equipement) {
        Equipement existant = findById(id);
        validerEquipement(equipement);
        existant.setLibelle(equipement.getLibelle());
        return equipementRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!equipementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Equipement", id);
        }
        equipementRepository.deleteById(id);
    }
}
