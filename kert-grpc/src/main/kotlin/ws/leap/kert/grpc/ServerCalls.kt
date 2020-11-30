package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.flow.*

/**
 * Utility functions for adapting [GrpcServerCallHandler]s to application service implementation,
 * meant to be used by the generated code.
 */
object ServerCalls {

  /**
   * Creates a `ServerCallHandler` for a unary call method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> unaryCall(method: suspend (REQ) -> RESP): GrpcServerHandler<REQ, RESP> {
    return { req ->
      val msg = req.messages.single() as REQ
      GrpcResponse(req.method, emptyMetadata(), flowOf(method(msg)))
    }
  }

  /**
   * Creates a `ServerCallHandler` for a server streaming method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> serverStreamingCall(method: suspend (REQ) -> Flow<RESP>): GrpcServerHandler<REQ, RESP> {
    return { req ->
      val msg = req.messages.single() as REQ
      GrpcResponse(req.method, emptyMetadata(), method(msg))
    }
  }

  /**
   * Creates a `ServerCallHandler` for a client streaming method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> clientStreamingCall(method: suspend(Flow<REQ>) -> RESP): GrpcServerHandler<REQ, RESP> {
    return { req ->
      val resp = method(req.messages as Flow<REQ>)
      GrpcResponse(req.method, emptyMetadata(), flowOf(resp))
    }
  }

  fun <REQ, RESP> bidiStreamingCall(method: suspend (Flow<REQ>) -> Flow<RESP>): GrpcServerHandler<REQ, RESP> {
    return { req ->
      val resp = method(req.messages as Flow<REQ>)
      GrpcResponse(req.method, emptyMetadata(), resp)
    }
  }

  fun <T> unimplementedUnaryCall(
    methodDescriptor: MethodDescriptor<*, *>): T {
    throw Status.UNIMPLEMENTED
      .withDescription("Method ${methodDescriptor.fullMethodName} is unimplemented")
      .asRuntimeException()
  }

  fun <T> unimplementedStreamingCall(methodDescriptor: MethodDescriptor<*, *>): Flow<T> {
    throw Status.UNIMPLEMENTED
      .withDescription("Method ${methodDescriptor.fullMethodName} is unimplemented")
      .asRuntimeException()
  }

  private fun getStatus(t: Throwable): Status {
    val status = Status.fromThrowable(t)
    return if (status.description == null) {
      status.withDescription(t.message)
    } else {
      status
    }
  }
}
