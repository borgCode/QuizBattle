import {Component, Input} from '@angular/core';
import {FullConversationDto} from '../../../../api/generated/models/full-conversation-dto';
import {NgForOf} from '@angular/common';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {FormsModule} from '@angular/forms';
import {WhisperWindowService} from '../../../services/whisper-window/whisper-window.service';

@Component({
  selector: 'app-whisper-window',
  imports: [
    NgForOf,
    FormsModule
  ],
  templateUrl: './whisper-window.component.html',
  styleUrl: './whisper-window.component.css'
})
export class WhisperWindowComponent {
  @Input() conversation: FullConversationDto

  storedPlayerId: number
  message: string

  constructor(
    private whisperWindowService: WhisperWindowService,
    private loginStateService: LoginStateService
  ) {
    this.storedPlayerId = this.loginStateService.loggedInUser.id;
  }

  sendMessage(conversationId: number, receiverId: number, userName: string) {
    console.log("Sending message")
    this.whisperWindowService.sendMessage({
      messageRequest: {
        userName: userName,
        senderId: this.storedPlayerId,
        conversationId: conversationId,
        receiverId: receiverId,
        message: this.message
      }
    })
  }

}
