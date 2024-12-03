import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-play-quiz-questions',
  imports: [],
  templateUrl: './play-quiz-questions.component.html',
  styleUrl: './play-quiz-questions.component.css'
})
export class PlayQuizQuestionsComponent {
  @Input() category: string = '';
  // questions: Question[] = [];

  currentQuestionIndex = 0;

  // constructor(e) {
  // }

  // ngOnInit() {
  //   if (this.category) {
  //     this.questionService.getQuestionsByCategory(this.category).subscribe((data) => {
  //       this.questions = data;
  //     })
  //   }
  // }
  //
  // goToNextQuestion() {
  //   if (this.currentQuestionIndex < this.questions.length - 1) {
  //     this.currentQuestionIndex++;
  //   }
  // }

}
