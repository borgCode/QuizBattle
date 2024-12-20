import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogClose, MatDialogContent, MatDialogTitle} from '@angular/material/dialog';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-round-results-dialog',
  imports: [
    MatDialogContent,
    MatDialogTitle,
    MatDialogClose,
    NgForOf
  ],
  templateUrl: './round-results-dialog.component.html',
  styleUrl: './round-results-dialog.component.css'
})
export class RoundResultsDialogComponent {

  numOfCorrect: number;
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: {roundResults: boolean[], winCondition: number},

  ) {
    this.numOfCorrect = data.roundResults.filter(value => value).length
  }
}
