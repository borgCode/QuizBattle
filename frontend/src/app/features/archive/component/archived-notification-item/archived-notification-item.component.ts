import {Component, Input} from '@angular/core';
import {Notification} from '../../../../api/generated/models/notification';
import {DatePipe, NgClass, NgIf} from '@angular/common';

@Component({
  selector: 'app-archived-notification-item',
  imports: [
    NgIf,
    DatePipe,
    NgClass
  ],
  templateUrl: './archived-notification-item.component.html',
  styleUrl: './archived-notification-item.component.css'
})
export class ArchivedNotificationItemComponent {
  @Input() notification!: Notification;


}
