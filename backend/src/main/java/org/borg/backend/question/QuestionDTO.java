package org.borg.backend.question;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class QuestionDTO {
    private String question;
    private List<String> options;

    public QuestionDTO(String question, List<String> options) {
        this.question = question;
        this.options = options;
    }

}
