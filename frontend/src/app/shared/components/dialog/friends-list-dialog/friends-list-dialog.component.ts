import {Component, Inject} from '@angular/core';
import {MatList, MatListItem} from '@angular/material/list';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {MatButton} from '@angular/material/button';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-friends-list-dialog',
  imports: [
    MatListItem,
    MatList,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatButton,
    NgForOf,
    NgIf
  ],
  templateUrl: './friends-list-dialog.component.html',
  styleUrl: './friends-list-dialog.component.css'
})
export class FriendsListDialogComponent {

  constructor(
    public dialogRef: MatDialogRef<FriendsListDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {friends: PlayerDto[] }
  ) {
    console.log(data.friends)

  }


  closeDialog() {
    this.dialogRef.close();
  }
}
