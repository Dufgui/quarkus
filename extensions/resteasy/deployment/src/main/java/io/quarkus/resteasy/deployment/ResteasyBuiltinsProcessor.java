package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.resteasy.deployment.SecurityTransformerUtils.hasSecurityAnnotation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.DisplayableEndpointBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.security.spi.AdditionalSecuredClassesBuildIem;
import io.quarkus.undertow.deployment.StaticResourceFilesBuildItem;

public class ResteasyBuiltinsProcessor {

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.RESTEASY);
    }

    @BuildStep
    void setUpDenyAllJaxRs(CombinedIndexBuildItem index,
            JaxRsSecurityConfig config,
            ResteasyDeploymentBuildItem resteasyDeployment,
            BuildProducer<AdditionalSecuredClassesBuildIem> additionalSecuredClasses) {
        if (config.denyJaxRs) {
            Set<ClassInfo> classes = new HashSet<>();

            List<String> resourceClasses = resteasyDeployment.getDeployment().getScannedResourceClasses();
            for (String className : resourceClasses) {
                ClassInfo classInfo = index.getIndex().getClassByName(DotName.createSimple(className));
                if (!hasSecurityAnnotation(classInfo)) {
                    classes.add(classInfo);
                }
            }

            additionalSecuredClasses.produce(new AdditionalSecuredClassesBuildIem(classes));
        }
    }

    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setUpSecurityExceptionMappers(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(UnauthorizedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(ForbiddenExceptionMapper.class.getName()));
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addStaticResourcesExceptionMapper(StaticResourceFilesBuildItem paths, BuildProducer<DisplayableEndpointBuildItem> endPointProducer) {
        //limit to 1000 to not have to many files to display
        Set<String> staticResources = paths.files.stream().filter(this::isHtmlFileName).limit(1000).collect(Collectors.toSet());
        if (staticResources.isEmpty()) {
            staticResources = paths.files.stream().limit(1000).collect(Collectors.toSet());
        }
        for (String staticResource : staticResources) {
        	endPointProducer.produce(new DisplayableEndpointBuildItem(staticResource, DisplayableEndpointBuildItem.STATIC_RESOURCE));
		}
    }

    private boolean isHtmlFileName(String fileName) {
        return fileName.endsWith(".html") || fileName.endsWith(".htm");
    }

}
