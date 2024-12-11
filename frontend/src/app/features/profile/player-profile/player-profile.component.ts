import {Component, OnInit} from '@angular/core';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {CategoryPieChartComponent} from './category-pie-chart/category-pie-chart.component';

@Component({
  selector: 'app-user-profile',
  imports: [
    CategoryPieChartComponent
  ],
  templateUrl: './player-profile.component.html',
  styleUrl: './player-profile.component.css'
})
export class PlayerProfileComponent implements OnInit {
  player!: PlayerDto;

  constructor(
    private loginStateService: LoginStateService
  ) {
  }

  ngOnInit() {

    this.player = this.loginStateService.loggedInUser;

    //TODO show user error
    if (!this.player) {
      console.warn('No logged-in user found!');
    }
  }
}
