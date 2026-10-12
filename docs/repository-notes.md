# Couche Repository Spring Data JPA & Qualité de code — AutoLoc

## 1. Choix des interfaces Repository

Toutes les interfaces de la couche persistance sont regroupées dans le package `repository` et suivent la convention de nommage avec le préfixe `I` (`IVehiculeRepository`, `IContratRepository`...). Elles étendent toutes `JpaRepository<T, Long>`, qui constitue le choix optimal pour AutoLoc : CRUD complet, renvoi direct de `List` au lieu d'`Iterable`, support natif du tri et de la pagination, et disponibilité des opérations avancées de persistance (`flush`, `saveAndFlush`, `getReferenceById`).

| Interface | Étend | Justification |
|---|---|---|
| `IContratRepository` | `JpaRepository<Contrat, Long>` | CRUD complet, `findAll` renvoie une `List`, `saveAndFlush` disponible pour persister immédiatement avant la génération de factures ou de paiements. |
| `IAgenceRepository` | `JpaRepository<Agence, Long>` | CRUD complet, renvoi de `List`, support de la pagination et du tri des agences par ville ou nom. |
| `IEmployeRepository` | `JpaRepository<Employe, Long>` | CRUD complet, manipulation simplifiée des listes d'employés (`ListCrudRepository`), tri par rôle et agence. |
| `IVehiculeRepository` | `JpaRepository<Vehicule, Long>` | CRUD complet, pagination et tri par marque/catégorie/tarif, requêtes de synchronisation avec la base. |
| `IEquipementRepository` | `JpaRepository<Equipement, Long>` | CRUD complet, manipulation en `List` pour l'affectation aux véhicules, méthodes de persistance immédiate. |
| `IClientRepository` | `JpaRepository<Client, Long>` | CRUD complet, pagination pour les listes volumineuses de clients, `getReferenceById` pour rattacher un client sans chargement inutile de son état complet. |
| `IReservationRepository` | `JpaRepository<Reservation, Long>` | CRUD complet, tri chronologique sur les dates de réservation, pagination de l'historique et synchronisation via `flush`. |
| `IPaiementRepository` | `JpaRepository<Paiement, Long>` | Permet la lecture directe et l'interrogation des paiements. Bien que le cycle de vie soit géré via `Contrat` (agrégat/composition), un repository dédié est requis pour la consultation et le reporting financier. |
| `IMaintenanceRepository` | `JpaRepository<Maintenance, Long>` | CRUD complet, tri et pagination des historiques d'interventions de maintenance sur le parc automobile. |

---

## 2. Principes architecturaux et bonnes pratiques

### Hiérarchie Spring Data JPA
- **`Repository<T, ID>`** : Interface marqueur vide. Utile uniquement pour restreindre l'accès à un sous-ensemble minimal de méthodes définies manuellement.
- **`CrudRepository<T, ID>`** : Fournit le CRUD de base, mais ses méthodes de collection (`findAll`, `saveAll`) renvoient un `Iterable<T>`.
- **`ListCrudRepository<T, ID>`** : Variante moderne (Spring Data 3+) renvoyant des `List<T>`, bien plus commode à manipuler (`size()`, indexation, Stream API).
- **`PagingAndSortingRepository<T, ID>`** : Gère la pagination (`Page<T>`, `Pageable`) et le tri (`Sort`). *Depuis Spring Data 3, cette interface n'étend plus `CrudRepository`*.
- **`JpaRepository<T, ID>`** : Réunit `ListCrudRepository`, `ListPagingAndSortingRepository`, `QueryByExampleExecutor` et ajoute les fonctionnalités propres à JPA (`flush()`, `saveAndFlush()`, `getReferenceById()`).

### Gestion du cycle de vie et suppressions
- **Persistance & fusion (`save`)** :
  - Identifiant `null` : déclenche `persist` (instruction SQL `INSERT`). L'identifiant généré par MySQL (`IDENTITY`) est automatiquement alimenté dans l'objet retourné.
  - Identifiant renseigné : déclenche `merge` (instruction SQL `SELECT` puis `UPDATE`). Il convient toujours d'exploiter l'instance retournée par `save()`.
- **Limites des suppressions en lot (`deleteAllInBatch`, `deleteAllByIdInBatch`)** :
  - Ces opérations exécutent une seule requête SQL `DELETE` directe sans charger les entités dans le contexte de persistance Hibernate.
  - En conséquence, les mécanismes de **cascade (`CascadeType.REMOVE`, `CascadeType.ALL`)** et d'**`orphanRemoval` ne sont PAS déclenchés**.
  - L'utilisation de ces méthodes sur `Contrat` conduirait à violer les contraintes d'intégrité référentielle de clé étrangère sur la table `paiement` ou à laisser des paiements orphelins. Les suppressions unitaires via `deleteById()` ou `delete()` sont donc privilégiées afin de respecter le graphe d'objets.

---

## 3. Analyse de la qualité du code (SonarQube for IDE / SonarLint)

L'analyse statique menée avec SonarQube for IDE a permis d'identifier et corriger plusieurs anomalies dans les entités du package `domain` et les interfaces du package `repository` :

| Anomalie SonarQube for IDE | Règle / explication | Correction apportée |
|---|---|---|
| **Imports inutilisés dans les entités** (`java.util.HashSet`, `java.util.ArrayList`, etc.) | **`java:S1128`** (*Unused imports should be removed*) : Des imports non utilisés polluent le code source, diminuent la lisibilité et augmentent la charge cognitive lors de la relecture. | Suppression de tous les imports superflus dans l'ensemble des entités (`Employe`, `Agence`, `Client`, `Vehicule`, `Reservation`, `Paiement`, `Maintenance`, `Equipement`, `Contrat`). |
| **Imports génériques avec astérisque / Wildcard** (`import jakarta.persistence.*`) | **`java:S2208`** (*Wildcard imports should not be used*) : L'importation globale masque les dépendances réelles de la classe et peut introduire des conflits de noms lors des montées de version. | Remplacement systématique des imports `*` par des imports explicites et ciblés (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@OneToMany`, etc.). |
| **Déclarations d'import redondantes / dupliquées** | **`java:S1128`** (*Redundant import statements*) : Plusieurs fichiers importaient simultanément des types spécifiques et le wildcard du même package, ou contenaient des doublons de directives d'import. | Suppression des déclarations redondantes et organisation méthodique des blocs d'import. |
| **Prévention des boucles infinies JPA via Lombok** (absence de `@Data`) | **`java:S2160` / Bonnes pratiques JPA Clean Code** : L'utilisation de `@Data` sur des entités JPA génère automatiquement `equals`, `hashCode` et `toString` sur tous les champs, provoquant des récursions infinies (`StackOverflowError`) et des requêtes SQL N+1 sur les relations bidirectionnelles. | Maintien exclusif des annotations ciblées : `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` sans aucun recours à `@Data`. |
| **Stéréotype inutile `@Repository` sur les interfaces Spring Data** | **`java:S4042` / Recommandation Spring Boot** : L'annotation `@Repository` est superflue sur les interfaces étendant `JpaRepository` car Spring Data instancie déjà dynamiquement le proxy au démarrage. | Interfaces définies de façon concise sans annotation `@Repository`, respectant la convention de nommage `I...Repository`. |
