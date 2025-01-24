import {Component, OnInit} from '@angular/core';
import {ArchivedNotificationItemComponent} from '../archived-notification-item/archived-notification-item.component';
import {NotificationArchiveService, TimeFilter} from '../../service/notification-archive.service';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {AsyncPipe, NgClass, NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-archived-notifications',
  imports: [
    ArchivedNotificationItemComponent,
    NgIf,
    AsyncPipe,
    NgForOf,
    NgClass
  ],
  templateUrl: './archived-notifications.component.html',
  styleUrl: './archived-notifications.component.css'
})
export class ArchivedNotificationsComponent implements OnInit {

  constructor(
    protected notificationArchiveService: NotificationArchiveService,
    private loginStateService: LoginStateService,
  ) {
  }

  ngOnInit() {
    this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.notificationArchiveService.init(this.loginStateService.loggedInUser.id);
      }
    })
  }

  onSearchInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.notificationArchiveService.updateSearchQuery(input.value);
  }

  nextPage() {
    this.notificationArchiveService.getNextPage();
  }

  previousPage() {
    this.notificationArchiveService.getPreviousPage();
  }

  setTimeFilter(timeFilter: TimeFilter) {
    this.notificationArchiveService.timeFilter = timeFilter;
    this.notificationArchiveService.getNotifications()
  }

  protected readonly TimeFilter = TimeFilter;
}
