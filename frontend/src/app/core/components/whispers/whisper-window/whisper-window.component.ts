import {Component, Input} from '@angular/core';
import {FullConversationDto} from '../../../../api/generated/models/full-conversation-dto';
import {NgForOf} from '@angular/common';
import {MessageService} from '../../../../api/generated/services/message.service';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {FormsModule} from '@angular/forms';

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
    private messageService: MessageService,
    private loginStateService: LoginStateService
  ) {
    this.storedPlayerId = this.loginStateService.loggedInUser.id;
  }

  sendMessage(conversationId: number, receiverId: number) {
    console.log("Sending message")
    this.messageService.sendMessage({
      messageRequest: {
        senderId: this.storedPlayerId,
        conversationId: conversationId,
        receiverId: receiverId,
        message: this.message
      }
    }).subscribe({
      error: err => {
        console.log(err)
      }
    })
  }
}
