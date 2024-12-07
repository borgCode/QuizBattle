package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import org.borg.backend.multiplayer.service.MultiplayerService;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
        String selectedCategory = request.getSelectedCategory();
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(selectedCategory);
        multiplayerService.updateSessionQuestionsAndCategory(request.getSessionId(), questions, selectedCategory);
        
        return questions.stream()
                .map(question -> new QuestionDTO(question.getId(), question.getQuestion(), question.getOptions()))
                .collect(Collectors.toList());
    }

    public AnswerValidationResponse validateAnswer(AnswerValidationRequest request) {
        if (request == null || request.getAnswer() == null) {
            throw new IllegalArgumentException("Request or answer cannot be null");
        }
        
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new NoSuchElementException("Question not found"));
        
        boolean isCorrect = question.getCorrectAnswer().equals(request.getAnswer());
        multiplayerService.updateGameState(
                request.getSessionId(),
                request.getPlayerId(),
                request.getQuestionId(),
                isCorrect
        );
        
        int indexOfCorrectAnswer = question.getOptions().indexOf(question.getCorrectAnswer());
        
        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
    }

    public List<QuestionDTO> getQuestionsForSession(Long sessionId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        List<Long> questionIds = session.getQuestionIds();
        List<Question> questions = questionRepository.findAllById(questionIds);
        return questions.stream()
                .map(question -> new QuestionDTO(question.getId(), question.getQuestion(), question.getOptions()))
                .collect(Collectors.toList());
    }

    public List<String> getThreeRandomCategories(Long sessionId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        List<String> allCategories = questionRepository.findAllCategories();
        
        List<String> categoriesNotPlayed = allCategories.stream()
                .filter(category -> !session.getPlayedCategories().contains(category))
                .collect(Collectors.toList());
        
        Collections.shuffle(categoriesNotPlayed);
        
        return categoriesNotPlayed.stream()
                .limit(3)
                .collect(Collectors.toList());
    }
}
