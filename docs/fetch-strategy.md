# Stratégie de fetch et de cascade — AutoLoc

| Association | Fetch | Cascade | Justification |
|---|---|---|---|
| Contrat → Paiement | LAZY | ALL + orphanRemoval | Un paiement n'existe que rattaché à son contrat ; supprimer le contrat supprime ses paiements. |
| Agence → Vehicule | LAZY | Aucune | Un véhicule survit à la suppression de son agence. |
| Agence → Employe | LAZY | Aucune | Un employé a un cycle de vie propre ; aucune suppression en cascade. |
| Vehicule ↔ Equipement | LAZY | Aucune | Les équipements sont partagés entre plusieurs véhicules ; Set pour éviter les doublons. |
| Client → Reservation | LAZY | PERSIST | Enregistrer un client avec de nouvelles réservations les enregistre aussi, mais supprimer un client ne doit pas effacer l'historique. |
| Reservation → Vehicule | LAZY | Aucune | Le véhicule existe indépendamment de la réservation. |
| Reservation ↔ Contrat | LAZY | ALL (côté Reservation) | Le contrat découle de la réservation et n'a pas de sens sans elle. |
| Vehicule → Maintenance | LAZY | PERSIST | Une maintenance est créée avec le véhicule ; pas de suppression en cascade pour garder l'historique. |

## Choix généraux
- `@ManyToOne` et `@OneToOne` sont EAGER par défaut ; ils sont forcés en LAZY pour éviter de charger toute la chaîne d'objets liés.
- `@OneToMany` et `@ManyToMany` restent LAZY, ce qui évite de charger des collections inutilisées.
- Cascade limitée aux relations de composition ; Lombok ciblé sans `@Data` pour éviter les boucles infinies de `toString` / `hashCode`.