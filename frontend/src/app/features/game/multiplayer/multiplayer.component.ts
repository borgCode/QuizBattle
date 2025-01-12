import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {NgForOf, NgIf, NgStyle, NgSwitch, NgSwitchCase} from '@angular/common';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {MatchFoundDialogComponent} from './dialog/match-found-dialog/match-found-dialog.component';
import {MatchConfirmedDialogComponent} from './dialog/match-confirmed-dialog/match-confirmed-dialog.component';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {MultiplayerSessionDto} from '../../../api/generated/models/multiplayer-session-dto';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {WebSocketService} from '../../../core/websocket/web-socket.service';
import {PlayerCardComponent} from '../../../shared/components/player-card/player-card-component';
import {
  FriendsListDialogComponent
} from '../../../shared/components/dialog/friends-list-dialog/friends-list-dialog.component';
import {FriendshipService} from '../../../api/generated/services/friendship.service';
import {AlertMessageService} from '../../../core/services/alert-message/alert-message.service';
import {MultiplayerGameService} from '../../../api/generated/services/multiplayer-game.service';
import {MultiplayerMatchmakingService} from '../../../api/generated/services/multiplayer-matchmaking.service';
import {MultiplayerMatchService} from '../../../api/generated/services/multiplayer-match.service';


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
  ],
  templateUrl: './multiplayer.component.html',
  styleUrl: './multiplayer.component.css'
})
export class MultiplayerComponent implements OnInit, OnDestroy {
  player!: PlayerDto;
  gameSessions: Array<MultiplayerSessionDto> = [];
  isSearching: boolean = false;
  shouldShowCancelMatchmaking = false;
  waitingForOpponent: boolean = false;
  friends: PlayerDto[] = [];
  image: string = '';

  constructor(
    private loginStateService: LoginStateService,
    private multiplayerGameService: MultiplayerGameService,
    private multiplayerMatchService: MultiplayerMatchService,
    private matchmakingService: MultiplayerMatchmakingService,
    private webSocketService: WebSocketService,
    private friendService: FriendshipService,
    private router: Router,
    private matchFoundDialog: MatDialog,
    private matchConfirmedDialog: MatDialog,
    private friendListDialog: MatDialog,
    private alertMessageService: AlertMessageService,
  ) {
  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;
    if (!this.player) {
      console.warn('No logged-in user found!');
    }

    this.image = 'data:image/jpeg;base64,' + this.player.base64Image;

    this.getFriends();


    this.multiplayerGameService.getPlayerSessions({playerId: this.player.id}).subscribe({
      next: value => {
        this.gameSessions = value;
      }
    })
  }
  ngOnDestroy() {
    if (this.isSearching) {
      this.leaveMatchmakingQueue();
    }
  }

  @HostListener('window:beforeunload')
  onBeforeUnload() {
    if (this.isSearching) {
      this.leaveMatchmakingQueue();
    }
  }

  openGame(sessionId: number) {
    this.router.navigate(['multiplayer', sessionId]);
  }

  findGame() {
    this.isSearching = true;

    setTimeout(() => {
      this.shouldShowCancelMatchmaking = true;
    }, 500);

    this.webSocketService.sendMessage('/app/matchmaking/find', this.player.id);

    this.webSocketService.subscribe('/topic/match' + this.player.id,
      (matchUpdate) => {
        console.log("Match update received", matchUpdate);
        switch (matchUpdate.matchStatus) {
          case 'MATCHED':
            console.log("Match was found")
            this.isSearching = false;
            this.openMatchFoundDialog(matchUpdate.matchmakingSessionId, matchUpdate.opponentDisplayName)
            break;
          case 'ACCEPTED':
            this.showMatchConfirmation(matchUpdate.sessionId, matchUpdate.opponentDisplayName);
            break;
          case 'DECLINED':
            this.alertMessageService.show("The match was declined", "error")
            this.waitingForOpponent = false;
            break;
          case 'WAITING_FOR_OTHER_PLAYER':
            this.waitingForOpponent = true;
            break;
        }
      });

  }

  leaveMatchmakingQueue() {
    this.isSearching = false;
    this.shouldShowCancelMatchmaking = false;
    this.matchmakingService.cancelMatchmaking({playerId: this.player.id}).subscribe({
      next: () => {
        console.log("Left queue")
      }
    })
  }

  private openMatchFoundDialog(matchmakingSessionId: number, opponentDisplayName: string) {
    const dialogRef = this.matchFoundDialog.open(MatchFoundDialogComponent, {
      data: {opponentName: opponentDisplayName},
      width: '300px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      const decision: MatchDecision = {
        matchmakingSessionId: matchmakingSessionId,
        playerId: this.player.id
      }
      if (result.timedOut) {
        this.webSocketService.sendMessage('/app/matchmaking/decline', decision);
        this.alertMessageService.show("You didn't accept the match in time", "error")
      } else if (result === true) {
        this.webSocketService.sendMessage('/app/matchmaking/accept', decision);
      } else {
        this.webSocketService.sendMessage('/app/matchmaking/decline', decision);
        this.alertMessageService.show("You declined the match", "success");
      }
    })
  }

  private showMatchConfirmation(sessionId: number, opponentDisplayName: string) {
    console.log("Showing confirmation dialog")
    const dialogRef = this.matchConfirmedDialog.open(MatchConfirmedDialogComponent, {
      data: {opponentName: opponentDisplayName},
      width: '300px',
      disableClose: true,
      autoFocus: false
    })
    dialogRef.afterClosed().subscribe(() => {
      console.log("Opening gamesession after closing dialog")
      this.openGame(sessionId);
    })

    setTimeout(() => {
      dialogRef.close();
      console.log("Closing confirmation dialog")
    }, 1000)

  }

  openFriendDialog() {

    const dialogRef = this.friendListDialog.open(FriendsListDialogComponent, {
      data: {friends: this.friends},
      maxHeight: '90vh',
      width: '300px',
      disableClose: true,
    })
    dialogRef.afterClosed().subscribe((result) => {
      if (result.friendId) {
        this.multiplayerMatchService.requestMatch({
          body: {
            senderId: this.player.id,
            receiverId: result.friendId
          }
        }).subscribe(() => {
          this.alertMessageService.show("Sent match request!", "success")
        })
      }
    })
  }

  private getFriends() {
    console.log(this.friends);
    this.friendService.getFriends({playerId: this.player.id}).subscribe({
      next: data => {
        this.friends = data;
      }
    })

  }

  getButtonText() {
    if (this.waitingForOpponent) {
      return 'Waiting for Opponent to accept';
    }
    if (this.isSearching) {
      return 'Finding Game';
    }
    return 'Find Game';
  }
}
