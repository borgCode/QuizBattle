import {Component} from '@angular/core';
import {WhisperWindowService} from '../../../services/whisper-window/whisper-window.service';
import {Observable} from 'rxjs';
import {FullConversationDto} from '../../../../api/generated/models';
import {WhisperWindowComponent} from '../whisper-window/whisper-window.component';
import {AsyncPipe, NgForOf} from '@angular/common';

@Component({
  selector: 'app-whispers-container',
  imports: [
    WhisperWindowComponent,
    NgForOf,
    AsyncPipe
  ],
  templateUrl: './whispers-container.component.html',
  styleUrl: './whispers-container.component.css'
})
export class WhispersContainerComponent {
  conversations$: Observable<FullConversationDto[]>;

  constructor(
    private whisperWindowService: WhisperWindowService
  ) {
    this.conversations$ = whisperWindowService.conversations$;
  }
}
