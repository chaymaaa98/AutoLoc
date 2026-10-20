# Notes d'Atelier 5 — Spring MVC, API REST et SpringDoc OpenAPI

Document de synthèse de l'Atelier 5 : Exposition des services métier en API REST, gestion des exceptions HTTP, sérialisation Jackson et documentation Swagger / OpenAPI.

---

## A. Endpoints exposés

L'ensemble des routes est préfixé par `/api` (défini de façon centralisée via `server.servlet.context-path=/api` dans `application.properties`).

| Contrôleur | Verbe HTTP | Route | Service appelé | Succès | Erreurs | Description |
|---|---|---|---|---|---|---|
| `AgenceController` | `GET` | `/api/agences` | `IAgenceService.findAll()` | `200 OK` | — | Liste l'ensemble des agences |
| `AgenceController` | `GET` | `/api/agences/{id}` | `IAgenceService.findById(id)` | `200 OK` | `404 Not Found` | Recherche une agence par son identifiant |
| `AgenceController` | `POST` | `/api/agences` | `IAgenceService.create(agence)` | `201 Created` | `400 Bad Request` | Crée une nouvelle agence (champs obligatoires) |
| `AgenceController` | `PUT` | `/api/agences/{id}` | `IAgenceService.update(id, agence)` | `200 OK` | `400 Bad Request`, `404 Not Found` | Met à jour une agence existante |
| `AgenceController` | `DELETE` | `/api/agences/{id}` | `IAgenceService.deleteById(id)` | `204 No Content` | `404 Not Found` | Supprime une agence par son identifiant |
| `VehiculeController` | `GET` | `/api/vehicules` | `IVehiculeService.findAll()` | `200 OK` | — | Liste l'ensemble des véhicules |
| `VehiculeController` | `GET` | `/api/vehicules/{id}` | `IVehiculeService.findById(id)` | `200 OK` | `404 Not Found` | Recherche un véhicule par son identifiant |
| `VehiculeController` | `POST` | `/api/vehicules` | `IVehiculeService.create(vehicule)` | `201 Created` | `400 Bad Request` | Crée un véhicule (tarif positif, sans ID) |
| `VehiculeController` | `PUT` | `/api/vehicules/{id}` | `IVehiculeService.update(id, vehicule)` | `200 OK` | `400 Bad Request`, `404 Not Found` | Met à jour un véhicule existant |
| `VehiculeController` | `DELETE` | `/api/vehicules/{id}` | `IVehiculeService.deleteById(id)` | `204 No Content` | `404 Not Found` | Supprime un véhicule par son identifiant |
| `ClientController` | `GET` | `/api/clients` | `IClientService.findAll()` | `200 OK` | — | Liste l'ensemble des clients |
| `ClientController` | `GET` | `/api/clients/{id}` | `IClientService.findById(id)` | `200 OK` | `404 Not Found` | Recherche un client par son identifiant |
| `ClientController` | `POST` | `/api/clients` | `IClientService.create(client)` | `201 Created` | `400 Bad Request` | Crée un client (nom, email, permis obligatoires) |
| `ClientController` | `PUT` | `/api/clients/{id}` | `IClientService.update(id, client)` | `200 OK` | `400 Bad Request`, `404 Not Found` | Met à jour un client existant |
| `ClientController` | `DELETE` | `/api/clients/{id}` | `IClientService.deleteById(id)` | `204 No Content` | `404 Not Found` | Supprime un client par son identifiant |

---

## B. Annotations Jackson et Sérialisation JSON

### 1. Associations et annotations retenues

