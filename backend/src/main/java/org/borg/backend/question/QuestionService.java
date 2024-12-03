package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.GameStateUpdate;
import org.borg.backend.multiplayer.MultiplayerService;
import org.borg.backend.multiplayer.MultiplayerSession;
import org.borg.backend.multiplayer.MultiplayerSessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    private final MultiplayerService multiplayerService;
    private final MultiplayerSessionRepository multiplayerSessionRepository;


    public List<QuestionDTO> getThreeQuestionsByCategory(CategorySelectionRequest request) {
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(request.getSelectedCategory().toLowerCase());
        multiplayerService.updateSessionQuestions(request.getSessionId(), questions);
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

    public List<QuestionDTO> getQuestionsForSession(Long sessionId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        List<Long> questionIds = session.getQuestionIds();
        List<Question> questions = questionRepository.findAllById(questionIds);
        return questions.stream()
                .map(question -> new QuestionDTO(question.getQuestion(), question.getOptions()))
                .collect(Collectors.toList());
    }
}
