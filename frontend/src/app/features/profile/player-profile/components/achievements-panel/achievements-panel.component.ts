import {Component, Input} from '@angular/core';
import {NgForOf} from '@angular/common';
import {AchievementDto} from '../../../../../api/generated/models/achievement-dto';

@Component({
  selector: 'app-achievements-panel',
  imports: [
    NgForOf
  ],
  templateUrl: './achievements-panel.component.html',
  styleUrl: './achievements-panel.component.css'
})
export class AchievementsPanelComponent {
  @Input() achievements!: AchievementDto[];

}
