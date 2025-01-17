import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AsyncPipe, NgForOf, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {GameStateResponse} from '../../../../../../../api/generated/models/game-state-response';
import {PlayerCardComponent} from '../../../../../../../shared/components/player-card/player-card-component';
import {AlertMessageService} from '../../../../../../../core/services/alert-message/alert-message.service';
import {MatDialog} from '@angular/material/dialog';
import {GameOverDialogComponent} from '../../dialog/game-over-dialog/game-over-dialog.component';
import {GameResult} from '../../../../../../../shared/enums/game-result';
import {MultiplayerGameService} from '../../../../../../../api/generated/services/multiplayer-game.service';
import {MultiplayerMatchService} from '../../../../../../../api/generated/services/multiplayer-match.service';
import {GameService} from '../../service/game.service';
import {PlayerInteractionService} from '../../service/player-interaction.service';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {Observable} from 'rxjs';

interface Box {
  color: string;
}

interface BoxRow {
  left: Box[];
  right: Box[];
}

@Component({
  selector: 'app-multiplayer-score-window',
  imports: [
    NgForOf,
    NgIf,
    PlayerCardComponent,
    NgSwitch,
    NgSwitchCase,
    AsyncPipe
  ],
  templateUrl: './multiplayer-score-window.component.html',
  styleUrl: './multiplayer-score-window.component.css'
})
export class MultiplayerScoreWindowComponent implements OnInit {

  gameState!: GameStateResponse;
  boxes: BoxRow[] = [];
  storedPlayerId: number;
  sessionId: number;
  isGameOver: boolean = false;
  hasAcknowledgedGameOver: boolean;

  constructor(
    protected gameService: GameService,
    protected playerInteractionService: PlayerInteractionService,
    protected loginStateService: LoginStateService,
    private multiplayerGameService: MultiplayerGameService,
    private multiplayerMatchService: MultiplayerMatchService,
    private alertMessageService: AlertMessageService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private gameOverDialog: MatDialog,
  ) {
  }

  ngOnInit() {

    this.activatedRoute.params.subscribe(value => {
      this.sessionId = value['sessionId'];
      this.resetGameComponents();
      this.storedPlayerId = this.loginStateService.loggedInUser.id;
      this.initBoxes();
      this.getGameState();
    })
  }

  private resetGameComponents() {
    this.isGameOver = false;
    this.boxes = [];
  }

  private initBoxes() {
    for (let i = 0; i < 6; i++) {
      this.boxes.push({
        left: Array.from({length: 3}, () => ({color: '#ccc'})),
        right: Array.from({length: 3}, () => ({color: '#ccc'}))
      });
    }
  }

  private getGameState() {
    this.gameService.getGameState(this.sessionId, this.storedPlayerId).subscribe({
      next: gameState => {
        this.gameState = gameState;

        this.gameState.playerDTO.questionResults.forEach((result) => {
          const position = this.indexToBoxPosition(result.questionIndex);
          this.updateBoxColor('left', position.rowIndex, position.colIndex, result.correct)
        })

        this.gameState.opponentDTO.questionResults.forEach((result) => {
          const position = this.indexToBoxPosition(result.questionIndex);
          this.updateBoxColor('right', position.rowIndex, position.colIndex, result.correct)
        })

        this.hasAcknowledgedGameOver = gameState.playerDTO.givenUp;

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

  private indexToBoxPosition(questionIndex: number) {
    const width = 3;
    return {
      rowIndex: Math.floor(questionIndex / width),
      colIndex: questionIndex % width
    }
  }

  updateBoxColor(side: "left" | "right", rowIndex: number, colIndex: number, correct: boolean) {
    if (correct) {
      this.boxes[rowIndex][side][colIndex].color = '#66FF00'
    } else {
      this.boxes[rowIndex][side][colIndex].color = '#EF0107'
    }

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

  handleButtonClick() {
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

  backToMultiplayerPage() {
    this.router.navigate(['multiplayer']);
  }

  sendRematchRequest() {
    this.multiplayerMatchService.requestRematch({
      body: {
        sessionId: this.sessionId,
        playerId: this.storedPlayerId
      }
    }).subscribe({
      next: () => this.alertMessageService.show('Sent rematch request!', 'success'),
    })
  }

  giveUpClick() {
    this.multiplayerGameService.giveUp({sessionId: this.sessionId, playerId: this.storedPlayerId}).subscribe({
      next: () => {
        const currentUrl = this.router.url;
        this.router.navigateByUrl('/', {skipLocationChange: true}).then(() => {
          this.router.navigate([currentUrl]);
        });
      }
    })
  }
}
