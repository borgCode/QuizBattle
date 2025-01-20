package org.borg.backend.unit.social.chat;

import org.borg.backend.player.model.Player;
import org.borg.backend.player.service.PlayerService;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.borg.backend.social.chat.dto.ConversationRequest;
import org.borg.backend.social.chat.dto.MarkAsReadRequest;
import org.borg.backend.social.chat.dto.SendMessageRequest;
import org.borg.backend.social.chat.mapper.ConversationMapper;
import org.borg.backend.social.chat.model.Conversation;
import org.borg.backend.social.chat.model.Message;
import org.borg.backend.social.chat.repository.ConversationRepository;
import org.borg.backend.social.chat.repository.MessageRepository;
import org.borg.backend.social.chat.service.MessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class MessagingServiceTest {
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private PlayerService playerService;
    @Mock
    private ConversationMapper conversationMapper;
    
    @InjectMocks
    private MessagingService messagingService;

    private Player player1;
    private Player player2;
    private Conversation conversation;
    private Message message;
    
    @BeforeEach
    void Setup() {
        MockitoAnnotations.openMocks(this);

        player1 = Player.builder()
                .id(1L)
                .username("player1")
                .build();

        player2 = Player.builder()
                .id(2L)
                .username("player2")
                .build();

        conversation = Conversation.builder()
                .id(1L)
                .player1(player1)
                .player2(player2)
                .build();

        message = Message.builder()
                .id(1L)
                .conversation(conversation)
                .senderId(player1.getId())
                .receiverId(player2.getId())
                .content("Test message")
                .read(false)
                .build();
    }


    @Test
    void sendMessage_ShouldHandleNotificationFailureGracefully() {
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .senderId(player1.getId())
                .receiverId(player2.getId())
                .receiverUsername(player2.getUsername())
                .conversationId(conversation.getId())
                .message(message.getContent()).build();

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        doThrow(new RuntimeException("Notification failed")).when(simpMessagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());

        assertDoesNotThrow(() -> messagingService.sendMessage(messageRequest));

        verify(messageRepository).save(any(Message.class));
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void markMessagesAsRead_ShouldThrowAccessDenied_WhenUnauthorized() {
        List<Long> messageIds = List.of(1L, 2L);
        MarkAsReadRequest request = new MarkAsReadRequest(messageIds, 1L, 2L, "player2");

        when(messageRepository.countByIdInAndReceiverIdNot(messageIds, 2L))
                .thenReturn(1L);

        assertThrows(AccessDeniedException.class,
                () -> messagingService.markMessagesAsRead(request));

        verify(messageRepository, never()).markMessagesAsRead(any());
    }

    @Test
    void getFullConversation_ShouldCreateNew_WhenNotExists() {
        ConversationRequest request = new ConversationRequest(1L, 2L);

        when(conversationRepository.findByBothPlayerIds(1L, 2L))
                .thenReturn(null);
        when(playerService.getPlayerById(1L)).thenReturn(player1);
        when(playerService.getPlayerById(2L)).thenReturn(player2);
        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(conversation);

        messagingService.getFullConversation(request);

        verify(conversationRepository).save(any(Conversation.class));
        verify(conversationMapper).toFullConversationDTO(any(), eq(1L));
    }

    @Test
    void getPreviewConversation_ShouldThrowNotFound_WhenConversationMissing() {
        when(conversationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> messagingService.getPreviewConversation(1L, 1L));
    }

    @Test
    void getPreviewConversation_ShouldThrowAccessDenied_WhenUnauthorized() {
        Player unauthorizedPlayer = Player.builder()
                .id(3L)
                .username("unauthorized")
                .build();

        Conversation conversation = Conversation.builder()
                .id(1L)
                .player1(player1)
                .player2(player2)
                .build();

        when(conversationRepository.findById(1L))
                .thenReturn(Optional.of(conversation));

        assertThrows(AccessDeniedException.class,
                () -> messagingService.getPreviewConversation(1L, unauthorizedPlayer.getId()));
        
        verify(conversationMapper, never()).toPreviewDTO(any(), any());
    }

    @Test
    void markMessagesAsRead_ShouldSendNotification_WhenLastMessageRead() {
        List<Long> messageIds = List.of(1L);
        MarkAsReadRequest request = new MarkAsReadRequest(messageIds, 1L, 2L, "player2");

        when(messageRepository.countByIdInAndReceiverIdNot(messageIds, 2L))
                .thenReturn(0L);
        when(conversationRepository.isLatestMessageRead(1L))
                .thenReturn(true);

        messagingService.markMessagesAsRead(request);

        verify(simpMessagingTemplate).convertAndSendToUser(
                eq("player2"),
                eq("/queue/conversation/read"),
                eq(1L)
        );
    }

    @Test
    void sendMessage_ShouldHandleConversationNotFound() {
        SendMessageRequest request = SendMessageRequest.builder()
                .conversationId(999L)
                .senderId(1L)
                .receiverId(2L)
                .message("Test message")
                .build();

        when(conversationRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> messagingService.sendMessage(request));
    }
}
