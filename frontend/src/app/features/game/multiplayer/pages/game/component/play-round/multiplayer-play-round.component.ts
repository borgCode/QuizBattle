import {Component, OnInit} from '@angular/core';
import {MultiplayerCategoryComponent} from '../category-selection/multiplayer-category.component';
import {ActivatedRoute, Router} from '@angular/router';
import {NgIf} from '@angular/common';
import {QuestionDto} from '../../../../../../../api/generated/models/question-dto';
import {AnswerValidationResponse} from '../../../../../../../api/generated/models/answer-validation-response';
import {QuestionPanelComponent} from '../../../../../../../shared/components/question-panel/question-panel.component';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {log} from '@angular-devkit/build-angular/src/builders/ssr-dev-server';
import {QuestionService} from '../../../../../../../api/generated/services/question.service';

@Component({
  selector: 'app-multiplayer-play-round',
  imports: [
    MultiplayerCategoryComponent,
    QuestionPanelComponent,
    NgIf,
    QuestionPanelComponent
  ],
  templateUrl: './multiplayer-play-round.component.html',
  styleUrl: './multiplayer-play-round.component.css'
})
export class MultiplayerPlayRoundComponent implements OnInit {
  categories: Array<string> = [];
  selectedCategory: string | null = null;
  questions: QuestionDto[] = [];
  sessionId: number;
  storedPlayerId: number;
  answerIsCorrect: boolean = null;
  correctAnswerIndex: number;
  currentQuestionIndex: number = 0;

  constructor(
    private questionService: QuestionService,
    private loginStateService: LoginStateService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
  ) {
  }

  ngOnInit() {
    this.storedPlayerId = this.loginStateService.userId;

    this.activatedRoute.params.subscribe(value => {
      this.sessionId = value['sessionId'];
    });


    this.questionService.restoreSessionQuestions({playerId: this.storedPlayerId}).subscribe({
      next: progress => {
        console.log(progress)
        if (progress && progress.questions.length > 0) {
          this.questions = progress.questions;
          this.currentQuestionIndex = progress.currentIndex;
        } else {

          let questionIds = (history.state as any).questionIds;

          if (questionIds) {
            this.questionService.getActiveSessionQuestions({
              sessionId: this.sessionId,
              playerId: this.storedPlayerId
            }).subscribe({
              next: questions => {
                this.questions = questions;
              },
              error: err => {
                console.log('No active questions found, returning to score screen');
                this.router.navigate(['multiplayer', this.sessionId]);
              }
            })

          } else {
            this.loadCategorySelection();
          }
        }
      },
      error: err => {
        console.error('Error getting session questions:', err);
        this.router.navigate(['multiplayer', this.sessionId]);
      }
    })
  }

  get currentQuestion(): QuestionDto | null {
    return this.questions && this.currentQuestionIndex < this.questions.length
      ? this.questions[this.currentQuestionIndex]
      : null;
  }

  private loadCategorySelection() {
    this.resetState();

    this.questionService.getThreeRandomCategories({
      sessionId: this.sessionId,
      playerId: this.storedPlayerId
    }).subscribe({
      next: categories =>
        this.categories = categories
    })
  }

  private resetState() {
    this.selectedCategory = null;
    this.questions = [];
    this.answerIsCorrect = null;
    this.correctAnswerIndex = null;
  }


  onCategorySelected(category: string) {
    this.selectedCategory = category;

    this.questionService.getNewQuestionsForCategory({
      body: {
        category: category,
        sessionId: this.sessionId,
        playerId: this.storedPlayerId
      }
    }).subscribe({
      next: data => {
        if (data && data.length > 0) {
          this.questions = data;
        } else {
          console.log("This category has already been played, please choose another");
          this.selectedCategory = null;
          this.loadCategorySelection();
        }
      },
      error: err => {
        console.log(err)
      }
    });
  }

  onNextQuestion() {
    this.currentQuestionIndex++;
    if (this.currentQuestionIndex >= this.questions.length) {
      this.router.navigate(['multiplayer', this.sessionId]);
    } else {
      this.resetQuestionState();
    }
  }
  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {

    const validationRequest = {
      body: {
        questionId: selectedAnswer.questionId,
        sessionId: this.sessionId,
        answer: selectedAnswer.answer,
        playerId: this.storedPlayerId
      }
    }

    this.questionService.validateMultiplayerAnswer(validationRequest).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.answerIsCorrect = response.correct;
        this.correctAnswerIndex = response.correctAnswerIndex
      },
      error: err => {
        console.log(err)
      }
    })
  }

  resetQuestionState() {
    this.answerIsCorrect = null;
    this.correctAnswerIndex = null;
  }

  onTimerRanOut($event: { questionId: number }) {
    const validationRequest = {
      body: {
        questionId: $event.questionId,
        sessionId: this.sessionId,
        answer: null,
        playerId: this.storedPlayerId
      }
    }
    this.questionService.validateMultiplayerAnswer(validationRequest).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.answerIsCorrect = response.correct;
        this.correctAnswerIndex = response.correctAnswerIndex
      }
    })
  }

  get shouldShowCategories(): boolean {
    return this.categories.length > 0 && !this.questions.length;
  }

  get shouldShowQuestions(): boolean {
    return this.questions.length > 0;
  }


}
