package org.dreamabout.sw.multitenancy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "multitenancy")
public class MultitenancyProperties {
    /**
     * The name of the default/public schema.
     */
    private String defaultSchema = "public";
}
