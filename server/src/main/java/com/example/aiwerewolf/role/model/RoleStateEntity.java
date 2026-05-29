package com.example.aiwerewolf.role.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_states", indexes = @Index(name = "idx_role_states_player", columnList = "roomId,playerId"))
public class RoleStateEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String roomId;
    @Column(nullable = false)
    private String playerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Role role;
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String stateJson = "{}";
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
