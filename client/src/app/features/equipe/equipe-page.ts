import { ChangeDetectionStrategy, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../core/api/api-client';
import { EquipeApi } from '../../core/api/equipe-api';
import { Chauffeur, ChauffeurRequest, Vehicule, VehiculeRequest } from '../../core/models/api';
import { formatDecimal, formatEuros, formatInteger } from '../../core/util/format';
import { Confirm } from '../../shared/confirm';
import { Icon } from '../../shared/icon';
import { Notifications } from '../../shared/notifications';

const EMPTY_CHAUFFEUR: ChauffeurRequest = { prenom: '', nom: '', telephone: null, tauxHoraire: null, actif: true };
const EMPTY_VEHICULE: VehiculeRequest = { immatriculation: '', modele: null, capaciteKg: null, consommationLitresAux100Km: null, actif: true };

@Component({
  selector: 'app-equipe-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  templateUrl: './equipe-page.html',
  styleUrl: './equipe-page.scss',
})
export class EquipePage {
  private readonly api = inject(EquipeApi);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);

  private readonly chauffeurDialog = viewChild.required<ElementRef<HTMLDialogElement>>('chauffeurDialog');
  private readonly vehiculeDialog = viewChild.required<ElementRef<HTMLDialogElement>>('vehiculeDialog');

  protected readonly chauffeurs = signal<Chauffeur[]>([]);
  protected readonly vehicules = signal<Vehicule[]>([]);

  protected readonly chauffeurId = signal<number | null>(null);
  protected readonly chauffeur = signal<ChauffeurRequest>(EMPTY_CHAUFFEUR);
  protected readonly vehiculeId = signal<number | null>(null);
  protected readonly vehicule = signal<VehiculeRequest>(EMPTY_VEHICULE);
  protected readonly formError = signal<string | null>(null);

  protected readonly euros = formatEuros;
  protected readonly integer = formatInteger;
  protected readonly decimal = formatDecimal;

  constructor() {
    void this.load();
  }

  protected editChauffeur(chauffeur: Chauffeur | null): void {
    this.chauffeurId.set(chauffeur?.id ?? null);
    this.chauffeur.set(chauffeur
      ? { prenom: chauffeur.prenom, nom: chauffeur.nom, telephone: chauffeur.telephone, tauxHoraire: chauffeur.tauxHoraire, actif: chauffeur.actif }
      : EMPTY_CHAUFFEUR);
    this.formError.set(null);
    this.chauffeurDialog().nativeElement.showModal();
  }

  protected editVehicule(vehicule: Vehicule | null): void {
    this.vehiculeId.set(vehicule?.id ?? null);
    this.vehicule.set(vehicule
      ? {
          immatriculation: vehicule.immatriculation,
          modele: vehicule.modele,
          capaciteKg: vehicule.capaciteKg,
          consommationLitresAux100Km: vehicule.consommationLitresAux100Km,
          actif: vehicule.actif,
        }
      : EMPTY_VEHICULE);
    this.formError.set(null);
    this.vehiculeDialog().nativeElement.showModal();
  }

  protected patchChauffeur(patch: Partial<ChauffeurRequest>): void {
    this.chauffeur.update((current) => ({ ...current, ...patch }));
  }

  protected patchVehicule(patch: Partial<VehiculeRequest>): void {
    this.vehicule.update((current) => ({ ...current, ...patch }));
  }

  protected async saveChauffeur(): Promise<void> {
    const request = this.chauffeur();
    if (!request.prenom.trim() || !request.nom.trim()) {
      this.formError.set('Le prénom et le nom sont obligatoires');
      return;
    }
    await this.save(
      () => this.api.saveChauffeur(this.chauffeurId(), { ...request, tauxHoraire: positiveOrNull(request.tauxHoraire) }),
      this.chauffeurDialog(),
      `${request.prenom} ${request.nom} enregistré`,
    );
  }

  protected async saveVehicule(): Promise<void> {
    const request = this.vehicule();
    if (!request.immatriculation.trim()) {
      this.formError.set("L'immatriculation est obligatoire");
      return;
    }
    await this.save(
      () => this.api.saveVehicule(this.vehiculeId(), {
        ...request,
        capaciteKg: positiveOrNull(request.capaciteKg),
        consommationLitresAux100Km: positiveOrNull(request.consommationLitresAux100Km),
      }),
      this.vehiculeDialog(),
      `Véhicule ${request.immatriculation.toUpperCase()} enregistré`,
    );
  }

  protected async removeChauffeur(chauffeur: Chauffeur): Promise<void> {
    if (await this.confirm.ask('Supprimer le chauffeur', `${chauffeur.prenom} ${chauffeur.nom} sera retiré des tournées qui lui sont confiées.`)) {
      await this.remove(() => this.api.deleteChauffeur(chauffeur.id), 'Chauffeur supprimé');
    }
  }

  protected async removeVehicule(vehicule: Vehicule): Promise<void> {
    if (await this.confirm.ask('Supprimer le véhicule', `${vehicule.immatriculation} sera retiré des tournées qui l'utilisent.`)) {
      await this.remove(() => this.api.deleteVehicule(vehicule.id), 'Véhicule supprimé');
    }
  }

  private async save(action: () => Promise<unknown>, dialog: ElementRef<HTMLDialogElement>, message: string): Promise<void> {
    try {
      await action();
      dialog.nativeElement.close();
      this.notifications.success(message);
      await this.load();
    } catch (error) {
      this.formError.set(errorMessage(error, 'Enregistrement impossible'));
    }
  }

  private async remove(action: () => Promise<void>, message: string): Promise<void> {
    try {
      await action();
      this.notifications.success(message);
      await this.load();
    } catch (error) {
      this.notifications.error(errorMessage(error, 'Suppression impossible'));
    }
  }

  private async load(): Promise<void> {
    try {
      const [chauffeurs, vehicules] = await Promise.all([this.api.chauffeurs(), this.api.vehicules()]);
      this.chauffeurs.set(chauffeurs);
      this.vehicules.set(vehicules);
    } catch (error) {
      this.notifications.error(errorMessage(error, "Impossible de charger l'équipe"));
    }
  }
}

function positiveOrNull(value: number | null): number | null {
  return value !== null && Number(value) > 0 ? Number(value) : null;
}
