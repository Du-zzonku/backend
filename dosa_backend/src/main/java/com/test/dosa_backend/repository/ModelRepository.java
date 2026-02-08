package com.test.dosa_backend.repository;

import com.test.dosa_backend.domain.Model;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, String> {

    Slice<Model> findAllBy(Pageable pageable);

    @Query("SELECT m.title FROM Model m WHERE m.modelId = :id")
    String findTitleByModelId(@Param("id") String modelId);

    @Query("SELECT m.overview FROM Model m WHERE m.modelId = :id")
    String findOverviewByModelId(@Param("id") String modelId);

    @Query("SELECT m.theory FROM Model m WHERE m.modelId = :id")
    String findTheoryByModelId(@Param("id") String modelId);

}
