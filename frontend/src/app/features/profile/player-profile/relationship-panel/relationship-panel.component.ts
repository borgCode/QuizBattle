import {Component, EventEmitter, Input, Output} from '@angular/core';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-relationship-panel',
  imports: [
    NgForOf,
    NgIf
  ],
  templateUrl: './relationship-panel.component.html',
  styleUrl: './relationship-panel.component.css'
})
export class RelationshipPanelComponent {
  @Input() relationships: PlayerDto[];
  @Input() type: string;
  @Output() relationshipActionSelected = new EventEmitter<{playerId: number, action: string}>


  emitRemoveFriend(id: number) {
    this.relationshipActionSelected.emit({playerId: id, action: "REMOVE"})
  }
  emitBlockPlayer(id: number) {
    this.relationshipActionSelected.emit({playerId: id, action: "BLOCK"})
  }
  emitSendInvite(id: number) {
    //TODO IMPLEMENT
  }

  emitUnblockPlayer(id: number) {
    this.relationshipActionSelected.emit({playerId: id, action: "UNBLOCK"})
  }

  emitSendGameInvite(id: number) {
    this.relationshipActionSelected.emit({playerId: id, action: "MATCH_REQUEST"})
  }
}
