"""A minimal REST API with FastAPI."""

import socket

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


class NewItem(BaseModel):
    name: str = Field(min_length=1)


class Item(BaseModel):
    id: int
    name: str


app = FastAPI()

# All handlers are async and run on one event loop, so no lock is needed.
items: dict[int, Item] = {}
next_id = 1


@app.get("/health")
async def health():
    return {"status": "ok"}


# Returns the hostname, which inside a container is the container ID.
@app.get("/hello")
async def hello():
    return {"message": "hello from fastapi", "hostname": socket.gethostname()}


@app.get("/items")
async def list_items() -> list[Item]:
    return list(items.values())


@app.post("/items", status_code=201)
async def create_item(new: NewItem) -> Item:
    global next_id
    item = Item(id=next_id, name=new.name)
    items[item.id] = item
    next_id += 1
    return item


@app.get("/items/{item_id}")
async def get_item(item_id: int) -> Item:
    if item_id not in items:
        raise HTTPException(status_code=404, detail="not found")
    return items[item_id]


@app.delete("/items/{item_id}", status_code=204)
async def delete_item(item_id: int) -> None:
    if items.pop(item_id, None) is None:
        raise HTTPException(status_code=404, detail="not found")
