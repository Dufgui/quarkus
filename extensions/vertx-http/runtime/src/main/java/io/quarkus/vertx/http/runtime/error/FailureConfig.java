package io.quarkus.vertx.http.runtime.error;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class FailureConfig {

    /**
     * Failure handler
     */
    @ConfigItem
    public Optional<String> handler;

    @Override
    public String toString() {
        return "FailureConfig{" +
                "handler=" + handler +
                '}';
    }
}
