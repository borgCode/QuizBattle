import {Injectable} from '@angular/core';
import {ChapterService} from "../../../../../api/generated/services/chapter.service";
import {QuestionService} from "../../../../../api/generated/services/question.service";
import {BehaviorSubject, Observable, switchMap, tap} from "rxjs";
import {QuestionDto} from "../../../../../api/generated/models/question-dto";
import {PlayChapterDto} from "../../../../../api/generated/models/play-chapter-dto";
import {AnswerValidationResponse} from '../../../../../api/generated/models/answer-validation-response';
import {HealthState} from '../interface/health-state';
import {LoginStateService} from '../../../../../core/services/login-state-service/login-state.service';
import {RoundResultsDialogComponent} from '../round-results-dialog/round-results-dialog.component';
import {MatDialog} from '@angular/material/dialog';
import {ContentDialogComponent} from '../../shared-components/content-dialog/content-dialog.component';
import {Router} from '@angular/router';
import {AnswerState} from '../../../interface/answer-state';

@Injectable({
  providedIn: 'root'
})
export class PlayChapterUiService {
  private chapterDetailsSubject = new BehaviorSubject<PlayChapterDto>(null);
  private questionsSubject: BehaviorSubject<QuestionDto[]> = new BehaviorSubject([]);
  private currentQuestionIndexSubject: BehaviorSubject<number> = new BehaviorSubject(0);
  private answerStateSubject: BehaviorSubject<AnswerState> = new BehaviorSubject<AnswerState>(null);
  private healthStateSubject: BehaviorSubject<HealthState> = new BehaviorSubject<HealthState>({
    currentHealth: 3,
    lastLostHeart: null,
    animationState: 'normal'
  });

  private playerId: number;
  private storyId: number;
  private chapterId: number;

  constructor(
    private chapterService: ChapterService,
    private questionService: QuestionService,
    private loginStateService: LoginStateService,
    private resultsDialog: MatDialog,
    private endGameDialog: MatDialog,
    private router: Router,
  ) {
  }

  startChapter() {
    this.playerId = this.loginStateService.loggedInUser.id;

    return this.chapterService.startChapter({
      body: {
        playerId: this.playerId,
        storyId: this.storyId,
        chapterId: this.chapterId
      }
    }).pipe(
      switchMap(() => this.chapterService.getChapter({chapterId: this.chapterId}).pipe(
        tap(chapter => this.chapterDetailsSubject.next(chapter)),
      ))
    )
  }

  fetchQuestions() {
    return this.questionService.restoreSessionQuestions({playerId: this.playerId}).pipe(
      switchMap(progress => {
        if (progress?.questions.length > 0) {
          this.questionsSubject.next(progress.questions);
          this.currentQuestionIndexSubject.next(progress.currentIndex);
          return this.questions$;
        } else {
          return this.questionService.getSinglePlayerRoundQuestions({playerId: this.playerId}).pipe(
            tap(questions => {
              this.questionsSubject.next(questions);
              this.currentQuestionIndexSubject.next(0);
            })
          );
        }
      })
    );
  }

  validateAnswer(questionId: number, answer: string) {
    return this.questionService.validateSingleplayerAnswer({
      body: {
        playerId: this.playerId,
        questionId: questionId,
        answer: answer
      }
    }).subscribe({
      next: response => {
        this.handleAnswerValidation(response);
      }
    })
  }

  private handleAnswerValidation(response: AnswerValidationResponse) {
    this.answerStateSubject.next({
      isCorrect: response.correct,
      correctAnswerIndex: response.correctAnswerIndex
    })
  }

  resetAnswerState() {
    this.answerStateSubject.next({
      isCorrect: null,
      correctAnswerIndex: null
    })
  }

  getRoundResults() {
    return this.questionService.getRoundResults({playerId: this.playerId})
  }

  updateHealth(newHealth: number) {
    const currentState = this.healthStateSubject.value;
    if (currentState.currentHealth !== newHealth) {
      const lostHeart = currentState.currentHealth;
      this.healthStateSubject.next({
        currentHealth: newHealth,
        lastLostHeart: lostHeart,
        animationState: 'lostHeart'
      });
      setTimeout(() => {
        this.healthStateSubject.next({
          currentHealth: newHealth,
          lastLostHeart: null,
          animationState: 'normal'
        })
      }, 500);
    }

  }

  advanceToNextQuestion() {
    this.currentQuestionIndexSubject.next(this.currentQuestionIndexSubject.value + 1);

    if (this.currentQuestionIndexSubject.value >= this.questionsSubject.value.length) {
      this.handleRoundFinished();
    } else {
      this.resetAnswerState();
    }
  }

  private handleRoundFinished() {
    this.questionsSubject.next([]);

    this.getRoundResults().subscribe({
      next: (roundResults) => {
        const dialogRef = this.resultsDialog.open(RoundResultsDialogComponent, {
          data: {
            roundResults: roundResults.questionResults,
            isRoundPassed: roundResults.roundPassed
          },
          maxHeight: "90vh",
          width: "300px"
        });

        dialogRef.afterClosed().subscribe(async () => {
          this.updateHealth(roundResults.currentHealth);

          if (roundResults.gameOver) {
            const retry = await this.handleLostGame();
            if (!retry) {
              return;
            }
          }
          this.currentQuestionIndexSubject.next(0);

          if (roundResults.chapterComplete) {
            this.handleChapterComplete();
          } else {
            this.fetchQuestions().subscribe();
          }
        });
      }
    });
  }

  private async handleLostGame() {
    return new Promise((resolve) => {
      this.endGameDialog.open(ContentDialogComponent, {
        data: {
          contentTitle: "Retry Chapter",
          message: "Would you like to retry the current chapter?",
          onlyOkButton: false
        }
      }).afterClosed().subscribe(result => {
        if (result === "yes") {
        this.healthStateSubject.next({
            currentHealth: 3,
            lastLostHeart: null,
            animationState: 'normal'
          });

        this.currentQuestionIndexSubject.next(0);
          this.startChapter().subscribe({
            next: () => {
              resolve(true);
            }
          });
        } else {
          this.router.navigate(['singleplayer/story', this.storyId]);
          resolve(false);
        }
      });
    });
  }

  private handleChapterComplete() {
    const refDialog = this.endGameDialog.open(ContentDialogComponent, {
      data: {
        contentTitle: "Chapter completed!",
        message: this.chapterDetailsSubject.value.rewardText,
        onlyOkButton: true
      }
    });

    refDialog.afterClosed().subscribe(() => {
      this.router.navigate(['singleplayer/story', this.storyId]);
    });
  }

  clearChapter() {
    return this.chapterService.clearChapter({
      playerId: this.playerId
    })
  }

  get chapterDetails$(): Observable<PlayChapterDto> {
    return this.chapterDetailsSubject.asObservable();
  }

  get questions$(): Observable<QuestionDto[]> {
    return this.questionsSubject.asObservable();
  }

  get currentQuestionIndex$(): Observable<number> {
    return this.currentQuestionIndexSubject.asObservable();
  }


  get answerStateSubject$(): Observable<AnswerState> {
    return this.answerStateSubject.asObservable();
  }

  get healthStateSubject$(): BehaviorSubject<HealthState> {
    return this.healthStateSubject;
  }

  setStoryId(storyId: number) {
    this.storyId = storyId;
  }

  setChapterId(chapterId: number) {
    this.chapterId = chapterId;
  }
}
