package ws.leap.kert.grpc

import kotlinx.coroutines.flow.Flow

interface ClientCallHandler<ReqT, RespT> {
  suspend fun invoke(requests: Flow<ReqT>): Flow<RespT>
}

interface ServerCallHandler<ReqT, RespT> {
  suspend fun invoke(requests: Flow<ReqT>): Flow<RespT>
}
