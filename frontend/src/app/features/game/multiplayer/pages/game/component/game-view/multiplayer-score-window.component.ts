import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AsyncPipe, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {GameStateResponse} from '../../../../../../../api/generated/models/game-state-response';
import {PlayerCardComponent} from '../../../../../../../shared/components/player-card/player-card-component';
import {GameService} from '../../service/game.service';
import {PlayerInteractionService} from '../../service/player-interaction.service';
import {LoginStateService} from '../../../../../../../core/services/login-state-service/login-state.service';
import {ScoreBoxesComponent} from '../score-boxes/score-boxes.component';
import {Observable} from 'rxjs';


@Component({
  selector: 'app-multiplayer-score-window',
  imports: [
    NgIf,
    PlayerCardComponent,
    NgSwitch,
    NgSwitchCase,
    AsyncPipe,
    ScoreBoxesComponent
  ],
  templateUrl: './multiplayer-score-window.component.html',
  styleUrl: './multiplayer-score-window.component.css'
})
export class MultiplayerScoreWindowComponent implements OnInit {

  gameState!: GameStateResponse;
  storedPlayerId: number;
  sessionId: number;
  hasAcknowledgedGameOver: boolean;
  resetBoxesTrigger = false;

  gameOver$: Observable<boolean>;
  constructor(
    protected gameService: GameService,
    protected playerInteractionService: PlayerInteractionService,
    protected loginStateService: LoginStateService,
    private activatedRoute: ActivatedRoute,
    private router: Router,

  ) {
    this.gameOver$ = this.gameService.gameOver$;
  }

  ngOnInit() {

    this.activatedRoute.params.subscribe(value => {
      this.gameService.sessionId = value['sessionId'];
      this.resetComponents();
      this.storedPlayerId = this.loginStateService.loggedInUser.id;
      this.getGameState();
    })
  }

  private resetComponents() {
    this.resetBoxesTrigger = false;
    setTimeout(() => {
      this.resetBoxesTrigger = true;
    });
  }

  private getGameState() {
    this.gameService.getGameState().subscribe({
      next: gameState => {
        this.gameState = gameState;
        this.playerInteractionService.loadRelationshipStatus(this.storedPlayerId, this.gameState.opponentDTO.playerId)
      }
    })
  }

  handlePlayButtonClick() {
    console.log(this.gameState.questionIds.length)
    if (this.gameState?.questionIds?.length > 0) {
      this.openPlayQuestions();
    } else {
      this.openCategorySelection();
    }
  }

  openPlayQuestions() {
    this.router.navigate(['multiplayer', this.gameService.sessionId, 'play'],
      {state: {questionIds: this.gameState.questionIds}});
  }

  openCategorySelection() {
    this.router.navigate(['multiplayer', this.gameService.sessionId, 'play']);
  }

  sendFriendRequest() {
    this.playerInteractionService.sendFriendRequest();
  }

  cancelFriendRequest() {
    this.playerInteractionService.cancelFriendRequest();
  }

  blockPlayer() {
    this.playerInteractionService.blockPlayer();

  }

  unBlockPlayer() {
    this.playerInteractionService.unblockPlayer();
  }

  removeFriend() {
    this.playerInteractionService.removeFriend();
  }

  acceptFriend() {
    this.playerInteractionService.acceptFriend();
  }

  sendRematchRequest() {
    this.gameService.requestRematch();
  }

  giveUpClick() {
    this.gameService.handleGiveUp();
  }

  backToMultiplayerPage() {
    this.router.navigate(['multiplayer']);
  }


}
