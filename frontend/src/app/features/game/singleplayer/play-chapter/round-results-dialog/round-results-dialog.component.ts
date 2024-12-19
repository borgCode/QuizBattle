import {Component, Inject} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle
} from '@angular/material/dialog';
import {NgClass, NgForOf} from '@angular/common';

@Component({
  selector: 'app-round-results-dialog',
  imports: [
    MatDialogContent,
    MatDialogTitle,
    MatDialogClose,
    NgForOf,
    NgClass
  ],
  templateUrl: './round-results-dialog.component.html',
  styleUrl: './round-results-dialog.component.css'
})
export class RoundResultsDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: {roundResults: boolean[]}
  ) {
    console.log(data.roundResults)
  }
}
