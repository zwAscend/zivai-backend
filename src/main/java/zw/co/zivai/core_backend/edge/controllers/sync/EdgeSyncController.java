package zw.co.zivai.core_backend.edge.controllers.sync;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.sync.EdgeSyncStatusDto;
import zw.co.zivai.core_backend.edge.services.sync.EdgeSyncService;

@RestController
@Profile("edge")
@RequestMapping("/api/sync/edge")
@RequiredArgsConstructor
public class EdgeSyncController {
    private final EdgeSyncService edgeSyncService;

    @GetMapping("/status")
    public EdgeSyncStatusDto getStatus() {
        return edgeSyncService.getStatus();
    }

    @PostMapping("/run")
    public EdgeSyncStatusDto runSync() {
        edgeSyncService.runSyncCycle();
        return edgeSyncService.getStatus();
    }
}
