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


@Component({
  selector: 'app-start-story-dialog',
  imports: [
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatButton,
    MatDialogClose,
    A11yModule
  ],
  templateUrl: './start-story-dialog.component.html',
  styleUrl: './start-story-dialog.component.css'
})
export class StartStoryDialogComponent {

  constructor(
    private dialogRef: MatDialogRef<StartStoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { storyTitle: string }

  ) {

    console.log(data.storyTitle)
  }


  onNoClick() {
    this.dialogRef.close();
  }
}
