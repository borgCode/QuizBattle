package org.borg.backend.question.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.service.MultiplayerQuestionService;
import org.borg.backend.game.singleplayer.SinglePlayerQuestionService;
import org.borg.backend.question.dto.*;
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
    
    private final QuestionSessionService questionSessionService;
    private final SinglePlayerQuestionService singlePlayerQuestionService;
    private final MultiplayerQuestionService multiplayerQuestionService;

    @GetMapping("/multiplayer/{sessionId}/categories")
    public ResponseEntity<List<String>> getThreeRandomCategories(@PathVariable Long sessionId) {
        return ResponseEntity.ok(multiplayerQuestionService.getThreeRandomCategories(sessionId));
    }
    
    @GetMapping("/multiplayer/{playerId}/restore")
    public ResponseEntity<List<QuestionDTO>> restoreSessionQuestions(@PathVariable Long playerId) {
        return ResponseEntity.ok(multiplayerQuestionService.restoreSessionQuestions(playerId));
    }
    
    @PostMapping("/multiplayer/{playerId}/clear")
    public ResponseEntity<Void> clearPlayerSession(@PathVariable Long playerId) {
        questionSessionService.finishSession(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/multiplayer/questions")
    public ResponseEntity<List<QuestionDTO>> getNewQuestionsForCategory(@RequestBody MultiplayerQuestionsRequest request) {
        return ResponseEntity.ok(multiplayerQuestionService.getNewQuestionsForCategory(request));
    }

    @PostMapping("/multiplayer/answer")
    public ResponseEntity<AnswerValidationResponse> validateMultiplayerAnswer(@RequestBody MultiplayerAnswerValidationRequest request) {
        return ResponseEntity.ok(multiplayerQuestionService.validateMultiplayerAnswer(request));
    }

    @GetMapping("/multiplayer/{sessionId}/{playerId}")
    public ResponseEntity<List<QuestionDTO>> getActiveSessionQuestions(@PathVariable Long sessionId, @PathVariable Long playerId) {
        return ResponseEntity.ok(multiplayerQuestionService.getActiveSessionQuestions(sessionId, playerId));
    }
    
    @PostMapping("/chapter/random-questions")
    public ResponseEntity<List<QuestionDTO>> getSinglePlayerRoundQuestions(@RequestBody SingleplayerQuestionsRequest request) {
        return ResponseEntity.ok(singlePlayerQuestionService.getSinglePlayerRoundQuestions(request));
    }

    @PostMapping("/chapter/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateSingleplayerAnswer(@RequestBody SinglePlayerAnswerValidationRequest request) {
        return ResponseEntity.ok(singlePlayerQuestionService.validateSingleplayerAnswer(request));
    }
    
    @GetMapping("/chapter/results/{playerId}")
    public ResponseEntity<List<Boolean>> getRoundResults(@PathVariable Long playerId) {
        return ResponseEntity.ok(questionSessionService.getSessionAnswers(playerId));
    }
    
    @PostMapping("chapter/clear-answers/{playerId}")
    public ResponseEntity<Void> clearRoundResults(@PathVariable Long playerId) {
        singlePlayerQuestionService.finishSession(playerId);
        return ResponseEntity.ok().build();
    }
}
