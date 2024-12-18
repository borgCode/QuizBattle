import {Component, OnInit} from '@angular/core';
import {MultiplayerCategoryComponent} from './multiplayer-category/multiplayer-category.component';
import {ActivatedRoute, Router} from '@angular/router';
import {NgIf} from '@angular/common';
import {QuestionDto} from '../../../../api/generated/models/question-dto';
import {QuestionsService} from '../../../../api/generated/services/questions.service';
import {AnswerValidationResponse} from '../../../../api/generated/models/answer-validation-response';
import {QuestionPanelComponent} from '../../../../shared/components/question-panel/question-panel.component';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';

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

  get hasQuestions(): boolean {
    return this.questions.length > 0;
  }

  constructor(
    private questionService: QuestionsService,
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

    let questionIds = (history.state as any).questionIds;

    if (questionIds) {
      this.questionService.getCurrentQuestions({currentQuestionIds: questionIds}).subscribe({
        next: data => {
          this.questions = data;
        }
      })

    } else {
      this.questionService.getThreeRandomCategories({sessionId: this.sessionId}).subscribe({
        next: categories =>
          this.categories = categories
      })

    }

  }


  onCategorySelected(category: string) {
    this.selectedCategory = category;
    this.fetchQuestions(category);
  }

  private fetchQuestions(category: string) {

    this.questionService.getThreeQuestionsByCategory({category, sessionId: this.sessionId}).subscribe({
      next: data => {
        this.questions = data;
      }
    });

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
      }
    })

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

  onNavigateBack() {
    this.router.navigate(['multiplayer', this.sessionId]);
  }

}
