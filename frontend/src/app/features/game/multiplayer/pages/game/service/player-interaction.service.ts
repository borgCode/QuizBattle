import { Injectable } from '@angular/core';
import {FriendshipService} from '../../../../../../api/generated/services/friendship.service';
import {BlockService} from '../../../../../../api/generated/services/block.service';
import {AlertMessageService} from '../../../../../../core/services/alert-message/alert-message.service';

@Injectable({
  providedIn: 'root'
})
export class PlayerInteractionService {

  constructor(
    private friendshipService: FriendshipService,
    private blockService: BlockService,
    private alertMessageService: AlertMessageService
  ) { }

  sendFriendRequest(playerId: number, opponentId: number) {
    return this.friendshipService.addFriend({body: {senderId: playerId, receiverId: opponentId}})
  }

  cancelFriendRequest(playerId: number, opponentId: number) {
    return this.friendshipService.removeAsFriend({body: {senderId: playerId, receiverId: opponentId}})
  }

  removeFriend(playerId: number, opponentId: number) {
    return this.friendshipService.removeAsFriend({body: {senderId: playerId, receiverId: opponentId}})
  }
  acceptFriend(playerId: number, opponentId: number) {
    return this.friendshipService.acceptFriend({body: {senderId: playerId, receiverId: opponentId}})
  }

  blockPlayer(playerId: number, opponentId: number) {
    return this.blockService.blockPlayer({blockerId: playerId, blockedId: opponentId});
  }

  unblockPlayer(playerId: number, opponentId: number) {
    return this.blockService.unblockPlayer({blockerId: playerId, blockedId: opponentId});
  }



  // private getFriendshipStatus() {
  //   this.friendshipService.getRelationshipStatus({
  //     relationshipStatusRequest: {
  //       playerId: this.storedPlayerId,
  //       targetPlayerId: this.opponentId
  //     }
  //   }).subscribe({
  //     next: value => {
  //       if (value) {
  //         if (value.blocked) {
  //           this.friendshipStatus = "BLOCKED"
  //         } else {
  //           this.friendshipStatus = value.friendshipStatus
  //         }
  //       }
  //
  //     },
  //     error: err => {
  //       console.log(err)
  //     }
  //   })
  // }
}
