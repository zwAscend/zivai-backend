package zw.co.zivai.core_backend.configs;

import java.io.EOFException;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.CalendarEvent;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentResult;
import zw.co.zivai.core_backend.models.lms.Chat;
import zw.co.zivai.core_backend.models.lms.ChatMember;
import zw.co.zivai.core_backend.models.lms.Message;
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
import zw.co.zivai.core_backend.models.lms.TermForecast;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.models.lookups.Role;
import zw.co.zivai.core_backend.repositories.classroom.ClassRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.calendar.CalendarEventRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentResultRepository;
import zw.co.zivai.core_backend.repositories.chat.ChatMemberRepository;
import zw.co.zivai.core_backend.repositories.chat.ChatRepository;
import zw.co.zivai.core_backend.repositories.chat.MessageRepository;
import zw.co.zivai.core_backend.repositories.development.PlanRepository;
import zw.co.zivai.core_backend.repositories.development.PlanSkillRepository;
import zw.co.zivai.core_backend.repositories.development.PlanStepRepository;
import zw.co.zivai.core_backend.repositories.development.PlanSubskillRepository;
import zw.co.zivai.core_backend.repositories.user.RoleRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SkillRepository;
import zw.co.zivai.core_backend.repositories.development.StudentAttributeRepository;
import zw.co.zivai.core_backend.repositories.development.StudentPlanRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.termforecast.TermForecastRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {
    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final SkillRepository skillRepository;
    private final TopicRepository topicRepository;
    private final StudentAttributeRepository studentAttributeRepository;
    private final PlanRepository planRepository;
    private final PlanStepRepository planStepRepository;
    private final PlanSkillRepository planSkillRepository;
    private final PlanSubskillRepository planSubskillRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final TermForecastRepository termForecastRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_SEEDED_PASSWORD = "TempPass123!";
    private static final int SEED_MAX_RETRIES = 5;
    private static final long SEED_RETRY_DELAY_MS = 1500L;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    @PreDestroy
    void markShutdownRequested() {
        shutdownRequested.set(true);
    }

    @Bean
    CommandLineRunner seedUsers() {
        return args -> runSeedingWithRetry();
    }

    private void runSeedingWithRetry() {
        long delayMs = SEED_RETRY_DELAY_MS;
        for (int attempt = 1; attempt <= SEED_MAX_RETRIES; attempt++) {
            if (isShutdownRequested()) {
                LOG.info("Skipping remaining seed attempts because shutdown is in progress.");
                return;
            }
            try {
                seedAll();
                if (attempt > 1) {
                    LOG.info("Data seeding recovered successfully on attempt {}/{}.", attempt, SEED_MAX_RETRIES);
                }
                return;
            } catch (RuntimeException ex) {
                if (isShutdownRequested()) {
                    LOG.info("Seeder interrupted by shutdown; stopping seeding.");
                    return;
                }
                if (!isTransientDbConnectivityIssue(ex) || attempt == SEED_MAX_RETRIES) {
                    throw ex;
                }
                LOG.warn(
                    "Transient DB connectivity issue while seeding (attempt {}/{}). Retrying in {} ms.",
                    attempt,
                    SEED_MAX_RETRIES,
                    delayMs,
                    ex
                );
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    LOG.info("Seeder retry sleep interrupted; stopping seeding.");
                    return;
                }
                delayMs *= 2;
            }
        }
    }

    private void seedAll() {
        if (isShutdownRequested()) {
            return;
        }
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

        ClassEntity classEntity = null;
        ClassSubject mathLink = null;
        ClassSubject engLink = null;

        if (teacherUser != null) {
            classEntity = classRepository.findByCode("FORM2-A")
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
                mathLink = seedClassSubject(school, classEntity, mathSubject, teacherUser, "2026", "Term 1");
                seedStudentSubjectEnrolment(studentUser, mathLink);
            }
            if (engSubject != null) {
                engLink = seedClassSubject(school, classEntity, engSubject, teacherUser, "2026", "Term 1");
                seedStudentSubjectEnrolment(studentUser, engLink);
            }
        }

        if (mathSubject != null) {
            List<Topic> mathCurriculum = seedMathCurriculum(mathSubject);
            if (mathLink != null && !mathCurriculum.isEmpty()) {
                List<java.util.UUID> termTopics = mathCurriculum.stream()
                    .filter(topic -> List.of("6.1", "6.2", "6.3").contains(topic.getCode()))
                    .map(Topic::getId)
                    .toList();
                if (!termTopics.isEmpty()) {
                    seedTermForecast(mathLink, teacherUser, "Term 1", "2026", 70.0,
                        termTopics,
                        "Focus on number, sets, and consumer arithmetic foundations.");
                }
            }
        }

        if (engSubject != null) {
            Topic comprehension = seedTopic(engSubject, "COMP", "Reading Comprehension", "Reading and interpretation.", 1);
            Topic writing = seedTopic(engSubject, "WRIT", "Writing Skills", "Grammar and structured writing.", 2);
            seedTopic(engSubject, "GRAM", "Grammar & Usage", "Core grammar rules and usage.", 3);

            if (engLink != null) {
                seedTermForecast(engLink, teacherUser, "Term 1", "2026", 65.0,
                    List.of(comprehension.getId(), writing.getId()),
                    "Prioritize comprehension and writing mastery.");
            }
        }

        if (teacherUser != null) {
            seedCalendarEvents(school, teacherUser);
        }

        if (teacherUser != null && studentUser != null) {
            seedChatData(school, teacherUser, studentUser);
        }

        if (teacherUser != null && studentUser != null) {
            seedAssessmentData(school, teacherUser, studentUser, classEntity, mathSubject, mathLink);
            seedAssessmentData(school, teacherUser, studentUser, classEntity, engSubject, engLink);
        }

        if (studentUser != null) {
            seedDevelopmentData(mathSubject, engSubject, studentUser);
        }
    }

    private boolean isShutdownRequested() {
        return shutdownRequested.get() || Thread.currentThread().isInterrupted();
    }

    private boolean isTransientDbConnectivityIssue(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (
                message.contains("password authentication failed") ||
                message.contains("no password was provided")
            )) {
                return false;
            }

            if (current instanceof SQLTransientException || current instanceof SQLRecoverableException || current instanceof EOFException) {
                return true;
            }
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
            }
            if (message != null && (
                message.contains("SQLSTATE(08006)") ||
                message.contains("Connection is closed") ||
                message.contains("An I/O error occurred while sending to the backend")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
            boolean updated = false;
            if (existing.getPasswordHash() == null || existing.getPasswordHash().isBlank()
                || !passwordEncoder.matches(DEFAULT_SEEDED_PASSWORD, existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode(DEFAULT_SEEDED_PASSWORD));
                updated = true;
            }
            if (!existing.isActive()) {
                existing.setActive(true);
                updated = true;
            }
            if (existing.getDeletedAt() != null) {
                existing.setDeletedAt(null);
                updated = true;
            }
            if (roles != null && !roles.isEmpty()) {
                for (Role role : roles) {
                    if (!existing.getRoles().contains(role)) {
                        existing.getRoles().add(role);
                        updated = true;
                    }
                }
            }
            if (updated) {
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

    private Topic seedTopic(Subject subject, String code, String name, String description, int sequenceIndex) {
        return topicRepository.findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(subject.getId()).stream()
            .filter(existing -> code.equalsIgnoreCase(existing.getCode()))
            .findFirst()
            .orElseGet(() -> {
                Topic topic = new Topic();
                topic.setSubject(subject);
                topic.setCode(code);
                topic.setName(name);
                topic.setDescription(description);
                topic.setSequenceIndex(sequenceIndex);
                return topicRepository.save(topic);
            });
    }

    private Skill seedSkill(Subject subject, Topic topic, String code, String name, String description, int sequenceIndex) {
        return skillRepository.findBySubject_IdAndCode(subject.getId(), code)
            .orElseGet(() -> {
                Skill skill = new Skill();
                skill.setSubject(subject);
                skill.setTopic(topic);
                skill.setCode(code);
                skill.setName(name);
                skill.setDescription(description);
                skill.setSequenceIndex(sequenceIndex);
                return skillRepository.save(skill);
            });
    }

    private List<Topic> seedMathCurriculum(Subject subject) {
        if (subject == null) {
            return List.of();
        }

        List<Topic> topics = new java.util.ArrayList<>();

        Topic number = seedTopic(subject, "6.1", "Number",
            "Number concepts, operations, approximations, bases, ratio and scale.", 1);
        topics.add(number);
        seedSkill(subject, number, "6.1.1", "Number concepts and operations",
            "Number types, fractions/decimals/percentages, number line, directed numbers, HCF/LCM, operations and precedence.", 1);
        seedSkill(subject, number, "6.1.2", "Approximations and estimates",
            "Significant figures, decimal places, rounding and estimation in context.", 2);
        seedSkill(subject, number, "6.1.3", "Limits of accuracy",
            "Upper and lower bounds for measurements and calculations.", 3);
        seedSkill(subject, number, "6.1.4", "Standard form",
            "Scientific notation A × 10^n.", 4);
        seedSkill(subject, number, "6.1.5", "Number bases",
            "Convert between bases 2–10 and interpret place value.", 5);
        seedSkill(subject, number, "6.1.6", "Ratio, proportion and rates",
            "Use ratio and proportion in practical contexts.", 6);
        seedSkill(subject, number, "6.1.7", "Scale and map problems",
            "Use scale factors and interpret maps/drawings.", 7);

        Topic sets = seedTopic(subject, "6.2", "Sets",
            "Set language, notation, operations, and Venn diagrams.", 2);
        topics.add(sets);
        seedSkill(subject, sets, "6.2.1", "Language and notation",
            "Set builder notation, elements, subsets, universal and empty sets.", 1);
        seedSkill(subject, sets, "6.2.2", "Operations on sets",
            "Union, intersection, complement and set laws.", 2);
        seedSkill(subject, sets, "6.2.3", "Venn diagrams",
            "Solve problems using Venn diagrams (up to three sets).", 3);

        Topic consumer = seedTopic(subject, "6.3", "Consumer Arithmetic",
            "Real-life finance, rates, and consumer calculations.", 3);
        topics.add(consumer);
        seedSkill(subject, consumer, "6.3.1", "Interpreting real-life data",
            "Bills, bank statements, mortgages, and media data interpretation.", 1);
        seedSkill(subject, consumer, "6.3.2", "Rates and currency exchange",
            "Rates, ratios, and foreign exchange calculations.", 2);
        seedSkill(subject, consumer, "6.3.3", "Interest and discounts",
            "Interest, discount, commission, depreciation and related calculations.", 3);
        seedSkill(subject, consumer, "6.3.4", "Tax and hire purchase",
            "Sales/income tax, hire purchase, and bank accounts.", 4);

        Topic measures = seedTopic(subject, "6.4", "Measures and Mensuration",
            "Units, time, perimeter, area, surface area, volume, density.", 4);
        topics.add(measures);
        seedSkill(subject, measures, "6.4.1", "Time and units",
            "12/24-hour time, SI units and unit conversions.", 1);
        seedSkill(subject, measures, "6.4.2", "Perimeter and area",
            "Rectangles, triangles, parallelograms, trapezia.", 2);
        seedSkill(subject, measures, "6.4.3", "Circles and arcs",
            "Circumference, arc length, sector and segment area.", 3);
        seedSkill(subject, measures, "6.4.4", "Surface area and volume",
            "Cylinder, cuboid, prisms, pyramid, cone, sphere.", 4);
        seedSkill(subject, measures, "6.4.5", "Density and capacity",
            "Density, volume and capacity in practical contexts.", 5);

        Topic graphs = seedTopic(subject, "6.5", "Graphs and Variation",
            "Coordinate graphs, variation, functional graphs and kinematics.", 5);
        topics.add(graphs);
        seedSkill(subject, graphs, "6.5.1", "Coordinates and graphs",
            "Plot and interpret Cartesian graphs from data.", 1);
        seedSkill(subject, graphs, "6.5.2", "Kinematics graphs",
            "Displacement-time and velocity-time graphs; speed and acceleration.", 2);
        seedSkill(subject, graphs, "6.5.3", "Variation",
            "Direct, inverse, joint and partial variation.", 3);
        seedSkill(subject, graphs, "6.5.4", "Functional graphs",
            "Linear, quadratic, power functions and f(x) notation.", 4);
        seedSkill(subject, graphs, "6.5.5", "Gradients and rates of change",
            "Gradients, turning points, and interpreting slopes.", 5);
        seedSkill(subject, graphs, "6.5.6", "Area under a curve",
            "Estimate area using squares or trapezia.", 6);

        Topic algebra = seedTopic(subject, "6.6", "Algebraic Concepts and Techniques",
            "Symbolic manipulation, factorisation, indices, equations and inequalities.", 6);
        topics.add(algebra);
        seedSkill(subject, algebra, "6.6.1", "Symbolic expressions and formulae",
            "Translate to algebra and substitute values.", 1);
        seedSkill(subject, algebra, "6.6.2", "Change of subject",
            "Rearrange formulae, including from other subjects.", 2);
        seedSkill(subject, algebra, "6.6.3", "Algebraic manipulation",
            "Operations, expansion and simplification.", 3);
        seedSkill(subject, algebra, "6.6.4", "Factorisation",
            "Factorise linear and quadratic expressions.", 4);
        seedSkill(subject, algebra, "6.6.5", "Indices and logarithms",
            "Laws of indices and basic logarithm rules.", 5);
        seedSkill(subject, algebra, "6.6.6", "Equations",
            "Linear, simultaneous, and quadratic equations.", 6);
        seedSkill(subject, algebra, "6.6.7", "Inequalities and linear programming",
            "Solve inequalities and interpret feasible regions.", 7);

        Topic geometry = seedTopic(subject, "6.7", "Geometric Concepts and Techniques",
            "Angles, polygons, circles, similarity, construction and loci.", 7);
        topics.add(geometry);
        seedSkill(subject, geometry, "6.7.1", "Points, lines and angles",
            "Angle properties, parallel lines, elevation and depression.", 1);
        seedSkill(subject, geometry, "6.7.2", "Bearings",
            "Three-figure bearings and compass directions.", 2);
        seedSkill(subject, geometry, "6.7.3", "Polygons and area properties",
            "Triangles, quadrilaterals, regular polygons.", 3);
        seedSkill(subject, geometry, "6.7.4", "Circles and theorems",
            "Chord, tangent, cyclic quadrilateral and circle theorems.", 4);
        seedSkill(subject, geometry, "6.7.5", "Similarity and congruency",
            "Similar figures and congruent triangles.", 5);
        seedSkill(subject, geometry, "6.7.6", "Constructions",
            "Angles, triangles, polygons and scale drawings.", 6);
        seedSkill(subject, geometry, "6.7.7", "Loci",
            "Loci in two dimensions using ruler and compass.", 7);
        seedSkill(subject, geometry, "6.7.8", "Symmetry",
            "Line and rotational symmetry of plane figures.", 8);

        Topic trig = seedTopic(subject, "6.8", "Trigonometry",
            "Pythagoras, trig ratios, and triangle area rules.", 8);
        topics.add(trig);
        seedSkill(subject, trig, "6.8.1", "Pythagoras and trig ratios",
            "Apply Pythagoras, sine, cosine, tangent in right triangles.", 1);
        seedSkill(subject, trig, "6.8.2", "Area of a triangle",
            "Use area = 1/2 ab sin C.", 2);
        seedSkill(subject, trig, "6.8.3", "Sine and cosine rules",
            "Solve non-right triangles using sine/cosine rules.", 3);
        seedSkill(subject, trig, "6.8.4", "3D trigonometry",
            "Solve 3D problems involving angles between lines and planes.", 4);

        Topic vectors = seedTopic(subject, "6.9", "Vectors and Matrices",
            "Vector notation and matrix operations in 2D.", 9);
        topics.add(vectors);
        seedSkill(subject, vectors, "6.9.1", "Vectors in two dimensions",
            "Translation, notation and representation.", 1);
        seedSkill(subject, vectors, "6.9.2", "Vector operations",
            "Addition, subtraction, scalar multiplication and magnitude.", 2);
        seedSkill(subject, vectors, "6.9.3", "Position and parallel vectors",
            "Identify position, equal and parallel vectors.", 3);
        seedSkill(subject, vectors, "6.9.4", "Matrices basics",
            "Order, addition, subtraction and scalar multiplication.", 4);
        seedSkill(subject, vectors, "6.9.5", "Matrix operations",
            "Multiplication, identity, determinant and inverse (2x2).", 5);

        Topic transforms = seedTopic(subject, "6.10", "Transformations",
            "Translation, reflection, rotation, enlargement, stretch and shear.", 10);
        topics.add(transforms);
        seedSkill(subject, transforms, "6.10.1", "Basic transformations",
            "Translation, reflection, rotation and enlargement.", 1);
        seedSkill(subject, transforms, "6.10.2", "Stretch and shear",
            "One-way and two-way stretch, shear with invariant lines.", 2);
        seedSkill(subject, transforms, "6.10.3", "Combined transformations",
            "Compose transformations and describe fully.", 3);
        seedSkill(subject, transforms, "6.10.4", "Matrices as operators",
            "Use matrices to represent transformations.", 4);

        Topic stats = seedTopic(subject, "6.11", "Statistics and Probability",
            "Data handling and probability concepts.", 11);
        topics.add(stats);
        seedSkill(subject, stats, "6.11.1", "Collection and classification",
            "Collect, classify and tabulate data.", 1);
        seedSkill(subject, stats, "6.11.2", "Data representation",
            "Charts, histograms, frequency tables and polygons.", 2);
        seedSkill(subject, stats, "6.11.3", "Measures of central tendency",
            "Mean, median, mode and use of assumed mean.", 3);
        seedSkill(subject, stats, "6.11.4", "Cumulative frequency",
            "Cumulative frequency curves/ogives and interpretation.", 4);
        seedSkill(subject, stats, "6.11.5", "Probability terms",
            "Random, certain, impossible, events and sample space.", 5);
        seedSkill(subject, stats, "6.11.6", "Probability calculations",
            "Single and combined events; tree diagrams/outcome tables.", 6);

        return topics;
    }

    private void seedTermForecast(
        ClassSubject classSubject,
        User teacher,
        String term,
        String academicYear,
        double expectedCoverage,
        List<java.util.UUID> topicIds,
        String notes
    ) {
        if (classSubject == null) {
            return;
        }
        if (termForecastRepository
            .findByClassSubject_IdAndTermAndAcademicYearAndDeletedAtIsNull(classSubject.getId(), term, academicYear)
            .isPresent()) {
            return;
        }
        TermForecast forecast = new TermForecast();
        forecast.setClassSubject(classSubject);
        forecast.setTerm(term);
        forecast.setAcademicYear(academicYear);
        forecast.setExpectedCoveragePct(expectedCoverage);
        JsonNode topicIdsJson = objectMapper.valueToTree(topicIds);
        forecast.setExpectedTopicIds(topicIdsJson);
        forecast.setNotes(notes);
        forecast.setCreatedBy(teacher);
        termForecastRepository.save(forecast);
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

    private void seedChatData(School school, User teacher, User student) {
        if (school == null || teacher == null || student == null) {
            return;
        }

        Chat existingChat = chatMemberRepository.findByUser_Id(student.getId()).stream()
            .map(ChatMember::getChat)
            .filter(chat -> chat != null && "direct".equalsIgnoreCase(chat.getChatType()))
            .filter(chat -> chatMemberRepository.findByChat_Id(chat.getId()).stream()
                .anyMatch(member -> member.getUser() != null && member.getUser().getId().equals(teacher.getId())))
            .findFirst()
            .orElse(null);

        Chat chat = existingChat;
        if (chat == null) {
            chat = new Chat();
            chat.setSchool(school);
            chat.setChatType("direct");
            chat.setTitle("Student Chat");
            chat = chatRepository.save(chat);

            ChatMember studentMember = new ChatMember();
            studentMember.setChat(chat);
            studentMember.setUser(student);
            studentMember.setRole("member");
            chatMemberRepository.save(studentMember);

            ChatMember teacherMember = new ChatMember();
            teacherMember.setChat(chat);
            teacherMember.setUser(teacher);
            teacherMember.setRole("admin");
            chatMemberRepository.save(teacherMember);
        }

        if (messageRepository.findByChatIdOrderByTsAsc(chat.getId()).isEmpty()) {
            seedMessage(school, chat, student, "Good day sir, I can't see my assignment on the portal.", 120);
            seedMessage(school, chat, teacher, "Thanks for letting me know. I'll check and update it.", 90);
            seedMessage(school, chat, student, "Appreciate it. Please let me know when it's fixed.", 60);
        }
    }

    private void seedMessage(School school, Chat chat, User sender, String content, int minutesAgo) {
        Message message = new Message();
        message.setSchool(school);
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(content);
        message.setTs(java.time.Instant.now().minusSeconds(minutesAgo * 60L));
        message.setRead(minutesAgo > 90);
        messageRepository.save(message);
    }

    private void seedAssessmentData(
        School school,
        User teacher,
        User student,
        ClassEntity classEntity,
        Subject subject,
        ClassSubject classSubject
    ) {
        if (school == null || teacher == null || student == null || subject == null) {
            return;
        }

        Assessment quiz = seedAssessment(school, subject, teacher, "Weekly Quiz", "Quick check on recent topics", "quiz", 30.0, 10.0);
        Assessment test = seedAssessment(school, subject, teacher, "Topic Test", "Assessment of module understanding", "test", 50.0, 20.0);

        AssessmentAssignment quizAssignment = seedAssessmentAssignment(quiz, classEntity, teacher, "Weekly Quiz", "Complete the quiz", 7);
        AssessmentAssignment testAssignment = seedAssessmentAssignment(test, classEntity, teacher, "Topic Test", "Answer all sections", 14);

        seedAssessmentResult(quizAssignment, student, 18.0, 24.0, "B+", "Good understanding overall.");
        seedAssessmentResult(testAssignment, student, 30.0, 40.0, "A-", "Strong performance, keep it up.");
    }

    private Assessment seedAssessment(
        School school,
        Subject subject,
        User teacher,
        String name,
        String description,
        String type,
        double maxScore,
        double weightPct
    ) {
        Assessment existing = assessmentRepository.findAll().stream()
            .filter(assessment -> assessment.getSubject() != null
                && assessment.getSubject().getId().equals(subject.getId())
                && assessment.getName() != null
                && assessment.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            return existing;
        }

        Assessment assessment = new Assessment();
        assessment.setSchool(school);
        assessment.setSubject(subject);
        assessment.setName(name);
        assessment.setDescription(description);
        assessment.setAssessmentType(type);
        assessment.setVisibility("private");
        assessment.setMaxScore(maxScore);
        assessment.setWeightPct(weightPct);
        assessment.setAiEnhanced(false);
        assessment.setStatus("published");
        assessment.setCreatedBy(teacher);
        assessment.setLastModifiedBy(teacher);
        return assessmentRepository.save(assessment);
    }

    private AssessmentAssignment seedAssessmentAssignment(
        Assessment assessment,
        ClassEntity classEntity,
        User teacher,
        String title,
        String instructions,
        int dueDays
    ) {
        if (assessment == null) {
            return null;
        }
        List<AssessmentAssignment> existing = assessmentAssignmentRepository.findByAssessment_Id(assessment.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        AssessmentAssignment assignment = new AssessmentAssignment();
        assignment.setAssessment(assessment);
        assignment.setClassEntity(classEntity);
        assignment.setAssignedBy(teacher);
        assignment.setTitle(title);
        assignment.setInstructions(instructions);
        assignment.setStartTime(java.time.Instant.now().minusSeconds(3 * 24L * 3600L));
        assignment.setDueTime(java.time.Instant.now().plusSeconds(dueDays * 24L * 3600L));
        assignment.setPublished(true);
        return assessmentAssignmentRepository.save(assignment);
    }

    private void seedAssessmentResult(
        AssessmentAssignment assignment,
        User student,
        double expected,
        double actual,
        String grade,
        String feedback
    ) {
        if (assignment == null || student == null) {
            return;
        }
        if (assessmentResultRepository
            .findFirstByAssessmentAssignment_IdAndStudent_Id(assignment.getId(), student.getId())
            .isPresent()) {
            return;
        }
        AssessmentResult result = new AssessmentResult();
        result.setAssessmentAssignment(assignment);
        result.setStudent(student);
        result.setExpectedMark(expected);
        result.setActualMark(actual);
        result.setGrade(grade);
        result.setFeedback(feedback);
        result.setSubmittedAt(java.time.Instant.now().minusSeconds(1 * 24L * 3600L));
        result.setGradedAt(java.time.Instant.now().minusSeconds(12 * 3600L));
        result.setStatus("published");
        assessmentResultRepository.save(result);
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
