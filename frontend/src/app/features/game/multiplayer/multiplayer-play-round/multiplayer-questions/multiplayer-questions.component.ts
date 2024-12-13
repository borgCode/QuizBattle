import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild} from '@angular/core';
import {NgForOf, NgIf} from '@angular/common';
import {QuestionDto} from '../../../../../api/generated/models/question-dto';
import {TimerComponent} from './timer/timer.component';

@Component({
  selector: 'app-multiplayer-questions',
  imports: [
    NgForOf,
    TimerComponent,
  ],
  templateUrl: './multiplayer-questions.component.html',
  styleUrl: './multiplayer-questions.component.css'
})
export class MultiplayerQuestionsComponent {
  @ViewChild(TimerComponent) timerComponent!: TimerComponent;
  @Input() questions: QuestionDto[] = [];
  @Input() isCorrect: boolean | null = null;
  @Input() correctAnswerIndex: number | null = null;
  @Output() answerSelected = new EventEmitter<{ questionId: number, answer: string }>
  @Output() navigateBackToScoreScreen = new EventEmitter<void>();
  @Output() timerRanOut = new EventEmitter<{questionId: number}>;

  currentQuestionIndex = 0;
  selectedAnswerIndex: number | null = null;
  hasClickedOption: boolean | null = null;
  timerHasRanOut: boolean = false;
  userClickedNext: boolean = false;


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
    this.timerHasRanOut = false;
    this.userClickedNext = true;
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
      this.timerComponent.resetTimer();
      this.timerComponent.startTimer();
    } else {
      this.navigateBackToScoreScreen.emit();
    }
  }

  onTimerRanOut() {
    if (!this.timerHasRanOut) {
      this.timerHasRanOut = true;
      console.log(this.timerHasRanOut)
      this.timerRanOut.emit({questionId: this.questions[this.currentQuestionIndex].questionId});
    }
  }

}
