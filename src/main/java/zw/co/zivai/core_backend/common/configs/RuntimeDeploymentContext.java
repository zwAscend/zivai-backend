package zw.co.zivai.core_backend.common.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Getter
public class RuntimeDeploymentContext {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeDeploymentContext.class);

    private final Environment environment;
    private final RuntimeProperties runtimeProperties;
    private final SyncProperties syncProperties;

    private RuntimeProperties.RuntimeRole runtimeRole;
    private RuntimeProperties.DeploymentScenario deploymentScenario;

    @PostConstruct
    void initialize() {
        boolean edgeProfile = environment.matchesProfiles("edge");
        boolean cloudProfile = environment.matchesProfiles("cloud");

        RuntimeProperties.RuntimeRole derivedRole = cloudProfile
            ? RuntimeProperties.RuntimeRole.CLOUD
            : RuntimeProperties.RuntimeRole.EDGE;

        if (runtimeProperties.getRole() != null && runtimeProperties.getRole() != derivedRole) {
            throw new IllegalStateException(
                "Configured app.runtime.role=" + runtimeProperties.getRole()
                    + " conflicts with active Spring profile role " + derivedRole + "."
            );
        }

        this.runtimeRole = derivedRole;
        this.deploymentScenario = resolveScenario(derivedRole, runtimeProperties.getScenario());

        LOG.info("Runtime role resolved to {} with deployment scenario {}", runtimeRole, deploymentScenario);
    }

    public boolean isEdgeRole() {
        return runtimeRole == RuntimeProperties.RuntimeRole.EDGE;
    }

    public boolean isCloudRole() {
        return runtimeRole == RuntimeProperties.RuntimeRole.CLOUD;
    }

    public boolean isHybridEdge() {
        return deploymentScenario == RuntimeProperties.DeploymentScenario.HYBRID;
    }

    public boolean isEdgeOnly() {
        return deploymentScenario == RuntimeProperties.DeploymentScenario.EDGE_ONLY;
    }

    public boolean isCloudOnly() {
        return deploymentScenario == RuntimeProperties.DeploymentScenario.CLOUD_ONLY;
    }

    private RuntimeProperties.DeploymentScenario resolveScenario(
        RuntimeProperties.RuntimeRole role,
        RuntimeProperties.DeploymentScenario configuredScenario
    ) {
        RuntimeProperties.DeploymentScenario scenario = configuredScenario == null
            ? RuntimeProperties.DeploymentScenario.AUTO
            : configuredScenario;

        if (role == RuntimeProperties.RuntimeRole.CLOUD) {
            if (scenario == RuntimeProperties.DeploymentScenario.AUTO) {
                return RuntimeProperties.DeploymentScenario.CLOUD_ONLY;
            }
            if (scenario != RuntimeProperties.DeploymentScenario.CLOUD_ONLY) {
                throw new IllegalStateException("Cloud runtime only supports app.runtime.scenario=CLOUD_ONLY or AUTO.");
            }
            return scenario;
        }

        if (scenario == RuntimeProperties.DeploymentScenario.AUTO) {
            return hasCloudSyncTarget()
                ? RuntimeProperties.DeploymentScenario.HYBRID
                : RuntimeProperties.DeploymentScenario.EDGE_ONLY;
        }
        if (scenario == RuntimeProperties.DeploymentScenario.CLOUD_ONLY) {
            throw new IllegalStateException("Edge runtime cannot use app.runtime.scenario=CLOUD_ONLY.");
        }
        if (scenario == RuntimeProperties.DeploymentScenario.HYBRID && !hasCloudSyncTarget()) {
            throw new IllegalStateException("HYBRID edge runtime requires app.sync.edge.cloud-base-url.");
        }
        return scenario;
    }

    private boolean hasCloudSyncTarget() {
        String cloudBaseUrl = syncProperties.getEdge().getCloudBaseUrl();
        return cloudBaseUrl != null && !cloudBaseUrl.isBlank();
    }
}
