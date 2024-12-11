import {Component, Input} from '@angular/core';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-friends-panel',
  imports: [
    NgForOf
  ],
  templateUrl: './friends-panel.component.html',
  styleUrl: './friends-panel.component.css'
})
export class FriendsPanelComponent {
  @Input() friendsList: PlayerDto[];

}
