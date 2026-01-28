package zw.co.zivai.core_backend.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI coreBackendOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("zivAI Core Backend API")
                .version("0.1.0")
                .description("Core LMS APIs for zivAI (GaussDB schema aligned)"));
    }
}
