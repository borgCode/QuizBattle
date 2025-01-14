package org.borg.backend.player.mapper;

import org.borg.backend.player.dto.PlayerConversationDTO;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.model.Player;
import org.borg.backend.shared.util.ImageUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = ImageUtil.class)
public interface PlayerMapper {

    @Mapping(target = "base64Image", expression = "java(ImageUtil.encodeAvatarImageFileToBase64(player.getAvatarPath()))")
    PlayerDTO toDTO(Player player);
    List<PlayerDTO> multipleToDTO(List<Player> players);

    default PlayerConversationDTO toPlayerConversationDTO(Player player) {
        return PlayerConversationDTO.builder()
                .id(player.getId())
                .username(player.getUsername())
                .displayName(player.getDisplayName())
                .base64Image(ImageUtil.encodeAvatarImageFileToBase64(player.getAvatarPath()))
                .build();
    }
}
