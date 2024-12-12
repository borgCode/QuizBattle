import {Component, OnInit} from '@angular/core';
import {NgForOf, NgIf, NgStyle, NgSwitch, NgSwitchCase} from '@angular/common';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {MatchFoundDialogComponent} from './dialog/match-found-dialog/match-found-dialog.component';
import {MatchConfirmedDialogComponent} from './dialog/match-confirmed-dialog/match-confirmed-dialog.component';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {MultiplayerSessionDto} from '../../../api/generated/models/multiplayer-session-dto';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {MultiplayerService} from '../../../api/generated/services/multiplayer.service';
import {WebSocketService} from '../../../core/websocket/web-socket.service';
import {PlayerCardComponent} from '../../../shared/components/player-card/player-card-component';
import {
  FriendsListDialogComponent
} from '../../../shared/components/dialog/friends-list-dialog/friends-list-dialog.component';
import {FriendshipService} from '../../../api/generated/services/friendship.service';


interface MatchDecision {
  pendingSessionId: number;
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
export class MultiplayerComponent implements OnInit {
  player!: PlayerDto;
  gameSessions: Array<MultiplayerSessionDto> = [];
  isSearching: boolean = false;
  friends: PlayerDto[] = [];
  image: string = '';

  constructor(
    private loginStateService: LoginStateService,
    private multiplayerService: MultiplayerService,
    private webSocketService: WebSocketService,
    private friendService: FriendshipService,
    private router: Router,
    private matchFoundDialog: MatDialog,
    private matchConfirmedDialog: MatDialog,
    private friendListDialog: MatDialog
  ) {
  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;
    //TODO show user error
    if (!this.player) {
      console.warn('No logged-in user found!');
    }

    this.image = 'data:image/jpeg;base64,' + this.player.base64Image;

    this.getFriends();


    this.multiplayerService.getPlayerSessions({playerId: this.player.id}).subscribe({
      next: value => {
        this.gameSessions = value;
      }
    })
  }

  openGame(sessionId: number) {
    this.router.navigate(['multiplayer', sessionId]);
  }

  findGame() {
    this.isSearching = true;

    this.webSocketService.sendMessage('/app/matchmaking/find', this.player.id);


    this.webSocketService.subscribe('/topic/match' + this.player.id,
      (matchUpdate) => {
        console.log("Match update received", matchUpdate);
        switch (matchUpdate.matchStatus) {
          case 'MATCHED':
            console.log("Match was found")
            this.isSearching = false;
            this.openMatchFoundDialog(matchUpdate.pendingSessionId, matchUpdate.opponentDisplayName)
            break;
          case 'ACCEPTED':
            this.showMatchConfirmation(matchUpdate.sessionId,matchUpdate.opponentDisplayName);
            break;
          case 'DECLINED':
            break;
        }
      });

  }

  private openMatchFoundDialog(pendingSessionId: number, opponentDisplayName: string) {
    const dialogRef = this.matchFoundDialog.open(MatchFoundDialogComponent, {
      data: {opponentName: opponentDisplayName},
      width: '300px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      const decision: MatchDecision = {
        pendingSessionId: pendingSessionId,
        playerId: this.player.id
      }

      if (result === true) {
        this.webSocketService.sendMessage('/app/matchmaking/accept', decision);
        console.log("Match accepted")
      } else {
        this.webSocketService.sendMessage('/app/matchmaking/decline', decision);
        console.log("Match declined")
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
    }, 3000)

  }

  openFriendDialog() {

    const dialogRef = this.friendListDialog.open(FriendsListDialogComponent, {
      data: {friends: this.friends},
      maxHeight: '90vh',
      width: '300px',
      disableClose: true,
    })
    dialogRef.afterClosed().subscribe(()  => {
      console.log("Closed friendsdialog");
    })
  }

  private getFriends() {
    console.log(this.friends);
    this.friendService.getFriends({playerId: this.player.id}).subscribe( {
      next: data => {
        this.friends = data;
      }
    })

  }
}
