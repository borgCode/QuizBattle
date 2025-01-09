import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {WebSocketService} from '../../websocket/web-socket.service';
import {filter} from 'rxjs/operators';

export interface AchievementNotification {
  achievementName: string;
  achievementDescription: string;
  base64Image: string;
  earnedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AchievementNotificationService {
  private achievementSubject = new Subject<AchievementNotification>();
  achievements$ = this.achievementSubject.asObservable();

  constructor(
    private webSocketService: WebSocketService
  ) {
    this.webSocketService.achievement$.subscribe(message => {
      this.achievementSubject.next(message);
    });
  }
}
