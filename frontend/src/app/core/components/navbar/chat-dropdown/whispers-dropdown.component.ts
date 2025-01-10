import {Component, OnInit} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {ConversationPreviewDto} from '../../../../api/generated/models/conversation-preview-dto';
import {MessageService} from '../../../../api/generated/services/message.service';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {filter} from 'rxjs/operators';
import {NavigationEnd, Router} from '@angular/router';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';

@Component({
  selector: 'app-whispers-dropdown',
  imports: [
    AsyncPipe,
    NgForOf,
    NgIf
  ],
  templateUrl: './whispers-dropdown.component.html',
  styleUrl: './whispers-dropdown.component.css'
})
export class WhispersDropdownComponent implements OnInit {
  private unreadConversations = new BehaviorSubject<ConversationPreviewDto[]>([]);
  unreadConversations$ = this.unreadConversations.asObservable();

  private readConversations = new BehaviorSubject<ConversationPreviewDto[]>([]);
  readConversations$ = this.readConversations.asObservable();

  constructor(
    private messageService: MessageService,
    private loginStateService: LoginStateService,
    private router: Router
  ) {
  }

  ngOnInit() {
   this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
     if (isLoggedIn) {
       this.fetchConversations();
     } else {
       this.unreadConversations.next([]);
       this.readConversations.next([]);
     }
   })

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)).subscribe(() => {
      if (this.loginStateService.isLoggedIn$) {
        this.fetchConversations();
      }
    })
  }

  private fetchConversations() {
    if (this.loginStateService.loggedInUser.id) {
      this.messageService.getPlayerConversations({playerId: this.loginStateService.loggedInUser.id}).subscribe(
        conversations => {
          const unreadConversations = conversations.filter(conversation => !conversation.latestMessageIsRead);
          const readConversations = conversations.filter(conversation => conversation.latestMessageIsRead);

          this.unreadConversations.next(unreadConversations);
          this.readConversations.next(readConversations);
        }
      )
    }
  }

  markAsRead(conversationId) {

  }

  deleteConversation(id: number) {

  }
}
