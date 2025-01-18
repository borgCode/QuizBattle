import { Injectable } from '@angular/core';
import {PlayerService} from '../../../../../api/generated/services/player.service';
import {FriendshipService} from '../../../../../api/generated/services/friendship.service';
import {LoginStateService} from '../../../../../core/services/login-state-service/login-state.service';
import {BehaviorSubject, switchMap, tap} from 'rxjs';
import {Relationship} from '../../interface/relationship';
import {AchievementService} from '../../../../../api/generated/services/achievement.service';
import {UserUnlockedAchievementDto} from '../../../../../api/generated/models/user-unlocked-achievement-dto';
import {AlertMessageService} from '../../../../../core/services/alert-message/alert-message.service';
import {WhisperWindowService} from '../../../../../core/services/whisper-window/whisper-window.service';
import {BlockService} from '../../../../../api/generated/services/block.service';
import {MultiplayerMatchService} from '../../../../../api/generated/services/multiplayer-match.service';
import {MessageService} from '../../../../../api/generated/services/message.service';


@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private relationshipSubject: BehaviorSubject<Relationship> = new BehaviorSubject<Relationship>({
    friendsList: [],
    blockedList: []
  })
  private achievementsSubject: BehaviorSubject<UserUnlockedAchievementDto[]> = new BehaviorSubject<UserUnlockedAchievementDto[]>(null);

  readonly relationships$ = this.relationshipSubject.asObservable();
  readonly achievements$ = this.achievementsSubject.asObservable();

  private playerId: number;

  constructor(
    private playerService: PlayerService,
    private friendshipService: FriendshipService,
    private loginStateService: LoginStateService,
    private achievementService: AchievementService,
    private alertMessageService: AlertMessageService,
    private whisperWindowService: WhisperWindowService,
    private blockService: BlockService,
    private multiplayerMatchService: MultiplayerMatchService,
    private messageService: MessageService,
  ) { }

  initProfileData(playerId: number) {
    this.playerService.getPlayerById({playerId: playerId}).pipe(
      tap(player => {
        this.loginStateService.loggedInUser = player
        this.playerId = player.id
      }),
      switchMap(() => this.getRelationships(playerId).pipe(
        switchMap(() => this.achievementService.getUnlockedAchievements({playerId: playerId}).pipe(
          tap(achievements => this.achievementsSubject.next(achievements))
        ))
      ))
    ).subscribe();
  }

  private getRelationships(playerId: number) {
    return this.friendshipService.getRelationships({playerId: playerId}).pipe(
      tap(relationships => this.relationshipSubject.next({
        friendsList: relationships.friends,
        blockedList: relationships.blocked
      }))
    );
  }

  handlePlayerInteraction(receiverId: number, action: string) {
    switch (action) {
      case "REMOVE":
        this.friendshipService.removeAsFriend({
          body: {
            senderId: this.playerId,
            receiverId: receiverId
          }
        }).pipe(
          tap(() => this.alertMessageService.show("Removed player from friends", "success")),
          switchMap(() => this.getRelationships(this.playerId))
        ).subscribe({
          error: err => {
            console.log(err)
          }
        });
        break;
      case "BLOCK":
        this.blockService.blockPlayer({blockerId: this.playerId, blockedId: receiverId}).pipe(
          tap(() => this.alertMessageService.show("Blocked player", "success")),
          switchMap(() => this.getRelationships(this.playerId))
        ).subscribe({
          error: err => {
            console.log(err)
          }
        });
        break;
      case "UNBLOCK":
        this.blockService.unblockPlayer({blockerId: this.playerId, blockedId: receiverId}).pipe(
          tap(() => this.alertMessageService.show("Unblocked player", "success")),
          switchMap(() => this.getRelationships(this.playerId))
        ).subscribe({
          error: err => {
            console.log(err)
          }
        });
        break;
      case "MATCH_REQUEST":
        this.multiplayerMatchService.requestMatch({
          body: {
            senderId: this.playerId,
            receiverId: receiverId
          }
        }).subscribe({
          next: () => this.alertMessageService.show('Sent match request!', 'success'),
        });
        break;
      case "MESSAGE":
        this.messageService.getFullConversation({
          body: {
            senderId: this.playerId,
            receiverId: receiverId
          }
        }).subscribe({
          next: conversation => {
            this.whisperWindowService.addToConversations(conversation)
          },
        });
        break;
      default:
        break;
    }
  }
}
