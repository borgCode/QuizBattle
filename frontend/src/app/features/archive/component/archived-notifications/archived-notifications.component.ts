import {Component, OnInit} from '@angular/core';
import {ArchivedNotificationItemComponent} from '../archived-notification-item/archived-notification-item.component';
import {NotificationArchiveService, TimeFilter} from '../../service/notification-archive.service';
import {LoginStateService} from '../../../../core/services/login-state-service/login-state.service';
import {AsyncPipe, NgClass, NgForOf, NgIf} from '@angular/common';
import {NotificationType} from "../../../../shared/enum/notification-type";

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
  enumKeys = Object.keys(NotificationType);

  selectedType: string = '';

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
    this.notificationArchiveService.updateTimeFilter(timeFilter);
    this.notificationArchiveService.getNotifications()
  }

  protected readonly TimeFilter = TimeFilter;

  updateTypeFilter(typeFilter: NotificationType) {
    this.selectedType = this.formatEnumValue(typeFilter);
    this.notificationArchiveService.updateTypeFilter(typeFilter);
    this.notificationArchiveService.getNotifications();
  }
  protected readonly NotificationType = NotificationType;

  removeTypeFilter() {
    this.selectedType = '';
    this.notificationArchiveService.removeTypeFilter();
    this.notificationArchiveService.getNotifications();
  }

  formatEnumValue(value: string): string {
    return value.charAt(0).toUpperCase() +
      value.slice(1).toLowerCase().replace('_', ' ');
  }
}
