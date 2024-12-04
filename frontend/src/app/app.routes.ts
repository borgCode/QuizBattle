import { Routes } from '@angular/router';
import {LoginComponent} from './components/login/login.component';
import {UserProfileComponent} from './components/user-profile/user-profile.component';
import {ModeSelectionComponent} from './components/mode-selection/mode-selection.component';
import {RegisterComponent} from './components/register/register.component';
import {authGuard} from './services/guard/auth.guard';
import {MultiplayerComponent} from './components/multiplayer/multiplayer.component';

export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'profile', component: UserProfileComponent, canActivate:[authGuard]},
  {path: 'multiplayer', component: MultiplayerComponent, canActivate:[authGuard]},
  {path: '', component: ModeSelectionComponent}
];
