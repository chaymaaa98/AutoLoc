package tn.esprit.chaymagjimacce11.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.chaymagjimacce11.domain.Client;
import tn.esprit.chaymagjimacce11.exception.ResourceNotFoundException;
import tn.esprit.chaymagjimacce11.repository.IClientRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements IClientService {

    private final IClientRepository clientRepository;

    private void validerClient(Client client) {
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du client ne peut pas être vide");
        }
        if (client.getPrenom() == null || client.getPrenom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom du client ne peut pas être vide");
        }
        if (client.getEmail() == null || client.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'e-mail du client ne peut pas être vide");
        }
        if (client.getNumPermis() == null || client.getNumPermis().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de permis du client ne peut pas être vide");
        }
    }

    @Override
    public Client create(Client client) {
        if (client.getIdClient() != null) {
            throw new IllegalArgumentException("Un nouveau client ne doit pas avoir d'identifiant");
        }
        validerClient(client);
        return clientRepository.save(client);
    }

    @Override
    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Override
    public Client update(Long id, Client client) {
        Client existant = findById(id);
        validerClient(client);
        existant.setNom(client.getNom());
        existant.setPrenom(client.getPrenom());
        existant.setEmail(client.getEmail());
        existant.setTelephone(client.getTelephone());
        existant.setNumPermis(client.getNumPermis());
        existant.setDateInscription(client.getDateInscription());
        return clientRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client", id);
        }
        clientRepository.deleteById(id);
    }
}
