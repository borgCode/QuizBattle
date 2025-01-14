package org.borg.backend.social.block.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.social.block.model.PlayerBlock;
import org.borg.backend.social.friendship.repository.FriendshipRepository;
import org.borg.backend.social.notification.repository.NotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.borg.backend.social.block.event.PlayerBlockEvent.PlayerBlockedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBlockListener {

    private final FriendshipRepository friendshipRepository;
    private final NotificationRepository notificationRepository;

    @EventListener
    @Transactional 
    public void handleFriendshipDeletion(PlayerBlockedEvent event) {
        PlayerBlock block = event.block();
        friendshipRepository.deleteByPlayer1AndPlayer2OrPlayer1AndPlayer2(
                block.getBlocker(), block.getBlocked(),
                block.getBlocked(), block.getBlocker()
        );
        log.info("Deleted friendships between players {} and {}",
                block.getBlocker().getId(), block.getBlocked().getId());
    }

    @EventListener
    @Async
    public void handleNotificationDeletion(PlayerBlockedEvent event) {
//        PlayerBlock block = event.block();
//        notificationRepository.deleteByFromPlayerAndToPlayerOrFromPlayerAndToPlayer(
//                block.getBlocker(), block.getBlocked(),
//                block.getBlocked(), block.getBlocker()
//        );
//        log.info("Deleted notifications between players {} and {}",
//                block.getBlocker().getId(), block.getBlocked().getId());
    }
}
