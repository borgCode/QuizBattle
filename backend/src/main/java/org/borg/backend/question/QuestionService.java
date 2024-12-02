package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;

    public List<Question> getAllUsers() {
        return questionRepository.findAll();
    }

    public List<QuestionDTO> getThreeQuestionsByCategory(String category) {
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(category.toLowerCase());
        return questions.stream()
                .map(question -> new QuestionDTO(question.getQuestion(), question.getOptions()))
                .collect(Collectors.toList());
    }
}
