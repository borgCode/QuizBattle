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
    this.hasClickedOption = true
    this.answerSelected.emit({
      questionId: this.questions[this.currentQuestionIndex].questionId,
      answer: answer
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['isCorrect'] && changes['correctAnswerIndex']) {
      console.log("Iscorrect changed to :" + this.isCorrect);
      console.log("CorrectAnswerIndex changed to :" + this.correctAnswerIndex);
      this.showAnswerFeedback();
    }
  }

  private showAnswerFeedback() {
    if (this.isCorrect !== null && this.selectedAnswerIndex !== null) {
      const answerBox = document.querySelectorAll('.answer-box')[this.selectedAnswerIndex];
      if (this.isCorrect) {
        console.log("Answer is correct")
        answerBox.classList.add('correct');
      } else {
        console.log("Answer is incorrect")
        answerBox.classList.add('incorrect')
        const correctAnswer = document.querySelectorAll('.answer-box')[this.correctAnswerIndex];
        correctAnswer.classList.add('correct');
      }
    }
  }
  goToNextQuestion() {
    this.selectedAnswerIndex = null;
    this.answerIsCorrect = null;
    this.hasClickedOption = false;
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
    } else {
      this.navigateBackToScoreScreen.emit();
    }
  }

}