| Association | Entité source | Entité cible | Annotation retenue | Justification |
|---|---|---|---|---|
| `Agence` $\leftrightarrow$ `Vehicule` | `Agence.vehicules` | `Vehicule.agence` | `@JsonIgnore` côté `Agence` | Le véhicule est géré indépendamment et désigne son agence par son identifiant (`{"agence": {"idAgence": 1}}`). `@JsonIgnore` coupe la boucle infinie sans empêcher l'association. |
| `Agence` $\leftrightarrow$ `Employe` | `Agence.employes` | `Employe.agence` | `@JsonIgnore` côté `Agence` | Évite de charger et sérialiser récursivement la liste complète des employés lors de la consultation d'une agence. |
| `Vehicule` $\leftrightarrow$ `Maintenance` | `Vehicule.maintenances` | `Maintenance.vehicule` | `@JsonIgnore` côté `Vehicule` | Évite la boucle de sérialisation et le chargement LAZY intempestif des maintenances. |
| `Vehicule` $\leftrightarrow$ `Reservation` | `Vehicule.reservations` | `Reservation.vehicule` | `@JsonIgnore` côté `Vehicule` | Évite d'embarquer tout l'historique des réservations dans la réponse d'un véhicule. |
| `Client` $\leftrightarrow$ `Reservation` | `Client.reservations` | `Reservation.client` | `@JsonIgnore` côté `Client` | Évite de sérialiser les réservations imbriquées avec boucle retour vers le client. |
| `Contrat` $\leftrightarrow$ `Paiement` | `Contrat.paiements` | `Paiement.contrat` | `@JsonManagedReference` côté `Contrat`<br>`@JsonBackReference` côté `Paiement` | Véritable composition : les paiements naissent et sont transmis directement dans le JSON du contrat (cascade JPA `ALL`). La référence inverse est restaurée à la désérialisation. |
| Proxies Hibernate LAZY | Classes entités (`Agence`, `Vehicule`, `Client`) | — | `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` | Évite l'exception `No serializer found for class ByteBuddyInterceptor` lorsque Jackson tente de sérialiser une référence LAZY non initialisée. |

### 2. Différence entre `@JsonBackReference` et `@JsonIgnore`

* **`@JsonBackReference`** :
  * Fonctionne toujours en binôme avec `@JsonManagedReference`.
  * Utilisé dans les relations de **composition forte** parent-enfant (ex. `Contrat` $\leftrightarrow$ `Paiement`).
  * À la sérialisation (Java $\to$ JSON), Jackson omet le champ annoté pour éviter la boucle infinie.
  * À la désérialisation (JSON $\to$ Java), Jackson **reconstitue automatiquement le lien** vers l'objet parent dans l'objet enfant.

* **`@JsonIgnore`** :
  * Omet purement et simplement le champ annoté, tant à la sérialisation qu'à la désérialisation.
  * Utilisé lorsque l'entité enfant est gérée de manière autonome et fait référence au parent par un simple identifiant imbriqué (ex. `{"agence": {"idAgence": 1}}`).

---

## C. Postman et Swagger

| Critère | Postman | Swagger UI / OpenAPI (SpringDoc) |
|---|---|---|
| **Rôle principal** | Outil client de **test**, d'automatisation et de validation fonctionnelle. | Outil de **documentation**, de contrat d'interface et d'exploration interactive. |
| **Point fort** | Création de scénarios enchaînés, scripts d'assertions de tests (`pm.test()`), variables d'environnement (`{{baseUrl}}`), rejouabilité. | Synchronisation automatique avec le code Java (annotations), aperçu des schémas JSON, documentation interactive accessible aux clients de l'API. |
| **Exemple concret rencontré dans l'atelier** | Vérifier qu'un `POST /api/agences` avec un nom vide renvoie un statut HTTP `400` avec le message explicite via `pm.test("Statut 400", () => pm.response.to.have.status(400));`. | Consulter l'interface graphique sur `http://localhost:8081/api/swagger-ui.html`, visualiser les codes de statut documentés (`200`, `201`, `204`, `400`, `404`) et exécuter directement un `GET /api/vehicules` via le bouton **"Try it out"**. |

---

## D. Analyse SonarQube for IDE

| Règle SonarQube | Explication | Correction appliquée |
|---|---|---|
| **`java:S6813`**<br>*Constructor injection should be used instead of field injection* | L'injection par champ (`@Autowired private IService service;`) masque les dépendances obligatoires du composant, empêche de déclarer les champs `final` et rend les tests unitaires hors conteneur difficiles (risque de `NullPointerException`). | Déclaration des dépendances en `private final` et utilisation de l'injection par constructeur générée par Lombok (`@RequiredArgsConstructor`) sur chaque contrôleur REST. |
| **`java:S1128`**<br>*Unused imports should be removed* | Les imports inutilisés encombrent le code source, alourdissent la maintenance et dégradent la lisibilité. | Nettoyage systématique des imports superflus dans l'ensemble des classes du package `web.controller`. |
| **`java:S4684`**<br>*Persistent entities should not be used as arguments or return types in `@RequestMapping` methods* | L'exposition directe des entités JPA dans les contrôleurs REST couple fortement le schéma de la base de données au contrat d'API publique et expose des données internes ou sensibles. | Prise en compte du constat pour l'étape de transition : protection provisoire via Jackson (`@JsonIgnore`, `@JsonIgnoreProperties`), en attendant la mise en place formelle des DTO (Data Transfer Objects) au Chapitre 10. |

