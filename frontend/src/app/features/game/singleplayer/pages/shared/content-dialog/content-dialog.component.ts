import {Component, inject, Inject} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent, MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';
import {A11yModule} from '@angular/cdk/a11y';
import {NgIf} from '@angular/common';


@Component({
  selector: 'app-content-dialog',
  imports: [
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatButton,
    MatDialogClose,
    A11yModule,
    NgIf
  ],
  templateUrl: './content-dialog.component.html',
  styleUrl: './content-dialog.component.css'
})
export class ContentDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { contentTitle: string, message: string, onlyOkButton: boolean}
  ) {
  }
}
