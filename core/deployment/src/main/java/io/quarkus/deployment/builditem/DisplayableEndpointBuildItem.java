package io.quarkus.deployment.builditem;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * End points to display in case of 404 not found 
 *
 */
public final class DisplayableEndpointBuildItem extends MultiBuildItem {

	public static final short REST_HTTP_ROOT = 0;
	public static final short STATIC_RESOURCE = 1;
	public static final short SERVLET_MAPPING = 2;
	public static final short ADDITIONNAL = 3;
	public static final short REST_RESOURCE = 4;
	public static final short NON_JAXRS_RESOURCE = 5;
	
	private final String endpoint;
	private final short type;
	private List<MethodDescription> methods;
	private NonJaxRsClassMappings nonJaxRsClassMappings;
	
	public DisplayableEndpointBuildItem(String endpoint, short type) {
		this.endpoint = endpoint;
		this.type = type;
	}
	
	public DisplayableEndpointBuildItem(String endpoint, List<MethodDescription> methods) {
		this(endpoint, REST_RESOURCE);
		this.methods = methods;
	}
	
	/**
     * Uses to register the paths of classes that are not annotated with JAX-RS annotations (like Spring Controllers for
     * example)
     *
     * @param nonJaxRsClassNameToMethodPaths A map that contains the class name as a key and a map that
     *        contains the method name to path as a value
     */
	public DisplayableEndpointBuildItem(String className, NonJaxRsClassMappings nonJaxRsClassMappings) {
		this(className, NON_JAXRS_RESOURCE);
		this.nonJaxRsClassMappings = nonJaxRsClassMappings;
	}

	public boolean isRestHttpRoot() {
		return type == REST_HTTP_ROOT;
	}
	
	public boolean isStaticResource() {
		return type == STATIC_RESOURCE;
	}

	public boolean isServletMappings() {
		return type == SERVLET_MAPPING;
	}

	public boolean isAdditionalEndpoint() {
		return type == ADDITIONNAL;
	}

	public boolean isRestResource() {
		return type == REST_RESOURCE;
	}

	public String getEndpoint() {
        return endpoint;
    }
	
	public List<MethodDescription> getMethodes() {
		return methods;
	}
	
	public NonJaxRsClassMappings getNonJaxRsClassMappings() {
		return nonJaxRsClassMappings;
	}
}
