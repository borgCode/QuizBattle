package org.borg.backend.player.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.player.dto.PlayerDTO;
import org.borg.backend.player.dto.UpdatePlayerRequest;
import org.borg.backend.player.mapper.PlayerMapper;
import org.borg.backend.player.model.Player;
import org.borg.backend.player.repository.PlayerRepository;
import org.borg.backend.shared.enums.BusinessErrorCodes;
import org.borg.backend.shared.exceptions.ResourceNotFoundException;
import org.borg.backend.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {
    
    private final PlayerRepository playerRepository;
    private final FileStorageService fileStorageService;

    public Player getPlayerById(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player not found for " + playerId));
    }
 

    public PlayerDTO getPlayerDTOById(Long playerId) {
        return PlayerMapper.toDTO(getPlayerById(playerId));
    }

    public void updatePlayer(UpdatePlayerRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player not found for " + request.getPlayerId()));
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
                .orElseThrow(() -> new ResourceNotFoundException(BusinessErrorCodes.RESOURCE_NOT_FOUND, "Player not found for " + playerId));
        log.warn("Saving profile pic");
        String profilePicturePath = fileStorageService.saveProfilePicture(file, playerId);
        log.warn("Saved + {}", profilePicturePath);
        player.setAvatarPath(profilePicturePath);
        playerRepository.save(player);
    }
}

