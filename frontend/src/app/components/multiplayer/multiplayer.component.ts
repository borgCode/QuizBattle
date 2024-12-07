import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../services/models/player-dto';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {MultiplayerService} from '../../services/services/multiplayer.service';
import {MultiplayerSessionDto} from '../../services/models/multiplayer-session-dto';
import {NgForOf, NgIf, NgStyle, NgSwitch, NgSwitchCase} from '@angular/common';
import {Router} from '@angular/router';
import {WebSocketService} from '../../services/websocket/web-socket.service';
import {MatDialog} from '@angular/material/dialog';
import {MatchFoundDialogComponent} from './dialog/match-found-dialog/match-found-dialog.component';

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
    private webSocketService: WebSocketService,
    private router: Router,
    private matchFoundDialog: MatDialog
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

    this.webSocketService.subscribe('/topic/match' + this.player.id,
      (matchUpdate) => {
        console.log("Match update received", matchUpdate);
        if (matchUpdate.matchStatus === 'MATCHED') {
          console.log("Match was found")
          this.isSearching = false;

          this.openMatchFoundDialog(matchUpdate.opponentDisplayName)
        }
      });

    this.multiplayerService.findMatch({playerId: this.player.id}).subscribe({
      next: response => {
        console.log("Matchmaking initiated", response);
      },
      error: err => {
        console.error('Error initiating matchmaking:', err)
        this.isSearching = false;
      }
    })
  }

  private openMatchFoundDialog(opponentName: string) {
    const dialogRef = this.matchFoundDialog.open(MatchFoundDialogComponent, {
      data: {opponentName},
      width: '300px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        console.log("Match accepted")
      } else {
        console.log("Match declined")
      }
    })
  }

}
