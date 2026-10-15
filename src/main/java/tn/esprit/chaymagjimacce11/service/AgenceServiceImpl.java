package tn.esprit.chaymagjimacce11.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.chaymagjimacce11.domain.Agence;
import tn.esprit.chaymagjimacce11.exception.ResourceNotFoundException;
import tn.esprit.chaymagjimacce11.repository.IAgenceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenceServiceImpl implements IAgenceService {

    private final IAgenceRepository agenceRepository;

    private void validerAgence(Agence agence) {
        if (agence.getNom() == null || agence.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'agence ne peut pas être vide");
        }
        if (agence.getVille() == null || agence.getVille().trim().isEmpty()) {
            throw new IllegalArgumentException("La ville de l'agence ne peut pas être vide");
        }
        if (agence.getAdresse() == null || agence.getAdresse().trim().isEmpty()) {
            throw new IllegalArgumentException("L'adresse de l'agence ne peut pas être vide");
        }
    }

    @Override
    public Agence create(Agence agence) {
        if (agence.getIdAgence() != null) {
            throw new IllegalArgumentException("Une nouvelle agence ne doit pas avoir d'identifiant");
        }
        validerAgence(agence);
        return agenceRepository.save(agence);
    }

    @Override
    public Agence findById(Long id) {
        return agenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agence", id));
    }

    @Override
    public List<Agence> findAll() {
        return agenceRepository.findAll();
    }

    @Override
    public Agence update(Long id, Agence agence) {
        Agence existante = findById(id);
        validerAgence(agence);
        existante.setNom(agence.getNom());
        existante.setVille(agence.getVille());
        existante.setAdresse(agence.getAdresse());
        existante.setTelephone(agence.getTelephone());
        return agenceRepository.save(existante);
    }

    @Override
    public void deleteById(Long id) {
        if (!agenceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Agence", id);
        }
        agenceRepository.deleteById(id);
    }
}
