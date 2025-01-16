import { Component } from '@angular/core';
import {QuestionDto} from '../../../../../../api/generated/models/question-dto';
import {Observable} from 'rxjs';
import {PlayChapterDto} from '../../../../../../api/generated/models/play-chapter-dto';
import {AnswerState} from '../../interface/answer-state';
import {HealthState} from '../../interface/health-state';
import {ChapterService} from '../../../../../../api/generated/services/chapter.service';
import {LoginStateService} from '../../../../../../core/services/login-state-service/login-state.service';
import {PlayChapterUiService} from '../../service/play-chapter-ui.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ChapterHeaderComponent} from '../chapter-header/chapter-header.component';
import {NgIf} from '@angular/common';
import {QuestionPanelComponent} from '../../../../../../shared/components/question-panel/question-panel.component';

@Component({
  selector: 'app-chapter-view',
  imports: [
    ChapterHeaderComponent,
    NgIf,
    QuestionPanelComponent
  ],
  templateUrl: './chapter-view.component.html',
  styleUrl: './chapter-view.component.css'
})
export class ChapterViewComponent {
  questions: QuestionDto[];
  storedPlayerId: number;
  answerIsCorrect: boolean = null;
  correctAnswerIndex: number;
  currentQuestionIndex: number = 0;

  showQuiz: boolean;
  currentHealth: number = 3;
  animationState: string = "normal";
  lastLostHeart: number;

  chapterData$: Observable<PlayChapterDto>
  questions$: Observable<QuestionDto[]>
  currentQuestionIndex$: Observable<number>
  answerState$: Observable<AnswerState>
  healthState$: Observable<HealthState>


  constructor(
    private chapterService: ChapterService,
    private loginStateService: LoginStateService,
    private playChapterUIService: PlayChapterUiService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {

    this.playChapterUIService.setStoryId(this.router.getCurrentNavigation().extras.state?.['storyId'])
  }

  ngOnInit() {
    this.storedPlayerId = this.loginStateService.userId;

    this.activatedRoute.paramMap.subscribe((params) => {
      this.playChapterUIService.setChapterId(+params.get("chapterId"));
    })

    this.healthState$ = this.playChapterUIService.healthStateSubject$;
    this.healthState$.subscribe(state => {
      this.currentHealth = state.currentHealth;
      this.animationState = state.animationState;
      this.lastLostHeart = state.lastLostHeart;
    })

    this.answerState$ = this.playChapterUIService.answerStateSubject$;
    this.answerState$.subscribe(state => {
      this.answerIsCorrect = state.isCorrect;
      this.correctAnswerIndex = state.correctAnswerIndex;
    });


    this.playChapterUIService.startChapter().subscribe({
      next: () => {
        this.chapterData$ = this.playChapterUIService.chapterDetails$;
        this.questions$ = this.playChapterUIService.questions$;
        this.currentQuestionIndex$ = this.playChapterUIService.currentQuestionIndex$;

        this.playChapterUIService.fetchQuestions().subscribe();

        this.questions$.subscribe(questions => this.questions = questions);
        this.currentQuestionIndex$.subscribe(index => this.currentQuestionIndex = index);
      }
    })
  }

  get hasQuestions(): boolean {
    return this.questions.length > 0;
  }

  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {
    this.playChapterUIService.validateAnswer(selectedAnswer.questionId, selectedAnswer.answer)
  }

  onTimerRanOut($event: { questionId: number }) {
    this.playChapterUIService.validateAnswer($event.questionId, null)
  }

  handleRoundFinished() {
    this.questions = [];

    this.playChapterUIService.getRoundResults().subscribe();
  }

  onNextQuestion() {
    this.playChapterUIService.advanceToNextQuestion();
  }

  get currentQuestion(): QuestionDto | null {
    return this.questions && this.currentQuestionIndex < this.questions.length
      ? this.questions[this.currentQuestionIndex]
      : null;
  }

  ngOnDestroy() {
    this.chapterService.clearChapter({
      playerId: this.storedPlayerId
    }).subscribe({
      error: err => console.warn("Failed to clear chapter session: ", err)
    })
  }
}
