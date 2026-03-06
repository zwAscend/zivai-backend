package zw.co.zivai.core_backend.common.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.runtime")
public class RuntimeProperties {
    private RuntimeRole role;
    private DeploymentScenario scenario = DeploymentScenario.AUTO;

    public enum RuntimeRole {
        EDGE,
        CLOUD
    }

    public enum DeploymentScenario {
        AUTO,
        CLOUD_ONLY,
        EDGE_ONLY,
        HYBRID
    }
}
