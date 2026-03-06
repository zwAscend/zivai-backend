package zw.co.zivai.core_backend.common.configs;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RuntimeRoleGuard {
    private final Environment environment;

    @PostConstruct
    void validateProfiles() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean edge = activeProfiles.contains("edge");
        boolean cloud = activeProfiles.contains("cloud");
        if (edge && cloud) {
            throw new IllegalStateException("Only one runtime role may be active at a time: edge or cloud.");
        }
        if (!edge && !cloud && activeProfiles.size() > 0) {
            throw new IllegalStateException("Runtime must activate either the edge or cloud profile.");
        }
    }
}
