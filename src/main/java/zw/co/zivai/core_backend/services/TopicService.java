package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateTopicRequest;
import zw.co.zivai.core_backend.dtos.TopicDto;
import zw.co.zivai.core_backend.dtos.UpdateTopicRequest;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.TopicRepository;

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

    public TopicDto create(UUID subjectId, CreateTopicRequest request) {
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setCode(request.getCode());
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setSequenceIndex(request.getSequenceIndex());

        return toDto(topicRepository.save(topic));
    }

    public TopicDto update(UUID topicId, UpdateTopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new IllegalArgumentException("Topic not found"));

        if (request.getCode() != null) {
            topic.setCode(request.getCode());
        }
        if (request.getName() != null) {
            topic.setName(request.getName());
        }
        if (request.getDescription() != null) {
            topic.setDescription(request.getDescription());
        }
        if (request.getSequenceIndex() != null) {
            topic.setSequenceIndex(request.getSequenceIndex());
        }

        return toDto(topicRepository.save(topic));
    }

    public void delete(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new IllegalArgumentException("Topic not found"));
        topic.setDeletedAt(Instant.now());
        topicRepository.save(topic);
    }

    private TopicDto toDto(Topic topic) {
        return TopicDto.builder()
            .id(topic.getId().toString())
            .subjectId(topic.getSubject() != null ? topic.getSubject().getId().toString() : null)
            .code(topic.getCode())
            .name(topic.getName())
            .description(topic.getDescription())
            .sequenceIndex(topic.getSequenceIndex())
            .build();
    }
}
