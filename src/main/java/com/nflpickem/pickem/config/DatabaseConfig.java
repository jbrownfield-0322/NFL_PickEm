package com.nflpickem.pickem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        
        // Convert Railway's postgresql:// URL to jdbc:postgresql:// format
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            String jdbcUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://");
            dataSource.setJdbcUrl(jdbcUrl);
        } else {
            // Fallback for local development
            dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/nflpickem");
            dataSource.setUsername("postgres");
            dataSource.setPassword("password");
        }
        
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        
        return dataSource;
    }
}
