import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {PlayerProgressDto} from '../../../../api/generated/models/player-progress-dto';
import {ChapterService} from '../../../../api/generated/services/chapter.service';
import {QuestionDto} from '../../../../api/generated/models/question-dto';
import {QuestionsService} from '../../../../api/generated/services/questions.service';
import {QuestionPanelComponent} from '../../../../shared/components/question-panel/question-panel.component';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {AnswerValidationResponse} from '../../../../api/generated/models/answer-validation-response';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-play-chapter',
  imports: [
    QuestionPanelComponent,
    NgIf,
    NgForOf
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
  chapterWinCondition: number;
  questions: QuestionDto[];

  showQuiz: boolean;
  storedPlayerId: number;
  answerIsCorrect: boolean = null;
  correctAnswerIndex: number;
  currentQuestionIndex: number = 0;

  currentHealth: number = 3;
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
        this.chapterWinCondition = chapter.roundWinCondition;


        this.fetchQuestions();
      }
    })

  }

  get hasQuestions(): boolean {
    return this.questions.length > 0;
  }

  private fetchQuestions() {

    this.questionService.getFiveQuestionsByCategory({category: this.categories[this.round]}).subscribe({
      next: questions => {
        this.questions = questions;
      }
    })

  }

  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {

    const validationRequest = {
      body: {
        questionId: selectedAnswer.questionId,
        answer: selectedAnswer.answer,
        index: this.currentQuestionIndex,
        playerId: this.storedPlayerId
      }
    }

    this.questionService.validateSingleplayerAnswer(validationRequest).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.answerIsCorrect = response.correct;
        this.correctAnswerIndex = response.correctAnswerIndex
        this.currentQuestionIndex++;
      }
    })

  }

  onTimerRanOut($event: { questionId: number }) {
    const validationRequest = {
      body: {
        questionId: $event.questionId,
        index: this.currentQuestionIndex,
        answer: null,
        playerId: this.storedPlayerId
      }
    }
    this.questionService.validateSingleplayerAnswer(validationRequest).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.answerIsCorrect = response.correct;
        this.correctAnswerIndex = response.correctAnswerIndex
        this.currentQuestionIndex++;
      }
    })

  }

  handleRoundFinished() {
    this.questionService.getRoundResults({playerId: this.storedPlayerId}).subscribe({

    })
  }
}
