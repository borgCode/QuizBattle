import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {GameService} from '../../service/game.service';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';

interface Box {
  color: string;
}

interface BoxRow {
  left: Box[];
  right: Box[];
}

@Component({
  selector: 'app-score-boxes',
  imports: [
    NgForOf,
    NgIf,
    AsyncPipe
  ],
  templateUrl: './score-boxes.component.html',
  styleUrl: './score-boxes.component.css'
})
export class ScoreBoxesComponent implements OnInit, OnChanges {
  @Input() resetTrigger: boolean;
  boxes: BoxRow[] = [];

  constructor(
    protected gameService: GameService
  ) {
  }

  ngOnInit() {
    this.initBoxes();

    this.gameService.gameState$.subscribe(gameState => {
      if (gameState) {

        gameState.playerDTO.questionResults.forEach(result => {
          const position = this.indexToBoxPosition(result.questionIndex);
          this.updateBoxColor('left', position.rowIndex, position.colIndex, result.correct);
        });

        gameState.opponentDTO.questionResults.forEach(result => {
          const position = this.indexToBoxPosition(result.questionIndex);
          this.updateBoxColor('right', position.rowIndex, position.colIndex, result.correct);
        });
      }
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resetTrigger'] && changes['resetTrigger'].currentValue) {
      this.initBoxes();
    }
  }

  private initBoxes() {
    this.boxes = [];
    for (let i = 0; i < 6; i++) {
      this.boxes.push({
        left: Array.from({length: 3}, () => ({color: '#ccc'})),
        right: Array.from({length: 3}, () => ({color: '#ccc'}))
      });
    }
  }

  private indexToBoxPosition(questionIndex: number) {
    const width = 3;
    return {
      rowIndex: Math.floor(questionIndex / width),
      colIndex: questionIndex % width
    }
  }

  private updateBoxColor(side: "left" | "right", rowIndex: number, colIndex: number, correct: boolean) {
    if (correct) {
      this.boxes[rowIndex][side][colIndex].color = '#66FF00'
    } else {
      this.boxes[rowIndex][side][colIndex].color = '#EF0107'
    }

  }
}


