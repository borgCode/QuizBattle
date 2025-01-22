import {AfterViewInit, Component, Input} from '@angular/core';
import {AchievementDto} from '../../../../../api/generated/models/achievement-dto';
import {DatePipe, NgForOf, NgIf} from '@angular/common';

declare var bootstrap: any;

@Component({
  selector: 'app-achievement-card',
  imports: [
    NgForOf,
    NgIf,
    DatePipe
  ],
  templateUrl: './achievement-card.component.html',
  styleUrl: './achievement-card.component.css'
})
export class AchievementCardComponent implements AfterViewInit {
  @Input() achievement: AchievementDto;

  ngAfterViewInit() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl)
    });
  }
}
