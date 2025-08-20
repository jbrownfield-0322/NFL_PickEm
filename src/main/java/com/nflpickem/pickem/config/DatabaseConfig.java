package com.nflpickem.pickem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                // Parse Railway's DATABASE_URL format: postgresql://user:pass@host:port/db
                URI uri = new URI(databaseUrl);
                
                // Extract components
                String username = uri.getUserInfo().split(":")[0];
                String password = uri.getUserInfo().split(":")[1];
                String host = uri.getHost();
                int port = uri.getPort();
                String database = uri.getPath().substring(1); // Remove leading slash
                
                // Build JDBC URL
                String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
                
                dataSource.setJdbcUrl(jdbcUrl);
                dataSource.setUsername(username);
                dataSource.setPassword(password);
                
            } catch (Exception e) {
                // Fallback: try simple string replacement
                String jdbcUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://");
                dataSource.setJdbcUrl(jdbcUrl);
            }
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
