import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NavbarComponent} from '../../core/components/navbar/navbar.component';
import {RouterOutlet} from '@angular/router';
import {AlertMessageComponent} from '../../core/alert-message/notification/alert-message.component';
import {LoginStateService} from '../../core/services/login-state-service/login-state.service';
import {WebSocketService} from '../../core/websocket/web-socket.service';
import {AchievementPopupComponent} from '../../core/achievement-popup/achievement-popup.component';
import {
  WhispersContainerComponent
} from '../../core/components/whispers/whispers-container/whispers-container.component';
import {distinctUntilChanged, Subscription} from 'rxjs';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterOutlet, AlertMessageComponent, AchievementPopupComponent, WhispersContainerComponent],
  templateUrl: 'app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private connectionInitialized = false;

  constructor(
    private loginStateService: LoginStateService,
    private webSocketService: WebSocketService
  ) {
  }

  ngOnInit() {
    this.loginStateService.isLoggedIn$.pipe(
      distinctUntilChanged()
    ).subscribe(isLoggedIn => {
      if (isLoggedIn && !this.connectionInitialized) {
        this.webSocketService.initWebSocketConnection();
        this.connectionInitialized = true;
      } else if (!isLoggedIn) {
        this.webSocketService.disconnectWebSocket();
        this.connectionInitialized = false;
      }
    })
  }
}
