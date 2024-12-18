import {Component, Input} from '@angular/core';
import {QuestionDto} from '../../../../../../api/generated/models/question-dto';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-question-panel',
  imports: [
    NgForOf
  ],
  templateUrl: './question-panel.component.html',
  styleUrl: './question-panel.component.css'
})
export class QuestionPanelComponent {
  @Input() questions!: QuestionDto[];

  currentQuestionIndex = 0;


}
