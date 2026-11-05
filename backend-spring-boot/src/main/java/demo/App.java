// A minimal REST API with Spring Boot.
package demo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class App {

    record Item(int id, String name) {}

    record NewItem(String name) {}

    // Tomcat serves requests on several threads, so both fields are thread-safe types.
    // The skip list keeps items sorted by id.
    private final Map<Integer, Item> items = new ConcurrentSkipListMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // Returns the hostname, which inside a container is the container ID.
    @GetMapping("/hello")
    Map<String, String> hello() throws UnknownHostException {
        return Map.of("message", "hello from spring boot", "hostname", InetAddress.getLocalHost().getHostName());
    }

    @GetMapping("/items")
    Collection<Item> list() {
        return items.values();
    }

    @PostMapping("/items")
    ResponseEntity<?> create(@RequestBody NewItem body) {
        if (body.name() == null || body.name().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "expected {\"name\": \"...\"}"));
        }
        Item item = new Item(nextId.getAndIncrement(), body.name());
        items.put(item.id(), item);
        return ResponseEntity.status(201).body(item);
    }

    @GetMapping("/items/{id}")
    ResponseEntity<Item> get(@PathVariable int id) {
        Item item = items.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/items/{id}")
    ResponseEntity<Void> delete(@PathVariable int id) {
        if (items.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
