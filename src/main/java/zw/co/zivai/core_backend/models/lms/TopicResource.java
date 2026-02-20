package zw.co.zivai.core_backend.models.lms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(
    name = "topic_resources",
    schema = "lms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_topic_resources_topic_resource", columnNames = {"topic_id", "resource_id"})
    }
)
public class TopicResource extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
