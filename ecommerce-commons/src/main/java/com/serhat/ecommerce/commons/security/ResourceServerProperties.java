package com.serhat.ecommerce.commons.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-service tuning for the shared resource-server security chain.
 */
@ConfigurationProperties(prefix = "ecommerce.security")
public class ResourceServerProperties {

    /** Paths reachable without an authenticated caller, for any HTTP method. */
    private List<String> publicPaths = new ArrayList<>();

    /** Paths reachable without an authenticated caller for GET only (e.g. public catalog browsing). */
    private List<String> publicGetPaths = new ArrayList<>();

    /** Set false to opt out of the shared chain and declare a bespoke one instead. */
    private boolean enabled = true;

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public List<String> getPublicGetPaths() {
        return publicGetPaths;
    }

    public void setPublicGetPaths(List<String> publicGetPaths) {
        this.publicGetPaths = publicGetPaths;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
