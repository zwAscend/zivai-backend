package zw.co.zivai.core_backend.configs;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.CalendarEvent;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.models.lookups.Role;
import zw.co.zivai.core_backend.repositories.ClassRepository;
import zw.co.zivai.core_backend.repositories.CalendarEventRepository;
import zw.co.zivai.core_backend.repositories.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.RoleRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_SEEDED_PASSWORD = "TempPass123!";

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            ensureAuthSchema();
            seedExamBoards();
            seedEnrolmentStatuses();
            seedAssessmentEnrollmentStatuses();
            seedGradingStatuses();
            seedQuestionTypes();

            Role student = roleRepository.findByCode("student")
                .orElseGet(() -> roleRepository.save(buildRole("student", "Student")));
            Role teacher = roleRepository.findByCode("teacher")
                .orElseGet(() -> roleRepository.save(buildRole("teacher", "Teacher")));
            Role admin = roleRepository.findByCode("admin")
                .orElseGet(() -> roleRepository.save(buildRole("admin", "Admin")));

            seedUser("teacher@zivai.local", "263712000001", "teacher1", "Tariro", "Moyo", List.of(teacher));
            seedUser("student@zivai.local", "263712000002", "student1", "Tinashe", "Dube", List.of(student));
            seedUser("admin@zivai.local", "263712000003", "admin1", "Admin", "User", List.of(admin));

            User teacherUser = userRepository.findByEmail("teacher@zivai.local").orElse(null);
            User studentUser = userRepository.findByEmail("student@zivai.local").orElse(null);

            School school = schoolRepository.findByCode("ZVHS")
                .orElseGet(() -> {
                    School created = new School();
                    created.setCode("ZVHS");
                    created.setName("zivAI High School");
                    created.setCountryCode("ZW");
                    return schoolRepository.save(created);
                });

            seedSubject("MATH", "Mathematics", "Core maths for Form 1-4");
            seedSubject("ENG", "English Language", "Reading, writing, and comprehension");
            seedSubject("PHY", "Physics", "Mechanics, waves, and electricity");

            if (teacherUser != null) {
                ClassEntity classEntity = classRepository.findByCode("FORM2-A")
                    .orElseGet(() -> {
                        ClassEntity created = new ClassEntity();
                        created.setSchool(school);
                        created.setCode("FORM2-A");
                        created.setName("Form 2A");
                        created.setGradeLevel("Form 2");
                        created.setAcademicYear("2026");
                        created.setHomeroomTeacher(teacherUser);
                        return classRepository.save(created);
                    });

                if (studentUser != null && enrolmentRepository
                    .findByClassEntity_IdAndStudent_Id(classEntity.getId(), studentUser.getId())
                    .isEmpty()) {
                    Enrolment enrolment = new Enrolment();
                    enrolment.setClassEntity(classEntity);
                    enrolment.setStudent(studentUser);
                    enrolment.setEnrolmentStatusCode("active");
                        enrolmentRepository.save(enrolment);
                }
            }

            if (teacherUser != null) {
                seedCalendarEvents(school, teacherUser);
            }
        };
    }

    private Role buildRole(String code, String name) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        return role;
    }

    private void seedUser(String email, String phoneNumber, String username, String firstName, String lastName, List<Role> roles) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()) {
                existing.setPasswordHash(passwordEncoder.encode(DEFAULT_SEEDED_PASSWORD));
                userRepository.save(existing);
            }
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_SEEDED_PASSWORD));
        user.getRoles().addAll(roles);
        userRepository.save(user);
    }

    private void ensureAuthSchema() {
        jdbcTemplate.update(
            "ALTER TABLE IF EXISTS lms.users ADD COLUMN IF NOT EXISTS password_hash varchar(255)");
    }

    private void seedSubject(String code, String name, String description) {
        if (subjectRepository.findByCode(code).isPresent()) {
            return;
        }
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        subject.setDescription(description);
        subject.setActive(true);
        subjectRepository.save(subject);
    }

    private void seedCalendarEvents(School school, User teacher) {
        if (calendarEventRepository.count() > 0) {
            return;
        }

        List<Subject> subjects = subjectRepository.findAll();
        Subject math = subjects.stream().filter(s -> "MATH".equalsIgnoreCase(s.getCode())).findFirst().orElse(null);
        Subject eng = subjects.stream().filter(s -> "ENG".equalsIgnoreCase(s.getCode())).findFirst().orElse(null);
        Subject phy = subjects.stream().filter(s -> "PHY".equalsIgnoreCase(s.getCode())).findFirst().orElse(null);

        CalendarEvent lesson = new CalendarEvent();
        lesson.setSchool(school);
        lesson.setTitle("Mathematics Lesson");
        lesson.setDescription("Introduction to algebraic expressions");
        lesson.setStartTime(java.time.Instant.now().plusSeconds(2 * 60 * 60));
        lesson.setEndTime(java.time.Instant.now().plusSeconds(3 * 60 * 60));
        lesson.setEventType("lesson");
        lesson.setSubject(math);
        lesson.setLocation("Room 101");
        lesson.setCreatedBy(teacher);
        calendarEventRepository.save(lesson);

        CalendarEvent assignmentDue = new CalendarEvent();
        assignmentDue.setSchool(school);
        assignmentDue.setTitle("English Assignment Due");
        assignmentDue.setDescription("Reading comprehension worksheet");
        assignmentDue.setStartTime(java.time.Instant.now().plusSeconds(24 * 60 * 60));
        assignmentDue.setAllDay(true);
        assignmentDue.setEventType("assignment_due");
        assignmentDue.setSubject(eng);
        assignmentDue.setLocation("Classroom");
        assignmentDue.setCreatedBy(teacher);
        calendarEventRepository.save(assignmentDue);

        CalendarEvent labSession = new CalendarEvent();
        labSession.setSchool(school);
        labSession.setTitle("Physics Lab Session");
        labSession.setDescription("Intro to basic circuits");
        labSession.setStartTime(java.time.Instant.now().plusSeconds(3 * 24 * 60 * 60));
        labSession.setEndTime(java.time.Instant.now().plusSeconds(3 * 24 * 60 * 60 + 2 * 60 * 60));
        labSession.setEventType("lab");
        labSession.setSubject(phy);
        labSession.setLocation("Lab 2");
        labSession.setCreatedBy(teacher);
        calendarEventRepository.save(labSession);

        CalendarEvent quiz = new CalendarEvent();
        quiz.setSchool(school);
        quiz.setTitle("Mathematics Quiz");
        quiz.setDescription("Algebra basics quiz");
        quiz.setStartTime(java.time.Instant.now().plusSeconds(5 * 24 * 60 * 60));
        quiz.setEndTime(java.time.Instant.now().plusSeconds(5 * 24 * 60 * 60 + 60 * 60));
        quiz.setEventType("quiz");
        quiz.setSubject(math);
        quiz.setLocation("Room 101");
        quiz.setCreatedBy(teacher);
        calendarEventRepository.save(quiz);
    }


    private void seedEnrolmentStatuses() {
        jdbcTemplate.update(
            "INSERT INTO lookups.enrolment_status (code, name) VALUES ('active', 'Active') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.enrolment_status (code, name) VALUES ('dropped', 'Dropped') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.enrolment_status (code, name) VALUES ('completed', 'Completed') ON CONFLICT (code) DO NOTHING");
    }

    private void seedExamBoards() {
        jdbcTemplate.update(
            "INSERT INTO lookups.exam_board (code, name) VALUES ('zimsec', 'ZIMSEC') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.exam_board (code, name) VALUES ('cambridge', 'CAMBRIDGE') ON CONFLICT (code) DO NOTHING");
    }

    private void seedAssessmentEnrollmentStatuses() {
        jdbcTemplate.update(
            "INSERT INTO lookups.assessment_enrollment_status (code, name) VALUES ('assigned', 'Assigned') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.assessment_enrollment_status (code, name) VALUES ('completed', 'Completed') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.assessment_enrollment_status (code, name) VALUES ('late', 'Late') ON CONFLICT (code) DO NOTHING");
    }

    private void seedGradingStatuses() {
        jdbcTemplate.update(
            "INSERT INTO lookups.grading_status (code, name) VALUES ('pending', 'Pending') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.grading_status (code, name) VALUES ('auto_graded', 'Auto Graded') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.grading_status (code, name) VALUES ('reviewed', 'Reviewed') ON CONFLICT (code) DO NOTHING");
    }

    private void seedQuestionTypes() {
        jdbcTemplate.update(
            "INSERT INTO lookups.question_type (code, name) VALUES ('short_answer', 'Short Answer') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.question_type (code, name) VALUES ('structured', 'Structured') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.question_type (code, name) VALUES ('mcq', 'Multiple Choice') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.question_type (code, name) VALUES ('true_false', 'True/False') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.question_type (code, name) VALUES ('essay', 'Essay') ON CONFLICT (code) DO NOTHING");
    }
}
