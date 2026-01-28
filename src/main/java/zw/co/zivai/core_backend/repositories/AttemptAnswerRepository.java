package zw.co.zivai.core_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AttemptAnswer;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
}
