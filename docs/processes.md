# Processus couverts par la solution

## Acteurs

| Acteur | Rôle |
|---|---|
| **Planificateur** | Prépare la journée de livraison du lendemain : sélectionne les livraisons, lance l'optimisation, valide la solution |
| **Responsable équipes** | Affecte les coursiers aux tournées (hors système — tableau physique) |
| **SI Client** | Système tiers qui gère les commandes ; nous envoie les livraisons à planifier |
| **Notre système** | CyberCommandes — calcule et optimise les tournées |
| **Coursier** | Effectue les livraisons sur le terrain (hors périmètre de notre système) |

---

## 1. Réception des livraisons depuis le SI client

Les livraisons à planifier arrivent du système de gestion des commandes du client. Notre système les stocke et les rend disponibles pour la planification.

```mermaid
sequenceDiagram
    actor SI as SI Client
    participant Sys as Notre système
    actor Planif as Planificateur

    SI->>Sys: Envoie les adresses et livraisons du lendemain
    Sys->>Sys: Stocke les livraisons (statut : non livré)

    Planif->>Sys: Consulte les livraisons disponibles pour demain
    Sys-->>Planif: Liste des livraisons avec adresses et statuts
```

---

## 2. Planification d'une journée de livraison

> Spec : [PlanifierUneJournée](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(0)%20vision-globale/README.md#planifierunejournée-cas-composé) — [ConstruireUneJournée](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(0)%20vision-globale/README.md#construireunejournée-sous-cas-de-planifierunejournée) — [OptimiserUneJournée](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(0)%20vision-globale/README.md#optimiserunejournée-sous-cas-de-planifierunejournée)
> Scénarios : [PlanifierUneJournée-00](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-00.md) · [01](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-01.md) · [02](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-02.md) · [03](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-03.md)

Le planificateur sélectionne les livraisons à inclure, configure les contraintes et déclenche l'optimisation.

```mermaid
sequenceDiagram
    actor Planif as Planificateur
    participant Sys as Notre système

    Planif->>Sys: Sélectionne une date de livraison
    Sys-->>Planif: Liste des livraisons non encore planifiées

    Planif->>Sys: Choisit les livraisons à inclure
    Planif->>Sys: Définit les contraintes (nombre de véhicules, durée max par tournée)
    Planif->>Sys: Lance l'optimisation

    Sys->>Sys: Calcule les tournées optimisées
    Note over Sys: Clustering géographique → optimisation génétique<br/>→ calcul itinéraires réels → exclusion des livraisons hors contraintes

    Sys-->>Planif: Solutions proposées (plusieurs variantes)
```

---

## 3. Comparaison et sélection d'une solution

> Spec : [VisualiserUneJournée](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(0)%20vision-globale/README.md#visualiserunejournée-sous-cas-de-planifierunejournée)
> Scénarios : [PlanifierUneJournée-02](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-02.md) · [03](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-03.md) · [04](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-04.md)

Le planificateur compare les solutions générées et sélectionne la plus adaptée à la réalité terrain.

```mermaid
sequenceDiagram
    actor Planif as Planificateur
    participant Sys as Notre système
    actor RespEq as Responsable équipes

    Sys-->>Planif: Tableau comparatif des solutions (distance, durée, carburant, livraisons effectuées)

    Planif->>Sys: Visualise une solution sur la carte
    Sys-->>Planif: Carte avec les tournées colorées et statistiques

    Note over Planif: Tient compte de contraintes terrain<br/>inconnues du système (travaux, zones bloquées)

    Planif->>Sys: Valide la solution retenue
    Sys-->>Planif: Solution marquée comme choisie pour la journée

    Planif->>RespEq: Communique le nombre de tournées à couvrir
    Note over RespEq: Affecte les équipes aux tournées<br/>(hors système — tableau physique / stickers)
```

---

## 4. Ajustement manuel d'une solution

> Spec : [AjusterUneJournée](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(0)%20vision-globale/README.md#ajusterunejournée-sous-cas-de-planifierunejournée)
> Scénarios : [PlanifierUneJournée-02](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-02.md) · [05](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(1)%20scenarios-utilisateurs/PlanifierUeJourn%C3%A9e/PlanifierUneJourn%C3%A9e-05.md)

Après optimisation, le planificateur peut réorganiser les livraisons dans une tournée pour tenir compte de contraintes terrain.

```mermaid
sequenceDiagram
    actor Planif as Planificateur
    participant Sys as Notre système

    Planif->>Sys: Ouvre une solution pour édition
    Sys-->>Planif: Vue éditeur — tournées avec livraisons ordonnées + carte

    Planif->>Sys: Déplace une livraison dans une autre tournée (ou modifie l'ordre)
    Sys-->>Planif: Itinéraire recalculé en temps réel sur la carte

    Planif->>Sys: Sauvegarde les modifications
    Sys->>Sys: Recalcule les statistiques (distance, durée, carburant)
    Sys-->>Planif: Solution mise à jour avec nouvelles statistiques
```

---

## 5. Exécution des tournées *(hors périmètre — pour situer notre solution)*

> Spec : [EffectuerUneTournée](https://github.com/L3-MIAGE-GRENOBLE/l3m-CyberCommandes-2025-2026/blob/master/(0)%20vision-globale/README.md#effectuerunetournée-cas-composé) *(hors périmètre)*

Le déroulement terrain est géré hors de notre système. Ce processus illustre où s'arrête notre périmètre.

```mermaid
sequenceDiagram
    actor Planif as Planificateur
    participant Sys as Notre système
    actor RespEq as Responsable équipes
    actor Coursier as Coursier

    Planif->>Sys: Consulte la solution validée de la journée
    Sys-->>Planif: Tournées avec ordre des livraisons et adresses

    Planif->>RespEq: Transmet les feuilles de route
    RespEq->>Coursier: Affecte une tournée à chaque équipe

    Coursier->>Coursier: Effectue les livraisons dans l'ordre
    Note over Coursier: Suivi terrain et mise à jour des statuts<br/>gérés par le SI client — hors périmètre

    Coursier->>Planif: Retours terrain (livraisons impossibles, incidents)
    Planif->>Sys: Ajuste la solution si nécessaire en cours de journée
```

---

## Vue d'ensemble

```mermaid
sequenceDiagram
    actor SI as SI Client
    actor Planif as Planificateur
    participant Sys as Notre système
    actor RespEq as Responsable équipes
    actor Coursier as Coursier

    SI->>Sys: Livraisons à planifier
    Planif->>Sys: Sélectionne les livraisons, configure les contraintes
    Planif->>Sys: Lance l'optimisation
    Sys-->>Planif: Solutions optimisées
    Planif->>Sys: Ajuste et valide la solution
    Sys-->>Planif: Tournées finalisées
    Planif->>RespEq: Nombre de tournées à couvrir
    RespEq->>Coursier: Affectation équipes ↔ tournées
    Coursier->>Coursier: Livraisons terrain
```
