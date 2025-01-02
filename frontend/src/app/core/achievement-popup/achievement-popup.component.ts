import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  AchievementNotification,
  AchievementNotificationService
} from '../services/achievement-notif-service/achievement-notification.service';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-achievement-popup',
  imports: [],
  templateUrl: './achievement-popup.component.html',
  styleUrl: './achievement-popup.component.css'
})
export class AchievementPopupComponent implements OnInit, OnDestroy {
  currentAchievement?: AchievementNotification;
  private subscription?: Subscription;

  constructor(private achievementService: AchievementNotificationService) {
  }

  ngOnInit() {
    this.subscription = this.achievementService.achievements$.subscribe(
      achievement => {
        this.currentAchievement = achievement;
        setTimeout(() => {
          this.currentAchievement = undefined;
        }, 5000);
      }
    );
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
}
