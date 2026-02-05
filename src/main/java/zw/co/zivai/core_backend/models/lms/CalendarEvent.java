package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "calendar_events", schema = "lms")
public class CalendarEvent extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "all_day", nullable = false)
    private boolean allDay = false;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private String location;

    @Column(name = "recurring")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode recurring;

    @Column(name = "reminders")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode reminders;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(nullable = false)
    private String status = "active";
}
