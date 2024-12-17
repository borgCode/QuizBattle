import {Component, OnInit} from '@angular/core';
import {StoryService} from '../../../../api/generated/services/story.service';
import {NgForOf, NgIf} from '@angular/common';
import {ChapterDto} from '../../../../api/generated/models/chapter-dto';
import {ChapterCardComponent} from './components/chapter-card/chapter-card.component';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {ActivatedRoute} from '@angular/router';
import {PlayerProgressDto} from '../../../../api/generated/models/player-progress-dto';

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
    private activatedRoute: ActivatedRoute
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

}
