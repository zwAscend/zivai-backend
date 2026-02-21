package zw.co.zivai.core_backend.repositories.peerstudy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.PeerStudyRequest;

public interface PeerStudyRequestRepository extends JpaRepository<PeerStudyRequest, UUID> {
    @EntityGraph(attributePaths = {"subject", "topic", "createdBy"})
    Optional<PeerStudyRequest> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        select distinct r
        from PeerStudyRequest r
        join fetch r.subject s
        left join fetch r.topic t
        join fetch r.createdBy cb
        where r.deletedAt is null
          and (:subjectId is null or s.id = :subjectId)
          and (:topicId is null or t.id = :topicId)
          and (:type is null or r.requestType = :type)
          and (:status is null or r.statusCode = :status)
          and (:createdBy is null or cb.id = :createdBy)
          and (:joinedBy is null or exists (
                select 1
                from PeerStudyRequestMember m
                where m.request = r
                  and m.user.id = :joinedBy
                  and m.deletedAt is null
          ))
        order by r.createdAt desc
    """)
    List<PeerStudyRequest> findFilteredForList(@Param("subjectId") UUID subjectId,
                                               @Param("topicId") UUID topicId,
                                               @Param("type") String type,
                                               @Param("status") String status,
                                               @Param("createdBy") UUID createdBy,
                                               @Param("joinedBy") UUID joinedBy);
}
