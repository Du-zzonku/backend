package com.test.dosa_backend.repository;

import com.test.dosa_backend.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartRepository extends JpaRepository<Part, String> {
    @Query("SELECT p FROM Part p WHERE p.model.modelId = :modelId")
    List<Part> findAllByModelId(@Param("modelId") String modelId);
}
