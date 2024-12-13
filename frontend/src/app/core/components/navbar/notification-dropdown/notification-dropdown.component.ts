import {AfterViewInit, Component, Input, OnInit} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {BehaviorSubject, Observable} from 'rxjs';
import {Notification} from '../../../../api/generated/models/notification';
import {NotificationService} from '../../../../api/generated/services/notification.service';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {filter} from 'rxjs/operators';
import {NavigationEnd, Router} from '@angular/router';
declare var bootstrap: any;

@Component({
  selector: 'app-notification-dropdown',
  imports: [
    AsyncPipe,
    NgForOf,
    NgIf,
    NgSwitchCase,
    NgSwitch
  ],
  templateUrl: './notification-dropdown.component.html',
  styleUrl: './notification-dropdown.component.css'
})
export class NotificationDropdownComponent implements OnInit, AfterViewInit{
  private notifications = new BehaviorSubject<Notification[]>([]);
  notifications$ = this.notifications.asObservable();

  constructor(
    protected notificationService: NotificationService,
    protected loginStateService: LoginStateService,
    private router: Router
  ) {
  }

  ngOnInit() {
    this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.fetchNotifications();
      } else {
        this.notifications.next([]);
      }
    });

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)).subscribe(() => {
      if (this.loginStateService.isLoggedIn$) {
        this.fetchNotifications();
      }
    })
  }

  ngAfterViewInit() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl)
    });
  }

  private fetchNotifications() {
    if (this.loginStateService.loggedInUser.id) {
      this.notificationService.getPlayerNotifications({playerId: this.loginStateService.loggedInUser.id}).subscribe(
        notifications => this.notifications.next(notifications)
      )
    }
  }

  acceptFriendRequest() {

  }

  declineFriendRequest() {

  }
}
