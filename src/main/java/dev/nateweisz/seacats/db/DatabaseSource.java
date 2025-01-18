package dev.nateweisz.seacats.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.freya022.botcommands.api.core.db.HikariSourceSupplier;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSource implements HikariSourceSupplier {
    private final HikariDataSource source;

    public DatabaseSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
            "jdbc:h2:file:./cache;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
        );
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);

        source = new HikariDataSource(config);
        Flyway.configure()
            .dataSource(source)
            .schemas("bc")
            .locations("bc_database_scripts")
            .validateMigrationNaming(true)
            .loggers("slf4j")
            .baselineOnMigrate(true)
            .load()
            .migrate();
    }

    @Override
    public @NotNull HikariDataSource getSource() {
        return source;
    }
}
