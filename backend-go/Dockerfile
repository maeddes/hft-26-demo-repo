# Stage 1: compile a static binary
FROM golang:1.26 AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /api .

# Stage 2: ship only the binary, no shell, no package manager
FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /api /api
EXPOSE 8080
ENTRYPOINT ["/api"]
