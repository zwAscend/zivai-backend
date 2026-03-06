package zw.co.zivai.core_backend.integration;

import java.io.IOException;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;

public abstract class AbstractPostgresIntegrationTest {
    private static EmbeddedPostgres embeddedPostgres;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        EmbeddedPostgres postgres = embeddedPostgres();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("app.seed.enabled", () -> false);
        registry.add("app.sync.edge.worker-enabled", () -> false);
        registry.add("app.sync.edge.capture-enabled", () -> false);
    }

    private static synchronized EmbeddedPostgres embeddedPostgres() {
        if (embeddedPostgres != null) {
            return embeddedPostgres;
        }
        try {
            embeddedPostgres = EmbeddedPostgres.builder().start();
            Flyway.configure()
                .dataSource(embeddedPostgres.getJdbcUrl("postgres"), "postgres", "")
                .locations("classpath:db/testschema")
                .load()
                .migrate();
            return embeddedPostgres;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start embedded PostgreSQL for integration tests", ex);
        }
    }
}
