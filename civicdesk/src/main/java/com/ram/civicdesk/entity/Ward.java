package com.ram.civicdesk.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "wards")
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ward_number", nullable = false, unique = true)
    private Integer wardNumber;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    public Ward() {}

    public Long getId() { return id; }
    public Integer getWardNumber() { return wardNumber; }
    public String getName() { return name; }
    public Zone getZone() { return zone; }
}
