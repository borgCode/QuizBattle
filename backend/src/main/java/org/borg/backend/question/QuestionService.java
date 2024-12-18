package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.multiplayer.service.MultiplayerService;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.multiplayer.repository.MultiplayerSessionRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final MultiplayerService multiplayerService;
    private final MultiplayerSessionRepository multiplayerSessionRepository;


    public List<QuestionDTO> getThreeQuestionsByCategory(String category, Long sessionId) {
        List<Question> questions = questionRepository.findThreeRandomQuestionsByCategory(category);
        multiplayerService.updateSessionQuestionsAndCategory(sessionId, questions, category);

        return QuestionMapper.multipleToDTO(questions);
    }

    public AnswerValidationResponse validateMultiplayerAnswer(MultiplayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
       
        AnswerValidationResponse validationResponse = validateAnswer(request.getQuestionId(), request.getAnswer());
        
        multiplayerService.updateGameState(
                request.getSessionId(),
                request.getPlayerId(),
                request.getQuestionId(),
                validationResponse.isCorrect()
        );
        
        return validationResponse;
        
    }

    private AnswerValidationResponse validateAnswer(Long questionId, String answer) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        boolean isCorrect;

        if (answer == null) {
            isCorrect = false;
        } else {
            isCorrect = question.getCorrectAnswer().equals(answer);
        }
        int indexOfCorrectAnswer = question.getOptions().indexOf(question.getCorrectAnswer());

        return new AnswerValidationResponse(isCorrect, indexOfCorrectAnswer);
        
    }

    public List<QuestionDTO> getQuestionsForSession(Long sessionId) {
        MultiplayerSession session = multiplayerSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        List<Long> questionIds = session.getQuestionIds();

        return QuestionMapper.multipleToDTO(questionRepository.findAllById(questionIds));
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

    public List<QuestionDTO> getCurrentQuestions(List<Long> currentQuestionIds) {
        return QuestionMapper.multipleToDTO(questionRepository.findAllById(currentQuestionIds));
    }

    public List<QuestionDTO> getFiveQuestionsByCategory(String category) {
        return QuestionMapper.multipleToDTO(questionRepository.findFiveRandomQuestionsByCategory(category));
    }


    public AnswerValidationResponse validateSingleplayerAnswer(SinglePlayerAnswerValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        AnswerValidationResponse validationResponse = validateAnswer(request.getQuestionId(), request.getAnswer());

        
        //TODO handle singleplayer logic
        
        
        
        
        return validationResponse;
    }
}
