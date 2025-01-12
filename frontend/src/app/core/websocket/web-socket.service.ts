import {Injectable} from '@angular/core';
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {TokenService} from '../services/token/token.service';
import {BehaviorSubject, Subject} from 'rxjs';
import {Message} from '../services/whisper-window/whisper-window.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client;
  private connectionState$ = new BehaviorSubject<boolean>(false);
  private subscriptionQueue: { destination: string, callback: (message: any) => void }[] = [];

  private achievementSubject = new Subject<any>();
  public achievement$ = this.achievementSubject.asObservable();

  private messageSubject = new Subject<Message>()
  message$ = this.messageSubject.asObservable();

  constructor(
    private tokenService: TokenService,
  ) {
  }


  public initWebSocketConnection() {
    const token = this.tokenService?.accessToken;
    if (token) {
      const websocket = new SockJS('http://localhost:8080/socket');
      this.stompClient = new Client({
        webSocketFactory: () => websocket,
        connectHeaders: {
          Authorization: 'Bearer ' + token
        },
        debug: msg => {
          console.log(msg);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: (frame) => {
          if (!frame?.headers?.['user-name']) {
            this.stompClient?.deactivate();
            setTimeout(() => this.stompClient?.activate(), 1000);
            return;
          }
          this.connectionState$.next(true);
          this.processSubscriptionQueue();
          this.initializeAppSubscriptions();
        },
        onDisconnect: () => {
          console.log("Disconnected from websocket");
          this.connectionState$.next(false);

          setTimeout(() => {
            if (!this.connectionState$.value) {
              console.log("Attempting to reactivate connection...");
              this.stompClient?.activate();
            }
          }, 1000)
        },
        onStompError: (frame) => {
          console.error('STOMP protocol error:', frame);
          this.connectionState$.next(false);
        },
        onWebSocketError: (event) => {
          console.error('WebSocket error:', event);
          this.connectionState$.next(false);
        }

      });

      this.stompClient.activate();
    } else {
      console.error("Token not available");
    }

  }

  processSubscriptionQueue() {
    while (this.subscriptionQueue.length > 0) {
      const sub = this.subscriptionQueue.shift();
      this.initSub(sub.destination, sub.callback);
    }
  }

   initializeAppSubscriptions() {
    if (this.connectionState$.value) {
      this.subscribe("/user/queue/achievements", message => {
        this.achievementSubject.next(message);
      });

      this.subscribe("/user/queue/message", message => {
        this.messageSubject.next(message);
      });
    }
  }

  disconnectWebSocket() {
    this.stompClient.deactivate();
  }

  sendMessage(destination: string, message: any) {
    if (this.connectionState$.value) {
      console.log(message);
      console.log(JSON.stringify(message));
      this.stompClient.publish({destination, body: JSON.stringify(message)});
    } else {
      console.error('Cannot send message: WebSocket not connected');
    }
  }

  subscribe(destination: string, callback: (message: any) => void) {
    console.log(`Attempting to subscribe to: ${destination}`);

    if (this.connectionState$.value) {
      this.initSub(destination, callback);
    } else {
      console.log(`Queuing subscription to: ${destination}`);
      this.subscriptionQueue.push({destination, callback});
    }
  }

  private initSub(destination: string, callback: (message: any) => void) {
    console.log(`Subscribing to: ${destination}`);
    this.stompClient.subscribe(destination, (message) => {
      console.log(`Received message on ${destination}:`, message.body);
      callback(JSON.parse(message.body));
    });
  }
}
