import {Component, OnInit} from '@angular/core';
import {LoginStateService} from '../../../../../core/services/login-state-service/login-state.service';
import {PlayerDto} from '../../../../../api/generated/models/player-dto';
import {CategoryPieChartComponent} from '../category-pie-chart/category-pie-chart.component';
import {RelationshipPanelComponent} from '../relationship-panel/relationship-panel.component';
import {Router} from '@angular/router';
import {AsyncPipe, NgIf} from '@angular/common';
import {AchievementsPanelComponent} from '../achievements-panel/achievements-panel.component';
import {ProfileService} from '../service/profile.service';

@Component({
  selector: 'app-user-profile',
  imports: [
    CategoryPieChartComponent,
    RelationshipPanelComponent,
    NgIf,
    AsyncPipe,
    AchievementsPanelComponent
  ],
  templateUrl: './player-profile.component.html',
  styleUrl: './player-profile.component.css'
})
export class PlayerProfileComponent implements OnInit {
  player!: PlayerDto;
  image: string = '';

  constructor(
    protected profileService: ProfileService,
    protected loginStateService: LoginStateService,
    private router: Router,
  ) {
  }

  ngOnInit() {
    this.player = this.loginStateService.loggedInUser;

    if (!this.player) {
      console.warn('No logged-in user found!');
    }
    this.profileService.initProfileData(this.player.id);

  }
  openEditProfile() {
    this.router.navigate(['edit-profile']);
  }

  handleAction($event: { playerId: number; action: string }) {
    this.profileService.handlePlayerInteraction($event.playerId, $event.action);
  }
}
