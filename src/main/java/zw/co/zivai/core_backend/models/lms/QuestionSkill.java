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
@Table(name = "question_skills", schema = "lms")
public class QuestionSkill {
    @EmbeddedId
    private QuestionSkillId id = new QuestionSkillId();

    @ManyToOne(optional = false)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(optional = false)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;
}
