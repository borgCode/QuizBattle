package org.borg.backend.game.shared.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class QuestionDTO {
    private Long questionId;
    private String question;
    private List<String> options;

    public QuestionDTO(Long id, String question, List<String> options) {
        this.questionId = id;
        this.question = question;
        this.options = options;
    }

}
