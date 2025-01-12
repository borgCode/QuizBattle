import {Component, OnInit} from '@angular/core';
import {BehaviorSubject, combineLatest, debounceTime, startWith} from 'rxjs';
import {ConversationPreviewDto} from '../../../../api/generated/models/conversation-preview-dto';
import {MessageService} from '../../../../api/generated/services/message.service';
import {LoginStateService} from '../../../services/login-state-service/login-state.service';
import {filter} from 'rxjs/operators';
import {NavigationEnd, Router} from '@angular/router';
import {AsyncPipe, NgForOf, NgIf} from '@angular/common';
import {WhisperWindowService} from '../../../services/whisper-window/whisper-window.service';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {WebSocketService} from '../../../websocket/web-socket.service';

@Component({
  selector: 'app-whispers-dropdown',
  imports: [
    AsyncPipe,
    NgForOf,
    NgIf,
    ReactiveFormsModule
  ],
  templateUrl: './whispers-dropdown.component.html',
  styleUrl: './whispers-dropdown.component.css'
})
export class WhispersDropdownComponent implements OnInit {
  private unreadConversations = new BehaviorSubject<ConversationPreviewDto[]>([]);
  unreadConversations$ = this.unreadConversations.asObservable();

  private readConversations = new BehaviorSubject<ConversationPreviewDto[]>([]);

  searchControl = new FormControl("");
  filteredUnread: ConversationPreviewDto[] = [];
  filteredRead: ConversationPreviewDto[] = [];

  playerId: number;

  constructor(
    private messageService: MessageService,
    private loginStateService: LoginStateService,
    private router: Router,
    private whisperWindowService: WhisperWindowService,
    private websocketService: WebSocketService
  ) {
    this.websocketService.conversationReadEvent$.subscribe(conversationId => {
      const currentUnread = this.unreadConversations.getValue();
      const updatedUnread = currentUnread.filter(
        conv => conv.id !== conversationId);
      this.unreadConversations.next(updatedUnread);

    })
  }


  ngOnInit() {
    this.loginStateService.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.playerId = this.loginStateService.loggedInUser.id;
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

    combineLatest([
      this.unreadConversations,
      this.readConversations,
      this.searchControl.valueChanges.pipe(
        startWith(""),
        debounceTime(150)
      )
    ]).subscribe(([unread, read, searchTerm]) => {
      this.filteredUnread = unread.filter(conversation =>
        conversation.otherPlayer.displayName.toLowerCase().includes(searchTerm.toLowerCase()));
      this.filteredRead = read.filter(conversation =>
        conversation.otherPlayer.displayName.toLowerCase().includes(searchTerm.toLowerCase()));
    })
  }

  private fetchConversations() {
    if (this.loginStateService.loggedInUser.id) {
      this.messageService.getPlayerConversations({playerId: this.playerId}).subscribe(
        conversations => {
          const unreadConversations = conversations.filter(conversation => !conversation.latestMessageIsRead);
          const readConversations = conversations.filter(conversation => conversation.latestMessageIsRead);

          this.unreadConversations.next(unreadConversations);
          this.readConversations.next(readConversations);
        }
      )
    }
  }

  openConversation(otherPlayerId: number) {
    this.messageService.getConversation({
      body: {
        senderId: this.playerId,
        receiverId: otherPlayerId
      }
    }).subscribe({
      next: conversation => {
        this.whisperWindowService.addToConversations(conversation)
      },
    })
  }
}
