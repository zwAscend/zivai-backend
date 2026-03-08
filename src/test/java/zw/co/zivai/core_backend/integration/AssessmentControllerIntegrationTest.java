package zw.co.zivai.core_backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.transaction.Transactional;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentQuestion;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentQuestionRepository;
import zw.co.zivai.core_backend.common.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssessmentControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Test
    void createAssessmentAcceptsMultipleChoiceAliasAndPersistsLookupSafeCode() throws Exception {
        Fixture fixture = createFixture();
        String payload = """
            {
              "schoolId": "%s",
              "subjectId": "%s",
              "name": "Alias Type Assessment",
              "description": "",
              "assessmentType": "quiz",
              "visibility": "private",
              "attemptsAllowed": 1,
              "maxScore": 100,
              "weightPct": 0,
              "status": "draft",
              "createdBy": "%s",
              "lastModifiedBy": "%s",
              "questions": [
                {
                  "stem": "Define OOP?",
                  "questionTypeCode": "multiple_choice",
                  "maxMark": 1,
                  "difficulty": 2,
                  "rubricJson": {
                    "correctAnswer": "Object Oriented Programming",
                    "markingGuide": "Object Oriented Programming"
                  },
                  "sequenceIndex": 1,
                  "points": 1
                }
              ]
            }
            """.formatted(
            fixture.school.getId(),
            fixture.subject.getId(),
            fixture.user.getId(),
            fixture.user.getId()
        );

        MvcResult createResult = mockMvc.perform(post("/api/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode created = MAPPER.readTree(createResult.getResponse().getContentAsString());
        UUID assessmentId = UUID.fromString(created.path("id").asText());

        mockMvc.perform(get("/api/assessments/{id}/with-questions", assessmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.questions[0].questionTypeCode").value("multiple_choice"));

        AssessmentQuestion link = assessmentQuestionRepository
            .findFirstByAssessment_IdOrderBySequenceIndexAsc(assessmentId)
            .orElseThrow();
        assertThat(link.getQuestion().getQuestionTypeCode()).isEqualTo("mcq");
    }

    @Test
    void createAssessmentRejectsQuestionWithoutAnyAnswerPayload() throws Exception {
        Fixture fixture = createFixture();
        String payload = """
            {
              "schoolId": "%s",
              "subjectId": "%s",
              "name": "Invalid Assessment",
              "assessmentType": "quiz",
              "visibility": "private",
              "attemptsAllowed": 1,
              "maxScore": 100,
              "weightPct": 0,
              "status": "draft",
              "createdBy": "%s",
              "lastModifiedBy": "%s",
              "questions": [
                {
                  "stem": "Explain encapsulation.",
                  "questionTypeCode": "short_answer",
                  "maxMark": 1,
                  "difficulty": 2,
                  "rubricJson": {},
                  "sequenceIndex": 1,
                  "points": 1
                }
              ]
            }
            """.formatted(
            fixture.school.getId(),
            fixture.subject.getId(),
            fixture.user.getId(),
            fixture.user.getId()
        );

        mockMvc.perform(post("/api/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Each question must include at least one expected/correct answer."));
    }

    private Fixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        School school = new School();
        school.setCode("ASMT-SCH-" + suffix);
        school.setName("Assessment School " + suffix);
        school.setCountryCode("ZW");
        school = schoolRepository.save(school);

        Subject subject = new Subject();
        subject.setCode("ASMT-SUB-" + suffix);
        subject.setName("Assessment Subject " + suffix);
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        User user = new User();
        user.setEmail("assessment-owner-" + suffix + "@example.com");
        user.setPhoneNumber("+26377" + Math.abs(suffix.hashCode() % 10000000));
        user.setFirstName("Assess");
        user.setLastName("Owner");
        user.setUsername("assessment_owner_" + suffix);
        user.setPasswordHash("hash");
        user.setActive(true);
        user = userRepository.save(user);

        return new Fixture(school, subject, user);
    }

    private record Fixture(
        School school,
        Subject subject,
        User user
    ) {
    }
}
