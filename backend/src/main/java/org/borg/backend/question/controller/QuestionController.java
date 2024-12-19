package org.borg.backend.question.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.question.dto.AnswerValidationResponse;
import org.borg.backend.question.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.question.dto.QuestionDTO;
import org.borg.backend.question.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.question.service.QuestionService;
import org.borg.backend.question.service.QuestionSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/questions")
@Tag(name = "Questions")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionSessionService questionSessionService;

    @GetMapping("/{sessionId}/category-selection")
    public ResponseEntity<List<String>> getThreeRandomCategories(@PathVariable Long sessionId) {
        return ResponseEntity.ok(questionService.getThreeRandomCategories(sessionId));
    }

    @GetMapping("/session/random-questions")
    public ResponseEntity<List<QuestionDTO>> getThreeQuestionsByCategory(
            @RequestParam String category,
            @RequestParam Long sessionId) {
        return ResponseEntity.ok(questionService.getThreeQuestionsByCategory(category, sessionId));
    }

    @PostMapping("/session/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateMultiplayerAnswer(@RequestBody MultiplayerAnswerValidationRequest request) {
        return ResponseEntity.ok(questionService.validateMultiplayerAnswer(request));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<QuestionDTO>> getSessionQuestions(@PathVariable Long sessionId) {
        return ResponseEntity.ok(questionService.getQuestionsForSession(sessionId));
    }
    
    @GetMapping("/session/current-questions")
    public ResponseEntity<List<QuestionDTO>> getCurrentQuestions(@RequestParam List<Long> currentQuestionIds) {
        return ResponseEntity.ok(questionService.getCurrentQuestions(currentQuestionIds));
    }
    @GetMapping("/chapter/random-questions/{category}")
    public ResponseEntity<List<QuestionDTO>> getFiveQuestionsByCategory(@PathVariable String category) {
        log.warn("Controller");
        return ResponseEntity.ok(questionService.getFiveQuestionsByCategory(category));
    }

    @PostMapping("/chapter/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateSingleplayerAnswer(@RequestBody SinglePlayerAnswerValidationRequest request) {
        return ResponseEntity.ok(questionService.validateSingleplayerAnswer(request));
    }
    
    @GetMapping("/chapter/results/{playerId}")
    public ResponseEntity<List<Boolean>> getRoundResults(@PathVariable Long playerId) {
        return ResponseEntity.ok(questionSessionService.getSessionAnswers(playerId));
    }
    
    @PostMapping("chapter/clear-answers/{playerId}")
    public ResponseEntity<Void> clearRoundResults(@PathVariable Long playerId) {
        questionSessionService.finishSession(playerId);
        return ResponseEntity.ok().build();
    }
}
