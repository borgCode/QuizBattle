import { Injectable } from '@angular/core';
import {FullConversationDto} from '../../../api/generated/models/full-conversation-dto';
import {BehaviorSubject, Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WhisperWindowService {
  private openConversations = new BehaviorSubject<FullConversationDto[]>([]);


  constructor() { }

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

  get conversations$ () {
    return this.openConversations.asObservable();
  }
}
