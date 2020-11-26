# kert
Kert is a concise HTTP & GRPC library for Kotlin. It's not an Android library, it's a JVM library for backend development.

Compare to the official [gRPC-Java](https://github.com/grpc/grpc-java), Kert provides the benefits like:
* No need for 2 separate libraries / ports to serve HTTP and GRPC requests.
* Simply to use HTTP health check in Kubernetes.
* Coroutine / Flow based interface more intuitive for async processing and context propagation.
* Simple filter & interceptor interface for async handling.

Server Example:
```kotlin
val server = server(8080) {
  // http service
  http {
    // http filter
    filter { request, next ->
      println("Serving request ${request.path}")
      next(req)
    }
    // http request handler
    get("/ping") {
      response("pong")
    }
  }
  // grpc service
  grpc {
    // grpc interceptor
    interceptor { request, next ->
      // intercept the request
      if (req.metadata["authentication"] == null) throw IllegalArgumentException("Authentication header is missing")

      // intercept each message in the streaming request
      val filteredReq = req.copy(messages = req.messages.map { msg -> ... })
      next(filteredReq)
    }
    // register service implementation
    service(EchoServiceImpl())
  }
}
server.start()
```

Client Example:
```kotlin
// http request
val client = client(URL("http://localhost:8080")) {}
client.get("ping")

// grpc request
val stub = EchoGrpcKt.stub(URL("http://localhost:8080"))
stub.unary(EchoReq.newBuilder().setId(1).setValue("hello").build())
```
