import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {PlayerProgressDto} from '../../../../api/generated/models/player-progress-dto';
import {ChapterService} from '../../../../api/generated/services/chapter.service';
import {QuestionDto} from '../../../../api/generated/models/question-dto';
import {QuestionsService} from '../../../../api/generated/services/questions.service';
import {QuestionPanelComponent} from '../../../../shared/components/question-panel/question-panel.component';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {AnswerValidationResponse} from '../../../../api/generated/models/answer-validation-response';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-play-chapter',
  imports: [
    QuestionPanelComponent,
    NgIf
  ],
  templateUrl: './play-chapter.component.html',
  styleUrl: './play-chapter.component.css'
})
export class PlayChapterComponent implements OnInit {
  playerProgress: PlayerProgressDto;
  storyTitle: string;
  chapterId: number;
  categories: string[];
  rewardText: string;
  chapterTitle: string;
  questions: QuestionDto[];

  storedPlayerId: number;
  answerIsCorrect: boolean = null;
  correctAnswerIndex: number;

  health: number = 3;
  round: number = 0;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private chapterService: ChapterService,
    private questionService: QuestionsService,
    private loginStateService: LoginStateService
  ) {
    this.playerProgress = this.router.getCurrentNavigation().extras.state?.['playerProgress'];
    this.storyTitle = this.router.getCurrentNavigation().extras.state?.['storyTitle'];
    console.log(this.storyTitle)
  }

  ngOnInit() {
    this.storedPlayerId = this.loginStateService.userId;


    this.activatedRoute.paramMap.subscribe((params) => {
      this.chapterId = +params.get("chapterId");
    })

    this.chapterService.getChapter({chapterId: this.chapterId}).subscribe({
      next: chapter => {
        this.categories = chapter.categories;
        this.rewardText = chapter.rewardText
        this.chapterTitle = chapter.title

        this.questionService.getFiveQuestionsByCategory({category: this.categories[this.round]}).subscribe({
          next: questions => {
            this.questions = questions;
          }
        })

      }
    })

  }

  get hasQuestions(): boolean {
    return this.questions.length > 0;
  }

  private fetchQuestions(category: string) {

    this.questionService.getFiveQuestionsByCategory({category: category}).subscribe({
      next: data => {
        this.questions = data;
      }
    });

  }

  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {

    const validationRequest = {
      body: {
        questionId: selectedAnswer.questionId,
        answer: selectedAnswer.answer,
        playerId: this.storedPlayerId
      }
    }

    this.questionService.validateSingleplayerAnswer(validationRequest).subscribe({
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
        answer: null,
        playerId: this.storedPlayerId
      }
    }
    this.questionService.validateSingleplayerAnswer(validationRequest).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.answerIsCorrect = response.correct;
        this.correctAnswerIndex = response.correctAnswerIndex
      }
    })

  }

}
