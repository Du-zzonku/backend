package com.test.dosa_backend.repository;

import com.test.dosa_backend.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 이미 푼 문제(excludedIds)는 제외하고 랜덤으로 count만큼 가져오기
    @Query(value = "SELECT * FROM question q " +
                   "WHERE q.model_id = :modelId " +
                   "AND q.question_id NOT IN (:excludedIds) " +
                   "ORDER BY RANDOM() " +
                   "LIMIT :count", nativeQuery = true)
    List<Question> findRandomQuestions(@Param("modelId") String modelId,
                                       @Param("excludedIds") List<Long> excludedIds,
                                       @Param("count") int count);
}