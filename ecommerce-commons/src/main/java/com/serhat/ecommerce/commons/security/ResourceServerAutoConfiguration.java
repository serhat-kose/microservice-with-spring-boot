package com.serhat.ecommerce.commons.security;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The default security chain for a downstream service: stateless, no login screens, caller
 * identity taken from the gateway-supplied headers, and everything authenticated unless a
 * service opts a path out via {@code ecommerce.security.public-paths} /
 * {@code public-get-paths}.
 *
 * <p>Before this existed the catalog and inventory services had no Spring Security on the
 * classpath at all, so any registered account could create, update or delete products and
 * overwrite stock levels. Method-level {@code @PreAuthorize} is enabled here so those
 * endpoints can require ADMIN/SELLER.
 *
 * <p>A service that needs something different simply declares its own
 * {@link SecurityFilterChain} bean, which takes precedence.
 */
@AutoConfiguration
@ConditionalOnClass({SecurityFilterChain.class, Filter.class})
// The servlet API is on the classpath even in the reactive gateway (spring-security-web
// pulls it in), so class presence alone is not enough to tell the two stacks apart.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "ecommerce.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ResourceServerProperties.class)
@EnableMethodSecurity
public class ResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain commonsSecurityFilterChain(HttpSecurity http,
                                                          IdentityHeaderFilter identityHeaderFilter,
                                                          ResourceServerProperties properties) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(identityHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
                    properties.getPublicGetPaths()
                            .forEach(path -> auth.requestMatchers(HttpMethod.GET, path).permitAll());
                    properties.getPublicPaths()
                            .forEach(path -> auth.requestMatchers(path).permitAll());
                    auth.anyRequest().authenticated();
                });

        return http.build();
    }
}
