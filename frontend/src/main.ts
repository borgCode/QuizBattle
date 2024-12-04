import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/components/app.component';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import {HTTP_INTERCEPTORS} from '@angular/common/http';
import {HttpTokenInterceptor} from './app/services/interceptor/http-token.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    appConfig.providers,
    provideRouter(routes),
  ],
}).catch((err) => console.error(err));
