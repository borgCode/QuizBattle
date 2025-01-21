import {Component, OnInit} from '@angular/core';
import {StoryService} from '../../../../../../../api/generated/services/story.service';
import {NgForOf, NgIf} from '@angular/common';
import {ChapterCardComponent} from '../chapter-card/chapter-card.component';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {ActivatedRoute, Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {ContentDialogComponent} from '../../../shared/content-dialog/content-dialog.component';
import {ChapterOverviewDto} from '../../../../../../../api/generated/models/chapter-overview-dto';
import {StoryProgressDto} from '../../../../../../../api/generated/models/story-progress-dto';

@Component({
    selector: 'app-story-overview',
    imports: [
        NgForOf,
        ChapterCardComponent,
        NgIf
    ],
    templateUrl: './story-overview.component.html',
    styleUrl: './story-overview.component.css'
})
export class StoryOverviewComponent implements OnInit {
    chapters: ChapterOverviewDto[]
    storyProgress: StoryProgressDto;
    storyTitle: string;
    storyId: number

    constructor(
        private storyService: StoryService,
        private loginStateService: LoginStateService,
        private activatedRoute: ActivatedRoute,
        private router: Router,
        private chapterDialog: MatDialog
    ) {
    }

    ngOnInit() {
        this.activatedRoute.paramMap.subscribe((params) => {
            this.storyId = +params.get('storyId')
            console.log(this.storyId)
        })

        this.storyService.getStoryOverview({
            body: {
                playerId: this.loginStateService.loggedInUser.id, storyId: this.storyId
            }
        }).subscribe({
            next: data => {
                console.log(data)
                this.chapters = data.chapters;
                this.storyProgress = data.storyProgressDTO
                console.log(this.storyProgress)
                this.storyTitle = data.title;
                console.log(this.storyProgress)
            }
        })
    }

    openLockedDialog(title: string, unlockCondition: string) {
        this.chapterDialog.open(ContentDialogComponent, {
            data: {contentTitle: title, message: unlockCondition, onlyOkButton: true},
            maxHeight: '90vh',
            width: '300px',
        })
    }

    openChapterDialog(title: string, id: number, index: number) {
        let message: string
        if (this.storyProgress.completedChapters <= index) {
            message = "Would you like to start this chapter?"
        } else {
            message = "You've already completed this chapter, do you want to play it again?"
        }

        const dialogRef = this.chapterDialog.open(ContentDialogComponent, {
            data: {contentTitle: title, message: message, onlyOkButton: false},
            maxHeight: '90vh',
            width: '300px',
        })

        dialogRef.afterClosed().subscribe((result) => {
            console.log(this.storyProgress.id)
            if (result === "yes") {
                this.router.navigate(['singleplayer/story/chapter', id], {
                    state: {
                        storyId: this.storyId
                    }
                })
            }
        })
    }
}
