// A minimal REST API with Javalin.
package demo;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class App {

    record Item(int id, String name) {}

    record NewItem(String name) {}

    // Jetty serves requests on several threads, so both fields are thread-safe types.
    // The skip list keeps items sorted by id.
    static final Map<Integer, Item> items = new ConcurrentSkipListMap<>();
    static final AtomicInteger nextId = new AtomicInteger(1);

    public static void main(String[] args) throws Exception {
        // Inside a container the hostname is the container ID.
        String hostname = InetAddress.getLocalHost().getHostName();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

            config.routes.get("/hello", ctx ->
                    ctx.json(Map.of("message", "hello from javalin", "hostname", hostname)));

            config.routes.get("/items", ctx -> ctx.json(items.values()));

            config.routes.post("/items", ctx -> {
                NewItem body = ctx.bodyValidator(NewItem.class)
                        .check(b -> b.name() != null && !b.name().isEmpty(), "expected {\"name\": \"...\"}")
                        .get();
                Item item = new Item(nextId.getAndIncrement(), body.name());
                items.put(item.id(), item);
                ctx.status(201).json(item);
            });

            config.routes.get("/items/{id}", ctx -> {
                int id = ctx.pathParamAsClass("id", Integer.class).get();
                Item item = items.get(id);
                if (item == null) {
                    throw new NotFoundResponse("not found");
                }
                ctx.json(item);
            });

            config.routes.delete("/items/{id}", ctx -> {
                int id = ctx.pathParamAsClass("id", Integer.class).get();
                if (items.remove(id) == null) {
                    throw new NotFoundResponse("not found");
                }
                ctx.status(204);
            });
        }).start(port);
    }
}
