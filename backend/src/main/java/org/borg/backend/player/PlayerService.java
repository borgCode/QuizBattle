package org.borg.backend.player;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.model.PlayerDTO;
import org.borg.backend.player.model.UpdatePlayerRequest;
import org.borg.backend.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {
    
    private final PlayerRepository playerRepository;
    private final FileStorageService fileStorageService;

    public PlayerDTO getPlayerByName(String username) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        return PlayerMapper.toDTO(player);
    }

    public void updatePlayer(UpdatePlayerRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Player not found"));

        switch (request.getUpdateField()) {
            case DISPLAY_NAME:
                player.setDisplayName(request.getNewDisplayName());
                break;
            default:
                throw new IllegalArgumentException("Invalid update field");
        }

        playerRepository.save(player);
    }

    public void uploadProfilePicture(Long playerId, MultipartFile file) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoSuchElementException("User not found!"));
        log.warn("Saving profile pic");
        String profilePicturePath = fileStorageService.saveProfilePicture(file, playerId);
        log.warn("SAved + {}", profilePicturePath);
        player.setAvatarPath(profilePicturePath);
        playerRepository.save(player);
    }
}

