package co.cobre.cbmm.accounts.adapters.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import javax.sql.DataSource;

/**
 * Configuración de DataSource para la aplicación.
 * Esta configuración NO se aplica en el perfil 'test'.
 */
@Configuration
public class DataSourceConfig {

    /** Nombre de la clase driver de la base de datos. */
    @Value("${database.driver-class-name}")
    private String driverClassName;

    /** URL de conexión a la base de datos. */
    @Value("${database.url}")
    private String url;

    /** Nombre de usuario para la conexión a la base de datos. */
    @Value("${database.username}")
    private String username;

    /** Contraseña para la conexión a la base de datos. */
    @Value("${database.password}")
    private String password;

    /**
     * Bean principal de DataSource.
     *
     * @return El DataSource configurado para la aplicación.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }
}
