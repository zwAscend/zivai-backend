package zw.co.zivai.core_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.School;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findByCode(String code);
}

