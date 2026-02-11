package zw.co.zivai.core_backend.dtos.calendar;

import lombok.Data;

@Data
public class CalendarEventRequest {
    private String title;
    private String description;
    private String start;
    private String end;
    private boolean allDay;
    private String type;
    private String subjectId;
    private String classId;
    private String location;
    private Object recurring;
    private Object reminders;
    private String createdBy;
    private String schoolId;
}
