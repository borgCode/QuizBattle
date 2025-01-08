package org.borg.backend.question.repository;

import org.borg.backend.question.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RAND() LIMIT 3", nativeQuery = true)
    List<Question> findThreeRandomQuestionsByCategory(@Param("category") String category);

    @Query(value = "SELECT DISTINCT question.category FROM Question question")
    List<String> findAllCategories();

    List<Question> findAllById(Iterable<Long> ids);
    

    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RAND() LIMIT 5", nativeQuery = true)
    List<Question> findFiveRandomQuestionsByCategory(@Param("category") String category);
}
