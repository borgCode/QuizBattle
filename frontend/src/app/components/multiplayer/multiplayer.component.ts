import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../services/models/player-dto';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {MultiplayerService} from '../../services/services/multiplayer.service';
import {MultiplayerSessionDto} from '../../services/models/multiplayer-session-dto';

@Component({
  selector: 'app-multiplayer',
  imports: [],
  templateUrl: './multiplayer.component.html',
  styleUrl: './multiplayer.component.css'
})
export class MultiplayerComponent implements OnInit{

  player!: PlayerDto;
  gameSessions: Array<MultiplayerSessionDto> = [];

  constructor(
    private loginStateService: LoginStateService,
    private multiplayerService: MultiplayerService
  ) {
  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;
    this.multiplayerService.getPlayerSessions({playerId: this.player.id}).subscribe({
      next: value => {
        this.gameSessions = value;
      }
    })


    //TODO show user error
    if (!this.player) {
      console.warn('No logged-in user found!');
    }
  }


}
