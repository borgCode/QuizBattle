import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {QuestionDto} from '../../../../services/models/question-dto';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-multiplayer-questions',
  imports: [
    NgForOf
  ],
  templateUrl: './multiplayer-questions.component.html',
  styleUrl: './multiplayer-questions.component.css'
})
export class MultiplayerQuestionsComponent implements OnChanges {
  @Input() questions: QuestionDto[] = [];
  @Input() isCorrect: boolean | null = null;
  @Input() correctAnswerIndex: number | null = null;
  @Output() answerSelected = new EventEmitter<{ questionId: number, answer: string }>
  @Output() navigateBackToScoreScreen = new EventEmitter<void>();
  currentQuestionIndex = 0;
  selectedAnswerIndex: number | null = null;
  answerIsCorrect: boolean | null = null;
  hasClickedOption: boolean | null = null;

  selectAnswer(answer: string, i: number) {
    this.selectedAnswerIndex = i;
    this.answerSelected.emit({
      questionId: this.questions[this.currentQuestionIndex].questionId,
      answer: answer
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['isCorrect'] && changes['correctAnswerIndex']) {
      this.showAnswerFeedback();
    }
  }

  private showAnswerFeedback() {
    if (this.isCorrect !== null && this.selectedAnswerIndex !== null) {
      this.hasClickedOption = true
      const answerBox = document.querySelectorAll('.answer-box')[this.selectedAnswerIndex];
      if (this.isCorrect) {
        answerBox.classList.add('correct');
      } else {
        answerBox.classList.add('incorrect')
        const correctAnswer = document.querySelectorAll('.answer-box')[this.correctAnswerIndex];
        correctAnswer.classList.add('correct');
      }
    }
  }
  goToNextQuestion() {
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
      this.hasClickedOption = false;
    } else {
      this.navigateBackToScoreScreen.emit();
    }
  }
}
