package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarEventDto {
    private String id;
    private String title;
    private String description;
    private Instant start;
    private Instant end;
    private boolean allDay;
    private String type;
    private String subjectId;
    private String subjectName;
    private String location;
    private Object recurring;
    private Object reminders;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
