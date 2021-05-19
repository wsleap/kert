package ws.leap.kert.grpc

import ws.leap.kert.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kert.http.httpServer

object EchoTest {
  val streamSize = 500
  val message = "hello".repeat(1024)
}

private val logger = KotlinLogging.logger {}

class EchoServiceImpl : EchoGrpcKt.EchoImplBase() {
  override suspend fun unary(req: EchoReq): EchoResp {
    return echoResp {
      id = req.id
      value = req.value
    }
  }

  override suspend fun serverStreaming(req: EchoCountReq): Flow<EchoResp> {
    return flow {
      for(i in 0 until req.count) {
        val msg = echoResp {
          id = i
          value = EchoTest.message
        }
        emit(msg)
        logger.trace { "Server sent id=${msg.id}" }
        delay(1)
      }
    }
  }

  override suspend fun clientStreaming(req: Flow<EchoReq>): EchoCountResp {
    var count = 0
    req.collect { msg ->
      logger.trace { "Server received id=${msg.id}" }
      count++
    }

    return echoCountResp { this.count = count }
  }

  override suspend fun bidiStreaming(req: Flow<EchoReq>): Flow<EchoResp> {
    return req.map { msg ->
      logger.trace { "Server received id=${msg.id}" }
      delay(1)
      val respMsg = echoResp {
        id = msg.id
        value = msg.value
      }
      logger.trace { "Server sent id=${respMsg.id}" }
      respMsg
    }
  }
}

/*
ghz --insecure -c 100 -z 30s --connections 100 \
  --proto kert-grpc/src/test/proto/echo.proto \
  --call ws.leap.kert.test.Echo.unary \
  -d '{"id":1, "value":"hello"}' \
  0.0.0.0:8551

With vertx-lang-kotlin-coroutines stream
Summary:
  Count:	3248366
  Total:	30.00 s
  Slowest:	53.73 ms
  Fastest:	0.07 ms
  Average:	0.83 ms
  Requests/sec:	108270.75

Response time histogram:
  0.074 [1]	|
  5.440 [992545]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  10.805 [6404]	|
  16.170 [754]	|
  21.536 [158]	|
  26.901 [18]	|
  32.266 [15]	|
  37.632 [1]	|
  42.997 [99]	|
  48.363 [3]	|
  53.728 [2]	|

Latency distribution:
  10 % in 0.31 ms
  25 % in 0.46 ms
  50 % in 0.66 ms
  75 % in 0.91 ms
  90 % in 1.31 ms
  95 % in 1.88 ms
  99 % in 4.78 ms

Status code distribution:
  [Canceled]      2 responses
  [OK]            3248321 responses
  [Unavailable]   43 responses

Error distribution:
  [43]   rpc error: code = Unavailable desc = transport is closing
  [2]    rpc error: code = Canceled desc = grpc: the client connection is closing



With own stream
Summary:
  Count:	3262371
  Total:	30.00 s
  Slowest:	30.28 ms
  Fastest:	0.07 ms
  Average:	0.82 ms
  Requests/sec:	108734.61

Response time histogram:
  0.074 [1]	|
  3.095 [978766]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  6.115 [15971]	|∎
  9.136 [3717]	|
  12.156 [1110]	|
  15.177 [245]	|
  18.197 [98]	|
  21.218 [62]	|
  24.238 [13]	|
  27.259 [8]	|
  30.280 [9]	|

Latency distribution:
  10 % in 0.31 ms
  25 % in 0.46 ms
  50 % in 0.65 ms
  75 % in 0.90 ms
  90 % in 1.30 ms
  95 % in 1.84 ms
  99 % in 4.69 ms

Status code distribution:
  [Canceled]      1 responses
  [Unavailable]   15 responses
  [OK]            3262355 responses

Error distribution:
  [1]    rpc error: code = Canceled desc = grpc: the client connection is closing
  [15]   rpc error: code = Unavailable desc = transport is closing
 */
fun main() = runBlocking {
  val server = httpServer(8551) {
    grpc {
      service(EchoServiceImpl())
    }
  }
  server.start()
}
