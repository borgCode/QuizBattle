import {Component, Input} from '@angular/core';
import {ChapterNoCategoriesDto} from '../../../../../../api/generated/models/chapter-no-categories-dto';



@Component({
  selector: 'app-chapter-card',
  imports: [
  ],
  templateUrl: './chapter-card.component.html',
  styleUrl: './chapter-card.component.css'
})
export class ChapterCardComponent{
  @Input() chapter!: ChapterNoCategoriesDto;

}
