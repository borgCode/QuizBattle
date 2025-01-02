import {Component, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import {NavbarComponent} from '../../core/components/navbar/navbar.component';
import {RouterOutlet} from '@angular/router';
import {AlertMessageComponent} from '../../core/alert-message/notification/alert-message.component';
import {LoginStateService} from '../../core/services/login-state-service/login-state.service';
import {WebSocketService} from '../../core/websocket/web-socket.service';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterOutlet, AlertMessageComponent],
  templateUrl: 'app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  constructor(
    private loginStateService: LoginStateService,
    private webSocketService: WebSocketService
  ) {
  }

  ngOnInit() {
    this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.webSocketService.initWebSocketConnection();
      } else {
        this.webSocketService.disconnectWebSocket();
      }
    })
  }


}
