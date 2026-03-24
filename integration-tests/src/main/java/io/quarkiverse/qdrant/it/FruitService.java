package io.quarkiverse.qdrant.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.qdrant.runtime.QdrantRestApi;
import io.quarkiverse.qdrant.runtime.model.DeleteRequest;
import io.quarkiverse.qdrant.runtime.model.PointStruct;
import io.quarkiverse.qdrant.runtime.model.ScoredPoint;
import io.quarkiverse.qdrant.runtime.model.SearchRequest;
import io.quarkiverse.qdrant.runtime.model.SearchResponse;
import io.quarkiverse.qdrant.runtime.model.UpsertRequest;

@ApplicationScoped
public class FruitService {

    private static final String COLLECTION = "fruits";

    @Inject
    QdrantRestApi qdrant;

    public String index(Fruit fruit) {
        if (fruit.id == null) {
            fruit.id = UUID.randomUUID().toString();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fruit.name);
        payload.put("color", fruit.color);

        // Simple deterministic vector based on name and color hash
        List<Float> vector = toVector(fruit);

        PointStruct point = new PointStruct(fruit.id, vector, payload);
        qdrant.upsert(COLLECTION, new UpsertRequest(List.of(point)));

        return fruit.id;
    }

    public List<Fruit> search(String text) {
        // Create a search vector from the text
        List<Float> vector = toVector(text);

        SearchRequest request = new SearchRequest();
        request.setVector(toFloatArray(vector));
        request.setLimit(10);
        request.setWithPayload(true);
        request.setWithVector(false);

        SearchResponse response = qdrant.search(COLLECTION, request);

        List<Fruit> fruits = new ArrayList<>();
        if (response.getResult() != null) {
            for (ScoredPoint point : response.getResult()) {
                Fruit fruit = new Fruit();
                fruit.id = point.getId();
                if (point.getPayload() != null) {
                    fruit.name = (String) point.getPayload().get("name");
                    fruit.color = (String) point.getPayload().get("color");
                }
                fruits.add(fruit);
            }
        }
        return fruits;
    }

    public void delete(String id) {
        qdrant.delete(COLLECTION, DeleteRequest.byIds(List.of(id)));
    }

    /**
     * Simple deterministic vector from a Fruit (4 dimensions).
     */
    private List<Float> toVector(Fruit fruit) {
        return toVector(fruit.name + " " + fruit.color);
    }

    /**
     * Simple deterministic vector from text (4 dimensions).
     */
    private List<Float> toVector(String text) {
        int hash = text.hashCode();
        return List.of(
                ((hash & 0xFF) / 255.0f),
                (((hash >> 8) & 0xFF) / 255.0f),
                (((hash >> 16) & 0xFF) / 255.0f),
                (((hash >> 24) & 0xFF) / 255.0f));
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
