import {Component, OnInit} from '@angular/core';
import {StoryService} from '../../../../api/generated/services/story.service';
import {StoryCardComponent} from './components/story-card/story-card.component';
import {NgForOf} from '@angular/common';
import {StoryDto} from '../../../../api/generated/models/story-dto';

import {MatDialog} from '@angular/material/dialog';
import {ContentDialogComponent} from '../shared-components/content-dialog/content-dialog.component';
import {Router} from '@angular/router';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {PlayerProgressDto} from '../../../../api/generated/models/player-progress-dto';

@Component({
  selector: 'app-story-selection',
  imports: [
    StoryCardComponent,
    NgForOf
  ],
  templateUrl: './story-selection.component.html',
  styleUrl: './story-selection.component.css'
})
export class StorySelectionComponent implements OnInit {
  stories: StoryDto[]
  playerProgress: PlayerProgressDto[]


  constructor(
    private storyService: StoryService,
    private startDialog: MatDialog,
    private router: Router,
    private loginStateService: LoginStateService
  ) {
  }

  ngOnInit() {
    this.storyService.getAllStories({playerId: this.loginStateService.loggedInUser.id}).subscribe({
      next: data => {
        this.stories = data.stories;
        this.playerProgress = data.playerProgressList;
      }
    })

  }

  showStartStoryDialog(title: string, id: number, index: number) {
    let message: string

    switch (this.playerProgress[index].progressStatus) {
      case "NOT_STARTED":
        message = "Would you like to start this story?"
        break;
      case "IN_PROGRESS":
        message = "Would you like to continue this story?"
        break;
      case "COMPLETED":
        message = "You've already completed this story, do you want to play it again?"
        break;
    }
    const dialogRef = this.startDialog.open(ContentDialogComponent, {
      data: {contentTitle: title, message: message},
      maxHeight: '90vh',
      width: '300px',
    })


    dialogRef.afterClosed().subscribe((result) => {
      if (result === "yes") {
        this.router.navigate(['singleplayer/story', id])
      }
    })
  }
}
