import {Component, OnInit} from '@angular/core';
import {MultiplayerCategoryComponent} from './multiplayer-category/multiplayer-category.component';
import {QuestionsService} from '../../../services/services/questions.service';
import {ActivatedRoute, Router} from '@angular/router';
import {QuestionDto} from '../../../services/models/question-dto';
import {MultiplayerQuestionsComponent} from './multiplayer-questions/multiplayer-questions.component';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-multiplayer-play-round',
  imports: [
    MultiplayerCategoryComponent,
    MultiplayerQuestionsComponent,
    NgIf
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

  constructor(
    private questionService: QuestionsService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
  ) {
  }

  ngOnInit() {
    this.initStoredPlayerId();

    this.activatedRoute.params.subscribe(value => {
      this.sessionId = value['sessionId'];

      this.questionService.getThreeRandomCategories({sessionId: this.sessionId}).subscribe({
        next: categories =>
          this.categories = categories
      })

    })
  }

  private initStoredPlayerId() {
    const storedPlayer = localStorage.getItem('loggedInUser');
    if (storedPlayer) {
      this.storedPlayerId = JSON.parse(storedPlayer).id;
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

    this.questionService.validateAnswer(validationRequest).subscribe({
      next: (response: boolean) => {
        this.answerIsCorrect = response;
        console.log("Setting correctness of validated answer: " + response)

      }
    })

  }
}
