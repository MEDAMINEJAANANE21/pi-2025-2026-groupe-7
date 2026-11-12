import { Client, IMessage } from '@stomp/stompjs';
import { Observable } from 'rxjs';
import { z } from 'zod';

export function websocketUrl(path: string, location: Pick<Location, 'protocol' | 'host'> = window.location): string {
  // Un navigateur bloque ws:// depuis une page https:// : le schéma suit celui de la page.
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${scheme}://${location.host}${path}`;
}

/** Messages d'un sujet STOMP, validés par le schéma ; la connexion se ferme au désabonnement. */
export function stompTopic<T>(brokerUrl: string, topic: string, schema: z.ZodType<T>): Observable<T> {
  return new Observable<T>((subscriber) => {
    const client = new Client({
      brokerURL: brokerUrl,
      reconnectDelay: 5_000,
      onConnect: () => {
        client.subscribe(topic, (message: IMessage) => {
          const parsed = schema.safeParse(safeJson(message.body));
          if (parsed.success) subscriber.next(parsed.data);
        });
      },
    });
    client.activate();
    return () => void client.deactivate();
  });
}

function safeJson(body: string): unknown {
  try {
    return JSON.parse(body);
  } catch {
    return null;
  }
}
