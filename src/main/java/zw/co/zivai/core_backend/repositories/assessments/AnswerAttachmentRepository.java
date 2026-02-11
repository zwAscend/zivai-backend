package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AnswerAttachment;

public interface AnswerAttachmentRepository extends JpaRepository<AnswerAttachment, UUID> {
    List<AnswerAttachment> findByAttemptAnswer_Id(UUID attemptAnswerId);
}
