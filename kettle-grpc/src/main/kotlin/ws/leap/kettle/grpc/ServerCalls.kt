package ws.leap.kettle.grpc

import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.flow.*

/**
 * Utility functions for adapting [ServerCallHandler]s to application service implementation,
 * meant to be used by the generated code.
 */
object ServerCalls {

  /**
   * Creates a `ServerCallHandler` for a unary call method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> unaryCall(method: suspend (REQ) -> RESP): ServerCallHandler<REQ, RESP> {
    return object: ServerCallHandler<REQ, RESP> {
      override suspend fun invoke(requests: Flow<REQ>): Flow<RESP> {
        val req = requests.single()
        return flowOf(method(req))
      }
    }
  }

  /**
   * Creates a `ServerCallHandler` for a server streaming method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> serverStreamingCall(method: suspend (REQ) -> Flow<RESP>): ServerCallHandler<REQ, RESP> {
    return object: ServerCallHandler<REQ, RESP> {
      override suspend fun invoke(requests: Flow<REQ>): Flow<RESP> {
        val req = requests.single()
        return method(req)
      }
    }
  }

  /**
   * Creates a `ServerCallHandler` for a client streaming method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> clientStreamingCall(method: suspend(Flow<REQ>) -> RESP): ServerCallHandler<REQ, RESP> {
    return object: ServerCallHandler<REQ, RESP> {
      override suspend fun invoke(requests: Flow<REQ>): Flow<RESP> {
        val resp = method(requests)
        return flowOf(resp)
      }
    }
  }

  fun <REQ, RESP> bidiStreamingCall(method: suspend (Flow<REQ>) -> Flow<RESP>): ServerCallHandler<REQ, RESP> {
    return object: ServerCallHandler<REQ, RESP> {
      override suspend fun invoke(requests: Flow<REQ>): Flow<RESP> {
        return method(requests)
      }
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
