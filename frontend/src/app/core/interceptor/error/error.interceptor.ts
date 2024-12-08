import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {catchError, Observable} from 'rxjs';


@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(req).pipe(
            catchError((err: HttpErrorResponse) => {
                let errorMessage = 'An unexpected error occurred';
                if (err.error?.businessErrorCode) {
                    switch (err.error.businessErrorCode) {
                        case 300:
                            errorMessage = 'Current password is incorrect';
                            break;
                        case 301:
                            errorMessage = 'New password does not match';
                            break;
                        case 302:
                            errorMessage = 'Your account is locked';
                            break;
                        case 303:
                            errorMessage = 'Your account is disabled';
                            break;
                        case 304:
                            errorMessage = 'Incorrect username or password';
                            break;
                        case 305:
                            errorMessage = 'Cannot send friend request to a blocked user';
                            break;
                        case 306:
                            errorMessage = 'Friend request is already pending';
                            break;
                        case 307:
                            errorMessage = 'You are already friends with this user';
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

                throw new Error(errorMessage);

            })
        )
    }


}

