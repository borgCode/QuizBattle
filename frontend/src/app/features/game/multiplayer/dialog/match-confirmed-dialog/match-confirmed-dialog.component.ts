import {Component, Inject} from '@angular/core';
import {MatIcon} from "@angular/material/icon";
import {MAT_DIALOG_DATA, MatDialog, MatDialogContent, MatDialogRef, MatDialogTitle} from "@angular/material/dialog";
import {MatProgressSpinner} from "@angular/material/progress-spinner";

@Component({
  selector: 'app-match-confirmed-dialog',
  imports: [
    MatIcon,
    MatDialogTitle,
    MatDialogContent,
    MatProgressSpinner
  ],
  templateUrl: './match-confirmed-dialog.component.html',
  styleUrl: './match-confirmed-dialog.component.css'
})
export class MatchConfirmedDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA)public data: {opponentName: string}
  ) {
  }

}
