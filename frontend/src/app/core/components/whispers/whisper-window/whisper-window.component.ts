import {AfterViewChecked, Component, ElementRef, Input, ViewChild} from '@angular/core';
import {FullConversationDto} from '../../../../api/generated/models/full-conversation-dto';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {FormsModule} from '@angular/forms';
import {Message, WhisperWindowService} from '../../../services/whisper-window/whisper-window.service';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {BehaviorSubject, Observable, tap} from 'rxjs';
import {MessageDto} from '../../../../api/generated/models/message-dto';

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
export class WhisperWindowComponent implements AfterViewChecked{
  @ViewChild("messageArea") private messageArea: ElementRef;
  private isScrolledToBottom = true;

  @Input() conversation: FullConversationDto

  storedPlayer: PlayerDto
  messageToSend: string

  private newMessages = new BehaviorSubject<Message[]>([])
  newMessages$ = this.newMessages.asObservable();

  message$: Observable<Message>

  constructor(
    private whisperWindowService: WhisperWindowService,
    private loginStateService: LoginStateService
  ) {
    this.storedPlayer = this.loginStateService.loggedInUser;
    this.message$ = this.whisperWindowService.message$;

    this.message$.pipe(
      tap(newMessage => {
        const currentMessages = this.newMessages.getValue();
        this.newMessages.next([...currentMessages, newMessage])
      })
    ).subscribe();

    setTimeout(() => {
      this.messageArea.nativeElement.addEventListener("scroll", () => {
        const element = this.messageArea.nativeElement;
        this.isScrolledToBottom = element.scrollHeight - element.scrollTop - element.clientHeight < 1;
      })
    })
  }

  ngAfterViewChecked() {
    if (this.isScrolledToBottom) {
      const element = this.messageArea.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
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

  closeConversation() {
    this.whisperWindowService.removeFromConversations(this.conversation);
  }
}
