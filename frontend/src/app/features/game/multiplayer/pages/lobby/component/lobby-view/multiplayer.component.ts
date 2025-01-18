import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf, NgStyle, NgSwitch, NgSwitchCase} from '@angular/common';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {PlayerDto} from '../../../../../../../api/generated/models/player-dto';
import {MultiplayerSessionDto} from '../../../../../../../api/generated/models/multiplayer-session-dto';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {PlayerCardComponent} from '../../../../../../../shared/components/player-card/player-card-component';
import {
  FriendsListDialogComponent
} from '../../../../../../../shared/components/dialog/friends-list-dialog/friends-list-dialog.component';
import {FriendshipService} from '../../../../../../../api/generated/services/friendship.service';
import {AlertMessageService} from '../../../../../../../core/services/alert-message/alert-message.service';
import {MultiplayerGameService} from '../../../../../../../api/generated/services/multiplayer-game.service';
import {MultiplayerMatchService} from '../../../../../../../api/generated/services/multiplayer-match.service';
import {LobbyService} from '../../service/lobby.service';


interface MatchDecision {
  matchmakingSessionId: number;
  playerId: number;
}

@Component({
  selector: 'app-multiplayer',
  imports: [
    NgForOf,
    NgSwitch,
    NgSwitchCase,
    NgStyle,
    NgIf,
    PlayerCardComponent,
    AsyncPipe
  ],
  templateUrl: './multiplayer.component.html',
  styleUrl: './multiplayer.component.css'
})
export class MultiplayerComponent implements OnInit, OnDestroy {
  player!: PlayerDto;
  gameSessions: Array<MultiplayerSessionDto> = [];
  friends: PlayerDto[] = [];
  image: string = '';

  constructor(
    protected lobbyService: LobbyService,
    private loginStateService: LoginStateService,
  ) {

  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;
    if (!this.player) {
      console.warn('No logged-in user found!');
    }

    this.image = 'data:image/jpeg;base64,' + this.player.base64Image;

    this.lobbyService.initLobbyData(this.player.id);

  }

  ngOnDestroy() {
    this.lobbyService.leaveMatchmaking();

  }

  @HostListener('window:beforeunload')
  onBeforeUnload() {
    this.lobbyService.leaveMatchmaking();
  }

  openGame(sessionId: number) {
    this.lobbyService.openGame(sessionId);
  }

  findGame() {
    this.lobbyService.findGame()
  }

  leaveMatchmakingQueue() {
    this.lobbyService.leaveMatchmaking();
  }

  openFriendDialog() {
    this.lobbyService.openFriendsDialog();
  }
}
