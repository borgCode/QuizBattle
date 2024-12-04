package org.borg.backend.player;

public class PlayerMapper {
    public static PlayerDTO toDTO(Player player) {
        if (player == null) {
            return null;
        }
        
        return PlayerDTO.builder()
                .username(player.getUsername())
                .displayName(player.getDisplayName())
                .numOfGames(player.getNumOfGames())
                .numOfWins(player.getNumOfWins())
                .numOfLosses(player.getNumOfLosses())
                .build();
    }
}
