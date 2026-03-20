package io.quarkiverse.qdrant.deployment;

import io.quarkiverse.qdrant.runtime.QdrantClientProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class QdrantProcessor {

    private static final String FEATURE = "qdrant";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerProducer() {
        return AdditionalBeanBuildItem.unremovableOf(QdrantClientProducer.class);
    }
}
