package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.GameStateUpdate;
import org.borg.backend.multiplayer.MultiplayerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final MultiplayerService multiplayerService;

    public List<Question> getAllUsers() {
        return questionRepository.findAll();
    }

    public List<QuestionDTO> getThreeQuestionsByCategory(String category) {
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(category.toLowerCase());
        return questions.stream()
                .map(question -> new QuestionDTO(question.getQuestion(), question.getOptions()))
                .collect(Collectors.toList());
    }

    public AnswerValidationResponse validateAnswer(AnswerValidationRequest request) {
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException("Question not found"));
        
        boolean isCorrect = question.getCorrectAnswer().equals(request.getAnswer());
        GameStateUpdate gameStateUpdate = multiplayerService.updateGameState(
                request.getSessionId(),
                request.getPlayerId(),
                isCorrect
        );
        
        return new AnswerValidationResponse(isCorrect, gameStateUpdate);
    }
}
