import {Component, OnInit} from '@angular/core';
import {MultiplayerService} from '../../../services/services/multiplayer.service';
import {ActivatedRoute} from '@angular/router';
import {MultiplayerSessionDto} from '../../../services/models/multiplayer-session-dto';

@Component({
  selector: 'app-multiplayer-score-window',
  imports: [],
  templateUrl: './multiplayer-score-window.component.html',
  styleUrl: './multiplayer-score-window.component.css'
})
export class MultiplayerScoreWindowComponent implements OnInit{
  gameSession!:MultiplayerSessionDto;

  constructor(
    private multiplayerService: MultiplayerService,
    private activatedRoute: ActivatedRoute,


  ) {
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe(value => {
      const sessionId = value['id'];
    });
  }


}
