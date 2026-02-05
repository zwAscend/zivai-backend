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
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.Plan;
import zw.co.zivai.core_backend.models.lms.PlanSkill;
import zw.co.zivai.core_backend.models.lms.PlanStep;
import zw.co.zivai.core_backend.models.lms.PlanSubskill;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Skill;
import zw.co.zivai.core_backend.models.lms.StudentAttribute;
import zw.co.zivai.core_backend.models.lms.StudentPlan;
import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.models.lookups.Role;
import zw.co.zivai.core_backend.repositories.ClassRepository;
import zw.co.zivai.core_backend.repositories.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.CalendarEventRepository;
import zw.co.zivai.core_backend.repositories.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.PlanRepository;
import zw.co.zivai.core_backend.repositories.PlanSkillRepository;
import zw.co.zivai.core_backend.repositories.PlanStepRepository;
import zw.co.zivai.core_backend.repositories.PlanSubskillRepository;
import zw.co.zivai.core_backend.repositories.RoleRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.SkillRepository;
import zw.co.zivai.core_backend.repositories.StudentAttributeRepository;
import zw.co.zivai.core_backend.repositories.StudentPlanRepository;
import zw.co.zivai.core_backend.repositories.StudentSubjectEnrolmentRepository;
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
    private final ClassSubjectRepository classSubjectRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final SkillRepository skillRepository;
    private final StudentAttributeRepository studentAttributeRepository;
    private final PlanRepository planRepository;
    private final PlanStepRepository planStepRepository;
    private final PlanSkillRepository planSkillRepository;
    private final PlanSubskillRepository planSubskillRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
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

            Subject mathSubject = subjectRepository.findByCode("MATH").orElse(null);
            Subject engSubject = subjectRepository.findByCode("ENG").orElse(null);

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

                if (mathSubject != null) {
                    ClassSubject mathLink = seedClassSubject(school, classEntity, mathSubject, teacherUser, "2026", "Term 1");
                    seedStudentSubjectEnrolment(studentUser, mathLink);
                }
                if (engSubject != null) {
                    ClassSubject engLink = seedClassSubject(school, classEntity, engSubject, teacherUser, "2026", "Term 1");
                    seedStudentSubjectEnrolment(studentUser, engLink);
                }
            }

            if (teacherUser != null) {
                seedCalendarEvents(school, teacherUser);
            }

            if (studentUser != null) {
                seedDevelopmentData(mathSubject, engSubject, studentUser);
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

    private void seedDevelopmentData(Subject math, Subject eng, User student) {
        if (math == null || eng == null) {
            return;
        }

        Skill algebra = seedSkill(math, "ALG", "Algebra Fundamentals", "Linear equations and expressions");
        Skill geometry = seedSkill(math, "GEO", "Geometry Basics", "Angles, shapes, and measurement");
        Skill statistics = seedSkill(math, "STAT", "Statistics", "Mean, median, and probability");
        Skill comprehension = seedSkill(eng, "COMP", "Comprehension", "Reading and interpretation");
        Skill writing = seedSkill(eng, "WRIT", "Writing Skills", "Grammar and structured writing");

        seedStudentAttribute(student, algebra, 62.0, 82.0);
        seedStudentAttribute(student, geometry, 68.0, 84.0);
        seedStudentAttribute(student, statistics, 55.0, 78.0);
        seedStudentAttribute(student, comprehension, 72.0, 86.0);
        seedStudentAttribute(student, writing, 65.0, 83.0);

        if (planRepository.findBySubject_Id(math.getId()).isEmpty()) {
            Plan mathPlan = new Plan();
            mathPlan.setSubject(math);
            mathPlan.setName("Math Growth Plan");
            mathPlan.setDescription("Focus on algebra and geometry fundamentals.");
            mathPlan.setProgress(0.0);
            mathPlan.setPotentialOverall(85.0);
            mathPlan.setEtaDays(30);
            mathPlan.setPerformance("Average");
            mathPlan = planRepository.save(mathPlan);

            PlanSkill algebraSkill = seedPlanSkill(mathPlan, algebra.getName(), 85.0);
            seedPlanSubskill(algebraSkill, "Linear equations", 60.0, "yellow");
            seedPlanSubskill(algebraSkill, "Simplifying expressions", 55.0, "yellow");

            PlanSkill geometrySkill = seedPlanSkill(mathPlan, geometry.getName(), 82.0);
            seedPlanSubskill(geometrySkill, "Angles and triangles", 65.0, "yellow");
            seedPlanSubskill(geometrySkill, "Area and perimeter", 60.0, "yellow");

            seedPlanStep(mathPlan, "Review algebra basics", "reading", 1, "https://example.com/algebra");
            seedPlanStep(mathPlan, "Practice geometry drills", "exercise", 2, null);
            seedPlanStep(mathPlan, "Weekly mastery quiz", "assessment", 3, null);

            seedStudentPlan(student, mathPlan, math);
        }

        if (planRepository.findBySubject_Id(eng.getId()).isEmpty()) {
            Plan engPlan = new Plan();
            engPlan.setSubject(eng);
            engPlan.setName("English Mastery Plan");
            engPlan.setDescription("Improve comprehension and writing fluency.");
            engPlan.setProgress(0.0);
            engPlan.setPotentialOverall(88.0);
            engPlan.setEtaDays(28);
            engPlan.setPerformance("Good");
            engPlan = planRepository.save(engPlan);

            PlanSkill compSkill = seedPlanSkill(engPlan, comprehension.getName(), 88.0);
            seedPlanSubskill(compSkill, "Inference", 70.0, "yellow");
            seedPlanSubskill(compSkill, "Summary writing", 65.0, "yellow");

            PlanSkill writingSkill = seedPlanSkill(engPlan, writing.getName(), 90.0);
            seedPlanSubskill(writingSkill, "Essay structure", 68.0, "yellow");
            seedPlanSubskill(writingSkill, "Grammar practice", 72.0, "yellow");

            seedPlanStep(engPlan, "Read short story", "reading", 1, null);
            seedPlanStep(engPlan, "Write a paragraph response", "assignment", 2, null);
            seedPlanStep(engPlan, "Weekly feedback session", "meeting", 3, null);

            seedStudentPlan(student, engPlan, eng);
        }
    }

    private Skill seedSkill(Subject subject, String code, String name, String description) {
        return skillRepository.findBySubject_IdAndCode(subject.getId(), code)
            .orElseGet(() -> {
                Skill skill = new Skill();
                skill.setSubject(subject);
                skill.setCode(code);
                skill.setName(name);
                skill.setDescription(description);
                return skillRepository.save(skill);
            });
    }

    private ClassSubject seedClassSubject(
        School school,
        ClassEntity classEntity,
        Subject subject,
        User teacher,
        String academicYear,
        String term
    ) {
        if (school == null || classEntity == null || subject == null) {
            return null;
        }
        return classSubjectRepository.findByClassEntity_IdAndDeletedAtIsNull(classEntity.getId()).stream()
            .filter(link -> link.getSubject() != null && link.getSubject().getId().equals(subject.getId()))
            .findFirst()
            .orElseGet(() -> {
                ClassSubject classSubject = new ClassSubject();
                classSubject.setSchool(school);
                classSubject.setClassEntity(classEntity);
                classSubject.setSubject(subject);
                classSubject.setTeacher(teacher);
                classSubject.setAcademicYear(academicYear);
                classSubject.setTerm(term);
                classSubject.setName(subject.getName());
                classSubject.setActive(true);
                return classSubjectRepository.save(classSubject);
            });
    }

    private void seedStudentSubjectEnrolment(User student, ClassSubject classSubject) {
        if (student == null || classSubject == null) {
            return;
        }
        boolean exists = studentSubjectEnrolmentRepository
            .findByStudent_IdAndDeletedAtIsNull(student.getId())
            .stream()
            .anyMatch(enrolment -> enrolment.getClassSubject() != null
                && enrolment.getClassSubject().getId().equals(classSubject.getId()));
        if (exists) {
            return;
        }
        StudentSubjectEnrolment enrolment = new StudentSubjectEnrolment();
        enrolment.setStudent(student);
        enrolment.setClassSubject(classSubject);
        enrolment.setStatusCode("active");
        studentSubjectEnrolmentRepository.save(enrolment);
    }

    private void seedStudentAttribute(User student, Skill skill, double current, double potential) {
        if (studentAttributeRepository.findByStudent_IdAndSkill_Id(student.getId(), skill.getId()).isPresent()) {
            return;
        }
        StudentAttribute attribute = new StudentAttribute();
        attribute.setStudent(student);
        attribute.setSkill(skill);
        attribute.setCurrentScore(current);
        attribute.setPotentialScore(potential);
        attribute.setLastAssessed(java.time.Instant.now());
        studentAttributeRepository.save(attribute);
    }

    private PlanSkill seedPlanSkill(Plan plan, String name, Double score) {
        PlanSkill planSkill = new PlanSkill();
        planSkill.setPlan(plan);
        planSkill.setName(name);
        planSkill.setScore(score);
        return planSkillRepository.save(planSkill);
    }

    private void seedPlanSubskill(PlanSkill planSkill, String name, Double score, String color) {
        PlanSubskill subskill = new PlanSubskill();
        subskill.setPlanSkill(planSkill);
        subskill.setName(name);
        subskill.setScore(score);
        subskill.setColor(color);
        planSubskillRepository.save(subskill);
    }

    private void seedPlanStep(Plan plan, String title, String type, int order, String link) {
        PlanStep step = new PlanStep();
        step.setPlan(plan);
        step.setTitle(title);
        step.setStepType(normalizePlanStepType(type));
        step.setStepOrder(order);
        step.setLink(link);
        planStepRepository.save(step);
    }

    private void seedStudentPlan(User student, Plan plan, Subject subject) {
        if (studentPlanRepository.findFirstByStudent_IdAndSubject_IdAndCurrentTrue(student.getId(), subject.getId()).isPresent()) {
            return;
        }
        StudentPlan studentPlan = new StudentPlan();
        studentPlan.setStudent(student);
        studentPlan.setPlan(plan);
        studentPlan.setSubject(subject);
        studentPlan.setStartDate(java.time.Instant.now());
        studentPlan.setCurrentProgress(10.0);
        studentPlan.setStatus("active");
        studentPlan.setCurrent(true);
        studentPlanRepository.save(studentPlan);
    }

    private String normalizePlanStepType(String value) {
        if (value == null || value.isBlank()) {
            return "document";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "video", "document", "assessment", "discussion" -> normalized;
            case "reading", "resource", "notes" -> "document";
            case "exercise", "practice", "quiz", "assignment", "test" -> "assessment";
            case "meeting", "collaboration", "group" -> "discussion";
            default -> "document";
        };
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
