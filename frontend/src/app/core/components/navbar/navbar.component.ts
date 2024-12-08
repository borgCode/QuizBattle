import {Component, OnInit} from '@angular/core';
import {NavigationEnd, Router, RouterLink, RouterLinkActive} from '@angular/router';
import {LoginStateService} from '../../services/login-state-service/login-state.service';
import {AsyncPipe, NgIf} from '@angular/common';
import {TokenService} from '../../services/token/token.service';
import {NotificationService} from '../../../api/generated/services/notification.service';
import {BehaviorSubject, takeUntil} from 'rxjs';
import {Notification} from '../../../api/generated/models/notification';
import {filter} from 'rxjs/operators';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLinkActive,
    RouterLink,
    NgIf,
    AsyncPipe
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit {
  private notifications = new BehaviorSubject<Notification[]>([]);
  notifications$ = this.notifications.asObservable();

  constructor(
    protected notificationService: NotificationService,
    protected loginStateService: LoginStateService,
    private tokenService: TokenService,
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

  private fetchNotifications() {
    if (this.loginStateService.loggedInUser.id) {
      this.notificationService.getPlayerNotifications({playerId: this.loginStateService.loggedInUser.id}).subscribe(
        notifications => this.notifications.next(notifications)
      )
    }
  }

  logout() {
    this.tokenService.clearToken();
    this.loginStateService.clearLoggedInUser();
    this.router.navigate(['/login']);
  }


}
