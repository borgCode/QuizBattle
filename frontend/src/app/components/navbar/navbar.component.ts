import {Component} from '@angular/core';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {AsyncPipe, NgIf} from '@angular/common';
import {TokenService} from '../../services/token/token.service';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLinkActive,
    RouterLink,
    NgIf,
    AsyncPipe
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent{

  constructor(
    public loginStateService: LoginStateService,
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
