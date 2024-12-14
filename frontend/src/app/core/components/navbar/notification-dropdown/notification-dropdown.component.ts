import {AfterViewInit, Component, OnInit} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {BehaviorSubject} from 'rxjs';
import {Notification} from '../../../../api/generated/models/notification';
import {NotificationService} from '../../../../api/generated/services/notification.service';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {filter} from 'rxjs/operators';
import {NavigationEnd, Router} from '@angular/router';
import {FriendshipService} from '../../../../api/generated/services/friendship.service';
import {AlertMessageService} from '../../../services/alert-message/alert-message.service';
import {MultiplayerService} from '../../../../api/generated/services/multiplayer.service';

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
export class NotificationDropdownComponent implements OnInit, AfterViewInit {
  private notifications = new BehaviorSubject<Notification[]>([]);
  notifications$ = this.notifications.asObservable();

  private readNotificationList = new Set<number>();

  constructor(
    protected notificationService: NotificationService,
    private loginStateService: LoginStateService,
    private friendshipService: FriendshipService,
    private alertMessageService: AlertMessageService,
    private multiplayerService: MultiplayerService,
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
        this.syncReadNotifications();
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

  acceptFriendRequest(originalSender: number, notificationId: number) {
    //Remove value from notif bell
    this.notifications.next(
      this.notifications.value.filter(n => n.id !== notificationId)
    )
    this.friendshipService.acceptFriend({
      body: {
        senderId: this.loginStateService.loggedInUser.id, receiverId: originalSender, notificationId: notificationId
      }
    }).subscribe({
      next: () => this.alertMessageService.show("Accepted friend request!", 'success'),
    })
  }

  declineFriendRequest(originalSender: number, notificationId: number) {
    this.notifications.next(
      this.notifications.value.filter(n => n.id !== notificationId))

    this.friendshipService.rejectFriendship({
      body: {
        senderId: this.loginStateService.loggedInUser.id, receiverId: originalSender, notificationId: notificationId
      }
    }).subscribe({
      next: () => this.alertMessageService.show("Friend request rejected!", 'success'),
    })
  }



  markAsRead(notificationId: number) {
    this.notifications.next(
      this.notifications.value.filter(n => n.id !== notificationId)
    )

    this.readNotificationList.add(notificationId);
  }

  private syncReadNotifications() {
    if (this.readNotificationList.size > 0) {
      const idsToSync = Array.from(this.readNotificationList);

      this.notificationService.markAsRead({notificationIds: idsToSync}).subscribe({
          next: () => {
            this.readNotificationList.clear();
          }
        }
      )
    }
  }

  acceptRematch(senderId: number, pendingSessionId: number, notificationId: number) {
    console.log(senderId)
    this.sendRematchResponse(senderId, pendingSessionId, notificationId, true, 'You accepted the rematch!')
  }

  declineRematchRequest(senderId: number, pendingSessionId: number, notificationId: number) {
    console.log(senderId)
    this.sendRematchResponse(senderId, pendingSessionId, notificationId, false, 'You declined the rematch!')
  }

  sendRematchResponse(senderId: number, pendingSessionId: number, notificationId: number, hasAccepted: boolean, alertMessage: string) {
    this.notifications.next(
      this.notifications.value.filter(n => n.id !== notificationId))

    this.multiplayerService.rematchResponse({
      body: {
        playerId: senderId,
        hasAccepted: hasAccepted,
        notificationId: notificationId,
        pendingSessionId: pendingSessionId,
        playerDisplayName: this.loginStateService.loggedInUser.displayName
      }
    }).subscribe({
      next: () => this.alertMessageService.show(alertMessage, 'success'),
    })
  }

}
