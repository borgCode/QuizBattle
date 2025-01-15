package org.borg.backend.game.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String question;
    private List<String> options;
}
