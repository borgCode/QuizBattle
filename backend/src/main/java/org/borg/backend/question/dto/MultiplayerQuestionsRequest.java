package org.borg.backend.question.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MultiplayerQuestionsRequest {
    private String category;
    private Long sessionId;
    private Long playerId;
}
