package org.borg.backend.game.singleplayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.player.dto.PlayerProgressDTO;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AllStoriesDTO {
    private List<StoryDTO> stories;
    private List<PlayerProgressDTO> playerProgressList;
}
