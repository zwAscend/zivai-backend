package zw.co.zivai.core_backend.common.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
public class SyncConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient restClient(RestClient.Builder builder, SyncProperties syncProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(1000, syncProperties.getEdge().getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Math.max(1000, syncProperties.getEdge().getReadTimeoutMs()));
        return builder.requestFactory(requestFactory).build();
    }
}
