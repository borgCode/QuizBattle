import {Injectable} from '@angular/core';
import {Notification} from '../../../api/generated/models/notification';
import {BehaviorSubject, debounceTime, distinctUntilChanged} from 'rxjs';
import {NotificationService} from '../../../api/generated/services/notification.service';
import {PageNotification} from '../../../api/generated/models/page-notification';

interface NotificationState {
  notifications: Notification[];
  currentPage: number;
  isLoading: boolean;
  hasMore: boolean;
  totalPages: number;
  searchQuery: string;
}

export enum TimeFilter {
  ALL_TIME = 'ALL_TIME',
  TODAY = 'TODAY',
  THIS_WEEK = 'THIS_WEEK',
  THIS_MONTH = 'THIS_MONTH'
}


@Injectable({
  providedIn: 'root'
})
export class NotificationArchiveService {
  private notificationSubject: BehaviorSubject<NotificationState> = new BehaviorSubject<NotificationState>({
    notifications: [],
    currentPage: 0,
    isLoading: false,
    hasMore: true,
    totalPages: 0,
    searchQuery: ''
  });

  readonly notificationState$ = this.notificationSubject.asObservable();

  private searchQuerySubject = new BehaviorSubject<string>('');

  playerId: number;
  private _timeFilter: TimeFilter

  constructor(
    private notificationService: NotificationService,
  ) {
    this.searchQuerySubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(query => {
      const currentState = this.notificationSubject.value;
      if (currentState) {
        this.notificationSubject.next({
          ...currentState,
          searchQuery: query,
          currentPage: 0
        });
        this.getNotifications();
      }
    });

  }

  init(playerId: number) {
    this.playerId = playerId;
    this._timeFilter = TimeFilter.ALL_TIME
    this.getNotifications()
  }

  getNotifications() {
    const currentState = this.notificationSubject.value;
    this.notificationSubject.next({
      ...currentState,
      isLoading: true
    });

    this.notificationService.getArchivedNotifications({
      playerId: this.playerId,
      pageable: {
        page: currentState.currentPage
      },
      timeFilter: this._timeFilter,
      searchFilter: currentState.searchQuery
    }).subscribe({
      next: pageResponse => this.loadNotifications(pageResponse, true),
      error: () => {
        this.notificationSubject.next({
          ...currentState,
          isLoading: false
        });
      }
    });
  }

  loadNotifications(pageResponse: PageNotification, advanceToNext: boolean) {
    const currentState = this.notificationSubject.value;

    this.notificationSubject.next({
      notifications: pageResponse.content,
      currentPage: advanceToNext ? currentState.currentPage + 1 : currentState.currentPage - 1,
      isLoading: false,
      hasMore: pageResponse.number < pageResponse.totalPages - 1,
      totalPages: pageResponse.totalPages,
      searchQuery: currentState.searchQuery
    });
  }

  getNextPage() {
    const currentState = this.notificationSubject.value;
    if (currentState.hasMore) {
      this.notificationService.getArchivedNotifications({
        playerId: this.playerId,
        pageable: {
          page: currentState.currentPage + 1
        },
        timeFilter: this._timeFilter,
        searchFilter: currentState.searchQuery
      }).subscribe({
        next: pageResponse => this.loadNotifications(pageResponse, true)
      })
    }
  }

  getPreviousPage() {
    const currentState = this.notificationSubject.value;
    this.notificationService.getArchivedNotifications({
      playerId: this.playerId,
      pageable: {
        page: currentState.currentPage - 1
      },
      timeFilter: this._timeFilter,
      searchFilter: currentState.searchQuery
    }).subscribe({
      next: pageResponse => this.loadNotifications(pageResponse, false)
    })

  }

  updateSearchQuery(query: string) {
    this.searchQuerySubject.next(query);
  }

  set timeFilter(value: TimeFilter) {
    this._timeFilter = value;
  }

  get timeFilter(): TimeFilter {
    return this._timeFilter;
  }
}
