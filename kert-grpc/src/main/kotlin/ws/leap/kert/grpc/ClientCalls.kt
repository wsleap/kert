package ws.leap.kert.grpc

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single

object ClientCalls {

  /**
   * Executes a unary call with a response.
   */
  suspend fun <REQ, RESP> unaryCall(
    call: GrpcClientCallHandler<REQ, RESP>,
    req: REQ): RESP {
    val responses = call(flowOf(req))
    return responses.single()
  }

  /**
   * Executes a server-streaming call with a response [Flow].
   */
  suspend fun <REQ, RESP> serverStreamingCall(
    call: GrpcClientCallHandler<REQ, RESP>,
    req: REQ): Flow<RESP> {
    return call(flowOf(req))
  }

  /**
   * Executes a client-streaming call by sending a [Flow] and returns a [Deferred]
   *
   * @return requestMore stream observer.
   */
  suspend fun <REQ, RESP> clientStreamingCall(
    call: GrpcClientCallHandler<REQ, RESP>,
    req: Flow<REQ>
  ): RESP {
    val responses = call(req)
    return responses.single()
  }

  /**
   * Executes a bidi-streaming call.
   *
   * @return requestMore stream observer.
   */
  suspend fun <REQ, RESP> bidiStreamingCall(
    call: GrpcClientCallHandler<REQ, RESP>,
    req: Flow<REQ>): Flow<RESP> {
    return call(req)
  }
}
