import {Component, Input} from '@angular/core';
import {NgStyle} from '@angular/common';

@Component({
  selector: 'app-player-card',
  imports: [
    NgStyle
  ],
  templateUrl: './player-card-component.html',
  styleUrl: './player-card-component.css'
})
export class PlayerCardComponent {
  @Input() playerDisplayName: string;
  @Input() playerAvatar: string;
  @Input() maxHeight?: string;

}
