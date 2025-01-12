import {
  AfterViewChecked,
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import {FullConversationDto} from '../../../../api/generated/models/full-conversation-dto';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {FormsModule} from '@angular/forms';
import {Message, WhisperWindowService} from '../../../services/whisper-window/whisper-window.service';
import {PlayerDto} from '../../../../api/generated/models/player-dto';
import {BehaviorSubject, Observable, tap} from 'rxjs';
import {MessageService} from '../../../../api/generated/services/message.service';

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
export class WhisperWindowComponent implements AfterViewChecked, AfterViewInit {
  @ViewChild("messageArea") private messageArea: ElementRef;
  @ViewChildren("messageElement") private messageElements: QueryList<ElementRef>
  private isScrolledToBottom = true;

  @Input() conversation: FullConversationDto

  storedPlayer: PlayerDto
  messageToSend: string

  private newMessages = new BehaviorSubject<Message[]>([])
  newMessages$ = this.newMessages.asObservable();

  message$: Observable<Message>

  constructor(
    private whisperWindowService: WhisperWindowService,
    private loginStateService: LoginStateService,
    private messageService: MessageService
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

  ngAfterViewInit() {
    const observer = new IntersectionObserver((entries) => {
      const visibleMessageIds = entries
        .filter(entry => entry.isIntersecting)
        .filter(entry => {
          const messageElement = entry.target as HTMLElement;
          return messageElement.querySelector(".other-player") !== null;
        })
        .map(entry => entry.target.getAttribute("data-message-id"));

      if (visibleMessageIds.length > 0) {
        const ids = visibleMessageIds.map(id => parseInt(id));
        this.messageService.markAsRead1({
          body: {
            messageIds: ids,
            playerId: this.storedPlayer.id,
            conversationId: this.conversation.id,
            username: this.storedPlayer.username
          }
        }).subscribe();
        visibleMessageIds.forEach(id => {
          const element = this.messageElements.find(el =>
            el.nativeElement.getAttribute('data-message-id') === id
          );
          if (element) {
            observer.unobserve(element.nativeElement);
          }
        });
      }
    });

    this.messageElements.changes.subscribe(() => {
      this.messageElements.forEach(element => {
        observer.observe(element.nativeElement);
      });
    });
    this.messageElements.forEach(element => {
      observer.observe(element.nativeElement)
    })
  }

  ngAfterViewChecked() {
    if (this.isScrolledToBottom) {
      const element = this.messageArea.nativeElement;
      element.scrollTop = element.scrollHeight;
    }
  }

  sendMessage(conversationId: number, receiverId: number, userName: string) {
    if (this.messageToSend) {
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

  closeConversation() {
    this.whisperWindowService.removeFromConversations(this.conversation);
  }
}
