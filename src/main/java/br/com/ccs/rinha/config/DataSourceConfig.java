package br.com.ccs.rinha.config;

import br.com.ccs.rinha.exception.DatasourceException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import static java.util.Objects.isNull;


public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    private static final DataSource instance;

    static {
        instance = initDataSource();
    }

    private DataSourceConfig() {
    }

    public static DataSource getInstance() {
        return instance;
    }

    private static DataSource initDataSource() {
        String minIdleEnv = System.getenv("datasource-minimum-idle").trim();
        String maxPoolEnv = System.getenv("datasource-maximum-pool-size").trim();
        String timeoutEnv = System.getenv("datasource-timeout").trim();

        int minIdle = minIdleEnv.isBlank() ? 10 : Integer.parseInt(minIdleEnv);
        int maxPoolSize = maxPoolEnv.isBlank() ? 10 : Integer.parseInt(maxPoolEnv);
        int dataSourceTimeout = timeoutEnv.isBlank() ? 5000 : Integer.parseInt(timeoutEnv);

        String dataSourceUrl = System.getenv("datasource-url").trim();
        String datasourceUsername = System.getenv("datasource-username").trim();
        String dataSourcePassword = System.getenv("datasource-password").trim();
        String dataSourceClassName = System.getenv("datasource-class-name").trim();

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
        config.setAutoCommit(true);
        config.setConnectionTestQuery("select 1 where 1=1");
        log.info("Data Source Configured {}", config);

        return new HikariDataSource(config);
    }

    private static void validate(String dataSourceUrl, String datasourceUsername, String dataSourcePassword, String dataSourceClassName) {
        if (isNull(dataSourceUrl) || dataSourceUrl.isBlank()) {
            throw new DatasourceException("Data Source URL must not be null");
        }
        if (isNull(datasourceUsername) || datasourceUsername.isBlank()) {
            throw new DatasourceException("Data Source Username must not be null");
        }
        if (isNull(dataSourcePassword) || dataSourcePassword.isBlank()) {
            throw new DatasourceException("Data Source Password must not be null");
        }
        if (isNull(dataSourceClassName) || dataSourceClassName.isBlank()) {
            throw new DatasourceException("Data Source Class Name must not be null");
        }
    }
}
