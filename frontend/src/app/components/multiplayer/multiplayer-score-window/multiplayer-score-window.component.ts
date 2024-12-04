import {Component, OnInit} from '@angular/core';
import {MultiplayerService} from '../../../services/services/multiplayer.service';
import {ActivatedRoute} from '@angular/router';
import {MultiplayerSessionDto} from '../../../services/models/multiplayer-session-dto';
import {NgForOf} from '@angular/common';

interface Box {
  color: string;
}

interface BoxRow {
  left: Box[];
  right: Box[];
}

@Component({
  selector: 'app-multiplayer-score-window',
  imports: [
    NgForOf
  ],
  templateUrl: './multiplayer-score-window.component.html',
  styleUrl: './multiplayer-score-window.component.css'
})
export class MultiplayerScoreWindowComponent implements OnInit{
  gameSession!:MultiplayerSessionDto;
  boxes: BoxRow[] = [];

  constructor(
    private multiplayerService: MultiplayerService,
    private activatedRoute: ActivatedRoute,


  ) {
  }

  ngOnInit() {
    this.initBoxes();
    this.activatedRoute.params.subscribe(value => {
      const sessionId = value['id'];
    });
  }

  private initBoxes() {
    for (let i = 0; i < 6; i++) {
      this.boxes.push({
        left: Array(3).fill({ color: '#ccc' }),
        right: Array(3).fill({ color: '#ccc' })
      });
    }
    }

  updateBoxColor(side: 'left' | 'right', rowIndex: number, colIndex: number, color: string) {
    this.boxes[rowIndex][side][colIndex].color = color;
  }



}
