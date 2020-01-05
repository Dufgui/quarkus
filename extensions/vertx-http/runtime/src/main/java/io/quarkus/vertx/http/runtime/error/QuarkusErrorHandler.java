package io.quarkus.vertx.http.runtime.error;

import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.TemplateHtmlBuilder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class QuarkusErrorHandler implements Handler<RoutingContext> {

    private static final Logger log = getLogger(QuarkusErrorHandler.class);

    /**
     * we don't want to generate a new UUID each time as it is slowish. Instead we just generate one based one
     * and then use a counter.
     */
    private static final String BASE_ID = UUID.randomUUID().toString() + "-";

    private static final AtomicLong ERROR_COUNT = new AtomicLong();

    private final boolean showStack;
    
    private final String httpRoot;
    private final List<String> servletMappings;
    private final List<String> staticResources;
    private final List<String> additionalEndpoints;
    private final List<ResourceDescription> descriptions;
    
    public QuarkusErrorHandler(boolean showStack, String httpRoot, List<String> servletMappings, List<String> staticResources, List<String> additionalEndpoints, List<ResourceDescription> descriptions) {
        this.showStack = showStack;
        this.httpRoot = httpRoot;
        this.servletMappings = servletMappings;
        this.staticResources = staticResources;
        this.additionalEndpoints = additionalEndpoints;
        this.descriptions = descriptions;
    }

    @Override
    public void handle(RoutingContext event) {
        if (event.failure() == null) {
        	if(HttpResponseStatus.NOT_FOUND.code() == event.statusCode()) {
        		handleNotFound(event);
        	} else {
        		event.response().setStatusCode(event.statusCode());
        		event.response().end();
        	}
            return;
        }
        event.response().setStatusCode(500);
        String uuid = BASE_ID + ERROR_COUNT.incrementAndGet();
        String details = "";
        String stack = "";
        Throwable exception = event.failure();
        if (showStack && exception != null) {
            details = generateHeaderMessage(exception, uuid);
            stack = generateStackTrace(exception);

        } else {
            details += "Error id " + uuid;
        }
        if (event.failure() instanceof IOException) {
            log.debugf(exception,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection. Error id: %s",
                    event.request().uri(), uuid);
        } else {
            log.errorf(exception, "HTTP Request to %s failed, error id: %s", event.request().uri(), uuid);
        }
        String accept = event.request().getHeader(HttpHeaderNames.ACCEPT);
        if (accept != null && accept.contains(HttpHeaderValues.APPLICATION_JSON)) {
            event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
            String escapedStack = stack.replace(System.lineSeparator(), "\\n").replace("\"", "\\\"");
            StringBuilder jsonPayload = new StringBuilder("{\"details\":\"").append(details).append("\",\"stack\":\"")
                    .append(escapedStack).append("\"}");
            event.response().end(jsonPayload.toString());
        } else {
            //We default to HTML representation
            event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
            final TemplateHtmlBuilder htmlBuilder = new TemplateHtmlBuilder("Internal Server Error", details, details);
            if (showStack && exception != null) {
                htmlBuilder.stack(exception);
            }
            event.response().end(htmlBuilder.toString());
        }
    }

    private void handleNotFound(RoutingContext event) {
    	String accept = event.request().getHeader(HttpHeaderNames.ACCEPT);
        if (accept != null && accept.contains(HttpHeaderValues.APPLICATION_JSON)) {
            event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
            
        } else {
            //We default to HTML representation
            event.response().headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
            TemplateHtmlBuilder sb = new TemplateHtmlBuilder("404 - Resource Not Found", "", "Resources overview");
            sb.resourcesStart("REST resources");
            for (ResourceDescription resource : descriptions) {
                sb.resourcePath(adjustRoot(resource.basePath));
                for (MethodDescription method : resource.calls) {
                    sb.method(method.verb, method.path);
                    if (method.consumes != null) {
                        sb.consumes(method.consumes);
                    }
                    if (method.produces != null) {
                        sb.produces(method.produces);
                    }
                    sb.methodEnd();
                }
                sb.resourceEnd();
            }
            if (descriptions.isEmpty()) {
                sb.noResourcesFound();
            }
            sb.resourcesEnd();

            if (!servletMappings.isEmpty()) {
                sb.resourcesStart("Servlet mappings");
                for (String servletMapping : servletMappings) {
                    sb.servletMapping(adjustRoot(servletMapping));
                }
                sb.resourcesEnd();
            }

            if (!staticResources.isEmpty()) {
                sb.resourcesStart("Static resources");
                for (String staticResource : staticResources) {
                    sb.staticResourcePath(adjustRoot(staticResource));
                }
                sb.resourcesEnd();
            }

            if (!additionalEndpoints.isEmpty()) {
                sb.resourcesStart("Additional endpoints");
                for (String additionalEndpoint : additionalEndpoints) {
                    sb.staticResourcePath(adjustRoot(additionalEndpoint));
                }
                sb.resourcesEnd();
            }
            event.response().end(sb.toString());
        }
	}

	private static String generateStackTrace(final Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        return escapeHtml(stringWriter.toString().trim());
    }

    private static String generateHeaderMessage(final Throwable exception, String uuid) {
        return escapeHtml(String.format("Error handling %s, %s: %s", uuid, exception.getClass().getName(),
                extractFirstLine(exception.getMessage())));
    }

    private static String extractFirstLine(final String message) {
        if (null == message) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines[0].trim();
    }

    private static String escapeHtml(final String bodyText) {
        if (bodyText == null) {
            return "null";
        }

        return bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
    
    private String adjustRoot(String basePath) {
        //httpRoot can optionally end with a slash
        //also some templates want the returned path to start with a / and some don't
        //to make this work we check if the basePath starts with a / or not, and make sure we
        //the return value follows the same pattern

        if (httpRoot.equals("/")) {
            //leave it alone
            return basePath;
        }
        if (basePath.startsWith("/")) {
            if (!httpRoot.endsWith("/")) {
                return httpRoot + basePath;
            }
            return httpRoot.substring(0, httpRoot.length() - 1) + basePath;
        }
        if (httpRoot.endsWith("/")) {
            return httpRoot.substring(1) + basePath;
        }
        return httpRoot.substring(1) + "/" + basePath;
    }
}
