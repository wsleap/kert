package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.vertx.core.MultiMap
import kotlinx.coroutines.flow.Flow
import ws.leap.kert.core.Filter
import ws.leap.kert.core.Handler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class GrpcContext(val method: MethodDescriptor<*, *>): AbstractCoroutineContextElement(GrpcContext) {
  companion object Key : CoroutineContext.Key<GrpcContext>
}

// todo the generic type here is a mess
data class GrpcStream<T>(val method: MethodDescriptor<*, *>, val metadata: MultiMap, val messages: Flow<*>)
typealias GrpcRequest<T> = GrpcStream<T>
typealias GrpcResponse<T> = GrpcStream<T>

// interface GrpcHandler<REQ, RESP> {
//   suspend operator fun invoke(req: REQ): RESP
// }

typealias GrpcHandler<REQ, RESP> = Handler<GrpcRequest<REQ>, GrpcResponse<RESP>>
typealias GrpcServerHandler<REQ, RESP> = GrpcHandler<REQ, RESP>
typealias GrpcClientHandler<REQ, RESP> = GrpcHandler<REQ, RESP>

// TODO interceptor is not type safe
typealias GrpcInterceptor = Filter<GrpcRequest<*>, GrpcResponse<*>>

typealias GrpcCallHandler<REQ, RESP> = Handler<Flow<REQ>, Flow<RESP>>
typealias GrpcServerCallHandler<REQ, RESP> = GrpcCallHandler<REQ, RESP>
typealias GrpcClientCallHandler<REQ, RESP> = GrpcCallHandler<REQ, RESP>
