import {Injectable} from '@angular/core';
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {TokenService} from '../services/token/token.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client;


  constructor(
    private tokenService: TokenService,
  ) {
  }


  public initWebSocketConnection() {
    const token = this.tokenService?.token;
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
        onConnect: () => {
          console.log("Connected to websocket");
        },
        onDisconnect: () => {
          console.log("Disconnected from websocket");
        }

      });

      this.stompClient.activate();
    } else {
      console.error("Token no available");
    }


  }

  disconnectWebSocket() {
    this.stompClient.deactivate();
  }

  sendMessage(destination: string, message: any) {
    console.log(message);
    console.log(JSON.stringify(message));
    this.stompClient.publish({destination, body: JSON.stringify(message)});
  }

  subscribe(destination: string, callback: (message: any) => void) {
    console.log(`Subscribing to: ${destination}`);
    this.stompClient.subscribe(destination, (message) => {
      console.log(`Received message on ${destination}:`, message.body);
      callback(JSON.parse(message.body));

    });
  }
}
