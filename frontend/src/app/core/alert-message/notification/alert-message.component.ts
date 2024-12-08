import {Component, OnDestroy} from '@angular/core';
import {Subscription} from 'rxjs';
import {AlertMessageService} from '../../services/alert-message/alert-message.service';
import {NgClass, NgIf} from '@angular/common';

@Component({
  selector: 'app-alert-message',
  imports: [
    NgClass,
    NgIf
  ],
  templateUrl: './alert-message.component.html',
  styleUrl: './alert-message.component.css'
})
export class AlertMessageComponent implements OnDestroy{

  message: string = '';
  type: 'success' | 'error' = 'success';
  private subscription: Subscription;

  constructor(private alertMessageService: AlertMessageService) {
    this.subscription = this.alertMessageService.alertMessage$.subscribe(alertMessage => {
      this.message = alertMessage.message;
      this.type = alertMessage.type;

      setTimeout(() => {
        this.message = '';
      }, 3000)
    })
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }


}
