# Stage 1: compile a release binary
FROM rust:1.99-slim-bookworm AS build
WORKDIR /src
COPY Cargo.toml Cargo.lock ./
COPY src ./src
RUN cargo build --release --locked

# Stage 2: ship only the binary plus the C runtime it links against
FROM gcr.io/distroless/cc-debian12:nonroot
COPY --from=build /src/target/release/backend-axum /api
EXPOSE 8080
ENTRYPOINT ["/api"]
