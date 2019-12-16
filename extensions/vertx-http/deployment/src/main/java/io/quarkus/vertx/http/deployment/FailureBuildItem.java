package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that is applied to every route
 */
public final class FailureBuildItem extends MultiBuildItem {

    private final Handler<RoutingContext> handler;
    
    /**
     * Creates a new instance of {@link FailureBuildItem}.
     *
     * @param handler the handler, if {@code null} the filter won't be used.
     */
    public FailureBuildItem(Handler<RoutingContext> handler) {
        this.handler = handler;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

}
