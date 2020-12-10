package ws.leap.kert.grpc

object Constants {
  // GRPC message header size (1 byte compression flag + 4 bytes size)
  const val messageHeaderSize = 5
  const val grpcStatus = "grpc-status"
  const val grpcMessage = "grpc-message"
  val contentTypeGrpcProto = "application/grpc"
  val contentTypeGrpcJson = "application/grpc+json"
  val contentTypeGrpcWeb = "application/grpc+web"
}
