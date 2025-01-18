import {Injectable} from '@angular/core';
import {MultiplayerGameService} from '../../../../../../api/generated/services/multiplayer-game.service';
import {GameStateResponse} from '../../../../../../api/generated/models/game-state-response';
import {BehaviorSubject, of, switchMap, tap} from 'rxjs';
import {MultiplayerMatchService} from '../../../../../../api/generated/services/multiplayer-match.service';
import {AlertMessageService} from '../../../../../../core/services/alert-message/alert-message.service';
import {Router} from '@angular/router';
import {QuestionService} from '../../../../../../api/generated/services/question.service';
import {QuestionDto} from '../../../../../../api/generated/models/question-dto';
import {AnswerState} from '../../../../shared/interface/answer-state';
import {LoginStateService} from '../../../../../../core/services/login-state-service/login-state.service';
import {GameResult} from '../enums/game-result';
import {MatDialog} from '@angular/material/dialog';
import {GameOverDialogComponent} from '../dialog/game-over-dialog/game-over-dialog.component';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private gameStateSubject = new BehaviorSubject<GameStateResponse | null>(null);
  readonly gameState$ = this.gameStateSubject.asObservable()

  private questionsSubject: BehaviorSubject<QuestionDto[]> = new BehaviorSubject([]);
  private currentQuestionIndexSubject: BehaviorSubject<number> = new BehaviorSubject(0);
  private answerStateSubject: BehaviorSubject<AnswerState> = new BehaviorSubject<AnswerState>(null);
  private categoriesSubject = new BehaviorSubject<string[]>([]);
  private gameOverSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  readonly questions$ = this.questionsSubject.asObservable();
  readonly currentQuestionIndex$ = this.currentQuestionIndexSubject.asObservable();
  readonly answerState$ = this.answerStateSubject.asObservable();
  readonly categories$ = this.categoriesSubject.asObservable();
  readonly gameOver$ = this.gameOverSubject.asObservable();

  private _playerId: number;
  private _sessionId: number;

  constructor(
    private multiplayerGameService: MultiplayerGameService,
    private multiplayerMatchService: MultiplayerMatchService,
    private alertMessageService: AlertMessageService,
    private loginStateService: LoginStateService,
    private questionService: QuestionService,
    private router: Router,
    private gameOverDialog: MatDialog,
  ) {
  }

  getGameState() {
    this.gameOverSubject.next(false);
    return this.multiplayerGameService.getGameState({sessionId: this.sessionId, playerId: this.playerId}).pipe(
      tap(gameState => {
        this.gameStateSubject.next(gameState)
      }),
      switchMap(gameState => {
        if (gameState.status === 'COMPLETED') {
          this.gameOverSubject.next(true);

          if (!gameState.playerDTO.hasAcknowledgedGameOver) {
            const gameResult = this.determineGameResult(gameState);
            this.showGameOverDialog(gameResult, gameState);
            return this.multiplayerGameService.acknowledgeGameOver({
              sessionId: this.sessionId,
              playerId: this.playerId
            }).pipe(
              map(() => gameState)
            );
          }
        }
        return of(gameState);
      })
    )
  }

  private determineGameResult(gameState: GameStateResponse) {
    if (gameState.playerWhoGaveUp) {
      console.log("A player gave up: " + gameState.playerWhoGaveUp);
      if (this.playerId === gameState.playerWhoGaveUp) {
        console.log("Player gave up");
        return GameResult.PLAYER_GAVE_UP;
      } else {
        console.log("Opponent gave up");
        return GameResult.OPPONENT_GAVE_UP;
      }
    }

    if (this.playerId === gameState.winnerId) {
      return GameResult.WIN;
    } else if (this.playerId === gameState.loserId) {
      return GameResult.LOSS;
    } else {
      return GameResult.TIE;
    }
  }

  private showGameOverDialog(gameResult: GameResult, gameState: GameStateResponse) {
    this.gameOverDialog.open(GameOverDialogComponent, {
      data: {gameResult: gameResult, opponentName: gameState.opponentDTO.displayName},
      width: '300px',
      disableClose: true,
      autoFocus: false
    })
  }

  getQuestions(questionIds: number[]) {
    console.log("MP: Attempting to restore questions for player:", this.playerId);
    return this.questionService.restoreSessionQuestions({playerId: this.playerId}).pipe(
      switchMap(roundProgress => {
        if (roundProgress?.questions.length > 0) {
          this.questionsSubject.next(roundProgress.questions);
          this.currentQuestionIndexSubject.next(roundProgress.currentIndex);
          return this.questions$;
        } else if (questionIds) {
          return this.questionService.getActiveSessionQuestions({
            sessionId: this.sessionId,
            playerId: this.playerId
          }).pipe(
            tap(questions => {
              this.questionsSubject.next(questions);
              this.currentQuestionIndexSubject.next(0);
            })
          );
        } else {
          this.loadCategorySelection();
          return this.questions$;
        }
      })
    );
  }

  private loadCategorySelection() {
    this.resetRoundState();
    this.getCategories();
  }

  resetRoundState() {
    this.resetAnswerState();
    this.currentQuestionIndexSubject.next(0);
    this.questionsSubject.next([]);
  }

  resetAnswerState() {
    this.answerStateSubject.next({
      isCorrect: null,
      correctAnswerIndex: null
    })
  }

  getCategories() {
    this.questionService.getThreeRandomCategories({
      sessionId: this.sessionId,
      playerId: this.playerId
    }).subscribe({
      next: categories => this.categoriesSubject.next(categories)
    })
  }

  onCategorySelected(category: string) {
    this.questionService.getNewQuestionsForCategory({
      body: {
        category: category,
        sessionId: this.sessionId,
        playerId: this.playerId
      }
    }).subscribe({
      next: questions => {
        if (questions && questions.length > 0) {
          this.questionsSubject.next(questions)
          this.currentQuestionIndexSubject.next(0);
        } else {
          console.log("This category has already been played, please choose another");
          this.loadCategorySelection();
        }
      },
      error: err => {
        console.log(err)
      }
    });
  }

  onNextQuestion() {
    this.currentQuestionIndexSubject.next(this.currentQuestionIndexSubject.value + 1);

    if (this.currentQuestionIndexSubject.value >= this.questionsSubject.value.length) {
      console.log("Rerouting to MP")
      this.router.navigate(['multiplayer', this.sessionId]);
    } else {
      this.resetAnswerState();
    }
  }

  validateAnswer(selectedAnswer: { questionId: number, answer: string | null }) {
    this.questionService.validateMultiplayerAnswer({
      body: {
        questionId: selectedAnswer.questionId,
        sessionId: this.sessionId,
        answer: selectedAnswer.answer,
        playerId: this.playerId
      }
    }).subscribe({
      next: (response) => {
        this.answerStateSubject.next({
          isCorrect: response.correct,
          correctAnswerIndex: response.correctAnswerIndex
        });
      },
      error: err => {
        console.error('Error validating answer:', err);
      }
    });
  }

  clearRoundQuestions() {
    return this.questionService.clearPlayerSession({playerId: this.playerId})
  }

  requestRematch() {
    this.multiplayerMatchService.requestRematch({
      body: {
        sessionId: this.sessionId,
        playerId: this.playerId
      }
    }).subscribe({
      next: () => this.alertMessageService.show('Sent rematch request!', 'success'),
    })
  }

  handleGiveUp() {
    this.multiplayerGameService.giveUp({sessionId: this.sessionId, playerId: this.playerId}).subscribe({
      next: () => {
        const currentUrl = this.router.url;
        this.router.navigateByUrl('/', {skipLocationChange: true}).then(() => {
          this.router.navigate([currentUrl]);
        });
      }
    })
  }

  get playerId(): number {
    if (!this._playerId) {
      this._playerId = this.loginStateService.loggedInUser.id;
    }
    return this._playerId;
  }

  get sessionId(): number {
    return this._sessionId;
  }

  set sessionId(value: number) {
    this._sessionId = value;
  }
}
