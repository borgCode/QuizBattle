import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {RouterLink, RouterLinkActive} from '@angular/router';
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
    private tokenService: TokenService
  ) {
  }

  ngOnInit() {
    this.player = this.loginStateService.loggedInUser;

    this.loginStateService.player$.subscribe(player => {
      if (player) {
        this.player = player;
      }
    });
  }

  logout() {
    this.tokenService.clearTokens();
    this.loginStateService.clearLoggedInUser();
    window.location.href = '/login';
  }
}
