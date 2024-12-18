package org.borg.backend.question.mapper;


import org.borg.backend.question.dto.Question;
import org.borg.backend.question.dto.QuestionDTO;

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
