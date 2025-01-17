import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AsyncPipe, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {GameStateResponse} from '../../../../../../../api/generated/models/game-state-response';
import {PlayerCardComponent} from '../../../../../../../shared/components/player-card/player-card-component';
import {MatDialog} from '@angular/material/dialog';
import {GameOverDialogComponent} from '../../dialog/game-over-dialog/game-over-dialog.component';
import {GameResult} from '../../../../../../../shared/enums/game-result';
import {MultiplayerGameService} from '../../../../../../../api/generated/services/multiplayer-game.service';
import {GameService} from '../../service/game.service';
import {PlayerInteractionService} from '../../service/player-interaction.service';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {ScoreBoxesComponent} from '../score-boxes/score-boxes.component';


@Component({
  selector: 'app-multiplayer-score-window',
  imports: [
    NgIf,
    PlayerCardComponent,
    NgSwitch,
    NgSwitchCase,
    AsyncPipe,
    ScoreBoxesComponent
  ],
  templateUrl: './multiplayer-score-window.component.html',
  styleUrl: './multiplayer-score-window.component.css'
})
export class MultiplayerScoreWindowComponent implements OnInit {

  gameState!: GameStateResponse;
  storedPlayerId: number;
  sessionId: number;
  isGameOver: boolean = false;
  hasAcknowledgedGameOver: boolean;
  resetBoxesTrigger = false;

  constructor(
    protected gameService: GameService,
    protected playerInteractionService: PlayerInteractionService,
    protected loginStateService: LoginStateService,
    private multiplayerGameService: MultiplayerGameService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private gameOverDialog: MatDialog,
  ) {
  }

  ngOnInit() {

    this.activatedRoute.params.subscribe(value => {
      this.sessionId = value['sessionId'];
      this.resetComponents();
      this.storedPlayerId = this.loginStateService.loggedInUser.id;
      this.getGameState();
    })
  }

  private resetComponents() {
    this.isGameOver = false;
    this.resetBoxesTrigger = false;
    setTimeout(() => {
      this.resetBoxesTrigger = true;
    });
  }


  private getGameState() {
    this.gameService.getGameState(this.sessionId, this.storedPlayerId).subscribe({
      next: gameState => {
        this.gameState = gameState;

        this.hasAcknowledgedGameOver = gameState.playerDTO.hasAcknowledgedGameOver;

        if (gameState.status == 'COMPLETED') {
          this.isGameOver = true;

          if (!this.hasAcknowledgedGameOver) {
            this.handleGameOver();
          }
        }
        this.playerInteractionService.loadRelationshipStatus(this.storedPlayerId, this.gameState.opponentDTO.playerId)
      }
    })
  }

  private handleGameOver() {
    const gameResult: GameResult = (() => {
      if (this.gameState.playerWhoGaveUp) {
        console.log("A player gave up: " + this.gameState.playerWhoGaveUp)
        if (this.storedPlayerId == this.gameState.playerWhoGaveUp) {
          console.log("Player gave up")
          return GameResult.PLAYER_GAVE_UP
        } else {
          console.log("Opponent gave up")
          return GameResult.OPPONENT_GAVE_UP
        }
      }
      if (this.storedPlayerId == this.gameState.sessionPlayerWinnerId) {
        return GameResult.WIN;
      } else if (this.storedPlayerId == this.gameState.sessionPlayerLoserId) {
        return GameResult.LOSS
      } else {
        return GameResult.TIE
      }
    })();

    this.showGameOverDialog(gameResult);

    console.log("Sending complete game")
    this.multiplayerGameService.acknowledgeGameOver({
      sessionId: this.sessionId,
      playerId: this.storedPlayerId
    }).subscribe({
      next: () => console.log('Request successful!'),
      error: (err) => console.error('Error occurred:', err),
    });
  }

  private showGameOverDialog(gameResult: GameResult) {
    this.gameOverDialog.open(GameOverDialogComponent, {
      data: {gameResult: gameResult, opponentName: this.gameState.opponentDTO.displayName},
      width: '300px',
      disableClose: true,
      autoFocus: false
    })
  }

  handlePlayButtonClick() {
    console.log(this.gameState.questionIds.length)
    if (this.gameState?.questionIds?.length > 0) {
      this.openPlayQuestions();
    } else {
      this.openCategorySelection();
    }
  }

  //When player should play the same category as the other player
  openPlayQuestions() {

    this.router.navigate(['multiplayer', this.sessionId, 'play'],
      {state: {questionIds: this.gameState.questionIds}});
  }

  openCategorySelection() {
    this.router.navigate(['multiplayer', this.sessionId, 'play']);
  }


  sendFriendRequest() {
    this.playerInteractionService.sendFriendRequest();
  }

  cancelFriendRequest() {
    this.playerInteractionService.cancelFriendRequest();
  }

  blockPlayer() {
    this.playerInteractionService.blockPlayer();

  }

  unBlockPlayer() {
    this.playerInteractionService.unblockPlayer();
  }

  removeFriend() {
    this.playerInteractionService.removeFriend();
  }

  acceptFriend() {
    this.playerInteractionService.acceptFriend();
  }

  sendRematchRequest() {
    this.gameService.requestRematch();
  }

  giveUpClick() {
    this.gameService.handleGiveUp();
  }

  backToMultiplayerPage() {
    this.router.navigate(['multiplayer']);
  }


}
