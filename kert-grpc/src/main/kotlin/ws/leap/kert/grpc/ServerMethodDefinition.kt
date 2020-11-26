package ws.leap.kert.grpc

import io.grpc.MethodDescriptor

class ServerMethodDefinition<REQ, RESP>(
  /** The `MethodDescriptor` for this method.  */
  val methodDescriptor: MethodDescriptor<REQ, RESP>,
  /** Handler for incoming calls.  */
  val callHandler: CallHandler<REQ, RESP>
)
