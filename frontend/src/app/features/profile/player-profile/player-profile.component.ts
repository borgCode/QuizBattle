import {Component, OnInit} from '@angular/core';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {CategoryPieChartComponent} from './category-pie-chart/category-pie-chart.component';
import {RelationshipPanelComponent} from './relationship-panel/relationship-panel.component';
import {Router} from '@angular/router';
import {PlayerService} from '../../../api/generated/services/player.service';
import {AlertMessageService} from '../../../core/services/alert-message/alert-message.service';
import {FriendshipService} from '../../../api/generated/services/friendship.service';
import {MultiplayerMatchService} from '../../../api/generated/services/multiplayer-match.service';

@Component({
  selector: 'app-user-profile',
  imports: [
    CategoryPieChartComponent,
    RelationshipPanelComponent
  ],
  templateUrl: './player-profile.component.html',
  styleUrl: './player-profile.component.css'
})
export class PlayerProfileComponent implements OnInit {
  player!: PlayerDto;
  friendsList!: PlayerDto[]
  blockedList!: PlayerDto[]
  image: string = '';

  constructor(
    private loginStateService: LoginStateService,
    private playerService: PlayerService,
    private friendshipService: FriendshipService,
    private router: Router,
    private alertMessageService: AlertMessageService,
    private multiplayerMatchService: MultiplayerMatchService
  ) {
  }

  ngOnInit() {
    this.player = this.loginStateService.loggedInUser;

    if (!this.player) {
      console.warn('No logged-in user found!');
    }
    this.playerService.getPlayerById({playerId: this.player.id}).subscribe({
      next: value => {
        this.player = value;
        this.loginStateService.loggedInUser = value;

        this.image = 'data:image/jpeg;base64,' + this.player.base64Image;

        this.getFriends();

      }
    })
  }
  private getFriends() {
    this.friendshipService.getRelationships({playerId: this.player.id}).subscribe({
      next: data => {
        this.friendsList = data.friends;
        this.blockedList = data.blocked;
        console.log(this.friendsList)
      }
    })
  }
  openEditProfile() {
    this.router.navigate(['edit-profile']);
  }

  handleAction($event: { playerId: number; action: string }) {
    switch ($event.action) {
      case "REMOVE":
        this.friendshipService.removeAsFriend({
          body: {
            senderId: this.player.id,
            receiverId: $event.playerId
          }
        }).subscribe({
          next: () => {
            this.alertMessageService.show("Removed player from friends", "success")
            this.getFriends();
          },
          error: err => {
            console.log(err)
          }
        })
        break;
      case "BLOCK":
        this.friendshipService.blockPlayer({
          body: {
            senderId: this.player.id,
            receiverId: $event.playerId
          }
        }).subscribe({
          next: () => {
            this.alertMessageService.show("Blocked player", "success")
            this.getFriends()
          },
          error: err => {
            console.log(err)
          }
        })
        break;
      case "UNBLOCK":
        this.friendshipService.unblockPlayer({
          body: {
            senderId: this.player.id,
            receiverId: $event.playerId
          }
        }).subscribe({
          next: () => {
            this.alertMessageService.show("Unblocked player", "success")
            this.getFriends()
          },
          error: err => {
            console.log(err)
          }
        })
        break;
      case "MATCH_REQUEST":
        this.multiplayerMatchService.requestMatch({
          body: {
            senderId: this.player.id,
            receiverId: $event.playerId
          }
        }).subscribe({
          next: () => this.alertMessageService.show('Sent match request!', 'success'),
        })
        break;
      case "MESSAGE":

      default:
        break;
    }
  }

}
