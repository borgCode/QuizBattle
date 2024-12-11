package org.borg.backend.player;

import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerDTO;

import java.util.ArrayList;
import java.util.List;

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
                .build();
    }
    
    public static List<PlayerDTO> multipleToDTO(List<Player> players) {
        List<PlayerDTO> dtoList = new ArrayList<>();
        for (Player player : players) {
            PlayerDTO playerDTO = PlayerDTO.builder()
                    .id(player.getId())
                    .username(player.getUsername())
                    .displayName(player.getDisplayName())
                    .stats(player.getStats())
                    .build();
            dtoList.add(playerDTO);
        }
        
        return dtoList;
    }

}
