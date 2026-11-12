import { LatLngExpression, Marker, divIcon, marker } from 'leaflet';

export interface MarkerCallbacks {
  onHover?: () => void;
  onLeave?: () => void;
  onClick?: () => void;
}

function pinSvg(color: string): string {
  return `<svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 20 30"
    width="20"
    height="30"
    class="map-pin"
    style="color:${color}"
    pointer-events="visibleFill"
  ><path d="M10 0C5.582 0 2 3.582 2 8c0 6.5 8 22 8 22s8-15.5 8-22C18 3.582 14.418 0 10 0z" fill="currentColor"
  /><circle cx="10" cy="8" r="3.5" fill="white" fill-opacity="0.85"/></svg>`;
}

export function getMarker(
  latLng: LatLngExpression,
  color: string = '#2563eb',
  callbacks?: MarkerCallbacks,
): Marker {
  const icon = divIcon({
    html: pinSvg(color),
    className: '',
    iconSize: [20, 30],
    iconAnchor: [10, 30],
  });

  const m = marker(latLng, { icon, interactive: false });

  if (callbacks) {
    m.on('add', () => {
      const svg = m.getElement()?.querySelector<SVGElement>('svg');
      if (!svg) return;
      svg.addEventListener('mouseenter', () => callbacks.onHover?.());
      svg.addEventListener('mouseleave', () => callbacks.onLeave?.());
      svg.addEventListener('mousedown', (e) => e.stopPropagation());
      svg.addEventListener('click', (e) => {
        e.stopPropagation();
        callbacks.onClick?.();
      });
    });
  }

  return m;
}
