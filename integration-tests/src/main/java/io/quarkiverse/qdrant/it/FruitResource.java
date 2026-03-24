package io.quarkiverse.qdrant.it;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/fruits")
public class FruitResource {

    @Inject
    FruitService service;

    @POST
    public Response index(Fruit fruit) {
        String id = service.index(fruit);
        return Response.status(201).entity(id).build();
    }

    @GET
    @Path("/search")
    public List<Fruit> search(@QueryParam("text") String text) {
        return service.search(text);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
