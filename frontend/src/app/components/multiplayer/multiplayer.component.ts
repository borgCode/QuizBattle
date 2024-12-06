import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../services/models/player-dto';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {MultiplayerService} from '../../services/services/multiplayer.service';
import {MultiplayerSessionDto} from '../../services/models/multiplayer-session-dto';
import {NgForOf, NgIf, NgStyle, NgSwitch, NgSwitchCase} from '@angular/common';
import {Router} from '@angular/router';

@Component({
  selector: 'app-multiplayer',
  imports: [
    NgForOf,
    NgSwitch,
    NgSwitchCase,
    NgStyle,
    NgIf
  ],
  templateUrl: './multiplayer.component.html',
  styleUrl: './multiplayer.component.css'
})
export class MultiplayerComponent implements OnInit {
  player!: PlayerDto;
  gameSessions: Array<MultiplayerSessionDto> = [];
  isSearching: boolean = false;

  constructor(
    private loginStateService: LoginStateService,
    private multiplayerService: MultiplayerService,
    private router: Router,
  ) {
  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;
    //TODO show user error
    if (!this.player) {
      console.warn('No logged-in user found!');
    }


    this.multiplayerService.getPlayerSessions({playerId: this.player.id}).subscribe({
      next: value => {
        this.gameSessions = value;
      }
    })
  }

  openGame(id: number) {
    this.router.navigate(['multiplayer', id]);
  }

  findGame() {
    this.isSearching = true;

    this.multiplayerService.findMatch({playerId: this.player.id}).subscribe({
      next: response => {

      }
    })
  }

}
