import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-match-found-dialog',
  imports: [],
  templateUrl: './match-found-dialog.component.html',
  standalone: true,
  styleUrl: './match-found-dialog.component.css'
})
export class MatchFoundDialogComponent implements OnInit {
  duration: number = 10;
  timeLeft: number = 0;
  progressPercentage: number = 100;
  interval: any;

  constructor(
    public dialogRef: MatDialogRef<MatchFoundDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { opponentName: string }
  ) {

  }

  ngOnInit() {
    this.startTimer()
  }

  startTimer() {
    this.timeLeft = this.duration;
    this.interval = setInterval(() => {
      if (this.timeLeft > 0) {
        this.timeLeft -= 0.25;
        this.progressPercentage = (this.timeLeft / this.duration) * 100;
      } else if (this.timeLeft <= 0) {
        this.dialogRef.close({timedOut: true})
      }
    }, 250);
  }


  onAccept() {
    this.dialogRef.close(true);
  }

  onDecline() {
    this.dialogRef.close(false);
  }
}
