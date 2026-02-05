package zw.co.zivai.core_backend.models.lms;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class QuestionSkillId implements Serializable {
    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "skill_id")
    private UUID skillId;
}
