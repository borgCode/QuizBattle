package org.borg.backend.question;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

//    @PostMapping("/validate-answer")
//    public ResponseEntity<Boolean> validateAnswer()
}
