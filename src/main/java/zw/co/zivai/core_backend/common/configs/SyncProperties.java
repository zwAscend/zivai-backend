package zw.co.zivai.core_backend.common.configs;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.sync")
public class SyncProperties {
    private final Edge edge = new Edge();
    private final Cloud cloud = new Cloud();

    @Getter
    @Setter
    public static class Edge {
        private boolean workerEnabled = true;
        private boolean captureEnabled = true;
        private int batchSize = 100;
        private long pollIntervalMs = 30000L;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 45000;
        private int httpMaxAttempts = 3;
        private long retryBackoffMs = 1000L;
        private String cloudBaseUrl;
        private UUID nodeId;
        private String authKey;
    }

    @Getter
    @Setter
    public static class Cloud {
        private int pullBatchSize = 200;
        private boolean requireAuth = false;
    }
}
