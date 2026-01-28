package zw.co.zivai.core_backend.configs;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.models.lookups.Role;
import zw.co.zivai.core_backend.repositories.ClassRepository;
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

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            seedEnrolmentStatuses();

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
        };
    }

    private Role buildRole(String code, String name) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        return role;
    }

    private void seedUser(String email, String phoneNumber, String username, String firstName, String lastName, List<Role> roles) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(true);
        user.getRoles().addAll(roles);
        userRepository.save(user);
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


    private void seedEnrolmentStatuses() {
        jdbcTemplate.update(
            "INSERT INTO lookups.enrolment_status (code, name) VALUES ('active', 'Active') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.enrolment_status (code, name) VALUES ('dropped', 'Dropped') ON CONFLICT (code) DO NOTHING");
        jdbcTemplate.update(
            "INSERT INTO lookups.enrolment_status (code, name) VALUES ('completed', 'Completed') ON CONFLICT (code) DO NOTHING");
    }
}
