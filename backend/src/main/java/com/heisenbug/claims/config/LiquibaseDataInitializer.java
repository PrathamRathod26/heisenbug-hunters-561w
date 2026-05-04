package com.heisenbug.claims.config;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LiquibaseDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LiquibaseDataInitializer.class);
    private static final String CHANGELOG = "classpath:db/changelog/db.changelog-master.yaml";

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;

    public LiquibaseDataInitializer(DataSource dataSource, ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Running Liquibase seed changelog: {}", CHANGELOG);
        SpringLiquibase lb = new SpringLiquibase();
        lb.setDataSource(dataSource);
        lb.setChangeLog(CHANGELOG);
        lb.setResourceLoader(resourceLoader);
        lb.setShouldRun(true);
        lb.afterPropertiesSet();
        log.info("Liquibase seed changelog applied.");
    }
}
