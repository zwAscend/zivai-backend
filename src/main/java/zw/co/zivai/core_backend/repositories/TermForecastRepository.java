package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.TermForecast;

public interface TermForecastRepository extends JpaRepository<TermForecast, UUID> {
    @Query("""
        select tf from TermForecast tf
        join tf.classSubject cs
        where tf.deletedAt is null
          and cs.deletedAt is null
          and cs.subject.id = :subjectId
          and (:term is null or tf.term = :term)
        order by tf.updatedAt desc
    """)
    List<TermForecast> findLatestBySubjectAndTerm(@Param("subjectId") UUID subjectId, @Param("term") String term);

    Optional<TermForecast> findByIdAndDeletedAtIsNull(UUID id);

    Optional<TermForecast> findByClassSubject_IdAndTermAndAcademicYearAndDeletedAtIsNull(
        UUID classSubjectId,
        String term,
        String academicYear
    );
}
