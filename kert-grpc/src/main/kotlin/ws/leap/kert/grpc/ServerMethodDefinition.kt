package ws.leap.kert.grpc

import io.grpc.MethodDescriptor

data class ServerMethodDefinition<REQ, RESP>(
  /** The `MethodDescriptor` for this method.  */
  val methodDescriptor: MethodDescriptor<REQ, RESP>,
  /** Handler for incoming calls.  */
  val handler: GrpcHandler<REQ, RESP>
) {
  fun intercepted(interceptor: GrpcInterceptor): ServerMethodDefinition<REQ, RESP> {
    return copy(handler = handler.intercepted(interceptor))
  }

  fun intercepted(vararg interceptors: GrpcInterceptor): ServerMethodDefinition<REQ, RESP> {
    if (interceptors.isEmpty()) return this

    val combinedInterceptor = combineInterceptors(*interceptors)!!
    return intercepted(combinedInterceptor)
  }
}
