import {Injectable} from '@angular/core';
import {BehaviorSubject, combineLatest, switchMap, tap} from 'rxjs';
import {MultiplayerMatchmakingService} from '../../../../../../api/generated/services/multiplayer-matchmaking.service';
import {WebSocketService} from '../../../../../../core/websocket/web-socket.service';
import {MatDialog} from '@angular/material/dialog';
import {AlertMessageService} from '../../../../../../core/services/alert-message/alert-message.service';
import {MatchFoundDialogComponent} from '../dialog/match-found-dialog/match-found-dialog.component';
import {MatchConfirmedDialogComponent} from '../dialog/match-confirmed-dialog/match-confirmed-dialog.component';
import {Router} from '@angular/router';
import {FriendshipService} from '../../../../../../api/generated/services/friendship.service';
import {MultiplayerGameService} from '../../../../../../api/generated/services/multiplayer-game.service';
import {PlayerDto} from '../../../../../../api/generated/models/player-dto';
import {MultiplayerSessionDto} from '../../../../../../api/generated/models/multiplayer-session-dto';
import {
  FriendsListDialogComponent
} from '../../../../../../shared/components/dialog/friends-list-dialog/friends-list-dialog.component';
import {MultiplayerMatchService} from '../../../../../../api/generated/services/multiplayer-match.service';
import {PageMultiplayerSessionDto} from '../../../../../../api/generated/models/page-multiplayer-session-dto';

interface MatchDecision {
  matchmakingSessionId: number;
  playerId: number;
}

interface SessionState {
  sessions: MultiplayerSessionDto[];
  currentPage: number;
  isLoading: boolean;
  hasMore: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class LobbyService {
  private searchingSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private showCancelMatchmakingSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private waitingForOpponentSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);
  private friendsSubject: BehaviorSubject<PlayerDto[]> = new BehaviorSubject<PlayerDto[]>(null);
  private sessionStateSubject: BehaviorSubject<SessionState> = new BehaviorSubject<SessionState>(null);

  readonly isSearching$ = this.searchingSubject.asObservable();
  readonly showCancelMatchmaking$ = this.showCancelMatchmakingSubject.asObservable();
  readonly waitingForOpponent$ = this.waitingForOpponentSubject.asObservable();
  readonly sessionState$ = this.sessionStateSubject.asObservable();

  readonly matchmakingState$ = combineLatest({
    isSearching: this.isSearching$,
    waitingForOpponent: this.waitingForOpponent$,
    showCancelMatchmaking: this.showCancelMatchmaking$
  });

  playerId: number;

  constructor(
    private matchmakingService: MultiplayerMatchmakingService,
    private webSocketService: WebSocketService,
    private matchFoundDialog: MatDialog,
    private matchConfirmedDialog: MatDialog,
    private alertMessageService: AlertMessageService,
    private router: Router,
    private friendService: FriendshipService,
    private multiplayerGameService: MultiplayerGameService,
    private friendListDialog: MatDialog,
    private multiplayerMatchService: MultiplayerMatchService,
  ) { }

  initLobbyData(playerId: number) {
    this.playerId = playerId;

    this.sessionStateSubject.next({
      sessions: [],
      currentPage: 0,
      isLoading: false,
      hasMore: true
    })

    this.friendService.getFriends({playerId: this.playerId}).pipe(
      tap(friends => this.friendsSubject.next(friends)),
      switchMap(() => this.multiplayerGameService.getPlayerSessions({
        playerId: this.playerId, pageable: {page: 0, size: 20}}).pipe(
        tap(pageResponse => {
          this.loadSessions(pageResponse)
        })
      ))
    ).subscribe()
  }

  findGame() {
    this.searchingSubject.next(true);

    setTimeout(() => {
      this.showCancelMatchmakingSubject.next(true);
    }, 500);


    this.webSocketService.sendMessage('/app/matchmaking/find', this.playerId);

    this.webSocketService.subscribe('/topic/match' + this.playerId,
      (matchUpdate) => {
        console.log("Match update received", matchUpdate);
        switch (matchUpdate.matchStatus) {
          case 'MATCHED':
            console.log("Match was found")
            this.searchingSubject.next(false);
            this.openMatchFoundDialog(matchUpdate.matchmakingSessionId, matchUpdate.opponentDisplayName)
            break;
          case 'ACCEPTED':
            this.showMatchConfirmation(matchUpdate.sessionId, matchUpdate.opponentDisplayName);
            break;
          case 'DECLINED':
            this.alertMessageService.show("The match was declined", "error")
            this.waitingForOpponentSubject.next(false);
            break;
          case 'WAITING_FOR_OTHER_PLAYER':
            this.waitingForOpponentSubject.next(true);
            break;
        }
      });
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
        playerId: this.playerId
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

  leaveMatchmaking() {
    this.searchingSubject.next(false);
    this.showCancelMatchmakingSubject.next(false);

    this.matchmakingService.cancelMatchmaking({playerId: this.playerId}).subscribe();
  }

   openGame(sessionId: number) {
    this.router.navigate(['multiplayer', sessionId]);
  }


  openFriendsDialog() {
    const dialogRef = this.friendListDialog.open(FriendsListDialogComponent, {
      data: {friends: this.friendsSubject.value},
      maxHeight: '90vh',
      width: '300px',
      disableClose: true,
    })
    dialogRef.afterClosed().subscribe((result) => {
      if (result.friendId) {
        this.multiplayerMatchService.requestMatch({
          body: {
            senderId: this.playerId,
            receiverId: result.friendId
          }
        }).subscribe(() => {
          this.alertMessageService.show("Sent match request!", "success")
        })
      }
    })
  }

  loadSessions(pageResponse: PageMultiplayerSessionDto) {
    const currentState = this.sessionStateSubject.getValue();
    const allSessions = [...currentState.sessions, ...pageResponse.content];

    const sortedSessions = allSessions.sort((a, b) => {
      if (a.status === 'ACTIVE' && b.status === 'COMPLETED') return -1;
      if (a.status === 'COMPLETED' && b.status === 'ACTIVE') return 1;
      return 0;
    });

    this.sessionStateSubject.next({
      sessions: [...sortedSessions],
      currentPage: currentState.currentPage + 1,
      hasMore: pageResponse.number < pageResponse.totalPages - 1,
      isLoading: false,
    });
  }

  loadMoreSessions() {
    const currentState = this.sessionStateSubject.getValue();
    if (currentState.isLoading || !currentState.hasMore) return;

    this.sessionStateSubject.next({ ...currentState, isLoading: true });

    this.multiplayerGameService.getPlayerSessions({
      playerId: this.playerId, pageable: {page: currentState.currentPage, size: 20}}).pipe(
      tap(pageResponse => {
        this.loadSessions(pageResponse)
      })
    ).subscribe();
  }
}
