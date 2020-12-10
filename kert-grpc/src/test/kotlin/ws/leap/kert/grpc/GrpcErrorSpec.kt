package ws.leap.kert.grpc

import io.grpc.StatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kert.http.httpClient
import ws.leap.kert.http.httpServer
import ws.leap.kert.test.*

class GrpcErrorSpec : FunSpec() {
  val logger = KotlinLogging.logger {}
  private val server = httpServer(8551) {
    grpc {
      val echoService = object: EchoGrpcKt.EchoImplBase() {
        override suspend fun unary(req: EchoReq): EchoResp {
          throw RuntimeException("mocked error")
        }

        override suspend fun serverStreaming(req: EchoCountReq): Flow<EchoResp> {
          return flow {
            for(i in 0 until req.count / 2) {
              val msg = EchoResp.newBuilder().setId(i).setValue(EchoTest.message).build()
              emit(msg)
              logger.info { "Server sent id=${msg.id}" }
              delay(1)
            }

            // throw error in the middle
            throw RuntimeException("mocked error")
          }
        }

        override suspend fun clientStreaming(req: Flow<EchoReq>): EchoCountResp {
          var count = 0
          req.collect { msg ->
            logger.info { "Server received id=${msg.id}" }
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
            logger.info { "Server received id=${msg.id}" }
            delay(1)
            val respMsg = EchoResp.newBuilder()
              .setId(msg.id)
              .setValue(msg.value)
              .build()
            logger.info { "Server sent id=${respMsg.id}" }

            count++
            if (count > EchoTest.streamSize / 2) {
              // throw error in the middle
              throw RuntimeException("mocked error")
            }

            respMsg
          }
        }
      }
      service(echoService)
    }
  }

  private val client = httpClient {
    options {
      defaultPort = 8551
      protocolVersion = HttpVersion.HTTP_2
    }
  }
  private val stub = EchoGrpcKt.stub(client)

  override fun beforeSpec(spec: Spec) = runBlocking<Unit> {
    server.start()
  }

  override fun afterSpec(spec: Spec) = runBlocking {
    server.stop()
  }

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
            logger.info { "Client received id=${msg.id}" }
            msg
          }.toList()
        }
      }

      test("client stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(i.toString()).build()
            emit(msg)
            logger.info { "Client sent id=${msg.id}" }
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
            logger.info { "Client sent id=${msg.id}" }
            delay(1)
          }
        }

        val resp = stub.bidiStreaming(req)
        shouldThrow<StatusException> {
          resp.collect { msg ->
            logger.info { "Client received id=${msg.id}" }
          }
        }
      }
    }
  }
}
