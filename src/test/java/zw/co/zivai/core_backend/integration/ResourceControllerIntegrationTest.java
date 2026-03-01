package zw.co.zivai.core_backend.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.transaction.Transactional;
import zw.co.zivai.core_backend.models.lms.resources.Resource;
import zw.co.zivai.core_backend.models.lms.school.School;
import zw.co.zivai.core_backend.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.models.lms.resources.Topic;
import zw.co.zivai.core_backend.models.lms.users.User;
import zw.co.zivai.core_backend.repositories.resource.ResourceRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    void addAndRemoveResourceTopicsWorkEndToEnd() throws Exception {
        TestResourceFixture fixture = createResourceFixture();

        mockMvc.perform(post("/api/resources/{id}/topics", fixture.resource.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "topicIds": ["%s", "%s"]
                    }
                    """.formatted(fixture.firstTopic.getId(), fixture.secondTopic.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topicIds", hasSize(2)))
            .andExpect(jsonPath("$.topicIds", containsInAnyOrder(
                fixture.firstTopic.getId().toString(),
                fixture.secondTopic.getId().toString()
            )));

        mockMvc.perform(delete("/api/resources/{id}/topics/{topicId}", fixture.resource.getId(), fixture.firstTopic.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topicIds", hasSize(1)))
            .andExpect(jsonPath("$.topicIds[0]").value(fixture.secondTopic.getId().toString()));
    }

    private TestResourceFixture createResourceFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        School school = new School();
        school.setCode("RES-SCH-" + suffix);
        school.setName("Resource School " + suffix);
        school.setCountryCode("ZW");
        school = schoolRepository.save(school);

        Subject subject = new Subject();
        subject.setCode("RES-SUB-" + suffix);
        subject.setName("Resource Subject " + suffix);
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        User uploader = new User();
        uploader.setEmail("uploader-" + suffix + "@example.com");
        uploader.setPhoneNumber("+26378" + Math.abs(suffix.hashCode() % 10000000));
        uploader.setFirstName("Upload");
        uploader.setLastName("Owner");
        uploader.setUsername("uploader_" + suffix);
        uploader.setPasswordHash("hash");
        uploader.setActive(true);
        uploader = userRepository.save(uploader);

        Topic firstTopic = new Topic();
        firstTopic.setSubject(subject);
        firstTopic.setCode("TOP1-" + suffix);
        firstTopic.setName("Topic One " + suffix);
        firstTopic.setSequenceIndex(1);
        firstTopic = topicRepository.save(firstTopic);

        Topic secondTopic = new Topic();
        secondTopic.setSubject(subject);
        secondTopic.setCode("TOP2-" + suffix);
        secondTopic.setName("Topic Two " + suffix);
        secondTopic.setSequenceIndex(2);
        secondTopic = topicRepository.save(secondTopic);

        Resource resource = new Resource();
        resource.setSchool(school);
        resource.setSubject(subject);
        resource.setUploadedBy(uploader);
        resource.setName("Resource " + suffix);
        resource.setOriginalName("Resource " + suffix);
        resource.setMimeType("text/html");
        resource.setResType("document");
        resource.setSizeBytes(42L);
        resource.setUrl("content://pending");
        resource.setContentType("text/html");
        resource.setContentBody("<p>Test resource</p>");
        resource.setStatus("draft");
        resource = resourceRepository.save(resource);

        return new TestResourceFixture(school, subject, uploader, firstTopic, secondTopic, resource);
    }

    private record TestResourceFixture(
        School school,
        Subject subject,
        User uploader,
        Topic firstTopic,
        Topic secondTopic,
        Resource resource
    ) {
    }
}
