package zw.co.zivai.core_backend.repositories.peerstudy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.PeerStudyRequestMember;

public interface PeerStudyRequestMemberRepository extends JpaRepository<PeerStudyRequestMember, UUID> {
    @EntityGraph(attributePaths = {"user"})
    List<PeerStudyRequestMember> findByRequest_IdAndDeletedAtIsNullOrderByJoinedAtAsc(UUID requestId);

    Optional<PeerStudyRequestMember> findByRequest_IdAndUser_IdAndDeletedAtIsNull(UUID requestId, UUID userId);

    long countByRequest_IdAndDeletedAtIsNull(UUID requestId);

    @Query("""
        select m.request.id, count(m.id)
        from PeerStudyRequestMember m
        where m.deletedAt is null
          and m.request.id in :requestIds
        group by m.request.id
    """)
    List<Object[]> countByRequestIds(@Param("requestIds") Collection<UUID> requestIds);

    @Query("""
        select m.request.id
        from PeerStudyRequestMember m
        where m.deletedAt is null
          and m.user.id = :userId
          and m.request.id in :requestIds
    """)
    List<UUID> findJoinedRequestIds(@Param("userId") UUID userId,
                                    @Param("requestIds") Collection<UUID> requestIds);
}
