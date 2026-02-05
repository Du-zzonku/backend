package com.test.dosa_backend.repository;

import com.test.dosa_backend.domain.Model;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, String> {

    Slice<Model> findAllBy(Pageable pageable);
}
