package ws.leap.kettle.grpc

import io.grpc.MethodDescriptor

class ServerMethodDefinition<ReqT, RespT>(
  /** The `MethodDescriptor` for this method.  */
  val methodDescriptor: MethodDescriptor<ReqT, RespT>,
  /** Handler for incoming calls.  */
  val callHandler: ServerCallHandler<ReqT, RespT>
)
