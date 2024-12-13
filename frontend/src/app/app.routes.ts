import { Routes } from '@angular/router';
import {LoginComponent} from './features/auth/login/login.component';
import {PlayerProfileComponent} from './features/profile/player-profile/player-profile.component';
import {ModeSelectionComponent} from './features/game/mode-selection/mode-selection.component';
import {RegisterComponent} from './features/auth/register/register.component';
import {authGuard} from './core/guard/auth.guard';

import {
  MultiplayerPlayRoundComponent
} from './features/game/multiplayer/multiplayer-play-round/multiplayer-play-round.component';
import {MultiplayerComponent} from './features/game/multiplayer/multiplayer.component';
import {
  MultiplayerScoreWindowComponent
} from './features/game/multiplayer/multiplayer-score-window/multiplayer-score-window.component';
import {EditProfileComponent} from './features/profile/edit-profile/edit-profile.component';


export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'profile', component: PlayerProfileComponent, canActivate:[authGuard]},
  {path: 'edit-profile', component: EditProfileComponent, canActivate:[authGuard]},
  {path: 'multiplayer', component: MultiplayerComponent, canActivate:[authGuard]},
  {path: 'multiplayer/:sessionId', component: MultiplayerScoreWindowComponent, canActivate:[authGuard]},
  {path: 'multiplayer/:sessionId/play', component: MultiplayerPlayRoundComponent, canActivate:[authGuard]},
  {path: '', component: ModeSelectionComponent}
];
