package org.borg.backend.question;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateAnswer(@RequestBody AnswerValidationRequest request) {
        return ResponseEntity.ok(questionService.validateAnswer(request));
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
}
