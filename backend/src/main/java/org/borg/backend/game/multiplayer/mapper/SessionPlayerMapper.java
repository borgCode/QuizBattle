package org.borg.backend.game.multiplayer.mapper;

import org.borg.backend.game.multiplayer.dto.SessionPlayerDTO;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = ImageUtil.class)
public interface SessionPlayerMapper {
    
    @Mapping(target = "base64Image",
            expression = "java(ImageUtil.encodeAvatarImageFileToBase64(sessionPlayer.getPlayer().getAvatarPath()))")
    @Mapping(target = "displayName", source = "player.displayName")
    SessionPlayerDTO toDTO(SessionPlayer sessionPlayer);
}
