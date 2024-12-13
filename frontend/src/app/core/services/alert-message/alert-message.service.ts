import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';

export interface AlertMessage {
  message: string;
  type: 'success' | 'error';
}

@Injectable({
  providedIn: 'root'
})
export class AlertMessageService {

  private alertMessage = new Subject<AlertMessage>
  alertMessage$ = this.alertMessage.asObservable();

  show(message: string, type: 'success' | 'error') {
    console.log("Showing alert")
    this.alertMessage.next({message, type});
  }
}
