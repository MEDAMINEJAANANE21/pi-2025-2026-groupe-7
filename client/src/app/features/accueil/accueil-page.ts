import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LivraisonsApi } from '../../core/api/livraisons-api';
import { today } from '../../core/util/dates';
import { Icon, IconName } from '../../shared/icon';
import { RouteIllustration } from './route-illustration';

interface Step {
  title: string;
  text: string;
}

interface Feature {
  icon: IconName;
  title: string;
  text: string;
}

const STEPS: Step[] = [
  { title: 'Préparer', text: 'Saisissez les livraisons à partir de la Base Adresse Nationale, avec leur poids et leur temps de montage.' },
  { title: 'Optimiser', text: 'Indiquez vos véhicules et la durée maximale d’une tournée : une solution est calculée pour chaque stratégie.' },
  { title: 'Ajuster', text: 'Comparez distance, durée et marge, retenez le meilleur plan puis déplacez une livraison si besoin.' },
  { title: 'Livrer', text: 'Affectez chauffeurs et véhicules, imprimez les feuilles de route et clôturez les tournées terminées.' },
];

const FEATURES: Feature[] = [
  { icon: 'route', title: 'Trois moteurs d’itinéraire', text: 'VROOM, algorithme génétique ou plus proche voisin, combinés à un découpage par secteurs ou par quartiers.' },
  { icon: 'refresh', title: 'Suivi en direct', text: 'Les solutions apparaissent au fil du calcul, sans recharger la page.' },
  { icon: 'euro', title: 'Coût et marge', text: 'Carburant, heures payées et recette facturée, pour chaque tournée et pour la journée.' },
  { icon: 'users', title: 'Équipe et flotte', text: 'Chauffeurs, véhicules et photos. Un chargement trop lourd ou un chauffeur déjà pris est refusé.' },
  { icon: 'printer', title: 'Feuille de route', text: 'Une page A4 par chauffeur, ou un export CSV qui s’ouvre directement dans Excel.' },
  { icon: 'pin', title: 'Prêt sans configuration', text: 'Sans clé d’API, les distances sont estimées : l’application fonctionne dès l’installation.' },
];

/** Page d'accueil : ce que fait CyberCommandes, pour qui, et comment s'en servir. */
@Component({
  selector: 'app-accueil-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, RouteIllustration],
  templateUrl: './accueil-page.html',
  styleUrl: './accueil-page.scss',
})
export class AccueilPage {
  protected readonly steps = STEPS;
  protected readonly features = FEATURES;
  protected readonly todayLink = ['/journees', today()];
  protected readonly backlog = signal<number | null>(null);

  constructor() {
    inject(LivraisonsApi).list({ livree: false })
      .then((livraisons) => this.backlog.set(livraisons.length))
      .catch(() => this.backlog.set(null));
  }
}
