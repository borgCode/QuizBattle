import {Injectable} from '@angular/core';
import {FullConversationDto} from '../../../api/generated/models/full-conversation-dto';
import {BehaviorSubject, Subject} from 'rxjs';
import {WebSocketService} from '../../websocket/web-socket.service';
import {filter} from 'rxjs/operators';

export interface Message {
  id: number,
  senderId: number,
  sentAt: string,
  isRead: boolean,
  content: string
}

@Injectable({
  providedIn: 'root'
})
export class WhisperWindowService {
  private openConversations = new BehaviorSubject<FullConversationDto[]>([]);

  private messageSubject = new Subject<Message>()
  message$ = this.messageSubject.asObservable();


  constructor(
    private webSocketService: WebSocketService

  ) {
    this.webSocketService.isConnected$
      .pipe(
        filter(connected => connected)
      )
      .subscribe(() => {
        this.webSocketService.subscribe("/user/queue/message", message => {
          this.messageSubject.next(message);
        });
      });
  }

  addToConversations(conversation: FullConversationDto) {
    const currentConversations = this.openConversations.getValue();
    if (currentConversations.indexOf(conversation) === -1) {
      const newConversations = [...currentConversations, conversation];
      this.openConversations.next(newConversations);

    }
  }

  removeFromConversations(conversation: FullConversationDto) {
    const currentConversations = this.openConversations.getValue();
    const index = currentConversations.indexOf(conversation)
    if (index > -1) {
      const newConversations = [
        ...currentConversations.slice(0, index),
        ...currentConversations.slice(index + 1)
      ];
      this.openConversations.next(newConversations);
    }
  }

  get conversations$() {
    return this.openConversations.asObservable();
  }

  sendMessage(param: {
    messageRequest: {
      senderId: number; receiverId: number; conversationId: number; receiverUsername: string; message: string }
  }) {
    this.webSocketService.sendMessage("/app/messages/send", param.messageRequest);

    this.messageSubject.next(
      {
        id: Date.now(),
        senderId: param.messageRequest.senderId,
        sentAt: new Date().toISOString(),
        isRead: true,
        content: param.messageRequest.message
      }
    )
  }
}
