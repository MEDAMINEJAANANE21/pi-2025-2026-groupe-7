import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../core/api/api-client';
import { EquipeApi } from '../../core/api/equipe-api';
import { Chauffeur, ChauffeurRequest, Vehicule, VehiculeRequest } from '../../core/models/api';
import { formatDecimal, formatEuros, formatInteger } from '../../core/util/format';
import { Avatar } from '../../shared/avatar';
import { Confirm } from '../../shared/confirm';
import { DemoDialog } from '../../shared/demo-dialog';
import { Icon } from '../../shared/icon';
import { LicensePlate } from '../../shared/license-plate';
import { Notifications } from '../../shared/notifications';
import { PhotoChange, PhotoPicker } from '../../shared/photo-picker';

const EMPTY_CHAUFFEUR: ChauffeurRequest = { prenom: '', nom: '', telephone: null, tauxHoraire: null, actif: true };
const EMPTY_VEHICULE: VehiculeRequest = { immatriculation: '', modele: null, capaciteKg: null, consommationLitresAux100Km: null, actif: true };
const UNCHANGED: PhotoChange = { kind: 'unchanged' };

@Component({
  selector: 'app-equipe-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon, Avatar, LicensePlate, PhotoPicker, DemoDialog],
  templateUrl: './equipe-page.html',
  styleUrl: './equipe-page.scss',
})
export class EquipePage {
  private readonly api = inject(EquipeApi);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);

  private readonly chauffeurDialog = viewChild.required<ElementRef<HTMLDialogElement>>('chauffeurDialog');
  private readonly vehiculeDialog = viewChild.required<ElementRef<HTMLDialogElement>>('vehiculeDialog');
  private readonly chauffeurPhoto = viewChild.required<PhotoPicker>('chauffeurPhoto');
  private readonly vehiculePhoto = viewChild.required<PhotoPicker>('vehiculePhoto');

  protected readonly chauffeurs = signal<Chauffeur[]>([]);
  protected readonly vehicules = signal<Vehicule[]>([]);
  protected readonly loaded = signal(false);

  protected readonly editedChauffeur = signal<Chauffeur | null>(null);
  protected readonly chauffeur = signal<ChauffeurRequest>(EMPTY_CHAUFFEUR);
  protected readonly editedVehicule = signal<Vehicule | null>(null);
  protected readonly vehicule = signal<VehiculeRequest>(EMPTY_VEHICULE);
  protected readonly photoChange = signal<PhotoChange>(UNCHANGED);
  protected readonly formError = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly disponibles = computed(() => this.chauffeurs().filter((c) => c.actif).length);
  protected readonly enService = computed(() => this.vehicules().filter((v) => v.actif));
  protected readonly capaciteTotale = computed(() => this.enService().reduce((sum, v) => sum + (v.capaciteKg ?? 0), 0));

  protected readonly euros = formatEuros;
  protected readonly integer = formatInteger;
  protected readonly decimal = formatDecimal;

  constructor() {
    void this.load();
  }

  protected reload(): void {
    void this.load();
  }

  protected fullName(chauffeur: Pick<Chauffeur, 'prenom' | 'nom'>): string {
    return `${chauffeur.prenom} ${chauffeur.nom}`;
  }

  protected editChauffeur(chauffeur: Chauffeur | null): void {
    this.editedChauffeur.set(chauffeur);
    this.chauffeur.set(chauffeur
      ? { prenom: chauffeur.prenom, nom: chauffeur.nom, telephone: chauffeur.telephone, tauxHoraire: chauffeur.tauxHoraire, actif: chauffeur.actif }
      : EMPTY_CHAUFFEUR);
    this.openForm(this.chauffeurPhoto(), this.chauffeurDialog());
  }

  protected editVehicule(vehicule: Vehicule | null): void {
    this.editedVehicule.set(vehicule);
    this.vehicule.set(vehicule
      ? {
          immatriculation: vehicule.immatriculation,
          modele: vehicule.modele,
          capaciteKg: vehicule.capaciteKg,
          consommationLitresAux100Km: vehicule.consommationLitresAux100Km,
          actif: vehicule.actif,
        }
      : EMPTY_VEHICULE);
    this.openForm(this.vehiculePhoto(), this.vehiculeDialog());
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
    await this.save(this.chauffeurDialog(), `${request.prenom} ${request.nom} enregistré`, async () => {
      const saved = await this.api.saveChauffeur(this.editedChauffeur()?.id ?? null, { ...request, tauxHoraire: positiveOrNull(request.tauxHoraire) });
      await this.applyPhoto(saved.id, (id, file) => this.api.envoyerPhotoChauffeur(id, file), (id) => this.api.retirerPhotoChauffeur(id));
    });
  }

  protected async saveVehicule(): Promise<void> {
    const request = this.vehicule();
    if (!request.immatriculation.trim()) {
      this.formError.set("L'immatriculation est obligatoire");
      return;
    }
    await this.save(this.vehiculeDialog(), `Véhicule ${request.immatriculation.toUpperCase()} enregistré`, async () => {
      const saved = await this.api.saveVehicule(this.editedVehicule()?.id ?? null, {
        ...request,
        capaciteKg: positiveOrNull(request.capaciteKg),
        consommationLitresAux100Km: positiveOrNull(request.consommationLitresAux100Km),
      });
      await this.applyPhoto(saved.id, (id, file) => this.api.envoyerPhotoVehicule(id, file), (id) => this.api.retirerPhotoVehicule(id));
    });
  }

  protected async removeChauffeur(chauffeur: Chauffeur): Promise<void> {
    if (await this.confirm.ask('Supprimer le chauffeur', `${this.fullName(chauffeur)} sera retiré des tournées qui lui sont confiées.`)) {
      await this.remove(() => this.api.deleteChauffeur(chauffeur.id), 'Chauffeur supprimé');
    }
  }

  protected async removeVehicule(vehicule: Vehicule): Promise<void> {
    if (await this.confirm.ask('Supprimer le véhicule', `${vehicule.immatriculation} sera retiré des tournées qui l'utilisent.`)) {
      await this.remove(() => this.api.deleteVehicule(vehicule.id), 'Véhicule supprimé');
    }
  }

  private openForm(picker: PhotoPicker, dialog: ElementRef<HTMLDialogElement>): void {
    picker.reset();
    this.photoChange.set(UNCHANGED);
    this.formError.set(null);
    dialog.nativeElement.showModal();
  }

  private async applyPhoto(
    id: number,
    upload: (id: number, file: File) => Promise<unknown>,
    remove: (id: number) => Promise<unknown>,
  ): Promise<void> {
    const change = this.photoChange();
    if (change.kind === 'replace') await upload(id, change.file);
    if (change.kind === 'remove') await remove(id);
  }

  private async save(dialog: ElementRef<HTMLDialogElement>, message: string, action: () => Promise<void>): Promise<void> {
    this.saving.set(true);
    try {
      await action();
      dialog.nativeElement.close();
      this.notifications.success(message);
      await this.load();
    } catch (error) {
      this.formError.set(errorMessage(error, 'Enregistrement impossible'));
      // La fiche a pu être créée avant l'échec de la photo : on rafraîchit quand même.
      await this.load();
    } finally {
      this.saving.set(false);
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
    } finally {
      this.loaded.set(true);
    }
  }
}

function positiveOrNull(value: number | null): number | null {
  return value !== null && Number(value) > 0 ? Number(value) : null;
}
