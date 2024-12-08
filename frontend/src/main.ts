import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/layout/app/app.component';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import {HTTP_INTERCEPTORS} from '@angular/common/http';
import {HttpTokenInterceptor} from './app/core/interceptor/token/http-token.interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {ErrorInterceptor} from './app/core/interceptor/error/error.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    appConfig.providers,
    provideRouter(routes), provideAnimationsAsync(),
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
  ],

}).catch((err) => console.error(err));
