import {Component} from '@angular/core';
import { RouterLink, RouterLinkActive} from '@angular/router';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {AsyncPipe, Location, NgIf} from '@angular/common';
import {WhispersDropdownComponent} from './chat-dropdown/whispers-dropdown.component';
import {PlayerDropdownComponent} from './player-dropdown/player-dropdown.component';
import {NotificationDropdownComponent} from './notification-dropdown/notification-dropdown.component';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLinkActive,
    RouterLink,
    NgIf,
    AsyncPipe,
    NotificationDropdownComponent,
    WhispersDropdownComponent,
    PlayerDropdownComponent
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {


  constructor(
    protected loginStateService: LoginStateService,
    private location: Location
  ) {
  }

  goBack() {
    this.location.back();
  }
}
