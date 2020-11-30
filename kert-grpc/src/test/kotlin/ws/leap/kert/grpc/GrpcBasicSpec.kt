package ws.leap.kert.grpc

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kert.http.server
import ws.leap.kert.test.EchoCountReq
import ws.leap.kert.test.EchoGrpcKt
import ws.leap.kert.test.EchoReq
import java.net.URL

class GrpcBasicSpec : FunSpec() {
  val logger = KotlinLogging.logger {}
  private val server = server(8551) {
    grpc {
      service(EchoServiceImpl())
    }
  }

  private val client = EchoGrpcKt.stub(URL("http://localhost:8551"))

  override fun beforeSpec(spec: Spec) = runBlocking<Unit> {
    server.start()

    // TODO client-stream and bidi-stream will fail (if run the case only)
    client.unary(EchoReq.newBuilder().setId(1).setValue("hello").build())
  }

  override fun afterSpec(spec: Spec) = runBlocking {
    server.stop()
  }

  init {
    context("Grpc server/client") {
      test("unary") {
        val req = EchoReq.newBuilder().setId(1).setValue(EchoTest.message).build()
        val resp = client.unary(req)
        resp.id shouldBe 1
        resp.value shouldBe EchoTest.message
      }

      test("server stream") {
        val req = EchoCountReq.newBuilder().setCount(EchoTest.streamSize).build()
        val resp = client.serverStreaming(req)
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

        val resp = client.clientStreaming(req)
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

        val resp = client.bidiStreaming(req)
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
