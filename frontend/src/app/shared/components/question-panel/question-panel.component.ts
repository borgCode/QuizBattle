import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild} from '@angular/core';
import {TimerComponent} from './timer/timer.component';
import {QuestionDto} from '../../../api/generated/models/question-dto';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-question-panel',
  imports: [
    NgForOf,
    TimerComponent
  ],
  templateUrl: './question-panel.component.html',
  styleUrl: './question-panel.component.css'
})
export class QuestionPanelComponent implements OnChanges{
  @ViewChild(TimerComponent) timerComponent!: TimerComponent;
  @Input() questions: QuestionDto[] = [];
  @Input() isCorrect: boolean | null = null;
  @Input() correctAnswerIndex: number | null = null;
  @Output() answerSelected = new EventEmitter<{ questionId: number, answer: string }>
  @Output() resetQuestionState = new EventEmitter<void>();
  @Output() navigateBackToScoreScreen? = new EventEmitter<void>();
  @Output() handleChapterRound = new EventEmitter<void>();
  @Output() timerRanOut = new EventEmitter<{ questionId: number }>;

  currentQuestionIndex = 0;
  selectedAnswerIndex: number | null = null;
  hasClickedOption: boolean | null = null;
  timerHasRanOut: boolean = false;
  userClickedNext: boolean = false;
  @Input() timestamp!: number;



ngOnChanges(changes: SimpleChanges) {
  if (changes['questions'] && !changes['questions'].firstChange) {
    this.resetQuestionPanel();
  }
  if (changes['isCorrect'] || changes['correctAnswerIndex']) {
    console.log('Validation changes:', {
      isCorrect: this.isCorrect,
      correctAnswerIndex: this.correctAnswerIndex,
      selectedAnswerIndex: this.selectedAnswerIndex
    });
  }
}

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
    this.resetQuestionState.emit();
    if (this.currentQuestionIndex < this.questions.length - 1) {
      console.log("Current question index: " + this.currentQuestionIndex)
      this.currentQuestionIndex++;
      this.timerComponent.resetTimer();
      this.timerComponent.startTimer();
    } else {
      //Multiplayer only
      this.navigateBackToScoreScreen.emit();

      //Singleplayer Only
      this.handleChapterRound.emit();
    }
  }

  onTimerRanOut() {
    if (!this.timerHasRanOut) {
      this.timerHasRanOut = true;
      console.log(this.timerHasRanOut)
      this.timerRanOut.emit({questionId: this.questions[this.currentQuestionIndex].questionId});
    }
  }


  private resetQuestionPanel() {

    this.currentQuestionIndex = 0;
    this.timerComponent.resetTimer();
    this.timerComponent.startTimer();
  }
}
