import {Component, EventEmitter, Output} from '@angular/core';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-play-quiz-category',
  imports: [
    NgForOf
  ],
  templateUrl: './play-quiz-category.component.html',
  styleUrl: './play-quiz-category.component.css'
})
export class PlayQuizCategoryComponent {
  //Emitter to emit the selected category to parent
  @Output() categorySelected = new EventEmitter<string>();

  categories = ['Math', 'Science', 'History'];

  selectCategory(category: string) {
    this.categorySelected.emit(category);
  }

}
