import {Component, Input} from '@angular/core';
import {ChapterOverviewDto} from '../../../../../../../api/generated/models/chapter-overview-dto';



@Component({
  selector: 'app-chapter-card',
  imports: [
  ],
  templateUrl: './chapter-card.component.html',
  styleUrl: './chapter-card.component.css'
})
export class ChapterCardComponent{
  @Input() chapter!: ChapterOverviewDto;

}
