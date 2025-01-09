package org.borg.backend.chat.service;

import org.borg.backend.auth.model.Role;
import org.borg.backend.auth.repository.RoleRepository;
import org.borg.backend.chat.dto.ConversationPreviewDTO;
import org.borg.backend.chat.dto.CreateConversationRequest;
import org.borg.backend.chat.dto.FullConversationDTO;
import org.borg.backend.chat.dto.SendMessageRequest;
import org.borg.backend.chat.model.Conversation;
import org.borg.backend.chat.model.Message;
import org.borg.backend.chat.repository.ConversationRepository;
import org.borg.backend.chat.repository.MessageRepository;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

    }

    private Player createAndSavePlayer(String name) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initialized"));

        Player player = Player.builder()
                .username(name.toLowerCase())
                .password("password")
                .displayName(name)
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(userRole))
                .build();

        return playerRepository.save(player);
    }

    @AfterEach
    void tearDown() {
        conversationRepository.deleteAll();
        messageRepository.deleteAll();
        playerRepository.deleteAll();

    }

    @Nested
    class sendMessageTests {
        private Player player1;
        private Player player2;

        @BeforeEach
        void setUp() {
            player1 = createAndSavePlayer("Player1");
            player2 = createAndSavePlayer("Player2");
        }

        @Test
        void createConversationAndSendMessage() {
            FullConversationDTO conversation = messagingService.createConversation(new CreateConversationRequest(player1.getId(), player2.getId()));

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .senderId(player1.getId())
                    .receiverId(player2.getId())
                    .conversationId(conversation.getId())
                    .message("Hello friend")
                    .build();

            messagingService.sendMessage(sendMessageRequest);

            List<Conversation> conversationList = conversationRepository.findAll();

            assertAll("Post first message conversation checks",
                    () -> assertFalse(conversationList.isEmpty(), "There should be one conversation saved"),
                    () -> assertEquals(player1.getId(), conversationList.get(0).getPlayer1().getId(), "First message sender should be player1"),
                    () -> assertEquals(player2.getId(), conversationList.get(0).getPlayer2().getId(), "First message receiver should be player2"),
                    () -> assertEquals("Hello friend", conversationList.get(0).getLatestMessage().getContent(), "The message content should be \"Hello friend\"")
            );

            List<Message> messageList = messageRepository.findAll();

            assertAll("Post first message message checks",
                    () -> assertFalse(messageList.isEmpty(), "There should be one message saved"),
                    () -> assertEquals(player1.getId(), messageList.get(0).getSenderId(), "Message sender should be player1"),
                    () -> assertEquals(conversationList.get(0).getId(), messageList.get(0).getConversation().getId(), "Conversation Id and Message's conversation ID should match")
            );
        }

        @Test
        void sendMessageBackAndForth() {
            FullConversationDTO conversation = messagingService.createConversation(new CreateConversationRequest(player1.getId(), player2.getId()));

            SendMessageRequest firstMessageRequest = SendMessageRequest.builder()
                    .senderId(player1.getId())
                    .receiverId(player2.getId())
                    .conversationId(conversation.getId())
                    .message("Hello friend")
                    .build();
            messagingService.sendMessage(firstMessageRequest);

            List<Conversation> conversationList = conversationRepository.findAll();
            Long conversationId = conversationList.get(0).getId();

            SendMessageRequest secondMessageRequest = SendMessageRequest.builder()
                    .senderId(player2.getId())
                    .receiverId(player1.getId())
                    .conversationId(conversationId)
                    .message("Hello back")
                    .build();
            messagingService.sendMessage(secondMessageRequest);

            List<Conversation> updatedConversationList = conversationRepository.findAll();
            List<Message> messageList = messageRepository.findAll();

            assertAll("Post back-and-forth message checks",
                    () -> assertEquals(1, updatedConversationList.size(), "There should only be one conversation"),
                    () -> assertEquals(2, messageList.size(), "There should be two messages sent"),
                    () -> assertEquals("Hello back", updatedConversationList.get(0).getLatestMessage().getContent(), "The latest message content should be \"Hello back\""),
                    () -> assertEquals(player2.getId(), messageList.get(1).getSenderId(), "Second message should be sent by player2"),
                    () -> assertEquals(player1.getId(), messageList.get(0).getSenderId(), "First message should be sent by player1")
            );
        }
    }

    @Nested
    class GettingConversationsTests {
        Player receiverPlayer;
        List<Player> players = new ArrayList<>();
        private final int NUM_OF_PLAYERS = 10;

        @BeforeEach
        void setUp() {
            receiverPlayer = createAndSavePlayer("Receiver");
            for (int i = 0; i < NUM_OF_PLAYERS; i++) {
                players.add(createAndSavePlayer("Player " + i));
            }
        }

        @Test
        void shouldReturnAllConversationsForReceiverPlayer() {
            Map<Long, String> expectedMessages = new HashMap<>();
            for (int i = 0; i < NUM_OF_PLAYERS; i++) {
                Player sendingPlayer = players.get(i);
                String message = "Hello from " + sendingPlayer.getDisplayName();

                FullConversationDTO conversation = messagingService.createConversation(new CreateConversationRequest(sendingPlayer.getId(), receiverPlayer.getId()));

                messagingService.sendMessage(SendMessageRequest.builder()
                        .senderId(sendingPlayer.getId())
                        .receiverId(receiverPlayer.getId())
                        .conversationId(conversation.getId())
                        .message(message)
                        .build());

                expectedMessages.put(sendingPlayer.getId(), message);
            }

            List<ConversationPreviewDTO> conversationPreviewDTOS = messagingService.getPlayerConversations(receiverPlayer.getId());

            assertAll("Post get conversations check",
                    () -> assertEquals(NUM_OF_PLAYERS, conversationPreviewDTOS.size(), String.format("There should be %s conversations", NUM_OF_PLAYERS)),
                    () -> assertTrue(conversationPreviewDTOS.stream()
                                    .allMatch(dto -> expectedMessages.containsKey(dto.getOtherPlayer().getId()) &&
                                            expectedMessages.get(dto.getOtherPlayer().getId())
                                                    .equals(dto.getLatestMessage())),
                            "Each conversation should have matching sender ID and message content")
            );
        }
        
    }
    
    
}
