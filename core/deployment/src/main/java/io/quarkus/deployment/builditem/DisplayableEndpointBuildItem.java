package io.quarkus.deployment.builditem;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * End points to display in case of 404 not found 
 *
 */
public final class DisplayableEndpointBuildItem extends MultiBuildItem {

	public DisplayableEndpointBuildItem(String staticResource) {
		// TODO Auto-generated constructor stub
	}

	public boolean isStaticResource() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isServletMappings() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isAdditionalEndpoint() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isRestResource() {
		// TODO Auto-generated method stub
		return false;
	}

	public List<MethodDescription> getMethodes() {
		// TODO Auto-generated method stub
		return null;
	}
}