---

## E. Questions de compréhension

### 1. Quel composant reçoit toutes les requêtes, et comment retrouve-t-il la méthode du contrôleur à appeler ?
Toutes les requêtes HTTP sont interceptées par le **`DispatcherServlet`** (Front Controller de Spring MVC). Pour identifier la méthode de contrôleur à appeler, il consulte le **`HandlerMapping`** (plus particulièrement `RequestMappingHandlerMapping`), qui mappe l'URL, la méthode HTTP (GET, POST, etc.) et les paramètres sur la méthode correspondante du contrôleur.

### 2. Pourquoi `ContratController` dépend-il de `IContratService` et non de `ContratRepository` ni de `ContratServiceImpl` ?
* Il dépend de l'interface **`IContratService`** (et non de son implémentation `ContratServiceImpl`) pour respecter le principe d'inversion des dépendances (DIP des principes SOLID), assurant un couplage lâche et facilitant le bouchonnage (*mocking*) lors des tests.
* Il ne dépend pas de **`ContratRepository`** pour respecter l'architecture en couches : la couche Web ne doit pas accéder directement aux données, mais déléguer la logique métier, la cohérence et les validations à la couche Service.

### 3. Pourquoi un POST renvoie-t-il 201 et un DELETE 204 ? Pourquoi une ressource absente donne-t-elle 404 et non 500 ?
* **`201 Created`** indique formellement qu'une nouvelle ressource a été persistée avec succès sur le serveur, renvoyant généralement son URI ou son contenu avec l'ID attribué.
* **`204 No Content`** indique que la commande de suppression a été exécutée avec succès et qu'il n'y a aucun contenu résiduel à renvoyer dans le corps de la réponse.
* Une ressource absente renvoie **`404 Not Found`** car il s'agit d'une erreur côté client (l'identifiant demandé n'existe pas dans le système). Le code **`500 Internal Server Error`** est réservé aux défaillances inattendues ou bugs du serveur.

### 4. Pourquoi `PaiementController` n'expose-t-il que des GET ? Quelle réponse reçoit un POST `/api/paiements` ?
* `PaiementController` n'expose que des `GET` car les paiements dépendent strictement du cycle de vie de leur `Contrat` (agrégat JPA géré en cascade `CascadeType.ALL`). Un paiement n'a pas de sens métier isolé sans son contrat rattaché.
* Un appel `POST /api/paiements` reçoit une réponse HTTP **`405 Method Not Allowed`**, car aucune route de création n'est acceptée par le contrôleur.

### 5. Quels risques présente l'exposition directe des entités JPA ? Pourquoi `Contrat.paiements` utilise-t-il `@JsonManagedReference` alors que `Agence.vehicules` utilise `@JsonIgnore` ?
* **Risques de l'exposition directe des entités JPA** :
  1. Couplage rigide entre le modèle relationnel de la base et le contrat de l'API publique.
  2. Fuite involontaire de données internes ou sensibles.
  3. Risque d'attaques par assignation de masse (*Over-Posting / Mass Assignment*).
  4. Problèmes techniques de sérialisation : boucles infinies ou exceptions liées aux proxies LAZY (`ByteBuddyInterceptor`).
* **`Contrat.paiements` vs `Agence.vehicules`** :
  * `Contrat.paiements` forme une **composition** : les paiements sont transmis imbriqués lors de la création du contrat. `@JsonManagedReference` permet de sérialiser les paiements tout en restaurant la référence du parent lors de la désérialisation.
  * `Agence.vehicules` représente une relation unitaire autonome : les véhicules sont créés indépendamment en ciblant l'agence par son ID. `@JsonIgnore` évite de charger l'ensemble des véhicules à chaque consultation d'agence et supprime la boucle de sérialisation.
