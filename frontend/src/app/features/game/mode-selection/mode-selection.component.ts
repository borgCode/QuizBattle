import { Component } from '@angular/core';
import {Router} from '@angular/router';

@Component({
  selector: 'app-mode-selection',
  imports: [],
  templateUrl: './mode-selection.component.html',
  styleUrl: './mode-selection.component.css'
})
export class ModeSelectionComponent {
  constructor(
    private router: Router,
  ) {
  }

  openMultiplayer() {
    this.router.navigate(['multiplayer']);
  }

  openStoryMode() {
    this.router.navigate(['singleplayer'])
  }
}
