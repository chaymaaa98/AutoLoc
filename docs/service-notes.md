# Couche Service, Inversion de Contrôle et Injection de Dépendances — AutoLoc

## A. Modes d'injection comparés (Étape 2)

| Critère | Injection par constructeur (`@RequiredArgsConstructor`) | Injection par attribut (`@Autowired`) |
|---|---|---|
| **Champ `final` possible ?** | **Oui** : les champs `final` doivent être initialisés à la construction. | **Non** : un champ `final` ne peut pas être réaffecté après l'instanciation. |
| **Dépendance visible dans la signature ?** | **Oui** : le constructeur déclare explicitement toutes les dépendances requises. | **Non** : la dépendance est cachée à l'intérieur de la classe. |
| **Utilisation hors conteneur sans dépendance `null` ?** | **Oui** : `new ContratServiceImpl(mockRepo)` garantit que l'objet est complet et utilisable immédiatement en test unitaire. | **Non** : `new ContratServiceImpl()` laisse les champs à `null`, risquant des `NullPointerException`. |
| **Signalement SonarQube for IDE / SonarLint ?** | **Non** : considéré comme la bonne pratique officielle Spring et Clean Code. | **Oui** : règle `java:S6813` (*Field dependency injection should be avoided in favor of constructor injection*). |

---

## B. Services implémentés (Couche Service)

Toutes les interfaces de service sont préfixées par `I` et localisées dans le package `service`. Les implémentations sont annotées avec `@Service`, utilisent l'injection par constructeur via `@RequiredArgsConstructor` de Lombok et dépendent d'interfaces de repository.

| Service | Dépendances injectées | Mode | Justification |
|---|---|---|---|
| `ContratServiceImpl` | `IContratRepository` | Constructeur | CRUD complet, validation du montant non négatif, suppression en cascade des paiements via JPA, protection contre l'écrasement en mise à jour. |
| `AgenceServiceImpl` | `IAgenceRepository` | Constructeur | CRUD complet, validation obligatoire des informations d'agence (nom, ville, adresse non vides). |
| `EmployeServiceImpl` | `IEmployeRepository` | Constructeur | CRUD complet, validation des attributs obligatoires (nom, prénom, rôle non vides). |
| `VehiculeServiceImpl` | `IVehiculeRepository` | Constructeur | CRUD complet, validation du tarif journalier non négatif. |
| `EquipementServiceImpl` | `IEquipementRepository` | Constructeur | CRUD complet, validation du libellé non vide. |
| `ClientServiceImpl` | `IClientRepository` | Constructeur | CRUD complet, validation des attributs obligatoires et d'identification (nom, prénom, e-mail, permis). |
| `ReservationServiceImpl` | `IReservationRepository` | Constructeur | CRUD complet, contrôle de cohérence chronologique (date de fin non antérieure à la date de début). |
| `PaiementServiceImpl` | `IPaiementRepository` | Constructeur | **Lecture seule** (`findById`, `findAll`) : aucune création, modification ou suppression directe car les paiements sont gérés exclusivement via le cycle de vie du `Contrat` (agrégat). |
| `MaintenanceServiceImpl` | `IMaintenanceRepository` | Constructeur | CRUD complet, vérification chronologique (date de fin non antérieure à la date de début). |

---

## C. Diagnostic des messages d'erreur du conteneur Spring (Étape 3)

### Message 1
```text
APPLICATION FAILED TO START
Description:
Parameter 0 of constructor in tn.esprit.autoloc.web.controller.ContratController
required a bean of type 'tn.esprit.autoloc.service.IContratService' that could not be found.
Action:
Consider defining a bean of type 'tn.esprit.autoloc.service.IContratService' in your configuration.
```
* **Exception levée** : `NoSuchBeanDefinitionException` (encapsulée dans une `UnsatisfiedDependencyException`).
* **Bean concerné** : `ContratController` nécessitant une instance de `IContratService`.
* **Deux causes les plus probables côté `ContratServiceImpl`** :
  1. *Oubli d'annotation* : L'annotation de stéréotype `@Service` (ou `@Component`) a été omise sur la classe `ContratServiceImpl`.
  2. *Package hors scan* : La classe `ContratServiceImpl` a été placée dans un package qui n'est pas scanné par `@SpringBootApplication` (en dehors de l'arborescence du package racine `tn.esprit.autoloc`).
* **Correction** : Ajouter `@Service` sur la classe `ContratServiceImpl` et s'assurer qu'elle se trouve bien dans un sous-package du package principal.

### Message 2
```text
Description:
Parameter 0 of constructor in tn.esprit.autoloc.web.controller.NotificationController
required a single bean, but 2 were found:
 - emailNotificateur
 - smsNotificateur
Action:
Consider marking one of the beans as @Primary, updating the consumer to accept
multiple beans, or using @Qualifier to identify the bean that should be consumed
```
* **Exception levée** : `NoUniqueBeanDefinitionException`.
* **Bean concerné** : `NotificationController` injectant une dépendance pour laquelle deux implémentations candidates existent (`emailNotificateur` et `smsNotificateur`).
* **Deux corrections possibles** :
  1. *Utilisation de `@Primary`* : Posée **au-dessus de la classe de l'implémentation choisie par défaut** (par exemple sur `@Component public class EmailNotificateur`), désignant ainsi le candidat prioritaire.
  2. *Utilisation de `@Qualifier("nomDuBean")`* : Posée **sur le paramètre du constructeur** dans la classe consommatrice (`NotificationController(@Qualifier("emailNotificateur") INotificateur notificateur)`), pour cibler explicitement le bean souhaité.

