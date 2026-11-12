import { Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import { z } from 'zod';

export function stompTopic(brokerURL: string, topic: string): Observable<unknown>;
export function stompTopic<T>(
  brokerURL: string,
  topic: string,
  schema: z.ZodType<T>,
): Observable<T>;
export function stompTopic<T>(
  brokerURL: string,
  topic: string,
  schema?: z.ZodType<T>,
): Observable<T | unknown> {
  return new Observable((subscriber) => {
    const client = new Client({
      brokerURL,
      onConnect: () => {
        client.subscribe(topic, (msg: IMessage) => {
          let parsed: unknown;
          try {
            parsed = JSON.parse(msg.body);
          } catch {
            subscriber.error(new Error(`Failed to parse STOMP message: ${msg.body}`));
            return;
          }

          if (!schema) {
            subscriber.next(parsed);
            return;
          }

          const result = schema.safeParse(parsed);
          if (result.success) {
            subscriber.next(result.data);
          } else {
            subscriber.error(result.error);
          }
        });
      },
      onStompError: (frame) => {
        subscriber.error(new Error(frame.headers['message'] ?? 'STOMP error'));
      },
    });
    client.activate();
    return () => client.deactivate();
  });
}
