package com.serhat.ecommerce.commons.security;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exposes {@link IdentityHeaderFilter} as a bean for services to insert into their
 * {@code SecurityFilterChain}.
 *
 * <p>Its plain servlet registration is deliberately disabled: registered that way it would
 * run <em>after</em> Spring Security's filter chain and so populate the security context too
 * late for authorization to see it. Services must wire it explicitly, e.g.
 * {@code http.addFilterBefore(identityHeaderFilter, UsernamePasswordAuthenticationFilter.class)}.
 */
@AutoConfiguration
@ConditionalOnClass({SecurityContextHolder.class, Filter.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityCommonsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdentityHeaderFilter identityHeaderFilter() {
        return new IdentityHeaderFilter();
    }

    @Bean
    public FilterRegistrationBean<IdentityHeaderFilter> identityHeaderFilterRegistration(
            IdentityHeaderFilter filter) {
        FilterRegistrationBean<IdentityHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
