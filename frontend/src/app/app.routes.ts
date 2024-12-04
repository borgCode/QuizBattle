import { Routes } from '@angular/router';
import {LoginComponent} from './components/login/login.component';
import {UserProfileComponent} from './components/user-profile/user-profile.component';
import {PlayComponent} from './components/play/play.component';
import {ModeSelectionComponent} from './components/mode-selection/mode-selection.component';
import {RegisterComponent} from './components/register/register.component';

export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'profile', component: UserProfileComponent},
  {path: 'play', component: PlayComponent},
  {path: '', component: ModeSelectionComponent}
];
