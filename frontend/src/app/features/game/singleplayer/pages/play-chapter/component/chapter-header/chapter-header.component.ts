import {Component, EventEmitter, Input, Output} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {Observable} from 'rxjs';
import {PlayChapterDto} from '../../../../../../../api/generated/models/play-chapter-dto';
import {animate, keyframes, style, transition, trigger} from '@angular/animations';

@Component({
  selector: 'app-chapter-header',
  imports: [
    AsyncPipe,
    NgForOf,
    NgIf
  ],
  templateUrl: './chapter-header.component.html',
  styleUrl: './chapter-header.component.css',
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
export class ChapterHeaderComponent {
  @Input() chapterData$: Observable<PlayChapterDto>
  @Input() currentHealth!: number;
  @Input() lastLostHeart!: number;
  @Input() showQuiz!: boolean;
  @Output() startQuiz = new EventEmitter<void>();

  emitStart() {
    this.startQuiz.emit();
  }
}
