package org.borg.backend.game.shared.mapper;


import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.dto.QuestionDTO;

import java.util.ArrayList;
import java.util.List;

public class QuestionMapper {

    public static List<QuestionDTO> multipleToDTO(List<Question> questions) {
        List<QuestionDTO> dtoList = new ArrayList<>();
        for (Question question : questions) {
            QuestionDTO questionDTO = QuestionDTO.builder()
                    .questionId(question.getId())
                    .question(question.getQuestion())
                    .options(question.getOptions())
                    .build();
            dtoList.add(questionDTO);
        }

        return dtoList;
    }
}
