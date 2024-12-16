import {Component, OnInit} from '@angular/core';
import {Story} from '../../../../api/generated/models/story';
import {StoryService} from '../../../../api/generated/services/story.service';
import {StoryCardComponent} from './components/story-card/story-card.component';
import {NgForOf} from '@angular/common';

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
  stories: Story[]

  constructor(
    private storyService: StoryService,
  ) {
  }

  ngOnInit() {
    this.storyService.getAllStories().subscribe({
      next: stories => {
        this.stories = stories;
      }
    })

  }

}
