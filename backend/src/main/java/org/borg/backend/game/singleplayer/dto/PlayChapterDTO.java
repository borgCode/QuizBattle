package org.borg.backend.game.singleplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PlayChapterDTO {
    private Integer roundWinCondition;
    private String title;
    private String rewardText;
}
