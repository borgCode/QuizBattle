package org.borg.backend.chat.service;

import org.borg.backend.chat.dto.SendMessageRequest;
import org.borg.backend.chat.model.Conversation;
import org.borg.backend.chat.model.Message;
import org.borg.backend.chat.repository.ConversationRepository;
import org.borg.backend.chat.repository.MessageRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class MessageServiceIntegrationTest {
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private MessagingService messagingService;
    
    @AfterEach
    void tearDown() {
        conversationRepository.deleteAll();
        messageRepository.deleteAll();
    }
    
    @Nested
    class sendMessageTests {
       
        
        @Test
        void sendMessageWhenConversationDoesNotExist_andCreateConversation() {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .senderId(1L)
                    .receiverId(2L)
                    .message("Hello friend").
                    build();
            
            messagingService.sendMessage(sendMessageRequest);

            List<Conversation> conversationList = conversationRepository.findAll();
            

            assertAll("Post first message conversation checks",
                    () -> assertFalse(conversationList.isEmpty(), "There should be one conversation saved"),
                    () -> assertEquals(1L, conversationList.get(0).getPlayer1Id(), "First message sender should be player1 Id"),
                    () -> assertEquals(2L, conversationList.get(0).getPlayer2Id(), "First message receiver should be player1 Id"),
                    () -> assertEquals("Hello friend", conversationList.get(0).getLatestMessage().getContent(), "The message content should be \"Hello friend\"")
            );
            
            List<Message> messageList = messageRepository.findAll();

            assertAll("Post first message message checks",
                    () -> assertFalse(messageList.isEmpty(), "There should be one message saved"),
                    () -> assertEquals(1L, messageList.get(0).getSenderId(), "Message sender id should be \"1L\""),
                    () -> assertEquals(conversationList.get(0).getId(), messageList.get(0).getConversation().getId(), "Conversation Id and Message's conversation ID should match")
            );
        }
    }
    
    
}