### Message 3
```text
The dependencies of some of the beans in the application context form a cycle:
 clientServiceImpl
 | ^
 v |
 reservationServiceImpl
```
* **Exception levée** : `BeanCurrentlyInCreationException` (dépendance circulaire).
* **Pourquoi l'injection par constructeur rend-elle ce cycle impossible à contourner ?**  
  Pour instancier `ClientServiceImpl`, Spring doit d'abord appeler son constructeur et donc disposer d'une instance déjà construite de `ReservationServiceImpl`. Mais pour instancier `ReservationServiceImpl`, Spring doit appeler son constructeur et donc avoir `ClientServiceImpl`. Aucun des deux objets ne pouvant être instancié avant l'autre, la construction est strictement bloquée à l'instanciation.
* **Piste de conception pour le supprimer** :  
  Refactoriser l'architecture en appliquant le principe de responsabilité unique (SRP) : extraire la logique commune ou croisée dans un service intermédiaire (ex. `ReservationValidationService` ou un composant orchestrateur), ou faire en sorte qu'un service ne dépende que des interfaces de repository nécessaires plutôt que du service pair.

---

## D. Anomalies SonarQube for IDE / SonarLint (Étape 4)

| Règle SonarQube | Type / Explication | Correction apportée |
|---|---|---|
| **`java:S6813`** | *Field dependency injection should be avoided* : L'injection par attribut (`@Autowired private X x;`) masque les dépendances, empêche l'immuabilité (`final`), rend les tests unitaires fragiles et risque des NPE hors conteneur. | Utilisation systématique de champs `private final` et de l'annotation Lombok `@RequiredArgsConstructor` pour générer le constructeur d'injection. |
| **`java:S112`** | *Generic exceptions should never be thrown* : Lever une exception générique comme `RuntimeException` ou `Exception` ne fournit aucune information métier exploitable aux couches supérieures ou aux gestionnaires d'exceptions REST. | Remplacement par l'exception métier dédiée [`ResourceNotFoundException`](file:///c:/Users/chaym/IdeaProjects/arch%20SI/chaymagjimaCCE11/src/main/java/tn/esprit/chaymagjimacce11/exception/ResourceNotFoundException.java) précisant le nom de la ressource et l'identifiant concerné. |
| **`java:S1128`** | *Unused imports should be removed* : Imports non utilisés qui encombrent le code source et réduisent sa lisibilité. | Nettoyage strict des imports dans toutes les classes de services et interfaces créées. |
| **`java:S2160` / Clean Code** | *Subclasses should override "equals"* / Mauvaise utilisation de `@Data` sur les entités liées aux services : `@Data` génère automatiquement `toString`, `equals` et `hashCode` provoquant des récursions infinies en JPA. | Utilisation exclusive des annotations ciblées Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`). |

---

## E. Questions de compréhension (Étape 4 / Section E)

1. **Où est le `new` de `ContratServiceImpl` et qui l'exécute ?**  
   Il n'y a aucun `new ContratServiceImpl(...)` écrit dans le code source de l'application. C'est le conteneur Spring IoC (`ApplicationContext`) qui instancie la classe par réflexion au démarrage lors de la phase d'instanciation des beans scannés.

2. **Pourquoi un contrôleur dépendra-t-il de `IContratService` et non de `ContratServiceImpl` ?**  
   Pour respecter le principe d'inversion des dépendances (DIP des principes SOLID) et favoriser le faible couplage. Le contrôleur dépend ainsi d'une abstraction et non d'une implémentation concrète, ce qui facilite les tests unitaires (mocking aisé) et permet de substituer l'implémentation du service sans impacter le contrôleur.

3. **Pourquoi un service singleton doit-il rester sans état ? Donner un exemple de bug.**  
   Un bean singleton est unique et partagé par l'ensemble des threads exécutant les requêtes HTTP concurrentes dans l'application web. S'il possède un attribut d'instance mutable (ex: `private Contrat contratCourant;`), un thread A traitant la requête de l'utilisateur A peut écraser `contratCourant` pendant qu'un thread B l'utilise pour l'utilisateur B, causant des incohérences majeures de données (race condition).

4. **Pourquoi `update` charge-t-il l'entité existante avant d'appeler `save` au lieu de lui passer directement l'objet reçu ?**  
   Passer directement l'objet reçu à `save()` déclenche un `merge()` qui écraserait toutes les colonnes non fournies (les mettant potentiellement à `null` en base) et, en présence d'`orphanRemoval = true` sur les collections (comme les paiements d'un contrat), supprimerait involontairement tous les enregistrements enfants associés. Le chargement préalable permet de ne mettre à jour que les attributs autorisés et de préserver l'intégrité du graphe d'objets.

5. **Quelle différence entre `@Component` et `@Bean` ? Entre `@Primary` et `@Qualifier` ? Pourquoi `IPaiementService` n'expose-t-il ni création, ni modification, ni suppression ?**  
   * **`@Component` vs `@Bean`** : `@Component` est une annotation posée sur une classe source gérée et découverte automatiquement par scan ; `@Bean` est posée sur une méthode dans une classe `@Configuration` pour enregistrer explicitement une instance (nécessaire pour les classes tierces ou bibliothèques externes qu'on ne peut annoter).  
   * **`@Primary` vs `@Qualifier`** : `@Primary` définit le bean préférentiel par défaut sur sa déclaration en cas d'ambiguïté de type ; `@Qualifier` est posé au point d'injection pour sélectionner précisément un bean par son nom.  
   * **`IPaiementService` en lecture seule** : Un paiement est une entité dépendante forte du contrat (relation de composition). Son cycle de vie complet est gouverné par l'agrégat parent `Contrat`. Toute opération de création, modification ou suppression doit donc transiter par `IContratService` afin de garantir les règles d'intégrité métier et la cohérence de la cascade JPA.
