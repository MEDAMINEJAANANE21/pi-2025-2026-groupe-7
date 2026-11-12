import {
  Component,
  computed,
  effect,
  input,
  output,
  Signal,
  signal,
  ChangeDetectionStrategy,
  untracked,
} from '@angular/core';
import { LeafletModule } from '@bluehalo/ngx-leaflet';
import { Map as LeafletMap, LatLng, latLng, Layer, MapOptions, polyline, tileLayer } from 'leaflet';
import { Coords } from '../../models/coords';
import { AdresseWithMeta, Adresse } from '../../models/adresse';
import { LivraisonResponse } from '../../models/backend';
import { getMarker } from '../../utils/marker';
import { ROUTE_COLORS } from '../../utils/routeColors';
import { LivraisonPopover } from '../livraison-popover/livraison-popover';

interface PopoverState {
  livraison: LivraisonResponse;
  latLng: LatLng;
  x: number;
  y: number;
  pinned: boolean;
}

@Component({
  selector: 'app-routes-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LeafletModule, LivraisonPopover],
  templateUrl: './RoutesMap.html',
  styleUrl: './RoutesMap.scss',
})
export class RoutesMap {
  public readonly bounds = input<readonly [Coords, Coords]>([
    { lat: 45.1, lng: 5.6 },
    { lat: 45.3, lng: 5.9 },
  ]);
  public readonly zoom = input<number>(11);
  public readonly center = input<Coords>({ lat: 45.188529, lng: 5.724524 });
  public readonly hidePins = input<boolean>(false);
  /** Points of departure */
  public readonly parkings = input<readonly Adresse[]>([]);
  /** Points of arrival — optionally coloured, flagged as hors-solution, and/or carrying livraison data */
  public readonly destinations = input<readonly AdresseWithMeta[]>([]);
  public readonly routes = input<Coords[][]>([]);
  /** Programmatically highlight a livraison by ID (hover mode, does not override a pinned popover) */
  public readonly highlightedLivraisonId = input<number | null>(null);
  /** Emits the focused livraison ID when the user hovers or clicks a pin, null when focus is lost */
  public readonly livraisonFocused = output<number | null>();
  /** Emits the livraison ID when the user clicks (pins) a pin, null when the popover is dismissed */
  public readonly livraisonPinned = output<number | null>();

  protected readonly mapOptions = computed<MapOptions>(() => ({
    zoom: this.zoom(),
    center: latLng(this.center().lat, this.center().lng),
  }));

  protected readonly mapRef = signal<LeafletMap | null>(null);
  protected readonly activePopover = signal<PopoverState | null>(null);

  private _hideTimer: ReturnType<typeof setTimeout> | null = null;

  // Stable IDs extracted from popover state — so effects only fire on ID changes, not on pan.
  private readonly _focusedId = computed(() => this.activePopover()?.livraison.id ?? null);
  private readonly _pinnedId = computed(() => {
    const p = this.activePopover();
    return p?.pinned ? p.livraison.id : null;
  });

  protected readonly layers: Signal<Layer[]>;

  constructor() {
    const back = tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '...',
    });

    this.layers = computed<Layer[]>(() => {
      const mkr = (a: AdresseWithMeta, color: string) => {
        const ll = latLng(a.coords.lat, a.coords.lng);
        return getMarker(ll, color, a.livraison
          ? {
              onHover: () => this._showPopover(a.livraison!, ll),
              onLeave: () => this.scheduleHide(),
              onClick: () => this._pinPopover(a.livraison!, ll),
            }
          : undefined);
      };

      return [
        back,
        ...(this.hidePins()
          ? []
          : this.destinations().map((a) =>
              mkr(a, a.horsSolution ? '#ef4444' : (a.color ?? '#2563eb')),
            )),
        ...this.parkings().map((a) => mkr(a, '#1f2937')),
        ...this.routes().map((r, i) =>
          polyline(r as Coords[], { color: ROUTE_COLORS[i % ROUTE_COLORS.length] }),
        ),
      ];
    });

    // External control: show a hover popup for the given livraison ID.
    // Does not override a pinned popover.
    effect(() => {
      const id = this.highlightedLivraisonId();
      const dest = id !== null ? this.destinations().find(d => d.livraison?.id === id) : null;
      const map = this.mapRef();

      untracked(() => {
        const current = this.activePopover();
        if (current?.pinned) return;
        if ((current?.livraison.id ?? null) === id) return;

        if (id === null || !dest?.livraison || !map) {
          this.activePopover.set(null);
          return;
        }
        const ll = latLng(dest.coords.lat, dest.coords.lng);
        const pt = map.latLngToContainerPoint(ll);
        this.activePopover.set({ livraison: dest.livraison, latLng: ll, x: pt.x, y: pt.y, pinned: false });
      });
    });

    // Notify parent whenever the focused livraison changes.
    effect(() => {
      this.livraisonFocused.emit(this._focusedId());
    });

    // Notify parent whenever the pinned livraison changes.
    effect(() => {
      this.livraisonPinned.emit(this._pinnedId());
    });
  }

  protected onMapReady(map: LeafletMap): void {
    this.mapRef.set(map);
    map.on('click', () => this._closePopover());
    map.on('move', () => {
      const current = this.activePopover();
      if (current) {
        const pt = map.latLngToContainerPoint(current.latLng);
        this.activePopover.set({ ...current, x: pt.x, y: pt.y });
      }
    });
  }

  protected cancelHide(): void {
    if (this._hideTimer !== null) {
      clearTimeout(this._hideTimer);
      this._hideTimer = null;
    }
  }

  protected scheduleHide(): void {
    this.cancelHide();
    this._hideTimer = setTimeout(() => {
      if (!this.activePopover()?.pinned) {
        this.activePopover.set(null);
      }
    }, 200);
  }

  protected closePopover(): void {
    this._closePopover();
  }

  private _showPopover(livraison: LivraisonResponse, ll: LatLng): void {
    this.cancelHide();
    if (this.activePopover()?.pinned) return;
    const map = this.mapRef();
    if (!map) return;
    const pt = map.latLngToContainerPoint(ll);
    this.activePopover.set({ livraison, latLng: ll, x: pt.x, y: pt.y, pinned: false });
  }

  private _pinPopover(livraison: LivraisonResponse, ll: LatLng): void {
    const map = this.mapRef();
    if (!map) return;
    const current = this.activePopover();
    if (current?.pinned && current.livraison.id === livraison.id) {
      this.activePopover.set(null);
      return;
    }
    const pt = map.latLngToContainerPoint(ll);
    this.activePopover.set({ livraison, latLng: ll, x: pt.x, y: pt.y, pinned: true });
  }

  private _closePopover(): void {
    this.activePopover.set(null);
  }
}
