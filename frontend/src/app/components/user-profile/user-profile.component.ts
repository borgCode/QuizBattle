import {Component, OnInit} from '@angular/core';
import {UserDto} from '../../services/models/user-dto';
import {LoginStateService} from '../../services/login-state-service/login-state.service';

@Component({
  selector: 'app-user-profile',
  imports: [],
  templateUrl: './user-profile.component.html',
  styleUrl: './user-profile.component.css'
})
export class UserProfileComponent implements OnInit {
  user!: UserDto;

  constructor(
    private loginStateService: LoginStateService
  ) {
  }

  ngOnInit() {

    this.user = this.loginStateService.loggedInUser;

    //TODO show user error
    if (!this.user) {
      console.warn('No logged-in user found!');
    }
  }
}
