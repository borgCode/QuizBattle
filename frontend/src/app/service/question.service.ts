import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Question} from '../model/question';

@Injectable({
  providedIn: 'root',
})

export class QuestionService {
  private apiUrl = 'http://localhost:8080/questions';

  constructor(private http: HttpClient) {}

  getQuestionsByCategory(category: string): Observable<Question[]> {
    return this.http.get<Question[]>(this.apiUrl + '/' + category);
  }

}
