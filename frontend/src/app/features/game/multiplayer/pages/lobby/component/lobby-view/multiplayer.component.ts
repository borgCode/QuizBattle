import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {AsyncPipe, NgClass, NgForOf, NgIf, NgStyle, NgSwitch, NgSwitchCase} from '@angular/common';
import {PlayerDto} from '../../../../../../../api/generated/models/player-dto';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {PlayerCardComponent} from '../../../../../../../shared/components/player-card/player-card-component';
import {LobbyService} from '../../service/lobby.service';
import {MultiplayerSessionDto} from '../../../../../../../api/generated/models/multiplayer-session-dto';
import {InfiniteScrollDirective} from 'ngx-infinite-scroll';

interface SessionState {
  sessions: MultiplayerSessionDto[];
  currentPage: number;
  isLoading: boolean;
  hasMore: boolean;
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
    AsyncPipe,
    NgClass,
    InfiniteScrollDirective
  ],
  templateUrl: './multiplayer.component.html',
  styleUrl: './multiplayer.component.css'
})
export class MultiplayerComponent implements OnInit, OnDestroy {
  player!: PlayerDto;
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

  onScroll() {
    this.lobbyService.loadMoreSessions();
  }
}
