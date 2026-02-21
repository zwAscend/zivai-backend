package zw.co.zivai.core_backend.controllers.peerstudy;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.peerstudy.CreatePeerStudyRequest;
import zw.co.zivai.core_backend.dtos.peerstudy.JoinPeerStudyRequest;
import zw.co.zivai.core_backend.dtos.peerstudy.PeerStudyRequestDetailDto;
import zw.co.zivai.core_backend.dtos.peerstudy.PeerStudyRequestDto;
import zw.co.zivai.core_backend.dtos.peerstudy.UpdatePeerStudyRequest;
import zw.co.zivai.core_backend.services.peerstudy.PeerStudyService;

@RestController
@RequestMapping("/api/peer-study/requests")
@RequiredArgsConstructor
public class PeerStudyController {
    private final PeerStudyService peerStudyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeerStudyRequestDto create(@RequestBody CreatePeerStudyRequest request) {
        return peerStudyService.create(request);
    }

    @GetMapping
    public List<PeerStudyRequestDto> list(@RequestParam(required = false) UUID subjectId,
                                          @RequestParam(required = false) UUID topicId,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) UUID createdBy,
                                          @RequestParam(required = false) UUID joinedBy) {
        return peerStudyService.list(subjectId, topicId, type, status, createdBy, joinedBy);
    }

    @GetMapping("/{id}")
    public PeerStudyRequestDetailDto get(@PathVariable UUID id,
                                         @RequestParam(required = false) UUID viewerId) {
        return peerStudyService.getDetail(id, viewerId);
    }

    @PutMapping("/{id}")
    public PeerStudyRequestDto update(@PathVariable UUID id,
                                      @RequestBody UpdatePeerStudyRequest request) {
        return peerStudyService.update(id, request);
    }

    @PostMapping("/{id}/join")
    public PeerStudyRequestDetailDto join(@PathVariable UUID id,
                                          @RequestBody JoinPeerStudyRequest request) {
        return peerStudyService.join(id, request);
    }
}
