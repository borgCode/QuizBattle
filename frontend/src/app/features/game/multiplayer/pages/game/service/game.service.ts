import {Injectable} from '@angular/core';
import {MultiplayerGameService} from '../../../../../../api/generated/services/multiplayer-game.service';
import {GameStateResponse} from '../../../../../../api/generated/models/game-state-response';
import {BehaviorSubject, tap} from 'rxjs';
import {MultiplayerMatchService} from '../../../../../../api/generated/services/multiplayer-match.service';
import {AlertMessageService} from '../../../../../../core/services/alert-message/alert-message.service';
import {Router} from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private gameStateSubject = new BehaviorSubject<GameStateResponse | null>(null);
  readonly gameState$ = this.gameStateSubject.asObservable()

  sessionId: number;
  playerId: number;


  constructor(
    private multiplayerGameService: MultiplayerGameService,
    private multiplayerMatchService: MultiplayerMatchService,
    private alertMessageService: AlertMessageService,
    private router: Router,
  ) {
  }

  getGameState(sessionId: number, playerId: number) {
    this.sessionId = sessionId;
    this.playerId = playerId;
    return this.multiplayerGameService.getGameState({sessionId: sessionId, playerId: playerId}).pipe(
      tap(gameState => this.gameStateSubject.next(gameState))
    )
  }


  requestRematch() {
    this.multiplayerMatchService.requestRematch({
      body: {
        sessionId: this.sessionId,
        playerId: this.playerId
      }
    }).subscribe({
      next: () => this.alertMessageService.show('Sent rematch request!', 'success'),
    })
  }

  handleGiveUp() {
    this.multiplayerGameService.giveUp({sessionId: this.sessionId, playerId: this.playerId}).subscribe({
      next: () => {
        const currentUrl = this.router.url;
        this.router.navigateByUrl('/', {skipLocationChange: true}).then(() => {
          this.router.navigate([currentUrl]);
        });
      }
    })
  }
}
