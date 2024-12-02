import {Component, Input, OnInit} from '@angular/core';
import {Question} from '../../../model/question';
import {QuestionService} from '../../../service/question.service';

@Component({
  selector: 'app-play-quiz-questions',
  imports: [],
  templateUrl: './play-quiz-questions.component.html',
  styleUrl: './play-quiz-questions.component.css'
})
export class PlayQuizQuestionsComponent implements OnInit {
  @Input() category: string = '';
  questions: Question[] = [];

  currentQuestionIndex = 0;

  constructor(private questionService: QuestionService) {
  }

  ngOnInit() {
    if (this.category) {
      this.questionService.getQuestionsByCategory(this.category).subscribe((data) => {
        this.questions = data;
      })
    }
  }

  goToNextQuestion() {
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
    }
  }

}
