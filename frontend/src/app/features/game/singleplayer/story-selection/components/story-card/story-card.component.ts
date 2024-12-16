import {Component, Input, OnInit} from '@angular/core';
import {StoryDto} from '../../../../../../api/generated/models/story-dto';


@Component({
  selector: 'app-story-card',
  imports: [],
  templateUrl: './story-card.component.html',
  styleUrl: './story-card.component.css'
})
export class StoryCardComponent implements OnInit{
  @Input() story!: StoryDto;

  ngOnInit() {
    console.log(this.story)
  }

}
