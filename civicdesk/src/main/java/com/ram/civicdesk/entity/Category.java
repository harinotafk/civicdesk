package com.ram.civicdesk.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "sla_hours", nullable = false)
    private Integer slaHours;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id")
    private Department department;

    public Category() {}

    public Integer getId() { return id; }
    public String getName() { return name; }
    public Integer getSlaHours() { return slaHours; }
    public Department getDepartment() { return department; }
}
