package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateTopicRequest;
import zw.co.zivai.core_backend.dtos.TopicDto;
import zw.co.zivai.core_backend.dtos.UpdateTopicRequest;
import zw.co.zivai.core_backend.services.TopicService;

@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
public class AdminTopicController {
    private final TopicService topicService;

    @GetMapping("/{subjectId}/topics")
    public List<TopicDto> listTopics(@PathVariable UUID subjectId) {
        return topicService.listBySubject(subjectId);
    }

    @PostMapping("/{subjectId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicDto createTopic(@PathVariable UUID subjectId, @RequestBody CreateTopicRequest request) {
        return topicService.create(subjectId, request);
    }

    @PutMapping("/topics/{topicId}")
    public TopicDto updateTopic(@PathVariable UUID topicId, @RequestBody UpdateTopicRequest request) {
        return topicService.update(topicId, request);
    }

    @DeleteMapping("/topics/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable UUID topicId) {
        topicService.delete(topicId);
    }
}
