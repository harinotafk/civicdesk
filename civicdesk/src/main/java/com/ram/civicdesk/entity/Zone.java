package com.ram.civicdesk.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "zones")
public class Zone {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    public Zone() {}

    public Integer getId() { return id; }
    public String getName() { return name; }
}
