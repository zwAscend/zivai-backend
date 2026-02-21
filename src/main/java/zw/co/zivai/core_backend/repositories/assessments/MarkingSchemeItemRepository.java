package zw.co.zivai.core_backend.repositories.assessments;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.MarkingSchemeItem;

public interface MarkingSchemeItemRepository extends JpaRepository<MarkingSchemeItem, UUID> {
    @Query("""
        select msi
        from MarkingSchemeItem msi
        join fetch msi.markingScheme ms
        where ms.id in :schemeIds
          and ms.deletedAt is null
          and msi.deletedAt is null
        order by ms.id asc, msi.stepIndex asc
    """)
    List<MarkingSchemeItem> findActiveByMarkingSchemeIds(@Param("schemeIds") Collection<UUID> schemeIds);
}
