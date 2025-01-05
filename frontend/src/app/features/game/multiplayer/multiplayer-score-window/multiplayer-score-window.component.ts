import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {NgForOf, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {GameStateResponse} from '../../../../api/generated/models/game-state-response';
import {PlayerQuestionResult} from '../../../../api/generated/models/player-question-result';
import {MultiplayerService} from '../../../../api/generated/services/multiplayer.service';
import {PlayerCardComponent} from '../../../../shared/components/player-card/player-card-component';
import {AlertMessageService} from '../../../../core/services/alert-message/alert-message.service';
import {MatDialog} from '@angular/material/dialog';
import {GameOverDialogComponent} from './dialog/game-over-dialog/game-over-dialog.component';
import {GameResult} from '../../../../shared/enums/game-result';
import {FriendshipService} from '../../../../api/generated/services/friendship.service';

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
    NgSwitchCase
  ],
  templateUrl: './multiplayer-score-window.component.html',
  styleUrl: './multiplayer-score-window.component.css'
})
export class MultiplayerScoreWindowComponent implements OnInit {
  gameState!: GameStateResponse;
  boxes: BoxRow[] = [];
  opponentIndex: number | null = null;
  results: Array<PlayerQuestionResult> = [];
  storedPlayerId: number;
  sessionId: number;
  isGameOver: boolean = false;
  playerTotalScore: number;
  opponentTotalScore: number;
  opponentId: number;
  opponentDisplayName: string;
  opponentAvatar: string = '';
  hasAcknowledgedGameOver: boolean;

  friendshipStatus: string;

  constructor(
    private multiplayerService: MultiplayerService,
    private friendshipService: FriendshipService,
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
      this.initStoredPlayerId();
      this.initBoxes();
      this.getGameState();
    })
  }

  private resetGameComponents() {
    this.isGameOver = false;
    this.boxes = [];
  }

  private initStoredPlayerId() {
    const storedPlayer = localStorage.getItem('loggedInUser');
    if (storedPlayer) {
      this.storedPlayerId = JSON.parse(storedPlayer).id;
    }
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

    this.multiplayerService.getGameState({sessionId: this.sessionId}).subscribe({
      next: gameState => {
        this.gameState = gameState;

        this.opponentIndex = this.gameState.playerDTOS.findIndex(player => player.id !== this.storedPlayerId);
        this.opponentId = this.gameState.playerDTOS[this.opponentIndex].id;

        this.playerTotalScore = gameState.scores[gameState.playerDTOS[(this.opponentIndex + 1) % 2].id];
        this.opponentTotalScore = gameState.scores[gameState.playerDTOS[this.opponentIndex].id];

        this.opponentDisplayName = gameState.playerDTOS[this.opponentIndex].displayName;
        this.opponentAvatar = 'data:image/jpeg;base64,' + gameState.playerDTOS[this.opponentIndex].base64Image;

        this.results = this.gameState.results

        this.results.forEach((result) => {
            if (result.playerId == this.storedPlayerId) {
              const position = this.indexToBoxPosition(result.questionIndex);
              this.updateBoxColor('left', position.rowIndex, position.colIndex, result.correct)
            } else {
              const position = this.indexToBoxPosition(result.questionIndex);
              this.updateBoxColor('right', position.rowIndex, position.colIndex, result.correct);
            }
          }
        )
        this.hasAcknowledgedGameOver = gameState.playerAcknowledgment[this.storedPlayerId];

        if (gameState.status == 'COMPLETED') {
          this.isGameOver = true;

          if (!this.hasAcknowledgedGameOver) {
            this.handleGameOver();
          }
        }
        this.getFriendshipStatus()
      }
    });
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

  private getFriendshipStatus() {
    this.friendshipService.getRelationshipStatus({
      relationshipStatusRequest: {
        playerId: this.storedPlayerId,
        targetPlayerId: this.opponentId
      }
    }).subscribe({
      next: value => {
        if (value) {
          this.friendshipStatus = value;
        }

      },
      error: err => {
        console.log(err)
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
      if (this.storedPlayerId == this.gameState.winnerId) {
        return GameResult.WIN;
      } else if (this.storedPlayerId == this.gameState.loserId) {
        return GameResult.LOSS
      } else {
        return GameResult.TIE
      }
    })();

    this.showGameOverDialog(gameResult);

    console.log("Sending complete game")
    this.multiplayerService.acknowledgeGameOver({sessionId: this.sessionId, playerId: this.storedPlayerId}).subscribe({
      next: () => console.log('Request successful!'),
      error: (err) => console.error('Error occurred:', err),
    });
  }

  private showGameOverDialog(gameResult: GameResult) {
    this.gameOverDialog.open(GameOverDialogComponent, {
      data: {gameResult: gameResult, opponentName: this.opponentDisplayName},
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
    this.friendshipService.addFriend({body: {senderId: this.storedPlayerId, receiverId: this.opponentId}}).subscribe({
      next: () => {
        console.log("Stored id: " + this.storedPlayerId)
        console.log("Opponent id: " + this.opponentId)
        this.alertMessageService.show('Friend request sent successfully', 'success')
        this.getFriendshipStatus();
      },
    });
  }

  cancelFriendRequest() {
    this.friendshipService.removeAsFriend({body: {senderId: this.storedPlayerId, receiverId: this.opponentId}}).subscribe({
      next: () => {
        console.log("Stored id: " + this.storedPlayerId)
        console.log("Opponent id: " + this.opponentId)
        this.alertMessageService.show('Friend request canceled', 'success')
        this.getFriendshipStatus();
      },
    });
  }

  backToMultiplayerPage() {
    this.router.navigate(['multiplayer']);
  }

  sendRematchRequest() {
    this.multiplayerService.requestRematch({
      body: {
        sessionId: this.sessionId,
        playerId: this.storedPlayerId
      }
    }).subscribe({
      next: () => this.alertMessageService.show('Send rematch request!', 'success'),
    })
  }


  giveUpClick() {
    this.multiplayerService.giveUp({sessionId: this.sessionId, playerId: this.storedPlayerId}).subscribe({
      next: () => {
        const currentUrl = this.router.url;
        this.router.navigateByUrl('/', {skipLocationChange: true}).then(() => {
          this.router.navigate([currentUrl]);
        });
      }
    })
  }
}
