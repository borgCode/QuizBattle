import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {catchError, Observable, throwError} from 'rxjs';
import {AlertMessageService} from '../../services/alert-message/alert-message.service';


@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private alertMessageService: AlertMessageService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.error?.validationErrors) {
          console.log("Throwing early error")
          return throwError(() => err);
        }

        //Parse the JSON format error
        let errorBody = err.error;
        if (typeof err.error === 'string') {
          try {
            errorBody = JSON.parse(err.error);
          } catch (e) {
            console.error('Failed to parse error response:', e);
          }
        }
        console.log(err.error)

        let errorMessage = 'An unexpected error occurred';
        if (errorBody.businessErrorCode) {
          switch (errorBody.businessErrorCode) {
            case 300:
              this.alertMessageService.show('Current password is incorrect', 'error');
              break;
            case 301:
              this.alertMessageService.show('New password does not match', 'error');
              break;
            case 302:
              this.alertMessageService.show('Your account has been locked', 'error');
              break;
            case 303:
              this.alertMessageService.show('Your account has been disabled', 'error');
              break;
            case 304:
              this.alertMessageService.show('Username and/or password is incorrect', 'error');
              break;
            case 305:
              this.alertMessageService.show("Username is already taken", 'error');
              break;
            case 320:
              this.alertMessageService.show('Cannot send friend request to a blocked user', 'error');
              break;
            case 321:
              this.alertMessageService.show('Friend request is already pending', 'error');
              break;
            case 322:
              this.alertMessageService.show('You are already friends with this user', 'error');
              break;
            case 323:
              this.alertMessageService.show('This friend request is no longer available', 'error');
              break;
            case 324:
              this.alertMessageService.show('You already sent a rematch request!', 'error');
              break;
            case 325:
              this.alertMessageService.show('You already have an ongoing match with this player!', 'error');
              break;
            case 413:
              this.alertMessageService.show('The file size is too big! Max 500kb', 'error');
              break;
          }
        } else if (err.status) {
          switch (err.status) {
            case 400:
              errorMessage = 'Bad request';
              break;
            case 403:
              errorMessage = 'You are not authorized to perform this action';
              break;
            case 404:
              errorMessage = 'Requested resource not found';
              break;
            case 500:
              errorMessage = 'Internal server error';
              break;
          }
        }
        return throwError(() => errorMessage)

      })
    )
  }


}

