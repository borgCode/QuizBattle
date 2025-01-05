import {Component, EventEmitter, Input, Output} from '@angular/core';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {NgForOf} from '@angular/common';

@Component({
  selector: 'app-relationship-panel',
  imports: [
    NgForOf
  ],
  templateUrl: './relationship-panel.component.html',
  styleUrl: './relationship-panel.component.css'
})
export class RelationshipPanelComponent {
  @Input() relationships: PlayerDto[];
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

}
