import {Injectable} from '@angular/core';
import {MultiplayerGameService} from '../../../../../../api/generated/services/multiplayer-game.service';
import {GameStateResponse} from '../../../../../../api/generated/models/game-state-response';
import {BehaviorSubject, tap} from 'rxjs';
import {map} from 'rxjs/operators';

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
  ) {
  }

  getGameState(sessionId: number, playerId: number) {
    this.sessionId = sessionId;
    this.playerId = playerId;
    return this.multiplayerGameService.getGameState({sessionId: sessionId, playerId: playerId}).pipe(
      tap(gameState => this.gameStateSubject.next(gameState))
    )
  }

  readonly gameStatus$ = this.gameState$.pipe(
    map(state => state?.status)
  );
}
