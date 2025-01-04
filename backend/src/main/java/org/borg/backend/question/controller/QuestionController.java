package org.borg.backend.question.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.question.dto.*;
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
    
    @GetMapping("/{playerId}/questions")
    public ResponseEntity<List<QuestionDTO>> getPlayerSessionQuestions(@PathVariable Long playerId) {
        return ResponseEntity.ok(questionService.getPlayerSessionQuestions(playerId));
    }
    
    @PostMapping("/{playerId}/clear")
    public ResponseEntity<Void> clearPlayerSession(@PathVariable Long playerId) {
        questionSessionService.finishSession(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/random-questions")
    public ResponseEntity<List<QuestionDTO>> getNewQuestionsForCategory(@RequestBody MultiplayerQuestionsRequest request) {
        return ResponseEntity.ok(questionService.getNewQuestionsForCategory(request));
    }

    @PostMapping("/session/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateMultiplayerAnswer(@RequestBody MultiplayerAnswerValidationRequest request) {
        return ResponseEntity.ok(questionService.validateMultiplayerAnswer(request));
    }

    @GetMapping("/session/{sessionId}/{playerId}")
    public ResponseEntity<List<QuestionDTO>> getActiveSessionQuestions(@PathVariable Long sessionId, @PathVariable Long playerId) {
        return ResponseEntity.ok(questionService.getActiveSessionQuestions(sessionId, playerId));
    }
    
    @PostMapping("/chapter/random-questions")
    public ResponseEntity<List<QuestionDTO>> getSinglePlayerRoundQuestions(@RequestBody SingleplayerQuestionsRequest request) {
        return ResponseEntity.ok(questionService.getSinglePlayerRoundQuestions(request));
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
        questionService.finishSession(playerId);
        return ResponseEntity.ok().build();
    }
}
