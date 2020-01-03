package io.quarkus.vertx.http.runtime.error;

import java.util.List;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ErrorRecorder {

    public Handler<RoutingContext> errorHandler(HttpConfiguration configuration, LaunchMode launchMode, List<String> servletMappings, List<String> staticResources, List<String> additionalEndpoints, List<ResourceDescription> descriptions) {
    	if (configuration.failure.handler.isPresent()) {
            //todo manage handler from config
    		return null;
        } else {
        	return new QuarkusErrorHandler(launchMode.isDevOrTest(), servletMappings, staticResources, additionalEndpoints, descriptions);
        }
    }
}
