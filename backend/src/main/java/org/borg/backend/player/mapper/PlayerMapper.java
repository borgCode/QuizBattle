package org.borg.backend.player.mapper;

import org.borg.backend.common.util.ImageUtil;
import org.borg.backend.player.dto.PlayerConversationDTO;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.model.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerMapper {
    public static PlayerDTO toDTO(Player player) {
        if (player == null) {
            return null;
        }
        
        return PlayerDTO.builder()
                .id(player.getId())
                .username(player.getUsername())
                .displayName(player.getDisplayName())
                .stats(player.getStats())
                .base64Image(ImageUtil.encodeAvatarImageFileToBase64(player.getAvatarPath()))
                .build();
    }
    
    public static List<PlayerDTO> multipleToDTO(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return Collections.emptyList();
        }

        return players.stream()
                .map(PlayerMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    public static PlayerConversationDTO toPlayerConversationDTO(Player player) {
        return PlayerConversationDTO.builder()
                .id(player.getId())
                .userName(player.getUsername())
                .displayName(player.getDisplayName())
                .base64Image(ImageUtil.encodeAvatarImageFileToBase64(player.getAvatarPath()))
                .build();
    }
    
}
