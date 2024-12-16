package org.borg.backend.player.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.player.mapper.PlayerMapper;
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



    public PlayerDTO getPlayerById(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoSuchElementException("Player not found"));
        return PlayerMapper.toDTO(player);
    }

    public void updatePlayer(UpdatePlayerRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new NoSuchElementException("Player not found"));
        log.warn("Updating player: " + player.getUsername());

        switch (request.getUpdateField()) {
            case DISPLAY_NAME:
                log.warn("Setting display name");
                player.setDisplayName(request.getNewDisplayName());
                break;
            default:
                throw new IllegalArgumentException("Invalid update field");
        }
        log.warn("SAving to repo");

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

