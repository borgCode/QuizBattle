package org.borg.backend.question;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/questions")
@Tag(name="Questions")
public class QuestionController {
    
    private final QuestionService questionService;


    @GetMapping("/{category}")
    public ResponseEntity<List<QuestionDTO>> getThreeQuestionsByCategory(@RequestBody CategorySelectionRequest request) {
        return ResponseEntity.ok(questionService.getThreeQuestionsByCategory(request));
    }

    @PostMapping("/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateAnswer(@RequestBody AnswerValidationRequest request) {
        return ResponseEntity.ok(questionService.validateAnswer(request));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<QuestionDTO>> getSessionQuestions(@PathVariable Long sessionId) {
        return ResponseEntity.ok(questionService.getQuestionsForSession(sessionId));
    }
}
