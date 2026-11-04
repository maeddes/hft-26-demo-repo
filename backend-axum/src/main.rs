// A minimal REST API with Axum.
use std::collections::BTreeMap;
use std::sync::{Arc, Mutex};

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};

#[derive(Clone, Serialize)]
struct Item {
    id: u32,
    name: String,
}

#[derive(Deserialize)]
struct NewItem {
    name: String,
}

// Tokio serves requests on several threads, so the store sits behind a mutex.
// The BTreeMap keeps items sorted by id.
struct Store {
    items: BTreeMap<u32, Item>,
    next_id: u32,
}

type AppState = Arc<Mutex<Store>>;

#[tokio::main]
async fn main() {
    let state: AppState = Arc::new(Mutex::new(Store { items: BTreeMap::new(), next_id: 1 }));

    let app = Router::new()
        .route("/health", get(health))
        .route("/hello", get(hello))
        .route("/items", get(list_items).post(create_item))
        .route("/items/{id}", get(get_item).delete(delete_item))
        .with_state(state);

    let port = std::env::var("PORT").unwrap_or_else(|_| "8080".to_string());
    let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{port}")).await.unwrap();
    println!("listening on :{port}");
    axum::serve(listener, app).await.unwrap();
}

async fn health() -> Json<Value> {
    Json(json!({ "status": "ok" }))
}

// Returns the hostname, which inside a container is the container ID.
async fn hello() -> Json<Value> {
    let hostname = hostname::get().unwrap_or_default().to_string_lossy().into_owned();
    Json(json!({ "message": "hello from axum", "hostname": hostname }))
}

async fn list_items(State(state): State<AppState>) -> Json<Vec<Item>> {
    let store = state.lock().unwrap();
    Json(store.items.values().cloned().collect())
}

async fn create_item(
    State(state): State<AppState>,
    Json(new): Json<NewItem>,
) -> Result<(StatusCode, Json<Item>), StatusCode> {
    if new.name.is_empty() {
        return Err(StatusCode::BAD_REQUEST);
    }
    let mut store = state.lock().unwrap();
    let item = Item { id: store.next_id, name: new.name };
    store.items.insert(item.id, item.clone());
    store.next_id += 1;
    Ok((StatusCode::CREATED, Json(item)))
}

async fn get_item(State(state): State<AppState>, Path(id): Path<u32>) -> Result<Json<Item>, StatusCode> {
    let store = state.lock().unwrap();
    match store.items.get(&id) {
        Some(item) => Ok(Json(item.clone())),
        None => Err(StatusCode::NOT_FOUND),
    }
}

async fn delete_item(State(state): State<AppState>, Path(id): Path<u32>) -> StatusCode {
    let mut store = state.lock().unwrap();
    match store.items.remove(&id) {
        Some(_) => StatusCode::NO_CONTENT,
        None => StatusCode::NOT_FOUND,
    }
}
