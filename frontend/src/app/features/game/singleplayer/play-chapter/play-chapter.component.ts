import {Component, OnInit} from '@angular/core';
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
export class PlayChapterComponent implements OnInit {
    playerProgressId: number;
    chapterProgressId: number;
    storyTitle: string;
    storyId: number;
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
    animationState: string = "normal";

    round: number = 0;
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
        this.playerProgressId = this.router.getCurrentNavigation().extras.state?.['playerProgressId'];
        this.storyTitle = this.router.getCurrentNavigation().extras.state?.['storyTitle'];
        this.storyId = this.router.getCurrentNavigation().extras.state?.['storyId'];
        console.log(this.storyTitle)
    }

    ngOnInit() {
        this.storedPlayerId = this.loginStateService.userId;


        this.activatedRoute.paramMap.subscribe((params) => {
            this.chapterId = +params.get("chapterId");
        })

        this.initProgress();

        this.chapterService.getChapter({chapterId: this.chapterId}).subscribe({
            next: chapter => {
                this.categories = chapter.categories;
                this.rewardText = chapter.rewardText
                this.chapterTitle = chapter.title
                this.chapterWinCondition = chapter.roundWinCondition;

                console.log(this.rewardText)


                this.fetchQuestions();
            }
        })

    }

    private initProgress() {
        this.chapterService.initiateProgress({
            body: {
                playerId: this.storedPlayerId,
                playerProgressId: this.playerProgressId,
                storyId: this.storyId,
                chapterId: this.chapterId
            }
        }).subscribe({
            next: response => {
                this.playerProgressId = response.playerProgressId;
                this.chapterProgressId = response.chapterProgressId;
            },
            error: err => {
                console.log(err)
            }
        })
    }

    get hasQuestions(): boolean {
        return this.questions.length > 0;
    }

    private fetchQuestions() {

        this.questionService.getFiveQuestionsByCategory({
            request: {
                category: this.categories[this.round],
                playerId: this.storedPlayerId
            }
        }).subscribe({
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

    resetQuestionState() {
        this.answerIsCorrect = null;
        this.correctAnswerIndex = null;
    }

    handleRoundFinished() {
        this.questionService.getRoundResults({playerId: this.storedPlayerId}).subscribe({
            next: results => {

                console.log("Getting round results")
                console.log(results)

                const dialogRef = this.resultsDialog.open(RoundResultsDialogComponent, {
                    data: {
                        roundResults: results,
                        winCondition: this.chapterWinCondition
                    },
                    maxHeight: "90vh",
                    width: "300px"
                })

                dialogRef.afterClosed().subscribe(async () => {
                    console.log("Dialog is closed")
                    const correctCount = results.filter(value => value).length;

                    if (correctCount <= 2) {
                        this.loseHeart();
                    } else {
                        this.round++;
                    }

                    if (this.currentHealth == 0) {
                        const retry = await this.handleLostGame();
                        if (!retry) {
                            return;
                        }
                    }

                    this.currentQuestionIndex = 0;

                    this.questionService.clearRoundResults({playerId: this.storedPlayerId}).subscribe({
                        next: () => {
                            if (this.round >= this.categories.length) {
                                console.log("Chapter complete")
                                this.handleChapterComplete();
                            } else {
                                this.fetchQuestions()
                            }
                        }
                    })
                })
            }
        })
    }

    loseHeart() {
        if (this.currentHealth > 0) {
            this.lastLostHeart = this.currentHealth;
            this.currentHealth--;
            this.animationState = "lostHeart"

            setTimeout(() => {
                this.animationState = "normal";
                this.lastLostHeart = null;
            }, 500)
        }
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
                    this.resetChapter();
                    resolve(true);
                } else {
                    this.router.navigate(['singleplayer/story', this.storyId])
                    resolve(false);
                }
            })
        })

    }

    private resetChapter() {
        this.currentHealth = 3;
        this.round = 0;
    }


    private handleChapterComplete() {
        this.chapterService.updateChapterProgress({chapterProgressId: this.chapterProgressId}).subscribe({
            next: () => {
                console.log(this.rewardText)
                const refDialog = this.endGameDialog.open(ContentDialogComponent, {
                    data: {
                        contentTitle: "Chapter completed!",
                        message: this.rewardText,
                        onlyOkButton: true
                    }
                })
                refDialog.afterClosed().subscribe(() => {
                    this.router.navigate(['singleplayer/story', this.storyId])
                })
            }
        })
    }
}
