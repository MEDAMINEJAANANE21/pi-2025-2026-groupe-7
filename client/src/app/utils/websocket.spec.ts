import { websocketUrl } from './websocket';

describe('websocketUrl', () => {
  it('uses ws:// on an http page', () => {
    expect(websocketUrl('/ws', { protocol: 'http:', host: 'localhost:4200' })).toBe(
      'ws://localhost:4200/ws',
    );
  });

  it('uses wss:// on an https page', () => {
    expect(websocketUrl('/ws', { protocol: 'https:', host: 'example.org' })).toBe(
      'wss://example.org/ws',
    );
  });
});
