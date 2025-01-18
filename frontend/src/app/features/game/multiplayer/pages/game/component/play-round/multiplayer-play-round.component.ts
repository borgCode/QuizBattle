import {Component, OnDestroy, OnInit} from '@angular/core';
import {MultiplayerCategoryComponent} from '../category-selection/multiplayer-category.component';
import {ActivatedRoute} from '@angular/router';
import {AsyncPipe, NgIf} from '@angular/common';
import {QuestionDto} from '../../../../../../../api/generated/models/question-dto';
import {QuestionPanelComponent} from '../../../../../shared/component/question-panel/question-panel.component';
import {GameService} from '../../service/game.service';
import {combineLatest, Observable} from 'rxjs';
import {AnswerState} from '../../../../../shared/interface/answer-state';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-multiplayer-play-round',
  imports: [
    MultiplayerCategoryComponent,
    QuestionPanelComponent,
    NgIf,
    QuestionPanelComponent,
    AsyncPipe
  ],
  templateUrl: './multiplayer-play-round.component.html',
  styleUrl: './multiplayer-play-round.component.css'
})
export class MultiplayerPlayRoundComponent implements OnInit, OnDestroy {
  questions: QuestionDto[] = [];
  sessionId: number;

  questions$: Observable<QuestionDto[]>
  currentQuestionIndex$: Observable<number>
  answerState$: Observable<AnswerState>
  categories$: Observable<string[]>
  currentQuestion$: Observable<QuestionDto | null>;
  shouldShowCategories$: Observable<boolean>
  shouldShowQuestions$: Observable<boolean>

  constructor(
    private activatedRoute: ActivatedRoute,
    protected gameService: GameService
  ) {
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(value => {
      this.gameService.sessionId = value['sessionId'];
    });

    this.questions$ = this.gameService.questions$;
    this.currentQuestionIndex$ = this.gameService.currentQuestionIndex$;
    this.answerState$ = this.gameService.answerState$;
    this.categories$ = this.gameService.categories$;
    this.currentQuestion$ = this.questions$.pipe(
      map(questions => questions && questions.length > 0 ? questions[0] : null)
    );

    this.currentQuestion$ = combineLatest([
      this.questions$,
      this.currentQuestionIndex$
    ]).pipe(
      map(([questions, currentIndex]) =>
        questions && questions.length > 0 ? questions[currentIndex] : null
      )
    );

    this.shouldShowCategories$ = combineLatest([
      this.categories$,
      this.questions$
    ]).pipe(
      map(([categories, questions]) =>
        categories?.length > 0 && (!questions || questions.length === 0)
      )
    );

    this.shouldShowQuestions$ = this.questions$.pipe(
      map(questions => questions?.length > 0)
    );

    const questionIds = (history.state as any).questionIds;
    this.gameService.getQuestions(questionIds).subscribe()

  }

  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {
    this.gameService.validateAnswer(selectedAnswer);
  }

  onCategorySelected(category: string) {
    this.gameService.onCategorySelected(category);
  }

  onNextQuestion() {
    this.gameService.onNextQuestion();
  }

  onTimerRanOut(event: { questionId: number }) {
    this.gameService.validateAnswer({
      questionId: event.questionId,
      answer: null
    });
  }

  ngOnDestroy() {
    this.gameService.clearRoundQuestions().subscribe({
      error: err => console.warn("Failed to clear round session: ", err)
    })
  }
}
