import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';
import {GameStateResponse} from '../../../../api/generated/models/game-state-response';
import {PlayerQuestionResult} from '../../../../api/generated/models/player-question-result';
import {MultiplayerService} from '../../../../api/generated/services/multiplayer.service';
import {PlayerCardComponent} from '../../../../shared/components/player-card/player-card-component';

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
    PlayerCardComponent
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

  constructor(
    private multiplayerService: MultiplayerService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
  ) {
  }

  ngOnInit() {
    console.log("Init player ID in score screen")
    this.initStoredPlayerId();
    console.log("Init boxes");
    this.initBoxes();
    console.log("Getting name state")
    this.getGameState();

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
        left: Array.from({ length: 3 }, () => ({ color: '#ccc' })),
        right: Array.from({ length: 3 }, () => ({ color: '#ccc' }))
      });
    }
  }

  private getGameState() {
    this.activatedRoute.params.subscribe(value => {
      this.sessionId = value['sessionId'];

      this.multiplayerService.getGameState({sessionId: this.sessionId}).subscribe({
        next: gameState => {
          this.gameState = gameState;

          this.opponentIndex = this.gameState.playerDTOS.findIndex(player =>
            player.id !== this.storedPlayerId
          );


          this.results = this.gameState.results
          for (const result of this.results) {
            console.log(result)
          }


          this.results.forEach((result) => {
              console.log(`Result playerId: ${result.playerId}, Stored playerId: ${this.storedPlayerId}`);
              if (result.playerId == this.storedPlayerId) {
                const position = this.indexToBoxPosition(result.questionIndex);
                console.log(`Index: ${result.questionIndex}, Position: Row ${position.rowIndex}, Col ${position.colIndex}`);
                console.log(`Result Correct: ${result.correct}, Side: ${result.playerId === this.storedPlayerId ? 'left' : 'right'}`);
                this.updateBoxColor('left', position.rowIndex, position.colIndex, result.correct)
              } else {
                const position = this.indexToBoxPosition(result.questionIndex);
                console.log(`Index: ${result.questionIndex}, Position: Row ${position.rowIndex}, Col ${position.colIndex}`);
                console.log(`Result Correct: ${result.correct}, Side: ${result.playerId === this.storedPlayerId ? 'left' : 'right'}`);
                this.updateBoxColor('right', position.rowIndex, position.colIndex, result.correct);
              }
            }
          )
        }
      })
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
    console.log("Result is: " + correct);
    if (correct) {
      console.log("Result in green is: " + correct)
      this.boxes[rowIndex][side][colIndex].color = '#66FF00'
    } else {
      console.log("Result in red is: " + correct)
      this.boxes[rowIndex][side][colIndex].color = '#EF0107'
    }

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

    console.log("Open play questions: " + this.gameState.questionIds)
    this.router.navigate(['multiplayer', this.sessionId, 'play'],
      {state: {questionIds: this.gameState.questionIds}});
  }

  openCategorySelection() {
    this.router.navigate(['multiplayer', this.sessionId, 'play']);
  }



}
