package com.matillion.techtest2025.repository;

import com.matillion.techtest2025.repository.entity.ColumnStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for database operations on
 * {@link ColumnStatisticsEntity}.
 * Extends {@link JpaRepository} which provides standard CRUD operations (save,
 * findById, findAll,
 * deleteById, etc.) without requiring implementation code. Spring Data JPA
 * generates the
 * implementation automatically at runtime.
 */
@Repository
public interface ColumnStatisticsRepository extends JpaRepository<ColumnStatisticsEntity, Long> {
}
