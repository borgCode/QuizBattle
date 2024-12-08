import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {NgForOf, NgIf} from '@angular/common';
import {QuestionDto} from '../../../../../api/generated/models/question-dto';

@Component({
  selector: 'app-multiplayer-questions',
  imports: [
    NgForOf,
  ],
  templateUrl: './multiplayer-questions.component.html',
  styleUrl: './multiplayer-questions.component.css'
})
export class MultiplayerQuestionsComponent {
  @Input() questions: QuestionDto[] = [];
  @Input() isCorrect: boolean | null = null;
  @Input() correctAnswerIndex: number | null = null;
  @Output() answerSelected = new EventEmitter<{ questionId: number, answer: string }>
  @Output() navigateBackToScoreScreen = new EventEmitter<void>();

  currentQuestionIndex = 0;
  selectedAnswerIndex: number | null = null;
  hasClickedOption: boolean | null = null;


  selectAnswer(answer: string, i: number) {
    this.selectedAnswerIndex = i;
    this.hasClickedOption = true
    this.answerSelected.emit({
      questionId: this.questions[this.currentQuestionIndex].questionId,
      answer: answer
    });
  }

  goToNextQuestion() {
    this.selectedAnswerIndex = null;
    this.isCorrect = null;
    this.correctAnswerIndex = null;
    this.hasClickedOption = false;
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
    } else {
      this.navigateBackToScoreScreen.emit();
    }
  }

}
