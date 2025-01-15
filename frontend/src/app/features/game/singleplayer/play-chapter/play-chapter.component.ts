import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ChapterService} from '../../../../api/generated/services/chapter.service';
import {QuestionDto} from '../../../../api/generated/models/question-dto';
import {QuestionsService} from '../../../../api/generated/services/questions.service';
import {QuestionPanelComponent} from '../../../../shared/components/question-panel/question-panel.component';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {AnswerValidationResponse} from '../../../../api/generated/models/answer-validation-response';
import {NgForOf, NgIf} from '@angular/common';
import {animate, keyframes, style, transition, trigger} from '@angular/animations';
import {MatDialog} from '@angular/material/dialog';
import {RoundResultsDialogComponent} from './round-results-dialog/round-results-dialog.component';
import {ContentDialogComponent} from "../shared-components/content-dialog/content-dialog.component";

@Component({
  selector: 'app-play-chapter',
  imports: [
    QuestionPanelComponent,
    NgIf,
    NgForOf
  ],
  templateUrl: './play-chapter.component.html',
  styleUrl: './play-chapter.component.css',
  animations: [
    trigger("heartState", [
      transition("* => lostHeart", [
        animate("0.5s",
          keyframes([
            style({transform: 'rotate(0)'}),
            style({transform: 'rotate(-15deg)'}),
            style({transform: 'rotate(15deg)'}),
            style({transform: 'rotate(-15deg)'}),
            style({transform: 'rotate(0)'})
          ])
        )
      ])
    ])
  ]
})
export class PlayChapterComponent implements OnInit, OnDestroy {

  storyId: number;
  chapterId: number;
  rewardText: string;
  chapterTitle: string;
  chapterWinCondition: number;

  questions: QuestionDto[];
  storedPlayerId: number;
  answerIsCorrect: boolean = null;
  correctAnswerIndex: number;
  currentQuestionIndex: number = 0;

  showQuiz: boolean;
  currentHealth: number = 3;
  animationState: string = "normal";
  lastLostHeart: number;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,

    private chapterService: ChapterService,
    private questionService: QuestionsService,
    private loginStateService: LoginStateService,
    private resultsDialog: MatDialog,
    private endGameDialog: MatDialog
  ) {
    this.storyId = this.router.getCurrentNavigation().extras.state?.['storyId'];
  }

  ngOnInit() {
    this.storedPlayerId = this.loginStateService.userId;


    this.activatedRoute.paramMap.subscribe((params) => {
      this.chapterId = +params.get("chapterId");
    })

    this.chapterService.startChapter({
      body: {
        playerId: this.storedPlayerId,
        storyId: this.storyId,
        chapterId: this.chapterId
      }
    }).subscribe({
      next: () => {
        this.chapterService.getChapter({chapterId: this.chapterId}).subscribe({
          next: chapter => {
            this.rewardText = chapter.rewardText;
            this.chapterTitle = chapter.title;
            this.chapterWinCondition = chapter.roundWinCondition;
            this.fetchQuestions();
          }
        });
      },
      error: err => {
        console.log(err)
      }
    });
  }

  get hasQuestions(): boolean {
    return this.questions.length > 0;
  }

  private fetchQuestions() {
    this.questionService.restoreSessionQuestions({playerId: this.storedPlayerId}).subscribe({
      next: progress => {
        if (progress && progress.questions.length > 0) {
          this.questions = progress.questions;
          this.currentQuestionIndex = progress.currentIndex;
          console.log("Restored index: " + this.currentQuestionIndex);
        } else {
          this.resetQuestionState()
          this.questionService.getSinglePlayerRoundQuestions({
            playerId: this.storedPlayerId
          }).subscribe({
            next: questions => {
              this.questions = questions;
            }
          })
        }
      }
    })
  }

  onAnswerSelected(selectedAnswer: { questionId: number, answer: string }) {

    this.questionService.validateSingleplayerAnswer({
      body: {
        questionId: selectedAnswer.questionId,
        answer: selectedAnswer.answer,
        playerId: this.storedPlayerId
      }
    }).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.handleAnswerValidation(response);
      }
    });

  }

  onTimerRanOut($event: { questionId: number }) {
    this.questionService.validateSingleplayerAnswer({
      body: {
        questionId: $event.questionId,
        answer: null,
        playerId: this.storedPlayerId
      }
    }).subscribe({
      next: (response: AnswerValidationResponse) => {
        this.handleAnswerValidation(response);
      }
    });

  }

  private handleAnswerValidation(response: AnswerValidationResponse) {
    this.answerIsCorrect = response.correct;
    this.correctAnswerIndex = response.correctAnswerIndex;
  }

  resetQuestionState() {
    this.answerIsCorrect = null;
    this.correctAnswerIndex = null;
  }

  handleRoundFinished() {
    this.questions = [];

    this.questionService.getRoundResults({playerId: this.storedPlayerId}).subscribe({
      next: (roundResults) => {
        console.log(roundResults)
        const dialogRef = this.resultsDialog.open(RoundResultsDialogComponent, {
          data: {
            roundResults: roundResults.questionResults,
            isRoundPassed: roundResults.roundPassed
          },
          maxHeight: "90vh",
          width: "300px"
        });

        dialogRef.afterClosed().subscribe(async () => {
          if (this.currentHealth !== roundResults.currentHealth) {
            this.lastLostHeart = this.currentHealth;
            this.currentHealth = roundResults.currentHealth;
            this.animationState = "lostHeart";

            setTimeout(() => {
              this.animationState = "normal";
              this.lastLostHeart = null;
            }, 500);
          }

          if (roundResults.gameOver) {
            const retry = await this.handleLostGame();
            if (!retry) {
              return;
            }
          }

          this.currentQuestionIndex = 0;

          if (roundResults.chapterComplete) {
            this.handleChapterComplete();
          } else {
            this.fetchQuestions();
          }
        });
      }
    });
  }

  private handleLostGame(): Promise<boolean> {
    return new Promise((resolve) => {
      this.endGameDialog.open(ContentDialogComponent, {
        data: {
          contentTitle: "Retry Chapter",
          message: "Would you like to retry the current chapter?",
          onlyOkButton: false
        }
      }).afterClosed().subscribe(result => {
        if (result === "yes") {
          this.currentHealth = 3;
          this.currentQuestionIndex = 0;
          this.chapterService.startChapter({
            body: {
              playerId: this.storedPlayerId,
              storyId: this.storyId,
              chapterId: this.chapterId
            }
          }).subscribe({
            next: () => {
              this.fetchQuestions();
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
        message: this.rewardText,
        onlyOkButton: true
      }
    });

    refDialog.afterClosed().subscribe(() => {
      this.router.navigate(['singleplayer/story', this.storyId]);
    });
  }

  onNextQuestion() {
    this.currentQuestionIndex++;
    if (this.currentQuestionIndex >= this.questions.length) {
      this.handleRoundFinished();
    } else {
      this.resetQuestionState();
    }
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
