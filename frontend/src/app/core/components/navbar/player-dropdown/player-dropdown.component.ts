import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {TokenService} from '../../../services/token/token.service';

@Component({
  selector: 'app-player-dropdown',
  imports: [
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './player-dropdown.component.html',
  styleUrl: './player-dropdown.component.css'
})
export class PlayerDropdownComponent implements OnInit {
  player: PlayerDto;

  constructor(
    private loginStateService: LoginStateService,
    private router: Router,
    private tokenService: TokenService
  ) {
  }

  ngOnInit() {
    this.loginStateService.loggedInUser$.subscribe( {
      next: loggedInUser => {
        this.player = loggedInUser;
      }
    })
  }

  logout() {
    this.tokenService.clearTokens();
    this.loginStateService.clearLoggedInUser();
    this.router.navigate(['/login']);
  }
}
