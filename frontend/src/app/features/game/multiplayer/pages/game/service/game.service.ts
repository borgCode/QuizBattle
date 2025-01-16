import {Injectable} from '@angular/core';
import {MultiplayerGameService} from '../../../../../../api/generated/services/multiplayer-game.service';

@Injectable({
  providedIn: 'root'
})
export class GameService {

  constructor(
    private multiplayerGameService: MultiplayerGameService,
  ) {
  }

  getGameState(sessionId: number, playerId: number) {
    return this.multiplayerGameService.getGameState({sessionId: sessionId, playerId: playerId})
  }
}
