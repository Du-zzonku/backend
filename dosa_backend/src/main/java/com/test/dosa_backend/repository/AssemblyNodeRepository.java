package com.test.dosa_backend.repository;

import com.test.dosa_backend.domain.AssemblyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssemblyNodeRepository extends JpaRepository<AssemblyNode, String> {
    @Query("SELECT n FROM AssemblyNode n WHERE n.model.modelId = :modelId")
    List<AssemblyNode> findAllByModelId(@Param("modelId") String modelId);
}
