package io.quarkiverse.qdrant.runtime;

import java.net.URI;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

@ApplicationScoped
public class QdrantClientProducer {

    @Inject
    QdrantConfig config;

    private QdrantRestApi client;

    @Produces
    @ApplicationScoped
    public QdrantRestApi qdrantRestApi() {
        String scheme = config.useTls() ? "https" : "http";
        URI baseUri = URI.create(scheme + "://" + config.host() + ":" + config.port());

        RestClientBuilder builder = RestClientBuilder.newBuilder()
                .baseUri(baseUri);

        config.apiKey().ifPresent(key -> builder.header("api-key", key));

        client = builder.build(QdrantRestApi.class);
        return client;
    }

    @PreDestroy
    public void close() {
        if (client instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
