package org.borg.backend.game.shared.repository;

import org.borg.backend.game.shared.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RAND() LIMIT 3", nativeQuery = true)
    List<Question> findThreeRandomQuestionsByCategory(@Param("category") String category);

    @Query(value = "SELECT DISTINCT question.category FROM Question question")
    List<String> findAllCategories();


    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RAND() LIMIT 5", nativeQuery = true)
    List<Question> findFiveRandomQuestionsByCategory(@Param("category") String category);

    List<Question> findAllById(Iterable<Long> ids);

    default List<Question> findQuestionsOrdered(List<Long> ids) {
        List<Question> entities = findAllById(ids);
        Map<Long, Question> entityMap = entities.stream()
                .collect(Collectors.toMap(Question::getId, entity -> entity));
        return ids.stream()
                .map(entityMap::get)
                .collect(Collectors.toList());
    }
}
