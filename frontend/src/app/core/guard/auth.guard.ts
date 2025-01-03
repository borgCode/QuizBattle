import {CanActivateFn, Router} from '@angular/router';
import {TokenService} from '../services/token/token.service';
import {inject} from '@angular/core';
import {LoginStateService} from '../services/login-state-service/login-state.service';

export const authGuard: CanActivateFn = async () => {
  const tokenService: TokenService = inject(TokenService);
  const loginStateService: LoginStateService = inject(LoginStateService);
  const router: Router = inject(Router);
  if (!tokenService.isTokenValid() && !tokenService.refreshToken) {
    console.log("Not valid tokens and no refresh token - clearing")
    tokenService.clearTokens();
    loginStateService.clearLoggedInUser();
    router.navigate(['login']);
    return false;
  }

  if (!tokenService.isTokenValid() && tokenService.refreshToken) {
    console.log("Not valid tokens and has valid refresh token ")
    try {
      const success = await tokenService.refreshAccessToken();
      if (!success) {
        console.log("Not valid tokens and refresh failed - clearing")
        tokenService.clearTokens();
        loginStateService.clearLoggedInUser();
        router.navigate(['login']);
        return false;
      }
      return true;
    } catch {
      console.log("failed - clearing")
      tokenService.clearTokens();
      loginStateService.clearLoggedInUser();
      router.navigate(['login']);
      return false;
    }
  }

  return tokenService.isTokenValid();

};
