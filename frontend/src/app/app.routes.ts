import { Routes } from '@angular/router';
import {LoginComponent} from './components/login/login.component';
import {UsersComponent} from './components/users/users.component';
import {UserProfileComponent} from './components/user-profile/user-profile.component';
import {PlayComponent} from './components/play/play.component';
import {ModeSelectionComponent} from './components/mode-selection/mode-selection.component';

export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'user', component: UsersComponent},
  {path: 'user/:username', component: UserProfileComponent},
  {path: 'play', component: PlayComponent},
  {path: '', component: ModeSelectionComponent}
];
