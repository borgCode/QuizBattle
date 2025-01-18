import {Component, OnDestroy, OnInit} from '@angular/core';
import {QuestionDto} from '../../../../../../../api/generated/models/question-dto';
import {combineLatest, Observable, switchMap, tap} from 'rxjs';
import {PlayChapterDto} from '../../../../../../../api/generated/models/play-chapter-dto';
import {HealthState} from '../../interface/health-state';
import {PlayChapterUiService} from '../../service/play-chapter-ui.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ChapterHeaderComponent} from '../chapter-header/chapter-header.component';
import {AsyncPipe, NgIf} from '@angular/common';
import {QuestionPanelComponent} from '../../../../../shared/component/question-panel/question-panel.component';
import {map} from 'rxjs/operators';
import {AnswerState} from '../../../../../shared/interface/answer-state';

@Component({
  selector: 'app-chapter-view',
  imports: [
    ChapterHeaderComponent,
    NgIf,
    QuestionPanelComponent,
    AsyncPipe
  ],
  templateUrl: './chapter-view.component.html',
  styleUrl: './chapter-view.component.css'
})
export class ChapterViewComponent implements OnInit, OnDestroy{

  showQuiz: boolean;

  chapterData$: Observable<PlayChapterDto>
  questions$: Observable<QuestionDto[]>
  currentQuestionIndex$: Observable<number>
  answerState$: Observable<AnswerState>
  healthState$: Observable<HealthState>

  currentQuestion$: Observable<QuestionDto | null>;
  hasQuestions$: Observable<boolean>;

  constructor(
    private playChapterUIService: PlayChapterUiService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {

    this.playChapterUIService.setStoryId(this.router.getCurrentNavigation().extras.state?.['storyId'])
  }

  ngOnInit() {
    this.activatedRoute.paramMap.pipe(
      tap(params => this.playChapterUIService.setChapterId(+params.get("chapterId"))),
      switchMap(() => this.playChapterUIService.startChapter()),
      tap(() => {

        this.chapterData$ = this.playChapterUIService.chapterDetails$;
        this.questions$ = this.playChapterUIService.questions$;
        this.currentQuestionIndex$ = this.playChapterUIService.currentQuestionIndex$;
        this.healthState$ = this.playChapterUIService.healthStateSubject$;
        this.answerState$ = this.playChapterUIService.answerStateSubject$;

        this.hasQuestions$ = this.questions$.pipe(
          map(questions => questions.length > 0)
        );

        this.currentQuestion$ = combineLatest([
          this.questions$,
          this.currentQuestionIndex$
        ]).pipe(
          map(([questions, index]) =>
            questions && index < questions.length ? questions[index] : null
          )
        );

        this.playChapterUIService.fetchQuestions().subscribe();
      })
    ).subscribe();
  }


  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {
    this.playChapterUIService.validateAnswer(selectedAnswer.questionId, selectedAnswer.answer)
  }

  onTimerRanOut($event: { questionId: number }) {
    this.playChapterUIService.validateAnswer($event.questionId, null)
  }

  handleRoundFinished() {
    this.playChapterUIService.getRoundResults().subscribe();
  }

  onNextQuestion() {
    this.playChapterUIService.advanceToNextQuestion();
  }
  ngOnDestroy() {
    this.playChapterUIService.clearChapter().subscribe({
      error: err => console.warn("Failed to clear chapter session: ", err)
    })
  }
}
