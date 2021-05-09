package ws.leap.kert.grpc

import io.grpc.StatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.DoNotParallelize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import ws.leap.kert.test.*

private val logger = KotlinLogging.logger {}

@DoNotParallelize
class GrpcErrorSpec : GrpcSpec() {
  override fun configureServer(builder: GrpcServerBuilder) {
    val echoService = object: EchoGrpcKt.EchoImplBase() {
      override suspend fun unary(req: EchoReq): EchoResp {
        throw RuntimeException("mocked error")
      }

      override suspend fun serverStreaming(req: EchoCountReq): Flow<EchoResp> {
        return flow {
          for(i in 0 until req.count / 2) {
            val msg = EchoResp.newBuilder().setId(i).setValue(EchoTest.message).build()
            emit(msg)
            logger.trace { "Server sent id=${msg.id}" }
            delay(1)
          }

          // throw error in the middle
          throw RuntimeException("mocked error")
        }
      }

      override suspend fun clientStreaming(req: Flow<EchoReq>): EchoCountResp {
        var count = 0
        req.collect { msg ->
          logger.trace { "Server received id=${msg.id}" }
          count++

          if (count > EchoTest.streamSize / 2) {
            // throw error in the middle
            throw RuntimeException("mocked error")
          }
        }

        return EchoCountResp.newBuilder().setCount(count).build()
      }

      override suspend fun bidiStreaming(req: Flow<EchoReq>): Flow<EchoResp> {
        var count = 0
        return req.map { msg ->
          logger.trace { "Server received id=${msg.id}" }
          delay(1)
          val respMsg = EchoResp.newBuilder()
            .setId(msg.id)
            .setValue(msg.value)
            .build()
          logger.trace { "Server sent id=${respMsg.id}" }

          count++
          if (count > EchoTest.streamSize / 2) {
            // throw error in the middle
            throw RuntimeException("mocked error")
          }

          respMsg
        }
      }
    }

    builder.service(echoService)
  }

  private val stub = EchoGrpcKt.stub(client)

  init {
    context("Grpc should capture the errors") {
      test("unary") {
        val req = EchoReq.newBuilder().setId(1).setValue(EchoTest.message).build()
        shouldThrow<StatusException> {
          stub.unary(req)
        }
      }

      test("server stream") {
        val req = EchoCountReq.newBuilder().setCount(EchoTest.streamSize).build()
        val resp = stub.serverStreaming(req)

        shouldThrow<StatusException> {
          resp.map { msg ->
            logger.trace { "Client received id=${msg.id}" }
            msg
          }.toList()
        }
      }

      test("client stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(i.toString()).build()
            emit(msg)
            logger.trace { "Client sent id=${msg.id}" }
            delay(1)
          }
        }

        shouldThrow<StatusException> {
          stub.clientStreaming(req)
        }
      }

      test("bidi stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(i.toString()).build()
            emit(msg)
            logger.trace { "Client sent id=${msg.id}" }
            delay(1)
          }
        }

        val resp = stub.bidiStreaming(req)
        shouldThrow<StatusException> {
          resp.collect { msg ->
            logger.trace { "Client received id=${msg.id}" }
          }
        }
      }
    }
  }
}
