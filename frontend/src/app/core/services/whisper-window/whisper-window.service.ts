import {Injectable} from '@angular/core';
import {FullConversationDto} from '../../../api/generated/models/full-conversation-dto';
import {BehaviorSubject, Subject} from 'rxjs';
import {WebSocketService} from '../../websocket/web-socket.service';
import {LoginStateService} from "../login-state-service/login-state.service";

export interface Message {
  id: number,
  senderId: number,
  receiverId: number,
  sentAt: string,
  read: boolean,
  content: string
}

@Injectable({
  providedIn: 'root'
})
export class WhisperWindowService {
  private openConversations = new BehaviorSubject<FullConversationDto[]>([]);
  private openConversationsIds: number[] = [];

  private messageSubject = new Subject<Message>()
  message$ = this.messageSubject.asObservable();


  constructor(
    private webSocketService: WebSocketService,
    private loginStateService: LoginStateService
  ) {
    this.webSocketService.message$.subscribe(message => {
      this.messageSubject.next(message);
    });

    this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
      if (!isLoggedIn) {
        console.log("Clearing convos")
        this.openConversations.next([]);
      }
    })
  }


  addToConversations(conversation: FullConversationDto) {
    const currentConversations = this.openConversations.getValue();
    console.log(conversation)
    if (!this.openConversationsIds.includes(conversation.id)) {
      const newConversations = [...currentConversations, conversation];
      this.openConversationsIds.push(conversation.id)
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
      this.openConversationsIds = this.openConversationsIds.filter((id) => id !== conversation.id);
    }
  }

  get conversations$() {
    return this.openConversations.asObservable();
  }

  sendMessage(param: {
    messageRequest: {
      senderId: number; receiverId: number; conversationId: number; receiverUsername: string; message: string
    }
  }) {
    this.webSocketService.sendMessage("/app/messages/send", param.messageRequest);

    this.messageSubject.next(
      {
        id: Date.now(),
        senderId: param.messageRequest.senderId,
        receiverId: param.messageRequest.receiverId,
        sentAt: new Date().toISOString(),
        read: true,
        content: param.messageRequest.message
      }
    )
  }
}
