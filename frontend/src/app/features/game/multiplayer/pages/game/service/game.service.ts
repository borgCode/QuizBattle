import {Injectable} from '@angular/core';
import {MultiplayerGameService} from '../../../../../../api/generated/services/multiplayer-game.service';
import {GameStateResponse} from '../../../../../../api/generated/models/game-state-response';
import {BehaviorSubject, tap} from 'rxjs';
import {MultiplayerMatchService} from '../../../../../../api/generated/services/multiplayer-match.service';
import {AlertMessageService} from '../../../../../../core/services/alert-message/alert-message.service';
import {Router} from '@angular/router';
import {QuestionService} from '../../../../../../api/generated/services/question.service';
import {QuestionDto} from '../../../../../../api/generated/models/question-dto';
import {AnswerState} from '../../../../interface/answer-state';

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

  questions$ = this.questionsSubject.asObservable();
  currentQuestionIndex$ = this.currentQuestionIndexSubject.asObservable();
  answerState$ = this.answerStateSubject.asObservable();
  categories$ = this.categoriesSubject.asObservable();

  sessionId: number;
  playerId: number;


  constructor(
    private multiplayerGameService: MultiplayerGameService,
    private multiplayerMatchService: MultiplayerMatchService,
    private alertMessageService: AlertMessageService,
    private questionService: QuestionService,
    private router: Router,
  ) {
  }

  getGameState(sessionId: number, playerId: number) {
    this.sessionId = sessionId;
    this.playerId = playerId;
    return this.multiplayerGameService.getGameState({sessionId: sessionId, playerId: playerId}).pipe(
      tap(gameState => this.gameStateSubject.next(gameState))
    )
  }

  getQuestions(questionIds: number[]) {
    return this.questionService.restoreSessionQuestions({playerId: this.playerId}).pipe(
      tap(roundProgress => {
        if (roundProgress?.questions.length > 0) {
          this.questionsSubject.next(roundProgress.questions)
          this.currentQuestionIndexSubject.next(roundProgress.currentIndex);
        } else if (questionIds) {
          this.questionService.getActiveSessionQuestions({
            sessionId: this.sessionId,
            playerId: this.playerId
          }).subscribe({
            next: questions => {
              this.questionsSubject.next(questions);
              this.currentQuestionIndexSubject.next(0);
            },
            error: err => {
              console.log('No active questions found, returning to score screen');
              this.router.navigate(['multiplayer', this.sessionId]);
            }
          })

        } else {
          this.loadCategorySelection();
        }
      })
    )
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
    const validationRequest = {
      body: {
        questionId: selectedAnswer.questionId,
        sessionId: this.sessionId,
        answer: selectedAnswer.answer,
        playerId: this.playerId
      }
    };

    this.questionService.validateMultiplayerAnswer(validationRequest).subscribe({
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
}
