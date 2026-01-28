package zw.co.zivai.core_backend.models.base;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "origin_node_id")
    private UUID originNodeId;

    @Column(name = "sync_version", nullable = false)
    private Long syncVersion = 0L;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (syncVersion == null) {
            syncVersion = 0L;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (syncVersion == null) {
            syncVersion = 0L;
        }
        syncVersion += 1;
    }
}
