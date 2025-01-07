import {AfterViewInit, Component, OnInit} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {BehaviorSubject} from 'rxjs';
import {Notification} from '../../../../api/generated/models/notification';
import {NotificationService} from '../../../../api/generated/services/notification.service';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {filter} from 'rxjs/operators';
import {NavigationEnd, Router} from '@angular/router';
import {AlertMessageService} from '../../../services/alert-message/alert-message.service';
import {MultiplayerService} from '../../../../api/generated/services/multiplayer.service';
import {FriendshipService} from '../../../../api/generated/services/friendship.service';

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
  private unreadNotifications = new BehaviorSubject<Notification[]>([]);
  unreadNotifications$ = this.unreadNotifications.asObservable();
  private readNotifications = new BehaviorSubject<Notification[]>([]);
  readNotifications$ = this.readNotifications.asObservable();


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
        this.unreadNotifications.next([]);
        this.readNotifications.next([]);
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
    console.log("User id: " + this.loginStateService.loggedInUser.id)
    if (this.loginStateService.loggedInUser.id) {
      this.notificationService.getActivePlayerNotifications({playerId: this.loginStateService.loggedInUser.id}).subscribe(
        notifications => {
          const readNotifications = notifications.filter(notification => notification.read);
          const unreadNotifications = notifications.filter(notification => !notification.read);

          this.readNotifications.next(readNotifications);
          this.unreadNotifications.next(unreadNotifications);
        }
      )
    }
  }

  acceptFriendRequest(originalSender: number, notificationId: number) {
    //Remove value from notif bell

    this.friendshipService.acceptFriend({
      body: {
        senderId: this.loginStateService.loggedInUser.id, receiverId: originalSender, notificationId: notificationId
      }
    }).subscribe({
      next: () => {
        this.alertMessageService.show("Accepted friend request!", 'success')
        this.fetchNotifications();
      },
    })
  }

  declineFriendRequest(originalSender: number, notificationId: number) {
    this.friendshipService.rejectFriendship({
      body: {
        senderId: this.loginStateService.loggedInUser.id, receiverId: originalSender, notificationId: notificationId
      }
    }).subscribe({
      next: () => {
        this.alertMessageService.show("Friend request rejected!", 'success')
        this.fetchNotifications();
      },
    })
  }

  acceptRematch(senderId: number, pendingSessionId: number, notificationId: number) {
    this.multiplayerService.acceptRematch({
      body: {
        originalSenderId: senderId,
        notificationId: notificationId,
        pendingSessionId: pendingSessionId,
        playerDisplayName: this.loginStateService.loggedInUser.displayName
      }
    }).subscribe({
      next: sessionId => {
        const modal = document.getElementById('goToGameModal');
        modal.classList.add('show')
        modal.style.display = 'block';

        const stayButton = document.getElementById('stay-button')
        const goToGameButton = document.getElementById('go-to-game-button')

        stayButton.addEventListener('click', () => {
          modal.style.display = 'none';
          modal.classList.remove('show');
        })

        goToGameButton.addEventListener('click', () => {
          this.router.navigate(['multiplayer', sessionId]);
          modal.style.display = 'none';
          modal.classList.remove('show');
        })
        this.fetchNotifications();
      }
    })
  }

  declineRematchRequest(senderId: number, pendingSessionId: number, notificationId: number) {
    this.multiplayerService.rejectRematch({
      body: {
        originalSenderId: senderId,
        notificationId: notificationId,
        pendingSessionId: pendingSessionId,
        playerDisplayName: this.loginStateService.loggedInUser.displayName
      }
    }).subscribe({
      next: () => {
        this.alertMessageService.show("You declined the rematch!", 'success')
        this.fetchNotifications()
      },
    })
  }

  async goToGame(notificationId: number, startedSessionId: number) {
    this.markAsRead(notificationId);
    this.router.navigate(['multiplayer', startedSessionId]);
  }

  requestRematch(notificationId: number, startedSessionId: number) {
    this.multiplayerService.requestRematch({
      body: {
        sessionId: startedSessionId,
        playerId: this.loginStateService.loggedInUser.id}}).subscribe({
      next: () => {
        this.alertMessageService.show('Send rematch request!', 'success')
        this.markAsRead(notificationId);
      }
    })
  }

  markAsRead(notificationId: number) {
    this.notificationService.markAsRead({notificationId: notificationId}).subscribe({
      next: () => {
        this.fetchNotifications();
      }
    });
  }

  archiveNotification(notificationId: number) {
    this.notificationService.archiveNotification({notificationId: notificationId}).subscribe({
      next: () => {
        this.fetchNotifications();
      }
    })
  }
}
