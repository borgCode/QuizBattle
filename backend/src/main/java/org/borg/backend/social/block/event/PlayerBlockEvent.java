package org.borg.backend.social.block.event;

import org.borg.backend.social.block.model.PlayerBlock;

public class PlayerBlockEvent {
    public record PlayerBlockedEvent(PlayerBlock block) {}
}
