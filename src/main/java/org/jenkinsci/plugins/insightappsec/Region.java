package org.jenkinsci.plugins.insightappsec;

import org.jenkinsci.plugins.insightappsec.exception.UnrecognizedRegionException;

import java.util.Arrays;

public enum Region {

    US(Messages.selectors_us(), resolveAPIHost("us")),
    CA(Messages.selectors_ca(), resolveAPIHost("ca")),
    EU(Messages.selectors_eu(), resolveAPIHost("eu")),
    AU(Messages.selectors_au(), resolveAPIHost("au")),
    AP(Messages.selectors_ap(), resolveAPIHost("ap"));

    private String displayName;
    private String apiHost;

    Region(String displayName, String apiHost) {
        this.displayName = displayName;
        this.apiHost = apiHost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAPIHost() {
        return apiHost;
    }

    private static String resolveAPIHost(String prefix) {
        return String.format("%s.api.insight.rapid7.com", prefix);
    }

    public static Region fromString(String value) {
        return Arrays.stream(Region.values())
                     .filter(e -> e.name().equalsIgnoreCase(value))
                     .findAny()
                     .orElseThrow(() -> new UnrecognizedRegionException(value));
    }
}