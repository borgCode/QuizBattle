import { Component } from '@angular/core';
import {WhisperWindowComponent} from '../whisper-window/whisper-window.component';

@Component({
  selector: 'app-whispers-container',
  imports: [
    WhisperWindowComponent
  ],
  templateUrl: './whispers-container.component.html',
  styleUrl: './whispers-container.component.css'
})
export class WhispersContainerComponent {

}
