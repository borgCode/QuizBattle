import { Injectable } from '@angular/core';
import {ChapterService} from "../../../../../api/generated/services/chapter.service";
import {QuestionService} from "../../../../../api/generated/services/question.service";
import {BehaviorSubject, Observable, switchMap, tap} from "rxjs";
import {QuestionDto} from "../../../../../api/generated/models/question-dto";
import {PlayChapterDto} from "../../../../../api/generated/models/play-chapter-dto";

@Injectable({
  providedIn: 'root'
})
export class PlayChapterFacadeService {
    private chapterDetailsSubject = new BehaviorSubject<PlayChapterDto>(null);
    private questionsSubject: BehaviorSubject<QuestionDto[]> = new BehaviorSubject([]);
    private currentQuestionIndexSubject: BehaviorSubject<number> = new BehaviorSubject(0);

  constructor(
      private chapterService: ChapterService,
      private questionService: QuestionService,
  ) { }

    get chapterDetails$(): Observable<PlayChapterDto> {
        return this.chapterDetailsSubject.asObservable();
    }

  get questions$(): Observable<QuestionDto[]> {
    return this.questionsSubject.asObservable();
  }

  get currentQuestionIndex$(): Observable<number> {
    return this.currentQuestionIndexSubject.asObservable();
  }

  startChapter(playerId: number, storyId: number, chapterId: number) {
    return this.chapterService.startChapter({
      body: {
        playerId: playerId,
        storyId: storyId,
        chapterId: chapterId
      }
    }).pipe(
        switchMap(() => this.chapterService.getChapter({chapterId: chapterId}).pipe(
            tap(chapter => this.chapterDetailsSubject.next(chapter)),
        ))
    )
  }
  fetchQuestions(playerId: number) {
    return this.questionService.restoreSessionQuestions({playerId}).pipe(
      switchMap(progress => {
        if (progress?.questions.length > 0) {
          this.questionsSubject.next(progress.questions);
          this.currentQuestionIndexSubject.next(progress.currentIndex);
          return this.questions$;
        } else {
          return this.questionService.getSinglePlayerRoundQuestions({playerId}).pipe(
            tap(questions => {
              this.questionsSubject.next(questions);
              this.currentQuestionIndexSubject.next(0);
            })
          );
        }
      })
    );
  }
}
