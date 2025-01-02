import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  AchievementNotification,
  AchievementNotificationService
} from '../services/achievement-notif-service/achievement-notification.service';
import {Subscription} from 'rxjs';
import {animate, keyframes, state, style, transition, trigger} from '@angular/animations';

@Component({
  selector: 'app-achievement-popup',
  imports: [],
  templateUrl: './achievement-popup.component.html',
  styleUrl: './achievement-popup.component.css',
  animations: [
    trigger('popupState', [
      state('show', style({
        opacity: 1,

      })),
      state('hide', style({
        opacity: 0,

      })),
      transition('hide => show', [
        animate('0.2s ease-out'),
        animate(
          "0.7s",
          keyframes([
            style({transform: 'rotate(0)'}),
            style({transform: 'rotate(-2deg)'}),
            style({transform: 'rotate(2deg)'}),
            style({transform: 'rotate(-2deg)'}),
            style({transform: 'rotate(2deg)'}),
            style({transform: 'rotate(-2deg)'}),
            style({transform: 'rotate(0)'})
          ])
        )
      ]),
      transition('show => hide', [
        animate('0.5s ease-in')
      ])
    ])
  ]
})
export class AchievementPopupComponent implements OnInit, OnDestroy {
  currentAchievement?: AchievementNotification;
  private subscription?: Subscription;
  isVisible = false;

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

  togglePopup() {
    this.isVisible = !this.isVisible;
  }

  // Method to show popup
  showPopup() {
    this.isVisible = true;
  }

  // Method to hide popup
  hidePopup() {
    this.isVisible = false;
  }
}
