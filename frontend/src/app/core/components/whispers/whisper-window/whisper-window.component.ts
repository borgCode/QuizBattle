import {Component, Input} from '@angular/core';
import {FullConversationDto} from '../../../../api/generated/models/full-conversation-dto';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {FormsModule} from '@angular/forms';
import {Message, WhisperWindowService} from '../../../services/whisper-window/whisper-window.service';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-whisper-window',
  imports: [
    NgForOf,
    FormsModule,
    NgIf,
    AsyncPipe
  ],
  templateUrl: './whisper-window.component.html',
  styleUrl: './whisper-window.component.css'
})
export class WhisperWindowComponent {
  @Input() conversation: FullConversationDto

  storedPlayer: PlayerDto
  messageToSend: string

  message$: Observable<Message>

  constructor(
    private whisperWindowService: WhisperWindowService,
    private loginStateService: LoginStateService
  ) {
    this.storedPlayer = this.loginStateService.loggedInUser;
    this.message$ = this.whisperWindowService.message$;
  }

  sendMessage(conversationId: number, receiverId: number, userName: string) {
    console.log("Sending message")
    this.whisperWindowService.sendMessage({
      messageRequest: {
        receiverUsername: userName,
        senderId: this.storedPlayer.id,
        conversationId: conversationId,
        receiverId: receiverId,
        message: this.messageToSend
      }
    })
  }

}
