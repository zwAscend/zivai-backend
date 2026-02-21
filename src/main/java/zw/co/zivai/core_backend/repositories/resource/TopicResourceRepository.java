package zw.co.zivai.core_backend.repositories.resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import zw.co.zivai.core_backend.models.lms.TopicResource;

public interface TopicResourceRepository extends JpaRepository<TopicResource, UUID> {
    @EntityGraph(attributePaths = {"topic", "resource", "resource.subject", "resource.uploadedBy"})
    List<TopicResource> findByTopic_IdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(UUID topicId);
    @EntityGraph(attributePaths = {"topic", "resource", "resource.subject", "resource.uploadedBy"})
    List<TopicResource> findByTopic_IdInAndDeletedAtIsNullOrderByTopic_SequenceIndexAscDisplayOrderAscCreatedAtAsc(List<UUID> topicIds);
    @EntityGraph(attributePaths = {"topic"})
    List<TopicResource> findByResource_IdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(UUID resourceId);
    @EntityGraph(attributePaths = {"topic"})
    List<TopicResource> findByResource_IdInAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(List<UUID> resourceIds);
    @EntityGraph(attributePaths = {"topic"})
    List<TopicResource> findByResource_Id(UUID resourceId);
    Optional<TopicResource> findByResource_IdAndTopic_Id(UUID resourceId, UUID topicId);
}
