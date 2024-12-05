import { Routes } from '@angular/router';
import {LoginComponent} from './components/login/login.component';
import {PlayerProfileComponent} from './components/player-profile/player-profile.component';
import {ModeSelectionComponent} from './components/mode-selection/mode-selection.component';
import {RegisterComponent} from './components/register/register.component';
import {authGuard} from './services/guard/auth.guard';
import {MultiplayerComponent} from './components/multiplayer/multiplayer.component';
import {
  MultiplayerScoreWindowComponent
} from './components/multiplayer/multiplayer-score-window/multiplayer-score-window.component';
import {
  MultiplayerCategoryComponent
} from './components/multiplayer/multiplayer-category/multiplayer-category.component';

export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'profile', component: PlayerProfileComponent, canActivate:[authGuard]},
  {path: 'multiplayer', component: MultiplayerComponent, canActivate:[authGuard]},
  {path: 'multiplayer/:sessionId', component: MultiplayerScoreWindowComponent, canActivate:[authGuard]},
  {path: 'multiplayer/:sessionId/category', component: MultiplayerCategoryComponent, canActivate:[authGuard]},
  {path: '', component: ModeSelectionComponent}
];
