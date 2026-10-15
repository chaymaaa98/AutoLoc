package tn.esprit.chaymagjimacce11.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String ressource, Long id) {
        super(ressource + " introuvable avec l'identifiant " + id);
    }
}
