package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ws.leap.kert.http.httpClient
import ws.leap.kert.http.httpServer
import ws.leap.kert.http.response
import ws.leap.kert.test.EchoGrpcKt
import ws.leap.kert.test.EchoReq

class Example {
  fun server() = runBlocking {
    val server = httpServer(8080) {
      // server side filter
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
          override suspend fun <REQ, RESP> invoke(
            method: MethodDescriptor<REQ, RESP>,
            req: GrpcRequest<REQ>,
            next: GrpcHandler<REQ, RESP>
          ): GrpcResponse<RESP> {
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
  }

  fun client() = runBlocking {
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
  }
}
