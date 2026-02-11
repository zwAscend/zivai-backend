package zw.co.zivai.core_backend.services.subject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.subjects.CreateTopicRequest;
import zw.co.zivai.core_backend.dtos.subjects.CreateCurriculumRequest;
import zw.co.zivai.core_backend.dtos.subjects.TopicDto;
import zw.co.zivai.core_backend.dtos.subjects.UpdateTopicRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    public List<TopicDto> listBySubject(UUID subjectId) {
        return topicRepository.findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(subjectId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<TopicDto> createCurriculum(UUID subjectId, CreateCurriculumRequest request) {
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));
        List<CreateTopicRequest> topics = request.getTopics();
        if (topics == null || topics.isEmpty()) {
            throw new BadRequestException("topics are required");
        }

        if (request.isReplaceExisting()) {
            List<Topic> existing = topicRepository.findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(subjectId);
            Instant deletedAt = Instant.now();
            existing.forEach(topic -> topic.setDeletedAt(deletedAt));
            topicRepository.saveAll(existing);
        }

        return topics.stream()
            .map(topicRequest -> {
                if (topicRequest.getCode() == null || topicRequest.getCode().isBlank()) {
                    throw new BadRequestException("Topic code is required");
                }
                if (topicRequest.getName() == null || topicRequest.getName().isBlank()) {
                    throw new BadRequestException("Topic name is required");
                }
                Topic topic = new Topic();
                topic.setSubject(subject);
                topic.setCode(topicRequest.getCode());
                topic.setName(topicRequest.getName());
                topic.setDescription(topicRequest.getDescription());
                topic.setObjectives(topicRequest.getObjectives() != null ? topicRequest.getObjectives() : topicRequest.getDescription());
                topic.setSequenceIndex(topicRequest.getSequenceIndex());
                return toDto(topicRepository.save(topic));
            })
            .collect(Collectors.toList());
    }

    public TopicDto create(UUID subjectId, CreateTopicRequest request) {
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Topic code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Topic name is required");
        }

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setCode(request.getCode());
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setObjectives(request.getObjectives() != null ? request.getObjectives() : request.getDescription());
        topic.setSequenceIndex(request.getSequenceIndex());

        return toDto(topicRepository.save(topic));
    }

    public TopicDto update(UUID topicId, UpdateTopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new NotFoundException("Topic not found: " + topicId));

        if (request.getCode() != null) {
            topic.setCode(request.getCode());
        }
        if (request.getName() != null) {
            topic.setName(request.getName());
        }
        if (request.getDescription() != null) {
            topic.setDescription(request.getDescription());
        }
        if (request.getObjectives() != null) {
            topic.setObjectives(request.getObjectives());
        }
        if (request.getSequenceIndex() != null) {
            topic.setSequenceIndex(request.getSequenceIndex());
        }

        return toDto(topicRepository.save(topic));
    }

    public void delete(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new NotFoundException("Topic not found: " + topicId));
        topic.setDeletedAt(Instant.now());
        topicRepository.save(topic);
    }

    private TopicDto toDto(Topic topic) {
        String objectives = topic.getObjectives() != null ? topic.getObjectives() : topic.getDescription();
        String description = topic.getDescription() != null ? topic.getDescription() : topic.getObjectives();
        return TopicDto.builder()
            .id(topic.getId().toString())
            .subjectId(topic.getSubject() != null ? topic.getSubject().getId().toString() : null)
            .code(topic.getCode())
            .name(topic.getName())
            .description(description)
            .objectives(objectives)
            .sequenceIndex(topic.getSequenceIndex())
            .build();
    }
}
