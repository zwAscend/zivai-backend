package zw.co.zivai.core_backend.repositories.development;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.ReteachCard;

public interface ReteachCardRepository extends JpaRepository<ReteachCard, UUID> {
    @EntityGraph(attributePaths = {"subject", "topic"})
    List<ReteachCard> findByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"subject", "topic"})
    Optional<ReteachCard> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"subject", "topic"})
    List<ReteachCard> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);

    @EntityGraph(attributePaths = {"subject", "topic"})
    List<ReteachCard> findByTopic_IdAndDeletedAtIsNull(UUID topicId);

    @Query("""
        select rc
        from ReteachCard rc
        left join fetch rc.subject s
        left join fetch rc.topic t
        where rc.deletedAt is null
          and (:subjectId is null or s.id = :subjectId)
          and (:topicId is null or t.id = :topicId)
          and (:priority is null or rc.priorityCode = :priority)
          and (:status is null or rc.statusCode = :status)
    """)
    List<ReteachCard> findFilteredForList(@Param("subjectId") UUID subjectId,
                                          @Param("topicId") UUID topicId,
                                          @Param("priority") String priority,
                                          @Param("status") String status);
}
