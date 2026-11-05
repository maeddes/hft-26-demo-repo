// A minimal REST API with Quarkus, using the Jakarta REST annotations.
package demo;

import jakarta.inject.Singleton;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

// One instance serves all requests, so the store lives in its fields.
@Singleton
@Path("/")
public class ItemResource {

    record Item(int id, String name) {}

    record NewItem(String name) {}

    // Requests run on several threads, so both fields are thread-safe types.
    // The skip list keeps items sorted by id.
    private final Map<Integer, Item> items = new ConcurrentSkipListMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @GET
    @Path("health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // Returns the hostname, which inside a container is the container ID.
    @GET
    @Path("hello")
    public Map<String, String> hello() throws UnknownHostException {
        return Map.of("message", "hello from quarkus", "hostname", InetAddress.getLocalHost().getHostName());
    }

    @GET
    @Path("items")
    public Collection<Item> list() {
        return items.values();
    }

    @POST
    @Path("items")
    public Response create(NewItem body) {
        if (body == null || body.name() == null || body.name().isEmpty()) {
            return Response.status(400).entity(Map.of("error", "expected {\"name\": \"...\"}")).build();
        }
        Item item = new Item(nextId.getAndIncrement(), body.name());
        items.put(item.id(), item);
        return Response.status(201).entity(item).build();
    }

    @GET
    @Path("items/{id}")
    public Item get(@PathParam("id") int id) {
        Item item = items.get(id);
        if (item == null) {
            throw new NotFoundException();
        }
        return item;
    }

    // A void method answers with 204 No Content.
    @DELETE
    @Path("items/{id}")
    public void delete(@PathParam("id") int id) {
        if (items.remove(id) == null) {
            throw new NotFoundException();
        }
    }
}
