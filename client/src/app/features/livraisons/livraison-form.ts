import { ChangeDetectionStrategy, Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../core/api/api-client';
import { AdresseTrouvee, CartoApi } from '../../core/api/carto-api';
import { LivraisonsApi } from '../../core/api/livraisons-api';
import { Livraison, LivraisonRequest } from '../../core/models/api';
import { isoToSeconds, minutesToIso } from '../../core/util/duration';
import { today } from '../../core/util/dates';
import { Icon } from '../../shared/icon';

interface FormModel {
  numero: string;
  rue: string;
  codePostal: string;
  ville: string;
  lat: number | null;
  lng: number | null;
  poids: number | null;
  montageMinutes: number;
  dateCommande: string;
}

const SEARCH_DELAY_MS = 300;

@Component({
  selector: 'app-livraison-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  templateUrl: './livraison-form.html',
  styleUrl: './livraison-form.scss',
})
export class LivraisonForm {
  readonly saved = output<Livraison>();

  private readonly livraisonsApi = inject(LivraisonsApi);
  private readonly cartoApi = inject(CartoApi);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly editing = signal<Livraison | null>(null);
  protected readonly model = signal<FormModel>(emptyModel());
  protected readonly query = signal('');
  protected readonly suggestions = signal<AdresseTrouvee[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  open(livraison: Livraison | null = null): void {
    this.editing.set(livraison);
    this.model.set(livraison ? toModel(livraison) : emptyModel());
    this.query.set('');
    this.suggestions.set([]);
    this.error.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  protected update<K extends keyof FormModel>(key: K, value: FormModel[K]): void {
    this.model.update((model) => ({ ...model, [key]: value }));
  }

  protected search(value: string): void {
    this.query.set(value);
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(async () => {
      try {
        this.suggestions.set(await this.cartoApi.chercherAdresse(value));
      } catch {
        this.suggestions.set([]);
      }
    }, SEARCH_DELAY_MS);
  }

  protected pick(adresse: AdresseTrouvee): void {
    this.model.update((model) => ({ ...model, ...adresse }));
    this.query.set(adresse.label);
    this.suggestions.set([]);
  }

  protected async submit(): Promise<void> {
    const request = toRequest(this.model());
    if (!request) {
      this.error.set('Renseignez la rue, la ville, le poids et la position de l’adresse');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      const editing = this.editing();
      const livraison = editing ? await this.livraisonsApi.update(editing.id, request) : await this.livraisonsApi.create(request);
      this.saved.emit(livraison);
      this.close();
    } catch (error) {
      this.error.set(errorMessage(error, 'La livraison n’a pas pu être enregistrée'));
    } finally {
      this.saving.set(false);
    }
  }
}

function emptyModel(): FormModel {
  return { numero: '', rue: '', codePostal: '', ville: '', lat: null, lng: null, poids: null, montageMinutes: 0, dateCommande: today() };
}

function toModel(livraison: Livraison): FormModel {
  const { adresse } = livraison;
  return {
    numero: adresse.numero ?? '',
    rue: adresse.rue ?? '',
    codePostal: adresse.codePostal ?? '',
    ville: adresse.ville ?? '',
    lat: adresse.coordonnees?.lat ?? null,
    lng: adresse.coordonnees?.lng ?? null,
    poids: livraison.poids,
    montageMinutes: Math.round((isoToSeconds(livraison.tempsMontage) ?? 0) / 60),
    dateCommande: livraison.dateCommande ?? today(),
  };
}

export function toRequest(model: FormModel): LivraisonRequest | null {
  const { lat, lng, poids } = model;
  if (!model.rue.trim() || !model.ville.trim() || !model.codePostal.trim() || lat === null || lng === null || poids === null || poids < 0) {
    return null;
  }
  return {
    adresse: { numero: model.numero.trim(), rue: model.rue.trim(), codePostal: model.codePostal.trim(), ville: model.ville.trim(), lat, lng },
    poids,
    tempsMontage: model.montageMinutes > 0 ? minutesToIso(model.montageMinutes) : null,
    dateCommande: model.dateCommande || null,
  };
}
