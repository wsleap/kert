package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import ws.leap.kert.core.combine

class ServerMethodDefinition<REQ, RESP>(
  /** The `MethodDescriptor` for this method.  */
  val methodDescriptor: MethodDescriptor<REQ, RESP>,
  /** Handler for incoming calls.  */
  val handler: GrpcHandler<REQ, RESP>
) {
  fun intercepted(interceptor: GrpcInterceptor): ServerMethodDefinition<REQ, RESP> {
    val interceptedCallHandler: GrpcHandler<REQ, RESP> = { req ->
      interceptor(req, handler as GrpcHandler<*, *>) as GrpcResponse<RESP>
    }
    return ServerMethodDefinition(methodDescriptor, interceptedCallHandler)
  }

  fun intercepted(vararg interceptors: GrpcInterceptor): ServerMethodDefinition<REQ, RESP> {
    if (interceptors.isEmpty()) return this

    val combinedInterceptor = combine(*interceptors)!!
    return intercepted(combinedInterceptor)
  }
}
