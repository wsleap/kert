package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
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
  private val server = server(8551) {
    grpc {
      interceptor(object: GrpcInterceptor {
        override suspend fun <REQ, RESP> invoke(method: MethodDescriptor<REQ, RESP>,
                                                req: GrpcRequest<REQ>,
                                                next: GrpcHandler<REQ, RESP>): GrpcResponse<RESP> {
          // fail if no authentication header
          if (req.metadata["authentication"] == null) throw IllegalArgumentException("Authentication header is missing")

          // fail if message value is "not-good"
          val filteredReq = req.copy(messages = req.messages.map { msg ->
            if (msg is EchoReq && msg.value == "not-good") throw IllegalArgumentException("Mocked exception")
            msg
          })
          return next(method, filteredReq)
        }
      })

      service(EchoServiceImpl())
    }
  }
  override fun beforeSpec(spec: Spec) = runBlocking { server.start() }
  override fun afterSpec(spec: Spec) = runBlocking { server.stop() }

  private val client = EchoGrpcKt.stub(URL("http://localhost:8551"))
  private val clientWithAuth = client.intercepted(object: GrpcInterceptor {
    override suspend fun <REQ, RESP> invoke(method: MethodDescriptor<REQ, RESP>,
                                            req: GrpcRequest<REQ>,
                                            next: GrpcHandler<REQ, RESP>): GrpcResponse<RESP> {
      req.metadata["authentication"] = "mocked-authentication"
      return next(method, req)
    }
  })

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
