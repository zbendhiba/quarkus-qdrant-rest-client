package io.quarkiverse.qdrant.deployment.devservices;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = DevServicesConfig.Enabled.class)
public class QdrantDevServicesProcessor {
    private static final Logger LOG = Logger.getLogger(QdrantDevServicesProcessor.class);
    private static final String FEATURE = "qdrant-rest-client";

    static volatile DevServicesResultBuildItem.RunningDevService devService;
    static volatile QdrantDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public List<DevServicesResultBuildItem> startQdrantDevServices(
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchModeBuildItem launchMode,
            QdrantBuildConfig qdrantBuildConfig,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        List<DevServicesResultBuildItem> result = new ArrayList<>();
        QdrantDevServiceCfg configuration = getConfiguration(qdrantBuildConfig);

        if (devService != null) {
            boolean shouldShutdown = !configuration.equals(cfg);
            if (!shouldShutdown) {
                result.add(devService.toBuildItem());
                return result;
            }

            shutdownContainers();

            cfg = null;
        }

        if (!qdrantBuildConfig.devservices().enabled()) {
            LOG.debug("Not starting Dev Services for Qdrant, as it has been disabled in the config.");
            return Collections.emptyList();
        }
        // if connection to Qdrant was explicitly specified, don't start Dev Services
        if (ConfigUtils.isPropertyPresent("quarkus.qdrant.host")) {
            return Collections.emptyList();
        }
        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Qdrant Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {

            DevServicesResultBuildItem.RunningDevService newDevService = startQdrantContainer(
                    dockerStatusBuildItem,
                    configuration,
                    launchMode,
                    devServicesConfig.timeout(),
                    !devServicesSharedNetworkBuildItem.isEmpty());

            if (newDevService != null) {
                devService = newDevService;
                if (devService.isOwner()) {
                    LOG.infof("Dev Services instance of Qdrant started (config: %s)", newDevService.getConfig());
                }
            }
            if (devService == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (devService == null) {
            return Collections.emptyList();
        }

        // Configure the watch dog
        if (first) {
            first = false;
            Runnable closeTask = () -> {
                shutdownContainers();
                first = true;
                cfg = null;
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
        }
        cfg = configuration;
        result.add(devService.toBuildItem());
        return result;
    }

    private void shutdownContainers() {
        if (devService != null) {
            try {
                devService.close();
            } catch (Throwable e) {
                LOG.error("Failed to stop the Qdrant server", e);
            } finally {
                devService = null;
            }
        }
    }

    private DevServicesResultBuildItem.RunningDevService startQdrantContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            QdrantDevServiceCfg config,
            LaunchModeBuildItem launchMode,
            Optional<Duration> timeout,
            boolean useSharedNetwork) {

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            LOG.warn("Docker isn't working, please configure the Qdrant server location.");
            return null;
        }

        QdrantContainer container = new QdrantContainer(
                config.imageName(),
                config.fixedPort(),
                launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT ? config.serviceName() : null,
                useSharedNetwork);

        final Supplier<DevServicesResultBuildItem.RunningDevService> defaultQdrantSupplier = () -> {
            timeout.ifPresent(container::withStartupTimeout);
            container.start();

            return getRunningQdrantDevService(
                    config.serviceName(),
                    container.getContainerId(),
                    container::close,
                    container.getHost(),
                    container.getPort());
        };

        return QdrantDevServices.LOCATOR
                .locateContainer(
                        config.serviceName(),
                        config.shared(),
                        launchMode.getLaunchMode())
                .map(containerAddress -> getRunningQdrantDevService(
                        config.serviceName(),
                        containerAddress.getId(),
                        null,
                        containerAddress.getHost(),
                        containerAddress.getPort()))
                .orElseGet(defaultQdrantSupplier);
    }

    private DevServicesResultBuildItem.RunningDevService getRunningQdrantDevService(
            String serviceName,
            String containerId,
            Closeable closeable,
            String host,
            int port) {

        Map<String, String> configMap = Map.of(
                "quarkus.qdrant.host", host,
                "quarkus.qdrant.port", String.valueOf(port));

        return new DevServicesResultBuildItem.RunningDevService(
                FEATURE,
                containerId,
                closeable,
                configMap);
    }

    private QdrantDevServiceCfg getConfiguration(QdrantBuildConfig cfg) {
        return new QdrantDevServiceCfg(
                cfg.devservices().enabled(),
                cfg.devservices().port(),
                cfg.devservices().qdrantImageName(),
                cfg.devservices().serviceName(),
                cfg.devservices().shared());
    }
}
