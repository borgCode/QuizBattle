import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogContent, MatDialogRef, MatDialogTitle} from '@angular/material/dialog';
import {MatIcon} from '@angular/material/icon';
import {MatProgressSpinner} from '@angular/material/progress-spinner';
import {NgIf} from '@angular/common';

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
    @Inject(MAT_DIALOG_DATA) public data: {isWon: boolean, opponentName: string}
  ) {
  }

  onConfirm() {
    this.dialogRef.close();
  }

}
