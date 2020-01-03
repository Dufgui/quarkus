package io.quarkus.deployment.builditem;

public final class MethodDescription {
    public String verb;
    public String path;
    public String produces;
    public String consumes;

    public MethodDescription(String verb, String path, String produces, String consumes) {
        super();
        this.verb = verb;
        this.path = path;
        this.produces = produces;
        this.consumes = consumes;
    }
}