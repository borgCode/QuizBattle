package org.borg.backend.game.multiplayer.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.borg.backend.game.shared.dto.PlayerQuestionResult;

import java.util.Set;

@Getter
@Setter
@Builder
public class SessionPlayerDTO {
        private Long id;
        private String displayName;
        private String base64Image;
        private Integer score;
        private Integer questionsAnswered;
        private boolean hasAcknowledgedGameOver;
        private boolean givenUp;
        private Set<PlayerQuestionResult> questionResults;
}
