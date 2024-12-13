import {Component} from '@angular/core';
import { Router, RouterLink, RouterLinkActive} from '@angular/router';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {AsyncPipe, NgIf} from '@angular/common';
import {TokenService} from '../../services/token/token.service';
import {NotificationDropdownComponent} from './notification-dropdown/notification-dropdown.component';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLinkActive,
    RouterLink,
    NgIf,
    AsyncPipe,
    NotificationDropdownComponent
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {


  constructor(
    protected loginStateService: LoginStateService,
    private tokenService: TokenService,
    private router: Router
  ) {
  }


  logout() {
    this.tokenService.clearToken();
    this.loginStateService.clearLoggedInUser();
    this.router.navigate(['/login']);
  }
}
