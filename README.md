# kert
Kert is a concise HTTP & GRPC library for Kotlin. It's not an Android library, it's a JVM library for backend development.

Compare to the official [gRPC-Java](https://github.com/grpc/grpc-java), Kert provides the benefits like:
* No need for 2 separate libraries / ports to serve HTTP and GRPC requests.
* Simply to use HTTP health check in Kubernetes.
* Coroutine / Flow based interface more intuitive for async processing and context propagation.
* Simple filter & interceptor interface for async handling.

Server Example:
```kotlin
val server = httpServer(8080) {
  // http filter
  filter { req, next ->
    println("Serving request ${req.path}")
    next(req)
  }

  // http service
  router {
    // http request handler
    get("/ping") {
      response(body = "pong")
    }
  }

  // grpc service
  grpc {
    // grpc interceptor
    interceptor( object : GrpcInterceptor {
      override suspend fun <REQ, RESP> invoke(method: MethodDescriptor<REQ, RESP>, req: GrpcRequest<REQ>, next: GrpcHandler<REQ, RESP>): GrpcResponse<RESP> {
        // intercept the request
        if (req.metadata["authentication"] == null) throw IllegalArgumentException("Authentication header is missing")

        // intercept each message in the streaming request
        val filteredReq = req.copy(messages = req.messages.map {
          println(it)
          it
        })
        return next(method, filteredReq)
      }
    })

    // register service implementation
    service(EchoServiceImpl())
  }
}

server.start()
```

Client Example:
```kotlin
// http request
val client = httpClient {
  options {
    defaultHost = "localhost"
    defaultPort = 8551
    protocolVersion = HttpVersion.HTTP_2
  }

  // a client side filter to set authorization header in request
  filter { req, next ->
    req.headers["authorization"] = "my-authorization-header"
    next(req)
  }
}
client.get("ping")

// grpc request
val stub = EchoGrpcKt.stub(client)
stub.unary(EchoReq.newBuilder().setId(1).setValue("hello").build())
```
