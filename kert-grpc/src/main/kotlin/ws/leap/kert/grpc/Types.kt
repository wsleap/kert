package ws.leap.kert.grpc

import io.vertx.core.MultiMap
import kotlinx.coroutines.flow.Flow

// todo will this better?
// data class GrpcStream<T>(val metadata: MultiMap, val messages: Flow<T>)
data class GrpcStream(val metadata: MultiMap, val messages: Flow<*>)
typealias GrpcRequest = GrpcStream
typealias GrpcResponse = GrpcStream

typealias GrpcInterceptor = suspend (req: GrpcRequest, next: suspend (GrpcRequest) -> GrpcResponse) -> GrpcResponse

interface CallHandler<REQ, RESP> {
  suspend fun invoke(request: Flow<REQ>): Flow<RESP>
}

typealias ServerCallHandler<REQ, RESP> = CallHandler<REQ, RESP>
typealias ClientCallHandler<REQ, RESP> = CallHandler<REQ, RESP>
