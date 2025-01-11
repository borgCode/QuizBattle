import {Component, Input} from '@angular/core';
import {UserUnlockedAchievementDto} from '../../../../api/generated/models/user-unlocked-achievement-dto';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-achievements-panel',
  imports: [
    NgForOf
  ],
  templateUrl: './achievements-panel.component.html',
  styleUrl: './achievements-panel.component.css'
})
export class AchievementsPanelComponent {
  @Input() achievements!: UserUnlockedAchievementDto[];

}
