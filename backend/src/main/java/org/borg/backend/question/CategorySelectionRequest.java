package org.borg.backend.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CategorySelectionRequest {
    private String selectedCategory;
    private Long sessionId;
}
