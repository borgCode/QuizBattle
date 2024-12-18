import {Component, Input} from '@angular/core';
import {QuestionDto} from '../../../../../../api/generated/models/question-dto';

@Component({
  selector: 'app-question-panel',
  imports: [],
  templateUrl: './question-panel.component.html',
  styleUrl: './question-panel.component.css'
})
export class QuestionPanelComponent {
  @Input() questions!: QuestionDto[];

  currentQuestionIndex = 0;


}
