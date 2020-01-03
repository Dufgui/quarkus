package io.quarkus.vertx.http.runtime.error;

import java.util.ArrayList;
import java.util.List;


public final class ResourceDescription {
    public final String basePath;
    public final List<MethodDescription> calls;

    public ResourceDescription(String basePath) {
        this.basePath = basePath;
        this.calls = new ArrayList<>();
    }
    
    public void addMethod(String verb, String path, String produces, String consumes) {
    	calls.add(new MethodDescription(verb, path, produces, consumes));
    }
}