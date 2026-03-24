package io.quarkiverse.qdrant.deployment;

import org.jboss.jandex.DotName;

import io.quarkiverse.qdrant.runtime.QdrantClientConfig;
import io.quarkiverse.qdrant.runtime.QdrantClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class QdrantProcessor {

    private static final String FEATURE = "qdrant";
    private static final DotName QDRANT_CLIENT_CONFIG = DotName.createSimple(QdrantClientConfig.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem build() {
        return AdditionalBeanBuildItem.unremovableOf(QdrantClientProducer.class);
    }

    @BuildStep
    void qdrantClientConfigSupport(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        // add the @QdrantClientConfig class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(QdrantClientConfig.class).build());

        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(QDRANT_CLIENT_CONFIG, DotNames.APPLICATION_SCOPED, false));
    }

}
