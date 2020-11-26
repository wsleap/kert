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
  http {
    filter { request, next ->
      println("Serving request ${request.path}")
      next(req)
    }
    get("/ping") {
      response("pong")
    }
  }
  grpc {
    interceptor { requests, next ->
      next(requests)
    }
    addService(EchoServiceImpl())
  }
}
server.start()
```

Client Example:
```kotlin

```
