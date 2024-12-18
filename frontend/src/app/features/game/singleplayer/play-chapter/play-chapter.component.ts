import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {PlayerProgressDto} from '../../../../api/generated/models/player-progress-dto';
import {ChapterService} from '../../../../api/generated/services/chapter.service';
import {QuestionDto} from '../../../../api/generated/models/question-dto';
import {QuestionsService} from '../../../../api/generated/services/questions.service';
import {QuestionPanelComponent} from '../../../../shared/components/question-panel/question-panel.component';

@Component({
  selector: 'app-play-chapter',
  imports: [
    QuestionPanelComponent
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
  health: number = 3;
  round: number = 0;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private chapterService: ChapterService,
    private questionService: QuestionsService

  ) {
    this.playerProgress = this.router.getCurrentNavigation().extras.state?.['playerProgress'];
    this.storyTitle = this.router.getCurrentNavigation().extras.state?.['storyTitle'];
    console.log(this.storyTitle)
  }

  ngOnInit() {

    this.activatedRoute.paramMap.subscribe((params) => {
      this.chapterId = +params.get("chapterId");
    })

    this.chapterService.getChapter({chapterId: this.chapterId}).subscribe({
      next: chapter => {
        this.categories = chapter.categories;
        this.rewardText = chapter.rewardText
        this.chapterTitle = chapter.title

        this.questionService.getFiveQuestionsByCategory({category: this.categories[this.round]}).subscribe( {
          next: questions => {
            this.questions = questions;
          }
        })

      }
    })

  }

}
