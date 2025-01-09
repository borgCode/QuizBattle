import {Component, Input} from '@angular/core';
import {FullConversationDto} from '../../../../api/generated/models/full-conversation-dto';

@Component({
  selector: 'app-whisper-window',
  imports: [],
  templateUrl: './whisper-window.component.html',
  styleUrl: './whisper-window.component.css'
})
export class WhisperWindowComponent {
  @Input() conversation: FullConversationDto

}
