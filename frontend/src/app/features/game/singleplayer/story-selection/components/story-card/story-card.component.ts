import {Component, Input} from '@angular/core';
import { Story } from '../../../../../../api/generated/models';

@Component({
  selector: 'app-story-card',
  imports: [],
  templateUrl: './story-card.component.html',
  styleUrl: './story-card.component.css'
})
export class StoryCardComponent {
  @Input() story!: Story;

}
