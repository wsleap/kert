package ws.leap.kert.grpc

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kert.http.httpServer
import ws.leap.kert.http.httpClient
import ws.leap.kert.test.EchoCountReq
import ws.leap.kert.test.EchoGrpcKt
import ws.leap.kert.test.EchoReq

class GrpcBasicSpec : FunSpec() {
  val logger = KotlinLogging.logger {}
  private val server = httpServer(8551) {
    grpc {
      service(EchoServiceImpl())
    }
  }

  private val client = httpClient {
    options {
      protocolVersion = HttpVersion.HTTP_2
      defaultPort = 8551
    }
  }
  private val stub = EchoGrpcKt.stub(client)

  override fun beforeSpec(spec: Spec) = runBlocking<Unit> {
    server.start()

    // TODO client-stream and bidi-stream will fail (if run the case only)
    stub.unary(EchoReq.newBuilder().setId(1).setValue("hello").build())
  }

  override fun afterSpec(spec: Spec) = runBlocking {
    server.stop()
  }

  init {
    context("Grpc server/client") {
      test("unary") {
        val req = EchoReq.newBuilder().setId(1).setValue(EchoTest.message).build()
        val resp = stub.unary(req)
        resp.id shouldBe 1
        resp.value shouldBe EchoTest.message
      }

      test("server stream") {
        val req = EchoCountReq.newBuilder().setCount(EchoTest.streamSize).build()
        val resp = stub.serverStreaming(req)
        val respMsgs = resp.map { msg ->
          logger.info { "Client received id=${msg.id}" }
          msg
        }.toList()
        respMsgs shouldHaveSize EchoTest.streamSize
      }

      test("client stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(EchoTest.message).build()
            emit(msg)
            logger.info { "Client sent id=${msg.id}" }
            delay(1)
          }
        }

        val resp = stub.clientStreaming(req)
        resp.count shouldBe EchoTest.streamSize
      }

      test("bidi stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(EchoTest.message).build()
            emit(msg)
            logger.info { "Client sent id=${msg.id}" }
            delay(1)
          }
        }

        val resp = stub.bidiStreaming(req)
        var count = 0
        resp.collect { msg ->
          count++
          logger.info { "Client received id=${msg.id}" }
        }
        count shouldBe EchoTest.streamSize
      }
    }
  }
}
