package zw.co.zivai.core_backend.common.configs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentPlanRuntimeSchemaInitializer implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(StudentPlanRuntimeSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Set<String> existingColumns = new HashSet<>(jdbcTemplate.queryForList(
            """
                select column_name
                from information_schema.columns
                where table_schema = 'lms'
                  and table_name = 'student_plans'
            """,
            String.class
        ));

        List<String> missingColumns = new ArrayList<>();
        if (!existingColumns.contains("active_step_id")) {
            missingColumns.add("active_step_id");
        }
        if (!existingColumns.contains("completed_step_ids")) {
            missingColumns.add("completed_step_ids");
        }

        if (missingColumns.isEmpty()) {
            return;
        }

        LOG.warn(
            "Detected older lms.student_plans schema. Applying runtime compatibility patch for columns: {}",
            missingColumns
        );

        jdbcTemplate.execute("""
            ALTER TABLE lms.student_plans
              ADD COLUMN IF NOT EXISTS active_step_id uuid REFERENCES lms.plan_steps(id) ON DELETE SET NULL
        """);
        jdbcTemplate.execute("""
            ALTER TABLE lms.student_plans
              ADD COLUMN IF NOT EXISTS completed_step_ids jsonb NOT NULL DEFAULT '[]'::jsonb
        """);
        jdbcTemplate.update("""
            UPDATE lms.student_plans
            SET completed_step_ids = '[]'::jsonb
            WHERE completed_step_ids IS NULL
        """);

        LOG.info("Ensured lms.student_plans runtime state columns exist.");
    }
}
