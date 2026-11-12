import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { divIcon, latLngBounds, LayerGroup, layerGroup, Map as LeafletMap, map as leafletMap, marker, polyline, tileLayer } from 'leaflet';
import { Adresse, Coordonnees, Livraison } from '../core/models/api';
import { cityLine, streetLine } from '../core/util/labels';
import { formatWeight } from '../core/util/format';

export interface MapStop {
  livraison: Livraison;
  color: string | null;
  order: number | null;
}

export interface MapRoute {
  color: string;
  points: Coordonnees[];
}

const GRENOBLE: [number, number] = [45.188, 5.724];

/**
 * Carte Leaflet des livraisons : arrêts numérotés dans la couleur de leur tournée,
 * livraisons non planifiées en creux, dépôt en carré sombre.
 */
@Component({
  selector: 'app-routes-map',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #container class="map"></div>`,
  styles: `
    :host {
      display: block;
      position: relative;
      min-height: 320px;
      border-radius: var(--r-card);
      overflow: hidden;
      border: 1px solid var(--line);
    }

    .map {
      position: absolute;
      inset: 0;
    }
  `,
})
export class RoutesMap {
  readonly depot = input<Adresse | null>(null);
  readonly stops = input<MapStop[]>([]);
  readonly routes = input<MapRoute[]>([]);
  readonly highlightedId = input<number | null>(null);
  readonly stopSelected = output<number>();

  private readonly container = viewChild.required<ElementRef<HTMLDivElement>>('container');
  private map: LeafletMap | null = null;
  private readonly markersLayer: LayerGroup = layerGroup();
  private readonly routesLayer: LayerGroup = layerGroup();
  private fittedOnce = false;

  constructor() {
    afterNextRender(() => this.createMap());
    inject(DestroyRef).onDestroy(() => this.map?.remove());

    effect(() => this.drawMarkers(this.stops(), this.depot(), this.highlightedId()));
    effect(() => this.drawRoutes(this.routes()));
  }

  private createMap(): void {
    this.map = leafletMap(this.container().nativeElement, { zoomControl: true, attributionControl: true })
      .setView(GRENOBLE, 12);
    // Fond OpenStreetMap désaturé en CSS (.leaflet-tile-pane) : les tournées restent la seule couleur.
    tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">contributeurs OpenStreetMap</a>',
    }).addTo(this.map);
    this.routesLayer.addTo(this.map);
    this.markersLayer.addTo(this.map);
    // La carte a pu être créée après les premières données : on redessine.
    this.drawRoutes(this.routes());
    this.drawMarkers(this.stops(), this.depot(), this.highlightedId());
    new ResizeObserver(() => this.map?.invalidateSize()).observe(this.container().nativeElement);
  }

  private drawMarkers(stops: MapStop[], depot: Adresse | null, highlightedId: number | null): void {
    if (!this.map) return;
    this.markersLayer.clearLayers();
    const points: [number, number][] = [];

    for (const stop of stops) {
      const coords = stop.livraison.adresse.coordonnees;
      if (!coords) continue;
      points.push([coords.lat, coords.lng]);
      const highlighted = stop.livraison.id === highlightedId;
      marker([coords.lat, coords.lng], { icon: stopIcon(stop, highlighted), zIndexOffset: highlighted ? 1000 : 0 })
        .bindTooltip(tooltip(stop.livraison), { className: 'stop-tooltip', direction: 'top', offset: [0, -12] })
        .on('click', () => this.stopSelected.emit(stop.livraison.id))
        .addTo(this.markersLayer);
    }

    const depotCoords = depot?.coordonnees;
    if (depot && depotCoords) {
      points.push([depotCoords.lat, depotCoords.lng]);
      marker([depotCoords.lat, depotCoords.lng], { icon: depotIcon(), zIndexOffset: 2000 })
        .bindTooltip(`<strong>Dépôt</strong><br><span>${escape(streetLine(depot))}</span>`, { className: 'stop-tooltip', direction: 'top' })
        .addTo(this.markersLayer);
    }

    if (!this.fittedOnce && points.length > 1) {
      this.map.fitBounds(latLngBounds(points), { padding: [32, 32] });
      this.fittedOnce = true;
    }
  }

  private drawRoutes(routes: MapRoute[]): void {
    if (!this.map) return;
    this.routesLayer.clearLayers();
    for (const route of routes) {
      polyline(route.points.map((p) => [p.lat, p.lng] as [number, number]), {
        color: route.color,
        weight: 4,
        opacity: 0.85,
        lineJoin: 'round',
      }).addTo(this.routesLayer);
    }
  }
}

function stopIcon(stop: MapStop, highlighted: boolean) {
  const classes = ['stop-marker', stop.color ? '' : 'free', highlighted ? 'highlighted' : ''].join(' ');
  const style = stop.color ? `style="--stop-color:${stop.color}"` : '';
  return divIcon({
    className: '',
    html: `<span class="${classes}" ${style}>${stop.order ?? ''}</span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  });
}

function depotIcon() {
  return divIcon({ className: '', html: '<span class="depot-marker"></span>', iconSize: [18, 18], iconAnchor: [9, 9] });
}

function tooltip(livraison: Livraison): string {
  const reference = livraison.reference ? `<span>${escape(livraison.reference)}</span><br>` : '';
  return `${reference}<strong>${escape(streetLine(livraison.adresse))}</strong><br>`
    + `<span>${escape(cityLine(livraison.adresse))} · ${formatWeight(livraison.poids)}</span>`;
}

function escape(text: string): string {
  return text.replace(/[&<>"']/g, (char) => `&#${char.charCodeAt(0)};`);
}
