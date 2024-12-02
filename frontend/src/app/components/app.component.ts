import { Component} from '@angular/core';
import { CommonModule } from '@angular/common';
import {NavbarComponent} from './navbar/navbar.component';
import {RouterOutlet} from '@angular/router';
import {PlayComponent} from './play/play.component';
import {ModeSelectionComponent} from './mode-selection/mode-selection.component';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterOutlet, PlayComponent, ModeSelectionComponent],
  templateUrl: 'app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'quiz-clash-fe';

}
