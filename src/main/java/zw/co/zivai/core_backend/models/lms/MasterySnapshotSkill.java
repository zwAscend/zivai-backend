package zw.co.zivai.core_backend.models.lms;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mastery_snapshot_skills", schema = "lms")
public class MasterySnapshotSkill {
    @EmbeddedId
    private MasterySnapshotSkillId id = new MasterySnapshotSkillId();

    @ManyToOne(optional = false)
    @MapsId("masterySnapshotId")
    @JoinColumn(name = "mastery_snapshot_id")
    private MasterySnapshot masterySnapshot;

    @ManyToOne(optional = false)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "mastery_prob", nullable = false)
    private Double masteryProb;
}
