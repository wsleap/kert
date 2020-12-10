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

class EchoServiceImpl : EchoGrpcKt.EchoImplBase() {
  private val logger = KotlinLogging.logger {}

  override suspend fun unary(req: EchoReq): EchoResp {
    return EchoResp.newBuilder()
      .setId(req.id)
      .setValue(req.value)
      .build()
  }

  override suspend fun serverStreaming(req: EchoCountReq): Flow<EchoResp> {
    return flow {
      for(i in 0 until req.count) {
        val msg = EchoResp.newBuilder()
          .setId(i)
          .setValue(EchoTest.message)
          .build()
        emit(msg)
        logger.info { "Server sent id=${msg.id}" }
        delay(1)
      }
    }
  }

  override suspend fun clientStreaming(req: Flow<EchoReq>): EchoCountResp {
    var count = 0
    req.collect { msg ->
      logger.info { "Server received id=${msg.id}" }
      count++
    }

    return EchoCountResp.newBuilder()
      .setCount(count)
      .build()
  }

  override suspend fun bidiStreaming(req: Flow<EchoReq>): Flow<EchoResp> {
    return req.map { msg ->
      logger.info { "Server received id=${msg.id}" }
      delay(1)
      val respMsg = EchoResp.newBuilder()
        .setId(msg.id)
        .setValue(msg.value)
        .build()
      logger.info { "Server sent id=${respMsg.id}" }
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

Summary:
  Count:	2957591
  Total:	30.00 s
  Slowest:	41.89 ms
  Fastest:	0.08 ms
  Average:	0.91 ms
  Requests/sec:	98578.21

Response time histogram:
  0.078 [1]	|
  4.259 [982402]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  8.440 [13805]	|∎
  12.622 [2524]	|
  16.803 [775]	|
  20.984 [421]	|
  25.165 [53]	|
  29.346 [10]	|
  33.528 [3]	|
  37.709 [3]	|
  41.890 [3]	|

Latency distribution:
  10 % in 0.33 ms
  25 % in 0.48 ms
  50 % in 0.69 ms
  75 % in 0.97 ms
  90 % in 1.46 ms
  95 % in 2.22 ms
  99 % in 5.64 ms

Status code distribution:
  [OK]            2957547 responses
  [Unavailable]   43 responses
  [Canceled]      1 responses
 */
fun main() = runBlocking {
  val server = httpServer(8551) {
    grpc {
      service(EchoServiceImpl())
    }
  }
  server.start()
}
