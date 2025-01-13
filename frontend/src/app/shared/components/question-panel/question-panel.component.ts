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
  @Input() question: QuestionDto;
  @Input() isCorrect: boolean | null = null;
  @Input() correctAnswerIndex: number | null = null;
  @Output() answerSelected = new EventEmitter<{ questionId: number, answer: string }>
  @Output() resetQuestionState = new EventEmitter<void>();
  @Output() navigateBackToScoreScreen? = new EventEmitter<void>();
  @Output() handleChapterRound = new EventEmitter<void>();
  @Output() timerRanOut = new EventEmitter<{ questionId: number }>;
  @Output() nextQuestion = new EventEmitter<void>();

  selectedAnswerIndex: number | null = null;
  hasClickedOption: boolean | null = null;
  timerHasRanOut: boolean = false;
  userClickedNext: boolean = false;
  @Input() timestamp!: number;



ngOnChanges(changes: SimpleChanges) {
  if (changes['questions'] && !changes['questions'].firstChange) {
    this.resetQuestionPanel();
  }

  if (changes['isCorrect'] && changes['isCorrect'].currentValue === null) {
    this.selectedAnswerIndex = null;
    this.hasClickedOption = false;
    this.timerHasRanOut = false;
  }
}

  selectAnswer(answer: string, i: number) {
    this.selectedAnswerIndex = i;
    this.hasClickedOption = true
    this.answerSelected.emit({
      questionId: this.question.questionId,
      answer: answer
    });
  }

  goToNextQuestion() {
    this.selectedAnswerIndex = null;
    this.hasClickedOption = false;
    this.timerHasRanOut = false;
    this.userClickedNext = true;
    this.resetQuestionState.emit();
    this.nextQuestion.emit();
    this.timerComponent.resetTimer();
    this.timerComponent.startTimer();
  }

  onTimerRanOut() {
    if (!this.timerHasRanOut) {
      this.timerHasRanOut = true;
      console.log(this.timerHasRanOut)
      this.timerRanOut.emit({questionId: this.question.questionId});
    }
  }


  private resetQuestionPanel() {

    this.selectedAnswerIndex = null;
    this.hasClickedOption = false;
    this.timerHasRanOut = false;
    this.userClickedNext = false;

    if (this.timerComponent) {
      this.timerComponent.resetTimer();
      this.timerComponent.startTimer();
    }
  }
}
