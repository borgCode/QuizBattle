import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';

export interface Notification {
  message: string;
  type: 'success' | 'error';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private notification = new Subject<Notification>
  notification$ = this.notification.asObservable();

  show(message: string, type: 'success' | 'error') {
    this.notification.next({message, type});
  }

  handleError(error: Error) {
    try {
      const errorData = JSON.parse(error.message);
      if (errorData.validationErrors) {
        this.show(errorData.validationErrors, 'error');
      } else if (errorData.errorMessage) {
        this.show(errorData.errorMessage, 'error');
      } else {
        this.show('An unexpected error occurred', 'error');
      }
    } catch {
      this.show(error.message, 'error');
    }
  }
}
