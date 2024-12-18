import {Component, OnInit} from '@angular/core';
import {StoryService} from '../../../../api/generated/services/story.service';
import {NgForOf, NgIf} from '@angular/common';
import {ChapterDto} from '../../../../api/generated/models/chapter-dto';
import {ChapterCardComponent} from './components/chapter-card/chapter-card.component';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {ActivatedRoute, Router} from '@angular/router';
import {PlayerProgressDto} from '../../../../api/generated/models/player-progress-dto';
import {MatDialog} from '@angular/material/dialog';
import {ContentDialogComponent} from '../shared-components/content-dialog/content-dialog.component';

@Component({
  selector: 'app-story-overview',
  imports: [
    NgForOf,
    ChapterCardComponent,
    NgIf
  ],
  templateUrl: './story-overview.component.html',
  styleUrl: './story-overview.component.css'
})
export class StoryOverviewComponent implements OnInit {
  chapters: ChapterDto[]
  playerProgress: PlayerProgressDto;
  storyTitle: string;
  storyId: number

  constructor(
    private storyService: StoryService,
    private loginStateService: LoginStateService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private chapterDialog: MatDialog

  ) {
  }

  ngOnInit() {
    this.activatedRoute.paramMap.subscribe((params) => {
      this.storyId = +params.get('storyId')
      console.log(this.storyId)
    })

    this.storyService.getStoryOverview({
      body: {
        playerId: this.loginStateService.loggedInUser.id, storyId: this.storyId}
    }).subscribe({
      next: data=> {
        this.chapters = data.chapters;
        this.playerProgress = data.playerProgress
        this.storyTitle = data.title;
        console.log(this.playerProgress)
      }

    })
  }

  openLockedDialog(title: string, unlockCondition: string) {
    this.chapterDialog.open(ContentDialogComponent, {
      data: {storyTitle: title, message: unlockCondition, isLocked: true},
      maxHeight: '90vh',
      width: '300px',
    })
  }

  openChapterDialog(title: string, id: number, index: number) {
    let message: string
    if (this.playerProgress.completedChapters <= index) {
      message = "Would you like to start this story?"
    } else {
      message = "You've already completed this story, do you want to play it again?"
    }


    const dialogRef = this.chapterDialog.open(ContentDialogComponent, {
      data: {storyTitle: title, message: message},
      maxHeight: '90vh',
      width: '300px',
    })

    dialogRef.afterClosed().subscribe((result) => {
      if (result === "yes") {
        this.router.navigate(['singleplayer/story/chapter', id])
      }
    })
  }
}
