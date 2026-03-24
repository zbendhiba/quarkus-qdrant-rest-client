package io.quarkiverse.qdrant.deployment.devservices;

import java.util.OptionalInt;

record QdrantDevServiceCfg(
        boolean devServicesEnabled,
        OptionalInt fixedPort,
        String imageName,
        String serviceName,
        boolean shared) {
}
