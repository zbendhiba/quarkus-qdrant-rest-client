package io.quarkiverse.qdrant.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class FruitResourceTest {

    @Test
    void testIndexAndSearch() {
        // Index an apple
        String id = given()
                .contentType("application/json")
                .body("{\"name\": \"Apple\", \"color\": \"Green\"}")
                .when().post("/fruits")
                .then()
                .statusCode(201)
                .extract().asString();

        assertThat(id).isNotEmpty();

        // Search for it
        List<Fruit> results = List.of(given()
                .queryParam("text", "Apple Green")
                .when().get("/fruits/search")
                .then()
                .statusCode(200)
                .extract().as(Fruit[].class));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).name).isEqualTo("Apple");

        // Delete it
        given()
                .when().delete("/fruits/" + id)
                .then()
                .statusCode(204);
    }
}
