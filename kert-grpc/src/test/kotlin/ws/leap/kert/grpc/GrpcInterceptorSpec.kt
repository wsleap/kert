package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.grpc.StatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.DoNotParallelize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.map
import ws.leap.kert.test.EchoGrpcKt
import ws.leap.kert.test.EchoReq

@DoNotParallelize
class GrpcInterceptorSpec : GrpcSpec() {
  override fun configureServer(builder: GrpcServerBuilder) = with(builder) {
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

  private val stub = EchoGrpcKt.stub(client)
  private val stubWithAuth = stub.intercepted(object: GrpcInterceptor {
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
        stub.unary(EchoReq.newBuilder().setId(1).setValue("good").build())
      }
    }
    test("should succeed if message is good") {
      val resp = stubWithAuth.unary(EchoReq.newBuilder().setId(1).setValue("good").build())
      resp.value shouldBe "good"
    }

    test("should fail if message is not-good") {
      shouldThrow<StatusException> {
        stubWithAuth.unary(EchoReq.newBuilder().setId(1).setValue("not-good").build())
      }
    }
  }
}
