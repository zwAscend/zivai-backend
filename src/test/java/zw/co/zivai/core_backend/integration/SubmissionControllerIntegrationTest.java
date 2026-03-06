package zw.co.zivai.core_backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;
import zw.co.zivai.core_backend.common.models.lms.assessments.Assessment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAssignment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAttempt;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentEnrollment;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentRepository;
import zw.co.zivai.core_backend.common.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SubmissionControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssessmentAssignmentRepository assessmentAssignmentRepository;

    @Autowired
    private AssessmentEnrollmentRepository assessmentEnrollmentRepository;

    @Autowired
    private AssessmentAttemptRepository assessmentAttemptRepository;

    @Test
    void pendingSubmissionsEndpointReturnsSubmissionDetails() throws Exception {
        ensureAssessmentLookupSeed();
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/submissions/teacher/pending")
                .param("teacherId", fixture.teacher().getId().toString())
                .param("subjectId", fixture.subject().getId().toString())
                .param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(fixture.attempt().getId().toString()))
            .andExpect(jsonPath("$[0].student.id").value(fixture.student().getId().toString()))
            .andExpect(jsonPath("$[0].assessment.id").value(fixture.assessment().getId().toString()))
            .andExpect(jsonPath("$[0].status").value("submitted"));
    }

    private void ensureAssessmentLookupSeed() {
        jdbcTemplate.update("""
            INSERT INTO lookups.assessment_enrollment_status(code, name)
            VALUES ('assigned', 'Assigned')
            ON CONFLICT (code) DO NOTHING
            """);
    }

    private Fixture createFixture() {
        School school = new School();
        school.setCode("TEST-SCH");
        school.setName("Test School");
        school.setCountryCode("ZW");
        school = schoolRepository.saveAndFlush(school);

        Subject subject = new Subject();
        subject.setCode("TEST-MATH");
        subject.setName("Mathematics");
        subject.setExamBoardCode("zimsec");
        subject = subjectRepository.saveAndFlush(subject);

        User teacher = new User();
        teacher.setEmail("teacher.pending@zivai.local");
        teacher.setPhoneNumber("263712111111");
        teacher.setFirstName("Pending");
        teacher.setLastName("Teacher");
        teacher.setUsername("pending-teacher");
        teacher = userRepository.saveAndFlush(teacher);

        User student = new User();
        student.setEmail("student.pending@zivai.local");
        student.setPhoneNumber("263712222222");
        student.setFirstName("Pending");
        student.setLastName("Student");
        student.setUsername("pending-student");
        student = userRepository.saveAndFlush(student);

        Assessment assessment = new Assessment();
        assessment.setSchool(school);
        assessment.setSubject(subject);
        assessment.setName("Pending Submission Assessment");
        assessment.setDescription("Assessment for pending submissions endpoint");
        assessment.setAssessmentType("assignment");
        assessment.setMaxScore(100.0);
        assessment.setWeightPct(20.0);
        assessment.setStatus("published");
        assessment.setCreatedBy(teacher);
        assessment.setLastModifiedBy(teacher);
        assessment = assessmentRepository.saveAndFlush(assessment);

        AssessmentAssignment assignment = new AssessmentAssignment();
        assignment.setAssessment(assessment);
        assignment.setAssignedBy(teacher);
        assignment.setTitle("Pending Assignment");
        assignment.setInstructions("Submit your work");
        assignment.setStartTime(Instant.now().minusSeconds(3600));
        assignment.setDueTime(Instant.now().plusSeconds(3600));
        assignment.setPublished(true);
        assignment = assessmentAssignmentRepository.saveAndFlush(assignment);

        AssessmentEnrollment enrollment = new AssessmentEnrollment();
        enrollment.setAssessmentAssignment(assignment);
        enrollment.setStudent(student);
        enrollment.setStatusCode("assigned");
        enrollment = assessmentEnrollmentRepository.saveAndFlush(enrollment);

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentEnrollment(enrollment);
        attempt.setAttemptNumber(1);
        attempt.setStartedAt(Instant.now().minusSeconds(1800));
        attempt.setSubmittedAt(Instant.now().minusSeconds(600));
        attempt.setSubmissionType("manual");
        attempt.setGradingStatusCode("pending");
        attempt.setTotalScore(55.0);
        attempt.setMaxScore(100.0);
        attempt = assessmentAttemptRepository.saveAndFlush(attempt);

        return new Fixture(teacher, student, subject, assessment, attempt);
    }

    private record Fixture(
        User teacher,
        User student,
        Subject subject,
        Assessment assessment,
        AssessmentAttempt attempt
    ) {}
}

