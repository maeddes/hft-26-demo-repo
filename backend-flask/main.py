"""A minimal REST API with Flask."""

import socket
from threading import Lock

from flask import Flask, request

app = Flask(__name__)

# Flask serves requests on several threads, so the lock guards the store.
lock = Lock()
items: dict[int, dict] = {}
next_id = 1


@app.get("/health")
def health():
    return {"status": "ok"}


# Returns the hostname, which inside a container is the container ID.
@app.get("/hello")
def hello():
    return {"message": "hello from flask", "hostname": socket.gethostname()}


@app.get("/items")
def list_items():
    with lock:
        return list(items.values())


@app.post("/items")
def create_item():
    global next_id
    body = request.get_json(silent=True) or {}
    name = body.get("name")
    if not isinstance(name, str) or not name:
        return {"error": 'expected {"name": "..."}'}, 400

    with lock:
        item = {"id": next_id, "name": name}
        items[next_id] = item
        next_id += 1
    return item, 201


@app.get("/items/<int:item_id>")
def get_item(item_id):
    with lock:
        item = items.get(item_id)
    if item is None:
        return {"error": "not found"}, 404
    return item


@app.delete("/items/<int:item_id>")
def delete_item(item_id):
    with lock:
        item = items.pop(item_id, None)
    if item is None:
        return {"error": "not found"}, 404
    return "", 204
