package org.borg.backend.game.shared.mapper;


import org.borg.backend.game.shared.model.Question;
import org.borg.backend.game.shared.dto.QuestionDTO;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    QuestionDTO toDTO(Question question);
    List<QuestionDTO> multipleToDTO(List<Question> questions);
}
