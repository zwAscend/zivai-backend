package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_profiles", schema = "lms")
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "grade_level")
    private String gradeLevel;

    @Column(name = "exam_board_code")
    private String examBoardCode;

    private Double overall;
    private String engagement;
    private String strength;
    private String performance;

    @OneToOne
    @JoinColumn(name = "active_student_plan_id")
    private StudentPlan activeStudentPlan;

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
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
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
