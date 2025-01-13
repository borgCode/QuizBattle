package org.borg.backend.game.shared.model;

import org.borg.backend.game.shared.dto.QuestionDTO;

import java.util.List;

public record RoundSessionProgress (List<QuestionDTO> questions, int currentIndex) {
}
