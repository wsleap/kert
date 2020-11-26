package ws.leap.kert.grpc

import io.grpc.StatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ws.leap.kert.http.server
import ws.leap.kert.test.EchoGrpcKt
import ws.leap.kert.test.EchoReq
import java.lang.IllegalArgumentException
import java.net.URL

class GrpcInterceptorSpec : FunSpec() {
  val logger = KotlinLogging.logger {}
  private val server = server(8081) {
    grpc {
      interceptor { req, next ->
        // fail if no authentication header
        if (req.metadata["authentication"] == null) throw IllegalArgumentException("Authentication header is missing")

        // fail if message value is "not-good"
        val filteredReq = req.copy(req.metadata, req.messages.map { msg ->
          if (msg is EchoReq && msg.value == "not-good") throw IllegalArgumentException("Mocked exception")
          msg
        })
        next(filteredReq)
      }

      service(EchoServiceImpl())
    }
  }
  override fun beforeSpec(spec: Spec) = runBlocking<Unit> { server.start() }
  override fun afterSpec(spec: Spec) = runBlocking { server.stop() }

  private val client = EchoGrpcKt.stub(URL("http://localhost:8081"))
  private val clientWithAuth = client.withInterceptors(
    listOf { req, next ->
      req.metadata["authentication"] = "mocked-authentication"
      next(req)
    }
  )

  init {
    test("should fail if no authentication") {
      shouldThrow<StatusException> {
        client.unary(EchoReq.newBuilder().setId(1).setValue("good").build())
      }
    }
    test("should succeed if message is good") {
      val resp = clientWithAuth.unary(EchoReq.newBuilder().setId(1).setValue("good").build())
      resp.value shouldBe "good"
    }

    test("should fail if message is not-good") {
      shouldThrow<StatusException> {
        clientWithAuth.unary(EchoReq.newBuilder().setId(1).setValue("not-good").build())
      }
    }
  }
}
