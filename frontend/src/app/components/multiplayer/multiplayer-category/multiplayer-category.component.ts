import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {QuestionsService} from '../../../services/services/questions.service';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-multiplayer-category',
  imports: [
    NgForOf
  ],
  templateUrl: './multiplayer-category.component.html',
  styleUrl: './multiplayer-category.component.css'
})
export class MultiplayerCategoryComponent implements OnInit{
  categories: Array<string> = [];

  constructor(
    private router: Router,
    private questionService: QuestionsService,
  private activatedRoute: ActivatedRoute,
  ) {
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(value => {
      const sessionId = value['sessionId'];

      this.questionService.getThreeRandomCategories({sessionId: sessionId}).subscribe({
      next: categories =>
      this.categories = categories
      })

    })
  }


  selectCategory(category: string) {

  }
}
