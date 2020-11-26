package ws.leap.kert.grpc

import ws.leap.kert.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kert.http.server

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
Summary:
  Count:	1007645
  Total:	10.00 s
  Slowest:	39.20 ms
  Fastest:	0.09 ms
  Average:	0.89 ms
  Requests/sec:	100749.94

Response time histogram:
  0.086 [1]	|
  3.997 [981567]	|∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎∎
  7.908 [14266]	|∎
  11.819 [2758]	|
  15.731 [863]	|
  19.642 [346]	|
  23.553 [135]	|
  27.465 [47]	|
  31.376 [12]	|
  35.287 [4]	|
  39.199 [1]	|

Latency distribution:
  10 % in 0.33 ms
  25 % in 0.47 ms
  50 % in 0.66 ms
  75 % in 0.94 ms
  90 % in 1.42 ms
  95 % in 2.16 ms
  99 % in 5.46 ms
 */
fun main() = runBlocking {
  val server = server(8888) {
    grpc {
      service(EchoServiceImpl())
    }
  }
  server.start()
}
