package org.borg.backend.game.shared.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.borg.backend.game.multiplayer.dto.MultiplayerAnswerValidationRequest;
import org.borg.backend.game.multiplayer.dto.MultiplayerQuestionsRequest;
import org.borg.backend.game.multiplayer.service.MultiplayerQuestionService;
import org.borg.backend.game.shared.service.RoundSessionService;
import org.borg.backend.game.singleplayer.dto.ChapterRoundResults;
import org.borg.backend.game.singleplayer.dto.SinglePlayerAnswerValidationRequest;
import org.borg.backend.game.singleplayer.service.SinglePlayerQuestionService;
import org.borg.backend.game.shared.dto.AnswerValidationResponse;
import org.borg.backend.game.shared.dto.QuestionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/questions")
@Tag(name = "Questions")
public class QuestionController {
    
    private final SinglePlayerQuestionService singlePlayerQuestionService;
    private final MultiplayerQuestionService multiplayerQuestionService;
    private final RoundSessionService roundSessionService;

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/multiplayer/{sessionId}/categories")
    public ResponseEntity<List<String>> getThreeRandomCategories(@PathVariable long sessionId, @RequestParam long playerId) {
        return ResponseEntity.ok(multiplayerQuestionService.getThreeRandomCategories(sessionId, playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/multiplayer/{playerId}/clear")
    public ResponseEntity<Void> clearPlayerSession(@PathVariable long playerId) {
        roundSessionService.finishSession(playerId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.playerId)")
    @PostMapping("/multiplayer/questions")
    public ResponseEntity<List<QuestionDTO>> getNewQuestionsForCategory(@RequestBody MultiplayerQuestionsRequest request) {
        return ResponseEntity.ok(multiplayerQuestionService.getNewQuestionsForCategory(request));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.playerId)")
    @PostMapping("/multiplayer/answer")
    public ResponseEntity<AnswerValidationResponse> validateMultiplayerAnswer(@RequestBody MultiplayerAnswerValidationRequest request) {
        return ResponseEntity.ok(multiplayerQuestionService.validateMultiplayerAnswer(request));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/multiplayer/{sessionId}/{playerId}")
    public ResponseEntity<List<QuestionDTO>> getActiveSessionQuestions(@PathVariable long sessionId, @PathVariable long playerId) {
        return ResponseEntity.ok(multiplayerQuestionService.getActiveSessionQuestions(sessionId, playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/{playerId}/restore")
    public ResponseEntity<List<QuestionDTO>> restoreSessionQuestions(@PathVariable long playerId) {
        return ResponseEntity.ok(roundSessionService.restoreSessionQuestions(playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @PostMapping("/chapter/random-questions")
    public ResponseEntity<List<QuestionDTO>> getSinglePlayerRoundQuestions(@RequestParam long playerId) {
        return ResponseEntity.ok(singlePlayerQuestionService.getSinglePlayerRoundQuestions(playerId));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#request.playerId)")
    @PostMapping("/chapter/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateSingleplayerAnswer(@RequestBody SinglePlayerAnswerValidationRequest request) {
        return ResponseEntity.ok(singlePlayerQuestionService.validateSingleplayerAnswer(request));
    }

    @PreAuthorize("@customSecurityExpression.isPlayerOwner(#playerId)")
    @GetMapping("/chapter/results/{playerId}")
    public ResponseEntity<ChapterRoundResults> getRoundResults(@PathVariable long playerId) {
        return ResponseEntity.ok(singlePlayerQuestionService.getRoundResults(playerId));
    }

}
