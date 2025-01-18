import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AsyncPipe, NgIf, NgSwitch, NgSwitchCase} from '@angular/common';
import {GameStateResponse} from '../../../../../../../api/generated/models/game-state-response';
import {PlayerCardComponent} from '../../../../../../../shared/components/player-card/player-card-component';
import {GameService} from '../../service/game.service';
import {PlayerInteractionService} from '../../service/player-interaction.service';
import {ScoreBoxesComponent} from '../score-boxes/score-boxes.component';
import {Observable} from 'rxjs';


@Component({
  selector: 'app-game-view',
  imports: [
    NgIf,
    PlayerCardComponent,
    NgSwitch,
    NgSwitchCase,
    AsyncPipe,
    ScoreBoxesComponent
  ],
  templateUrl: './game-view.component.html',
  styleUrl: './game-view.component.css'
})
export class GameViewComponent implements OnInit {

  sessionId: number;
  hasAcknowledgedGameOver: boolean;
  resetBoxesTrigger = false;

  gameOver$: Observable<boolean>;
  gameState$: Observable<GameStateResponse>;
  constructor(
    protected gameService: GameService,
    protected playerInteractionService: PlayerInteractionService,
    private activatedRoute: ActivatedRoute,
    private router: Router,

  ) {
    this.gameOver$ = this.gameService.gameOver$;
    this.gameState$ = this.gameService.gameState$;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(value => {
      this.gameService.sessionId = value['sessionId'];
      this.resetComponents();
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
        this.playerInteractionService.loadRelationshipStatus(this.gameService.playerId, gameState.opponentDTO.playerId)
      }
    })
  }

  handlePlayButtonClick(gameState: GameStateResponse) {
    if (gameState.questionIds.length > 0) {
      this.openPlayQuestions(gameState);
    } else {
      this.openCategorySelection();
    }
  }

  openPlayQuestions(gameState: GameStateResponse) {
    this.router.navigate(['multiplayer', this.gameService.sessionId, 'play'],
      {state: {questionIds: gameState.questionIds}});
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
