package zw.co.zivai.core_backend.repositories.termforecast;

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

    @Query("""
        select tf from TermForecast tf
        join tf.classSubject cs
        where tf.deletedAt is null
          and cs.deletedAt is null
          and cs.subject.id = :subjectId
          and tf.term = :term
          and tf.academicYear = :academicYear
        order by tf.updatedAt desc
    """)
    List<TermForecast> findLatestBySubjectTermAndYear(
        @Param("subjectId") UUID subjectId,
        @Param("term") String term,
        @Param("academicYear") String academicYear
    );

    Optional<TermForecast> findByIdAndDeletedAtIsNull(UUID id);

    @Query(value = "select expected_topic_ids::text from lms.term_forecasts where id = :id", nativeQuery = true)
    String findExpectedTopicIdsTextById(@Param("id") UUID id);

    Optional<TermForecast> findByClassSubject_IdAndTermAndAcademicYearAndDeletedAtIsNull(
        UUID classSubjectId,
        String term,
        String academicYear
    );

    Optional<TermForecast> findByClassSubject_IdAndTermAndAcademicYear(
        UUID classSubjectId,
        String term,
        String academicYear
    );
}
