package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    @EntityGraph(attributePaths = {"department"})
    List<Category> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"department"})
    Optional<Category> findWithDepartmentById(Integer id);
}
