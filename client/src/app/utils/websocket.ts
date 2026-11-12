export function websocketUrl(path: string, location: Pick<Location, 'protocol' | 'host'> = window.location): string {
  // Browsers block plain ws:// from an https:// page, so the scheme must follow the page.
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${scheme}://${location.host}${path}`;
}
