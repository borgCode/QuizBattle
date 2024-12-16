import {Component, OnInit} from '@angular/core';
import {StoryService} from '../../../../api/generated/services/story.service';
import {StoryCardComponent} from './components/story-card/story-card.component';
import {NgForOf} from '@angular/common';
import {StoryDto} from '../../../../api/generated/models/story-dto';

import {MatDialog} from '@angular/material/dialog';
import {StartStoryDialogComponent} from './components/start-story-dialog/start-story-dialog.component';

@Component({
  selector: 'app-story-selection',
  imports: [
    StoryCardComponent,
    NgForOf
  ],
  templateUrl: './story-selection.component.html',
  styleUrl: './story-selection.component.css'
})
export class StorySelectionComponent implements OnInit{
  stories: StoryDto[]

  constructor(
    private storyService: StoryService,
    private startDialog: MatDialog,
  ) {
  }

  ngOnInit() {
    this.storyService.getAllStories().subscribe({
      next: stories => {
        this.stories = stories;
      }
    })

  }

  showStartStoryDialog(title: string, id: number) {
    const dialogRef = this.startDialog.open(StartStoryDialogComponent, {
      data: {storyTitle: title},
      maxHeight: '90vh',
      width: '300px',
    })


    console.log(title)
    dialogRef.afterClosed().subscribe((result)  => {
      if (result === "yes") {
        console.log("accepted")
      } else {
        console.log("declined")
      }
    })
  }
}
