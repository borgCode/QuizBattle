import {Routes} from '@angular/router';
import {LoginComponent} from './features/auth/login/login.component';
import {PlayerProfileComponent} from './features/profile/player-profile/components/profile-view/player-profile.component';
import {ModeSelectionComponent} from './features/game/mode-selection/mode-selection.component';
import {RegisterComponent} from './features/auth/register/register.component';
import {authGuard} from './core/guard/auth.guard';

import {
  MultiplayerPlayRoundComponent
} from './features/game/multiplayer/pages/game/component/play-round/multiplayer-play-round.component';
import {MultiplayerComponent} from './features/game/multiplayer/pages/lobby/component/lobby-view/multiplayer.component';
import {
  GameViewComponent
} from './features/game/multiplayer/pages/game/component/game-view/game-view.component';
import {EditProfileComponent} from './features/profile/player-profile/components/edit-profile/edit-profile.component';
import {StorySelectionComponent} from './features/game/singleplayer/pages/story-selection/components/story-selection/story-selection.component';
import {StoryOverviewComponent} from './features/game/singleplayer/pages/story-overview/components/story-overview/story-overview.component';
import {SettingsComponent} from './features/settings/settings.component';
import {
  ChapterViewComponent
} from './features/game/singleplayer/pages/play-chapter/component/chapter-view/chapter-view.component';
import {
  ArchivedNotificationsComponent
} from './features/archive/component/archived-notifications/archived-notifications.component';


export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'profile', component: PlayerProfileComponent, canActivate:[authGuard]},
  {path: 'edit-profile', component: EditProfileComponent, canActivate:[authGuard]},
  {path: 'settings', component: SettingsComponent, canActivate: [authGuard]},
  {path: 'archive', component: ArchivedNotificationsComponent, canActivate: [authGuard]},
  {path: 'multiplayer', component: MultiplayerComponent, canActivate:[authGuard]},
  {path: 'multiplayer/:sessionId', component: GameViewComponent, canActivate:[authGuard]},
  {path: 'multiplayer/:sessionId/play', component: MultiplayerPlayRoundComponent, canActivate:[authGuard]},
  {path: 'singleplayer', component: StorySelectionComponent, canActivate:[authGuard]},
  {path: 'singleplayer/story/:storyId', component: StoryOverviewComponent, canActivate:[authGuard]},
  {path: 'singleplayer/story/chapter/:chapterId', component: ChapterViewComponent, canActivate: [authGuard]},
  {path: '', component: ModeSelectionComponent}
];
