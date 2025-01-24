import {Injectable} from '@angular/core';
import {NotificationService} from '../../../api/generated/services/notification.service';
import {FriendshipService} from '../../../api/generated/services/friendship.service';
import {AlertMessageService} from '../alert-message/alert-message.service';
import {MultiplayerMatchService} from '../../../api/generated/services/multiplayer-match.service';
import {BehaviorSubject, tap} from 'rxjs';
import {Notification} from '../../../api/generated/models/notification';
import {LoginStateService} from '../login-state-service/login-state.service';

interface NotificationState {
  unread: Notification[],
  read: Notification[]
}

@Injectable({
  providedIn: 'root'
})
export class NotificationStateService {
  private notificationStateSubject = new BehaviorSubject<NotificationState>({
    unread: [],
    read: []
  })

  readonly notificationState$ = this.notificationStateSubject.asObservable();

  private _playerId: number;

  constructor(
    private notificationService: NotificationService,
    private friendshipService: FriendshipService,
    private alertMessageService: AlertMessageService,
    private multiplayerMatchService: MultiplayerMatchService,
    private loginStateService: LoginStateService,
  ) {
  }

  loadNotifications() {
    console.log("Fetching notifs with id: ", this._playerId)
    this.notificationService.getActivePlayerNotifications({playerId: this._playerId}).subscribe(
      notifications => {
        const readNotifications = notifications.filter(notification => notification.read);
        const unreadNotifications = notifications.filter(notification => !notification.read);
        this.notificationStateSubject.next({
          unread: unreadNotifications,
          read: readNotifications
        })
      }
    )
  }

  acceptFriendRequest(originalSender: number, notificationId: number) {
    this.friendshipService.acceptFriend({
      body: {
        senderId: this.playerId, receiverId: originalSender, notificationId: notificationId
      }
    }).subscribe({
      next: () => {
        this.alertMessageService.show("Accepted friend request!", 'success')
        this.removeFromList(notificationId);
      },
    })
  }

  declineFriendRequest(originalSender: number, notificationId: number) {
    this.friendshipService.rejectFriendship({
      body: {
        senderId: this.playerId, receiverId: originalSender, notificationId: notificationId
      }
    }).subscribe({
      next: () => {
        this.alertMessageService.show("Friend request rejected!", 'success')
        this.removeFromList(notificationId);
      },
    })
  }

  acceptRematch(senderId: number, pendingSessionId: number, notificationId: number) {
    return this.multiplayerMatchService.acceptMatch({
      body: {
        senderId: this._playerId,
        receiverId: senderId,
        notificationId: notificationId,
        pendingSessionId: pendingSessionId,
        playerDisplayName: this.loginStateService.loggedInUser.displayName,
        rematch: true
      }
    }).pipe(
      tap(() => this.removeFromList(notificationId))
    )
  }

  declineRematchRequest(senderId: number, pendingSessionId: number, notificationId: number) {
    this.multiplayerMatchService.rejectMatch({
      body: {
        senderId: this._playerId,
        receiverId: senderId,
        notificationId: notificationId,
        pendingSessionId: pendingSessionId,
        playerDisplayName: this.loginStateService.loggedInUser.displayName,
        rematch: true
      }
    }).subscribe({
      next: () => {
        this.alertMessageService.show("You declined the rematch!", 'success')
        this.removeFromList(notificationId);
      },
    })
  }

  requestRematch(notificationId: number, startedSessionId: number) {
    this.multiplayerMatchService.requestRematch({
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
    this.notificationService.markAsRead({
      notificationId: notificationId,
      playerId: this._playerId
    }).subscribe({
      next: () => this.moveToRead(notificationId)
    })
  }

  markAllAsRead() {
    const notificationIds: number[] = this.notificationStateSubject.value.unread.map(notification => notification.id)
    this.notificationService.markAllAsRead({
      notificationIds: notificationIds,
      playerId: this._playerId
    }).subscribe({
      next: () => this.loadNotifications()
    })
  }

  private moveToRead(notificationId: number) {
    const currentUnread = this.notificationStateSubject.getValue().unread;
    const currentRead = this.notificationStateSubject.getValue().read;

    const notificationToMove = currentUnread.find(n => n.id === notificationId);
    const newUnread = currentUnread.filter(n => n.id !== notificationId);

    const readNotification = {...notificationToMove, read: true};

    this.notificationStateSubject.next({
      unread: newUnread,
      read: [...currentRead, readNotification]
    })
  }

  private removeFromList(notificationId: number) {
    const currentState = this.notificationStateSubject.getValue();
    const notificationExists = currentState.unread.some(n => n.id === notificationId) ||
      currentState.read.some(n => n.id === notificationId);

    if (notificationExists) {
      const newUnread = currentState.unread.filter(notification =>
        notification && notification.id !== notificationId
      );
      const newRead = currentState.read.filter(notification =>
        notification && notification.id !== notificationId
      );

      if (newUnread.length !== currentState.unread.length ||
        newRead.length !== currentState.read.length) {
        this.notificationStateSubject.next({
          unread: newUnread,
          read: newRead
        });
      }
    }
  }

  archiveNotification(notificationId: number) {
    this.notificationService.archiveNotification({
      notificationId: notificationId,
      playerId: this._playerId
    }).subscribe({
      next: () => {
        this.removeFromList(notificationId);
      },
      error: (error) => {
        console.error('Error archiving notification:', error);
        this.loadNotifications();
      }
    });
  }

  set playerId(value: number) {
    this._playerId = value;
  }
}
