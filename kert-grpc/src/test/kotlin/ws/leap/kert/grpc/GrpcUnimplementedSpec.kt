package ws.leap.kert.grpc

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.DoNotParallelize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import ws.leap.kert.test.*

@DoNotParallelize
class GrpcUnimplementedSpec : GrpcSpec() {
  override fun configureServer(builder: GrpcServerBuilder) {
    val echoService = object: EchoGrpcKt.EchoImplBase() {
      override suspend fun serverStreaming(req: EchoCountReq): Flow<EchoResp> {
        throw StatusRuntimeException(Status.DATA_LOSS)
      }

      override suspend fun clientStreaming(req: Flow<EchoReq>): EchoCountResp {
        throw StatusRuntimeException(Status.INTERNAL)
      }

      override suspend fun bidiStreaming(req: Flow<EchoReq>): Flow<EchoResp> {
        throw StatusException(Status.NOT_FOUND)
      }
    }
    builder.service(echoService)
  }

  private val stub = EchoGrpcKt.stub(client)

  init {
    context("Grpc") {
      test("unary is not implemented") {
        val req = EchoReq.newBuilder().setId(1).setValue(EchoTest.message).build()
        val exception = shouldThrow<StatusException> {
          stub.unary(req)
        }
        exception.status.code shouldBe Status.Code.UNIMPLEMENTED
      }

      test("server stream should cause DATA_LOSS") {
        val req = EchoCountReq.newBuilder().setCount(EchoTest.streamSize).build()
        val exception = shouldThrow<StatusException> {
          stub.serverStreaming(req)
            .collect {}  // collect is required to raise exception from a failed flow
        }
        exception.status.code shouldBe Status.Code.DATA_LOSS
      }

      test("client stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(i.toString()).build()
            emit(msg)
          }
        }

        val exception = shouldThrow<StatusException> {
          stub.clientStreaming(req)
        }
        exception.status.code shouldBe Status.Code.INTERNAL
      }

      test("bidi stream") {
        val req = flow {
          for(i in 0 until EchoTest.streamSize) {
            val msg = EchoReq.newBuilder().setId(i).setValue(i.toString()).build()
            emit(msg)
          }
        }

        val exception = shouldThrow<StatusException> {
          stub.bidiStreaming(req)
            .collect {}  // collect is required to raise exception from a failed flow
        }
        exception.status.code shouldBe Status.Code.NOT_FOUND
      }
    }
  }
}
