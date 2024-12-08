import {Component, OnDestroy} from '@angular/core';
import {Subscription} from 'rxjs';
import {NotificationService} from '../../services/notification.service';
import {NgClass, NgIf} from '@angular/common';

@Component({
  selector: 'app-notification',
  imports: [
    NgClass,
    NgIf
  ],
  templateUrl: './notification.component.html',
  styleUrl: './notification.component.css'
})
export class NotificationComponent implements OnDestroy{

  message: string = '';
  type: 'success' | 'error' = 'success';
  private subscription: Subscription;

  constructor(private notificationService: NotificationService) {
    this.subscription = this.notificationService.notification$.subscribe(notification => {
      this.message = notification.message;
      this.type = notification.type;

      setTimeout(() => {
        this.message = '';
      }, 3000)
    })
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }


}
