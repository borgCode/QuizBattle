import { Component } from '@angular/core';
import {PlayQuizCategoryComponent} from './play-quiz-category/play-quiz-category.component';
import {PlayQuizQuestionsComponent} from './play-quiz-questions/play-quiz-questions.component';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-play',
  imports: [
    PlayQuizCategoryComponent,
    PlayQuizQuestionsComponent,
    NgIf
  ],
  templateUrl: './play.component.html',
  styleUrl: './play.component.css'
})
export class PlayComponent {
  showCategory: boolean = true;
  selectedCategory: string = '';

  //Show questions component when a category is selected
  onCategorySelected(category: string) {
    this.selectedCategory = category;
    this.showCategory = false;
  }

  resetToCategorySelection() {
    this.showCategory = true;
    this.selectedCategory = '';
  }

}
