import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogContent, MatDialogRef, MatDialogTitle} from '@angular/material/dialog';
import {MatIcon} from '@angular/material/icon';
import {MatProgressSpinner} from '@angular/material/progress-spinner';
import {NgIf} from '@angular/common';
import {GameResult} from '../../../../../../shared/enums/game-result';

@Component({
  selector: 'app-game-over-dialog',
  imports: [
    MatDialogTitle,
    NgIf,
    MatDialogContent
  ],
  templateUrl: './game-over-dialog.component.html',
  styleUrl: './game-over-dialog.component.css'
})
export class GameOverDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<GameOverDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {gameResult: GameResult, opponentName: string}
  ) {
  }

  getGameResultMessage(): string {
    console.log(this.data.gameResult)
    switch (this.data.gameResult) {
      case GameResult.WIN:
        return "You won against " + this.data.opponentName + "!"
      case GameResult.LOSS:
        return "You lost against " + this.data.opponentName + "!"
      case GameResult.TIE:
        return "You tied against " + this.data.opponentName + "!"
      case GameResult.OPPONENT_GAVE_UP:
        return this.data.opponentName + " gave up!"
      case GameResult.PLAYER_GAVE_UP:
        return "You gave up against " + this.data.opponentName + "!"
      default:
        return "Unknown result"
    }
  }

  onConfirm() {
    this.dialogRef.close();
  }

}
