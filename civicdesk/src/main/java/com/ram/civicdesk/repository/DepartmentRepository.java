package com.ram.civicdesk.repository;

import com.ram.civicdesk.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
