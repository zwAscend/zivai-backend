package zw.co.zivai.core_backend.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import zw.co.zivai.core_backend.models.lms.development.Plan;
import zw.co.zivai.core_backend.models.lms.development.PlanStep;
import zw.co.zivai.core_backend.models.lms.classroom.ClassEntity;
import zw.co.zivai.core_backend.models.lms.classroom.ClassSubject;
import zw.co.zivai.core_backend.models.lms.school.School;
import zw.co.zivai.core_backend.models.lms.students.StudentPlan;
import zw.co.zivai.core_backend.models.lms.students.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.models.lms.users.User;
import zw.co.zivai.core_backend.repositories.classroom.ClassRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.development.PlanRepository;
import zw.co.zivai.core_backend.repositories.development.PlanStepRepository;
import zw.co.zivai.core_backend.repositories.development.StudentPlanRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DevelopmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanStepRepository planStepRepository;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

    @Test
    void stepCrudAndReorderEndpointsWorkEndToEnd() throws Exception {
        TestDevelopmentFixture fixture = createDevelopmentFixture();

        String createResponse = mockMvc.perform(post("/api/development/plans/student-plan/{studentPlanId}/steps", fixture.studentPlan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "First practice",
                      "type": "document",
                      "content": "<p>Draft content</p>",
                      "link": "https://example.com/first",
                      "order": 1
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.plan.steps", hasSize(1)))
            .andExpect(jsonPath("$.plan.steps[0].title").value("First practice"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode createdJson = objectMapper.readTree(createResponse);
        String firstStepId = createdJson.at("/plan/steps/0/id").asText();

        mockMvc.perform(post("/api/development/plans/student-plan/{studentPlanId}/steps", fixture.studentPlan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Second practice",
                      "type": "quiz",
                      "content": "<p>Quiz content</p>",
                      "order": 2
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.plan.steps", hasSize(2)));

        PlanStep secondStep = planStepRepository.findByPlan_IdOrderByStepOrderAsc(fixture.plan.getId()).stream()
            .filter(step -> "Second practice".equals(step.getTitle()))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(put("/api/development/plans/student-plan/{studentPlanId}/steps/{stepId}", fixture.studentPlan.getId(), firstStepId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Updated first practice",
                      "type": "assignment",
                      "content": "<p>Updated content</p>",
                      "link": "https://example.com/updated",
                      "order": 1
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.steps[0].title").value("Updated first practice"))
            .andExpect(jsonPath("$.plan.steps[0].type").value("assessment"));

        mockMvc.perform(put("/api/development/plans/student-plan/{studentPlanId}/steps/reorder", fixture.studentPlan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "stepIds": ["%s", "%s"]
                    }
                    """.formatted(secondStep.getId(), firstStepId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.steps", hasSize(2)))
            .andExpect(jsonPath("$.plan.steps[0].id").value(secondStep.getId().toString()))
            .andExpect(jsonPath("$.plan.steps[0].order").value(1))
            .andExpect(jsonPath("$.plan.steps[1].id").value(firstStepId))
            .andExpect(jsonPath("$.plan.steps[1].order").value(2));

        mockMvc.perform(delete("/api/development/plans/student-plan/{studentPlanId}/steps/{stepId}", fixture.studentPlan.getId(), firstStepId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.steps", hasSize(1)))
            .andExpect(jsonPath("$.plan.steps[0].id").value(secondStep.getId().toString()));
    }

    @Test
    void publishAndUnpublishControlsStudentVisibility() throws Exception {
        TestDevelopmentFixture fixture = createDevelopmentFixture();

        mockMvc.perform(get("/api/students/{studentId}/development-plans", fixture.student.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/development/plans/student-plan/{studentPlanId}/publish", fixture.studentPlan.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Active"));

        mockMvc.perform(get("/api/students/{studentId}/development-plans", fixture.student.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(fixture.studentPlan.getId().toString()));

        mockMvc.perform(post("/api/development/plans/student-plan/{studentPlanId}/unpublish", fixture.studentPlan.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("On Hold"));

        mockMvc.perform(get("/api/students/{studentId}/development-plans", fixture.student.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void readEndpointsAutoCreateStarterPlanForSubjectEnrolment() throws Exception {
        TestStarterPlanFixture fixture = createStarterPlanFixture();

        mockMvc.perform(get("/api/development/plans/student/{studentId}/subject/{subjectId}", fixture.student.getId(), fixture.subject.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.name").value("Test Student " + fixture.subject.getName() + " Development Plan"))
            .andExpect(jsonPath("$.status").value("On Hold"))
            .andExpect(jsonPath("$.plan.steps", hasSize(0)));

        mockMvc.perform(get("/api/development/plans/student/{studentId}", fixture.student.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].plan.steps", hasSize(0)));

        mockMvc.perform(get("/api/development/plans")
                .param("subjectId", fixture.subject.getId().toString())
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].plan.steps", hasSize(0)));
    }

    @Test
    void savingSubjectEnrolmentImmediatelyCreatesStarterPlan() {
        TestStarterPlanFixture fixture = createStarterPlanFixtureWithoutEnrolment();

        StudentSubjectEnrolment enrolment = new StudentSubjectEnrolment();
        enrolment.setStudent(fixture.student());
        enrolment.setClassSubject(fixture.classSubject());
        enrolment.setStatusCode("active");
        studentSubjectEnrolmentRepository.saveAndFlush(enrolment);

        List<StudentPlan> plans = studentPlanRepository
            .findByStudent_IdAndSubject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                fixture.student().getId(),
                fixture.subject().getId()
            );

        org.assertj.core.api.Assertions.assertThat(plans).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(plans.get(0).getPlan()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(plans.get(0).getStatus()).isEqualTo("on_hold");
    }

    private TestDevelopmentFixture createDevelopmentFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        School school = new School();
        school.setCode("TST-SCH-" + suffix);
        school.setName("Test School " + suffix);
        school.setCountryCode("ZW");
        school = schoolRepository.save(school);

        Subject subject = new Subject();
        subject.setCode("TST-SUB-" + suffix);
        subject.setName("Test Subject " + suffix);
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        User student = new User();
        student.setEmail("student-" + suffix + "@example.com");
        student.setPhoneNumber("+26377" + Math.abs(suffix.hashCode() % 10000000));
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setUsername("student_" + suffix);
        student.setPasswordHash("hash");
        student.setActive(true);
        student = userRepository.save(student);

        Plan plan = new Plan();
        plan.setSubject(subject);
        plan.setName("Plan " + suffix);
        plan.setDescription("Plan for integration tests");
        plan.setProgress(0.0);
        plan.setPotentialOverall(75.0);
        plan.setEtaDays(10);
        plan.setPerformance("Tracking");
        plan = planRepository.save(plan);

        StudentPlan studentPlan = new StudentPlan();
        studentPlan.setStudent(student);
        studentPlan.setPlan(plan);
        studentPlan.setSubject(subject);
        studentPlan.setStartDate(Instant.now());
        studentPlan.setCurrentProgress(0.0);
        studentPlan.setStatus("on_hold");
        studentPlan.setCurrent(false);
        studentPlan = studentPlanRepository.save(studentPlan);

        return new TestDevelopmentFixture(school, subject, student, plan, studentPlan);
    }

    private TestStarterPlanFixture createStarterPlanFixture() {
        TestStarterPlanFixture fixture = createStarterPlanFixtureWithoutEnrolment();

        StudentSubjectEnrolment enrolment = new StudentSubjectEnrolment();
        enrolment.setStudent(fixture.student());
        enrolment.setClassSubject(fixture.classSubject());
        enrolment.setStatusCode("active");
        studentSubjectEnrolmentRepository.save(enrolment);

        return fixture;
    }

    private TestStarterPlanFixture createStarterPlanFixtureWithoutEnrolment() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        School school = new School();
        school.setCode("TST-SCH-" + suffix);
        school.setName("Test School " + suffix);
        school.setCountryCode("ZW");
        school = schoolRepository.save(school);

        Subject subject = new Subject();
        subject.setCode("TST-SUB-" + suffix);
        subject.setName("Starter Subject " + suffix);
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        User student = new User();
        student.setEmail("starter-student-" + suffix + "@example.com");
        student.setPhoneNumber("+26378" + Math.abs(suffix.hashCode() % 10000000));
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setUsername("starter_student_" + suffix);
        student.setPasswordHash("hash");
        student.setActive(true);
        student = userRepository.save(student);

        ClassEntity classEntity = new ClassEntity();
        classEntity.setSchool(school);
        classEntity.setCode("CLS-" + suffix);
        classEntity.setName("Class " + suffix);
        classEntity.setGradeLevel("Form 3");
        classEntity.setAcademicYear("2026");
        classEntity = classRepository.save(classEntity);

        ClassSubject classSubject = new ClassSubject();
        classSubject.setSchool(school);
        classSubject.setClassEntity(classEntity);
        classSubject.setSubject(subject);
        classSubject.setAcademicYear("2026");
        classSubject.setTerm("Term 1");
        classSubject.setName(subject.getName());
        classSubject.setActive(true);
        classSubject = classSubjectRepository.save(classSubject);

        return new TestStarterPlanFixture(school, subject, student, classEntity, classSubject);
    }

    private record TestDevelopmentFixture(School school, Subject subject, User student, Plan plan, StudentPlan studentPlan) {
    }

    private record TestStarterPlanFixture(School school,
                                          Subject subject,
                                          User student,
                                          ClassEntity classEntity,
                                          ClassSubject classSubject) {
    }
}
