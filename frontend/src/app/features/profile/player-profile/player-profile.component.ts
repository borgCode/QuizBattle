import {Component, OnInit} from '@angular/core';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {CategoryPieChartComponent} from './category-pie-chart/category-pie-chart.component';
import {FriendsPanelComponent} from './friends-panel/friends-panel.component';
import {FriendshipService} from '../../../api/generated/services/friendship.service';

@Component({
  selector: 'app-user-profile',
  imports: [
    CategoryPieChartComponent,
    FriendsPanelComponent
  ],
  templateUrl: './player-profile.component.html',
  styleUrl: './player-profile.component.css'
})
export class PlayerProfileComponent implements OnInit {
  player!: PlayerDto;
  friendsList!: PlayerDto[]

  constructor(
    private loginStateService: LoginStateService,
    private friendshipService: FriendshipService
  ) {
  }

  ngOnInit() {
    this.player = this.loginStateService.loggedInUser;
    //TODO show user error
    if (!this.player) {
      console.warn('No logged-in user found!');
    }

    this.getFriends();
  }

  private getFriends() {
    this.friendshipService.getFriends({playerId: this.player.id}).subscribe({
      next: data => {
        this.friendsList = data;
        console.log(this.friendsList)
      }
    })
  }
}
