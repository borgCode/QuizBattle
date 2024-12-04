import {Component, OnInit} from '@angular/core';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {PlayerDto} from '../../services/models/player-dto';

@Component({
  selector: 'app-user-profile',
  imports: [],
  templateUrl: './player-profile.component.html',
  styleUrl: './player-profile.component.css'
})
export class PlayerProfileComponent implements OnInit {
  player!: PlayerDto;

  constructor(
    private loginStateService: LoginStateService
  ) {
  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;

    //TODO show user error
    if (!this.player) {
      console.warn('No logged-in user found!');
    }
  }
}
