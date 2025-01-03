import {Injectable} from '@angular/core';
import {LoginStateService} from '../login-state-service/login-state.service';
import {Router} from '@angular/router';
import {AuthenticationService} from '../../../api/generated/services/authentication.service';
import {jwtDecode} from 'jwt-decode';
import {firstValueFrom} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TokenService {

  constructor(
    private loginStateService: LoginStateService,
    private router: Router,
    private authService: AuthenticationService
  ) {
    this.loginStateService.updateLoginState(this.isTokenValid());
  }

  set accessToken(token: string) {
    localStorage.setItem('accessToken', token);
    this.loginStateService.updateLoginState(!!token);
  }

  get accessToken() {
    return localStorage.getItem('accessToken') as string;
  }

  set refreshToken(token: string) {
    localStorage.setItem('refreshToken', token);
  }

  get refreshToken() {
    return localStorage.getItem('refreshToken');
  }

  clearTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.loginStateService.updateLoginState(false);
  }

  isTokenValid() {
    const token = this.accessToken;
    if (!token) return false;

    try {
      const decodedToken: any = jwtDecode(token);
      const expirationDate = new Date(decodedToken.exp * 1000);
      return expirationDate > new Date();
    } catch {
      return false;
    }
  }

  async refreshAccessToken(): Promise<Boolean> {
    const refreshToken = this.refreshToken;
    if (!refreshToken) {
      this.handleExpiredToken();
      return false;
    }

    try {
      const response = await firstValueFrom(this.authService.refresh({body: {refreshToken: refreshToken}}))
      if (response) {
        this.accessToken = response.accessToken;
        return true;
      }
      return false;
    } catch {
      this.handleExpiredToken();
      return false;
    }
  }

  private handleExpiredToken() {
    this.clearTokens();
    this.loginStateService.clearLoggedInUser();
    this.router.navigate(["/login"]);
  }
}
