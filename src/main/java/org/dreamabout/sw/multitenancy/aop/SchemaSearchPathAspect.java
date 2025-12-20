package org.dreamabout.sw.multitenancy.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.dreamabout.sw.multitenancy.config.MultitenancyProperties;
import org.dreamabout.sw.multitenancy.core.MultitenancyThreadContext;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaSearchPathAspect {

    private final JdbcTemplate jdbcTemplate;
    private final MultitenancyProperties properties;

    @Pointcut("@within(org.dreamabout.sw.multitenancy.core.Multitenant) || @annotation(org.dreamabout.sw.multitenancy.core.Multitenant)")
    public void multitenantMethod() {
    }

    @Around("multitenantMethod()")
    public Object aroundMultitenant(ProceedingJoinPoint pjp) throws Throwable {

        var tenantSchema = Optional.ofNullable(TenantContext.getCurrentTenant())
                .map(TenantIdentifier::getTenantId)
                .orElse(properties.getDefaultSchema());

        var expectedPath = tenantSchema.equals(properties.getDefaultSchema())
                ? properties.getDefaultSchema()
                : tenantSchema + ", " + properties.getDefaultSchema();

        if (!expectedPath.equals(MultitenancyThreadContext.getCurrentSearchPath())) {
            log.info("Setting search path to {}", expectedPath);
            jdbcTemplate.execute("SET search_path TO " + expectedPath);
            MultitenancyThreadContext.setCurrentSearchPath(expectedPath);
        }

        return pjp.proceed();
    }
}
