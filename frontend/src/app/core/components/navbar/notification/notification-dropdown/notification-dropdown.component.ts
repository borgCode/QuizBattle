import {AfterViewInit, Component, OnInit} from '@angular/core';
import {AsyncPipe, NgForOf, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {LoginStateService} from '../../../../services/login-state-service/login-state.service';
import {filter} from 'rxjs/operators';
import {NavigationEnd, Router} from '@angular/router';
import {NotificationStateService} from '../../../../services/notification/notification-state.service';

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


  playerId: number;
  private dropdown: any;

  constructor(
    private loginStateService: LoginStateService,
    protected notificationStateService: NotificationStateService,
    private router: Router
  ) {
  }

  ngOnInit() {
    this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.playerId = this.loginStateService.loggedInUser.id;
        this.notificationStateService.playerId = this.playerId;
        this.notificationStateService.loadNotifications();
    }

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)).subscribe(() => {
      if (this.loginStateService.isLoggedIn$) {
        this.notificationStateService.loadNotifications();
      }
    })
  })
  }

  ngAfterViewInit() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl)
    });

    const dropdownElement = document.getElementById('notificationsDropdown');
    this.dropdown = new bootstrap.Dropdown(dropdownElement);
  }

  private closeDropdown() {
    if (this.dropdown) {
      this.dropdown.hide();
    }
  }
  acceptFriendRequest(originalSender: number, notificationId: number) {
    this.notificationStateService.acceptFriendRequest(originalSender, notificationId);

  }

  declineFriendRequest(originalSender: number, notificationId: number) {
    this.notificationStateService.declineFriendRequest(originalSender, notificationId);
  }

  acceptRematch(senderId: number, pendingSessionId: number, notificationId: number) {
    this.notificationStateService.acceptRematch(senderId, pendingSessionId, notificationId).subscribe({
      next: sessionId => {
        const modal = document.getElementById('goToGameModal');
        modal.classList.add('show')
        modal.style.display = 'block';

        const stayButton = document.getElementById('stay-button')
        const goToGameButton = document.getElementById('go-to-game-button')
        const closeButton = document.querySelector('.btn-close');

        closeButton.addEventListener('click', () => {
          modal.style.display = 'none';
          modal.classList.remove('show');
        });

        stayButton.addEventListener('click', () => {
          modal.style.display = 'none';
          modal.classList.remove('show');
        })

        goToGameButton.addEventListener('click', () => {
          this.router.navigate(['multiplayer', sessionId]);
          this.closeDropdown();
          modal.style.display = 'none';
          modal.classList.remove('show');
        })
      }
    })
  }

  declineRematchRequest(senderId: number, pendingSessionId: number, notificationId: number) {
    this.notificationStateService.declineRematchRequest(senderId, pendingSessionId, notificationId);
  }

  async goToGame(notificationId: number, startedSessionId: number) {
    this.markAsRead(notificationId);
    this.router.navigate(['multiplayer', startedSessionId]);
    this.closeDropdown();
  }

  requestRematch(notificationId: number, startedSessionId: number) {
    this.notificationStateService.requestRematch(notificationId, startedSessionId);
  }

  markAsRead(notificationId: number) {
    this.notificationStateService.markAsRead(notificationId);
  }

  archiveNotification(notificationId: number) {
    this.notificationStateService.archiveNotification(notificationId);
  }

  markAllAsRead() {
    this.notificationStateService.markAllAsRead();
  }
}
