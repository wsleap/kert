package ws.leap.kert.grpc

object Constants {
  // GRPC message header size (1 byte compression flag + 4 bytes size)
  const val messageHeaderSize = 5
  const val maxMessageSize = 4 * 1024 * 1024 // 4MB
  const val grpcStatus = "grpc-status"
  const val grpcMessage = "grpc-message"
  val contentTypeProto = "application/grpc+proto"
}
