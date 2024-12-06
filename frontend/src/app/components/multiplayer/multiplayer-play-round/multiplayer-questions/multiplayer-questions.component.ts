import {Component, EventEmitter, Input, Output} from '@angular/core';
import {QuestionDto} from '../../../../services/models/question-dto';

@Component({
  selector: 'app-multiplayer-questions',
  imports: [],
  templateUrl: './multiplayer-questions.component.html',
  styleUrl: './multiplayer-questions.component.css'
})
export class MultiplayerQuestionsComponent {
  @Input() questions: QuestionDto[] = [];
  @Output() answerSelected = new EventEmitter<{questionId: number, answer: string}>
  currentQuestionIndex = 0;

  selectAnswer(answer: string) {
    this.answerSelected.emit({
      questionId: this.questions[this.currentQuestionIndex].questionId,
      answer: answer});
  }


  goToNextQuestion() {
    //TODO Next question logic
    if (this.currentQuestionIndex < this.questions.length - 1) {
          this.currentQuestionIndex++;
        }
  }

}
