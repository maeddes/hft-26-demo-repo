// A minimal REST API using only the Go standard library.
package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strconv"
	"sync"
)

type Item struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
}

// store keeps all items in memory. The mutex guards against
// concurrent requests, since each request runs in its own goroutine.
type store struct {
	mu     sync.Mutex
	items  map[int]Item
	nextID int
}

func main() {
	s := &store{items: map[int]Item{}, nextID: 1}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", health)
	mux.HandleFunc("GET /hello", hello)
	mux.HandleFunc("GET /items", s.list)
	mux.HandleFunc("POST /items", s.create)
	mux.HandleFunc("GET /items/{id}", s.get)
	mux.HandleFunc("DELETE /items/{id}", s.delete)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("listening on :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, mux))
}

func health(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

// hello returns the hostname, which inside a container is the container ID.
func hello(w http.ResponseWriter, r *http.Request) {
	hostname, _ := os.Hostname()
	writeJSON(w, http.StatusOK, map[string]string{"message": "hello from go", "hostname": hostname})
}

func (s *store) list(w http.ResponseWriter, r *http.Request) {
	s.mu.Lock()
	defer s.mu.Unlock()

	items := make([]Item, 0, len(s.items))
	for id := 1; id < s.nextID; id++ {
		if item, ok := s.items[id]; ok {
			items = append(items, item)
		}
	}
	writeJSON(w, http.StatusOK, items)
}

func (s *store) create(w http.ResponseWriter, r *http.Request) {
	var item Item
	if err := json.NewDecoder(r.Body).Decode(&item); err != nil || item.Name == "" {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "expected {\"name\": \"...\"}"})
		return
	}

	s.mu.Lock()
	item.ID = s.nextID
	s.items[item.ID] = item
	s.nextID++
	s.mu.Unlock()

	writeJSON(w, http.StatusCreated, item)
}

func (s *store) get(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "id must be a number"})
		return
	}

	s.mu.Lock()
	item, ok := s.items[id]
	s.mu.Unlock()

	if !ok {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "not found"})
		return
	}
	writeJSON(w, http.StatusOK, item)
}

func (s *store) delete(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "id must be a number"})
		return
	}

	s.mu.Lock()
	_, ok := s.items[id]
	delete(s.items, id)
	s.mu.Unlock()

	if !ok {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "not found"})
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(body)
}
