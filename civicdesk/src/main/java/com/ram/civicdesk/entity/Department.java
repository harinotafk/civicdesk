package com.ram.civicdesk.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    public Department() {}

    public Integer getId() { return id; }
    public String getName() { return name; }
}
