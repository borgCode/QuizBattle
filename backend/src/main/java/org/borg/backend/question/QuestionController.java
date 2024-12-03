package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/questions")
public class QuestionController {
    
    private final QuestionService questionService;


    @GetMapping("/{category}")
    public List<QuestionDTO> getThreeQuestionsByCategory(@PathVariable String category) {
        return questionService.getThreeQuestionsByCategory(category);
    }

    @PostMapping("/validate-answer")
    public ResponseEntity<AnswerValidationResponse> validateAnswer(@RequestBody AnswerValidationRequest request) {
        return ResponseEntity.ok(questionService.validateAnswer(request));
    }
}
