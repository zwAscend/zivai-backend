package zw.co.zivai.core_backend.common.aspects;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.models.lms.classroom.ClassSubject;
import zw.co.zivai.core_backend.common.models.lms.students.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.services.development.DevelopmentService;

@Aspect
@Component
@RequiredArgsConstructor
public class StudentSubjectEnrolmentAspect {
    private final DevelopmentService developmentService;

    @AfterReturning(
        pointcut = "execution(* zw.co.zivai.core_backend.common.repositories.classroom.StudentSubjectEnrolmentRepository.save(..))",
        returning = "result"
    )
    public void afterSave(Object result) {
        if (result instanceof StudentSubjectEnrolment enrolment) {
            ensureStarterPlan(enrolment);
        }
    }

    @AfterReturning(
        pointcut = "execution(* zw.co.zivai.core_backend.common.repositories.classroom.StudentSubjectEnrolmentRepository.saveAndFlush(..))",
        returning = "result"
    )
    public void afterSaveAndFlush(Object result) {
        if (result instanceof StudentSubjectEnrolment enrolment) {
            ensureStarterPlan(enrolment);
        }
    }

    @AfterReturning(
        pointcut = "execution(* zw.co.zivai.core_backend.common.repositories.classroom.StudentSubjectEnrolmentRepository.saveAll(..)) || " +
            "execution(* zw.co.zivai.core_backend.common.repositories.classroom.StudentSubjectEnrolmentRepository.saveAllAndFlush(..))",
        returning = "result"
    )
    public void afterSaveAll(Object result) {
        if (!(result instanceof Iterable<?> items)) {
            return;
        }

        List<StudentSubjectEnrolment> enrolments = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof StudentSubjectEnrolment enrolment) {
                enrolments.add(enrolment);
            }
        }

        for (StudentSubjectEnrolment enrolment : enrolments) {
            ensureStarterPlan(enrolment);
        }
    }

    private void ensureStarterPlan(StudentSubjectEnrolment enrolment) {
        if (enrolment == null || enrolment.getDeletedAt() != null) {
            return;
        }

        User student = enrolment.getStudent();
        ClassSubject classSubject = enrolment.getClassSubject();
        Subject subject = classSubject == null ? null : classSubject.getSubject();
        if (student == null || student.getId() == null || subject == null || subject.getId() == null) {
            return;
        }

        developmentService.ensureStarterPlanForStudentSubject(student.getId(), subject.getId());
    }
}
