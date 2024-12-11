package org.borg.backend.player;

import lombok.RequiredArgsConstructor;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerDTO;
import org.borg.backend.player.model.UpdatePlayerRequest;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PlayerService {
    
    private final PlayerRepository playerRepository;
    
    public PlayerDTO getPlayerByName(String username) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        return PlayerMapper.toDTO(player);
    }

    public void updatePlayer(UpdatePlayerRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Player not found"));

        switch (request.getUpdateField()) {
            case USERNAME:
                player.setUsername(request.getNewUsername());
                break;
            case DISPLAY_NAME:
                player.setDisplayName(request.getNewDisplayName());
                break;
            default:
                throw new IllegalArgumentException("Invalid update field");
        }

        playerRepository.save(player);
    }
}

