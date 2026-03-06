package zw.co.zivai.core_backend.cloud.controllers.sync;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.cloud.services.sync.CloudSyncService;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPullResponse;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushRequest;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushResponse;

@RestController
@Profile("cloud")
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class CloudSyncController {
    private final CloudSyncService cloudSyncService;

    @PostMapping("/push")
    public SyncPushResponse push(
        @RequestBody SyncPushRequest request,
        @RequestHeader(value = "X-Edge-Auth-Key", required = false) String authKey
    ) {
        return cloudSyncService.acceptPush(request, authKey);
    }

    @GetMapping("/pull")
    public SyncPullResponse pull(
        @RequestHeader("X-Edge-Node-Id") UUID edgeNodeId,
        @RequestHeader(value = "X-Edge-Auth-Key", required = false) String authKey,
        @RequestParam(defaultValue = "0") long afterChangeId
    ) {
        return cloudSyncService.pullChanges(edgeNodeId, afterChangeId, authKey);
    }
}
