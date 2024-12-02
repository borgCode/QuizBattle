import { Component } from '@angular/core';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {LoginService} from '../../service/login.service';
import {HttpClient} from '@angular/common/http';
import {finalize} from 'rxjs';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLinkActive,
    RouterLink
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {

  constructor(private loginService: LoginService, private http: HttpClient, private router: Router) {
  }

  authenticated() {
    return this.loginService.authenticated;
  }

  logout() {
    this.http.post('logout', {}).pipe(finalize(() => {
      this.loginService.authenticated = false;
      this.router.navigateByUrl('/login');
    })).subscribe();
  }

}
