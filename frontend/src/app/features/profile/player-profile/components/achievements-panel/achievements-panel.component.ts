import {Component, Input} from '@angular/core';
import {NgForOf} from '@angular/common';
import {AchievementDto} from '../../../../../api/generated/models/achievement-dto';
import {AchievementCardComponent} from '../achievement-card/achievement-card.component';

@Component({
  selector: 'app-achievements-panel',
  imports: [
    NgForOf,
    AchievementCardComponent
  ],
  templateUrl: './achievements-panel.component.html',
  styleUrl: './achievements-panel.component.css'
})
export class AchievementsPanelComponent {
  @Input() achievements!: AchievementDto[];

}
