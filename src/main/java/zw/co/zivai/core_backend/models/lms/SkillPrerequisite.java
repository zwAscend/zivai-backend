package zw.co.zivai.core_backend.models.lms;

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
@Table(name = "skill_prerequisites", schema = "lms")
public class SkillPrerequisite {
    @EmbeddedId
    private SkillPrerequisiteId id = new SkillPrerequisiteId();

    @ManyToOne(optional = false)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne(optional = false)
    @MapsId("prerequisiteSkillId")
    @JoinColumn(name = "prerequisite_skill_id")
    private Skill prerequisiteSkill;
}
