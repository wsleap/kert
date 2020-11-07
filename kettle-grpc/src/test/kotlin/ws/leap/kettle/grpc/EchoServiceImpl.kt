package ws.leap.kettle.grpc

import ws.leap.kettle.test.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kettle.http.server

object EchoTest {
  val streamSize = 500
  val message = "hello".repeat(4000)
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

fun main() = runBlocking {
  val server = server(8888) {
    grpc {
      addService(EchoServiceImpl())
    }
  }
  server.start()
}
