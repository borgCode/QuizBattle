import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-match-found-dialog',
  imports: [],
  templateUrl: './match-found-dialog.component.html',
  styleUrl: './match-found-dialog.component.css'
})
export class MatchFoundDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<MatchFoundDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {opponentName: string}
  ) {}



  onAccept() {
    this.dialogRef.close(true);
  }

  onDecline() {
    this.dialogRef.close(false);
  }
}
