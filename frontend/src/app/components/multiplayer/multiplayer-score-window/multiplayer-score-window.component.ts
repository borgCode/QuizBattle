import {Component, OnInit} from '@angular/core';
import {MultiplayerService} from '../../../services/services/multiplayer.service';
import {ActivatedRoute, Router} from '@angular/router';
import {NgForOf, NgIf} from '@angular/common';
import {GameStateResponse} from '../../../services/models/game-state-response';
import {PlayerQuestionResult} from '../../../services/models/player-question-result';

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
    NgIf
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
        left: Array(3).fill({color: '#ccc'}),
        right: Array(3).fill({color: '#ccc'})
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
    if (correct) {
      this.boxes[rowIndex][side][colIndex].color = '#66FF00'
    } else {
      this.boxes[rowIndex][side][colIndex].color = '#EF0107'
    }

  }


  openCategorySelection() {
    this.router.navigate(['multiplayer', this.sessionId, 'play']);
  }
}
