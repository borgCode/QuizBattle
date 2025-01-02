import {Component, OnDestroy, OnInit} from '@angular/core';
import {
  AchievementNotification,
  AchievementNotificationService
} from '../services/achievement-notif-service/achievement-notification.service';
import {Subscription} from 'rxjs';
import {animate, keyframes, state, style, transition, trigger} from '@angular/animations';
import {NgIf} from '@angular/common';

@Component({
  selector: 'app-achievement-popup',
  imports: [
    NgIf
  ],
  templateUrl: './achievement-popup.component.html',
  styleUrl: './achievement-popup.component.css',
  animations: [
    trigger('popupState', [
      transition(':enter', [
        style({opacity: 0}), animate('0.2s ease-out', style({opacity: 1})),
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
      transition(':leave', [
        animate('0.5s 3s ease-in', style({opacity: 0}))
      ])
    ])
  ]
})
export class AchievementPopupComponent implements OnInit, OnDestroy {
  currentAchievement?: AchievementNotification;
  private subscription?: Subscription;
  shouldShowPopup = false;

  constructor(private achievementService: AchievementNotificationService) {
  }

  ngOnInit() {
    this.subscription = this.achievementService.achievements$.subscribe(
      achievement => {
        this.currentAchievement = achievement;
        this.shouldShowPopup = true;
      }
    );
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  clearAchievement() {
    this.currentAchievement = null;
    this.shouldShowPopup = false;
  }
}
