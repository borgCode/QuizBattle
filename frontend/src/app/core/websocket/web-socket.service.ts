import {Injectable} from '@angular/core';
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {TokenService} from '../services/token/token.service';
import {BehaviorSubject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client;
  private connectionState$ = new BehaviorSubject<boolean>(false);
  public isConnected$ = this.connectionState$.asObservable();
  private subscriptionQueue: { destination: string, callback: (message: any) => void }[] = [];

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
        onConnect: (frame) => {
          if (!frame?.headers?.['user-name']) {
            this.stompClient?.deactivate();
            setTimeout(() => this.stompClient?.activate(), 1000);
            return;
          }
          this.connectionState$.next(true);
          this.processSubscriptionQueue();
        },
        onDisconnect: () => {
          console.log("Disconnected from websocket");
        },

      });

      this.stompClient.activate();
    } else {
      console.error("Token no available");
    }

  }

  processSubscriptionQueue() {
    while (this.subscriptionQueue.length > 0) {
      const sub = this.subscriptionQueue.shift();
      this.initSub(sub.destination, sub.callback);
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
