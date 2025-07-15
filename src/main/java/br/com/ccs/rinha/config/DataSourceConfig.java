package br.com.ccs.rinha.config;

import br.com.ccs.rinha.exception.DatasourceException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.Objects;

@Singleton
public class DataSourceConfig {

    private final Logger log;

    @Inject
    public DataSourceConfig(Logger log) {
        this.log = log;
    }

    @Produces
    @Singleton
    @RinhaDataSource
    public DataSource createDataSource() {
        String minIdleEnv = System.getenv("datasource-minimum-idle");
        String maxPoolEnv = System.getenv("datasource-maximum-pool-size");
        String timeoutEnv = System.getenv("datasource-timeout");

        int minIdle = minIdleEnv == null ? 10 : Integer.parseInt(minIdleEnv);
        int maxPoolSize = maxPoolEnv == null ? 10 : Integer.parseInt(maxPoolEnv);
        int dataSourceTimeout = timeoutEnv == null ? 5000 : Integer.parseInt(timeoutEnv);
        String dataSourceUrl = System.getenv("datasource-url");
        String datasourceUsername = System.getenv("datasource-username");
        String dataSourcePassword = System.getenv("datasource-password");
        String dataSourceClassName = System.getenv("datasource-class-name");

        log.info("Data Source URL: {}", dataSourceUrl);
        log.info("Data Source Username: {}", datasourceUsername);
        log.info("Data Source Password: {}", dataSourcePassword);
        log.info("Data Source Class Name: {}", dataSourceClassName);
        log.info("Data Source Timeout: {}", dataSourceTimeout);
        log.info("Data Source Minimum Idle: {}", minIdle);
        log.info("Data Source Maximum Pool Size: {}", maxPoolSize);

        validate(dataSourceUrl, datasourceUsername, dataSourcePassword, dataSourceClassName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(dataSourcePassword);
        config.setMinimumIdle(minIdle);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(dataSourceTimeout);
        config.setDriverClassName(dataSourceClassName);

        return new HikariDataSource(config);
    }

    private void validate(String dataSourceUrl, String datasourceUsername, String dataSourcePassword, String dataSourceClassName) {
        if (Objects.isNull(dataSourceUrl) || dataSourceUrl.isBlank()) {
            throw new DatasourceException("Data Source URL must not be null");
        }
        if (Objects.isNull(datasourceUsername) || datasourceUsername.isBlank()) {
            throw new DatasourceException("Data Source Username must not be null");
        }
        if (Objects.isNull(dataSourcePassword) || dataSourcePassword.isBlank()) {
            throw new DatasourceException("Data Source Password must not be null");
        }
        if (Objects.isNull(dataSourceClassName) || dataSourceClassName.isBlank()) {
            throw new DatasourceException("Data Source Class Name must not be null");
        }
    }

    public void close(@Disposes @RinhaDataSource DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }
}
