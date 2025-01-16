import {Injectable} from '@angular/core';
import {FriendshipService} from '../../../../../../api/generated/services/friendship.service';
import {BlockService} from '../../../../../../api/generated/services/block.service';
import {AlertMessageService} from '../../../../../../core/services/alert-message/alert-message.service';
import {BehaviorSubject, tap} from 'rxjs';
import {ConfirmationDialogComponent} from '../../../../../../shared/components/dialog/confirmation-dialog/confirmation-dialog.component';
import {MatDialog} from '@angular/material/dialog';

@Injectable({
  providedIn: 'root'
})
export class PlayerInteractionService {
  private relationshipStatusSubject = new BehaviorSubject<string>(null);


  readonly relationshipState$ = this.relationshipStatusSubject.asObservable();

  playerId: number;
  opponentId: number;

  constructor(
    private friendshipService: FriendshipService,
    private blockService: BlockService,
    private alertMessageService: AlertMessageService,
    private confirmationDialog: MatDialog,
  ) {
  }

  loadRelationshipStatus(playerId: number, opponentId: number) {
    this.playerId = playerId;
    this.opponentId = opponentId;
    this.friendshipService.getRelationshipStatus({
      relationshipStatusRequest: {
        playerId: playerId,
        targetPlayerId: opponentId
      }
    }).pipe(
      tap(status => {
          this.relationshipStatusSubject.next(status.blocked ? 'BLOCKED' : status.friendshipStatus)
        }
      )).subscribe()
  }

  sendFriendRequest() {
    this.friendshipService.addFriend({body: {senderId: this.playerId, receiverId: this.opponentId}}).subscribe({
      next: () => {
        this.alertMessageService.show('Friend request sent', 'success')
        this.loadRelationshipStatus(this.playerId, this.opponentId);
      }
    })
  }

  cancelFriendRequest() {
    this.friendshipService.removeAsFriend({body: {senderId: this.playerId, receiverId: this.opponentId}}).subscribe({
      next: () => {
        this.alertMessageService.show('Friend request canceled', 'success')
        this.loadRelationshipStatus(this.playerId, this.opponentId);
      }
    })
  }

  removeFriend() {
    const dialogRef = this.confirmationDialog.open(ConfirmationDialogComponent, {
      data: {title: "Remove friend", description: "Are you sure you want to remove this player from your friend list?"},
      width: "300px",
      disableClose: true
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.friendshipService.removeAsFriend({body: {senderId: this.playerId, receiverId: this.opponentId}}).subscribe({
          next: () => {
            this.alertMessageService.show('Removed friend', 'success')
            this.loadRelationshipStatus(this.playerId, this.opponentId);
          }
        })
      }
    })
  }

  acceptFriend() {
    this.friendshipService.acceptFriend({body: {senderId: this.playerId, receiverId: this.opponentId}}).subscribe({
      next: () => {
        this.alertMessageService.show('Accepted friend request', 'success')
        this.loadRelationshipStatus(this.playerId, this.opponentId);
      },
    });
  }

  blockPlayer() {
    const dialogRef = this.confirmationDialog.open(ConfirmationDialogComponent, {
      data: {title: "Block player", description: "Are you sure you want to block this player?"},
      width: "300px",
      disableClose: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
         this.blockService.blockPlayer({blockerId: this.playerId, blockedId: this.opponentId}).subscribe({
           next: () => {
             this.alertMessageService.show('Blocked player', 'success')
             this.loadRelationshipStatus(this.playerId, this.opponentId);
           }
         })
      }
    })
  }

  unblockPlayer() {
    const dialogRef = this.confirmationDialog.open(ConfirmationDialogComponent, {
      data: {title: "Unblock player", description: "Are you sure you want to unblock this player?"},
      width: "300px",
      disableClose: true
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.blockService.unblockPlayer({blockerId: this.playerId, blockedId: this.opponentId}).subscribe({
            next: () => {
              this.alertMessageService.show('Unblocked player', 'success')
              this.loadRelationshipStatus(this.playerId, this.opponentId);
            }
          }
        )
      }
    })
  }
}
