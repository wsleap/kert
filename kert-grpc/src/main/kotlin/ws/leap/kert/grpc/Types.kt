package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.vertx.core.MultiMap
import kotlinx.coroutines.flow.Flow
import ws.leap.kert.http.Handler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class GrpcContext(val method: MethodDescriptor<*, *>): AbstractCoroutineContextElement(GrpcContext) {
  companion object Key : CoroutineContext.Key<GrpcContext>
}

data class GrpcStream<T>(
  val metadata: MultiMap,
  val messages: Flow<T>,
)

typealias GrpcRequest<T> = GrpcStream<T>
typealias GrpcResponse<T> = GrpcStream<T>

typealias GrpcHandler<REQ, RESP> = suspend (method: MethodDescriptor<REQ, RESP>, req: GrpcRequest<REQ>) -> GrpcResponse<RESP>
//interface GrpcHandler<REQ, RESP> {
//  suspend operator fun invoke(method: MethodDescriptor<REQ, RESP>, req: GrpcRequest<REQ>): GrpcResponse<RESP>
//}
typealias GrpcServerHandler<REQ, RESP> = GrpcHandler<REQ, RESP>
typealias GrpcClientHandler<REQ, RESP> = GrpcHandler<REQ, RESP>

interface GrpcInterceptor {
  suspend operator fun <REQ, RESP> invoke(method: MethodDescriptor<REQ, RESP>,
                                          req: GrpcRequest<REQ>,
                                          next: GrpcHandler<REQ, RESP>): GrpcResponse<RESP>
}

typealias GrpcCallHandler<REQ, RESP> = Handler<Flow<REQ>, Flow<RESP>>
typealias GrpcServerCallHandler<REQ, RESP> = GrpcCallHandler<REQ, RESP>
typealias GrpcClientCallHandler<REQ, RESP> = GrpcCallHandler<REQ, RESP>

fun <REQ, RESP> intercept(handler: GrpcHandler<REQ, RESP>, interceptor: GrpcInterceptor): GrpcHandler<REQ, RESP> {
  return { method, req ->
    interceptor(method, req, handler)
  }
}

fun <REQ, RESP> GrpcHandler<REQ, RESP>.intercepted(interceptor: GrpcInterceptor): GrpcHandler<REQ, RESP> {
  return intercept(this, interceptor)
}

fun combineInterceptors(vararg interceptors: GrpcInterceptor): GrpcInterceptor? {
  if (interceptors.isEmpty()) return null

  return interceptors.reduce { left, right ->
    object: GrpcInterceptor {
      override suspend fun <REQ, RESP> invoke(method: MethodDescriptor<REQ, RESP>,
                                              req: GrpcRequest<REQ>,
                                              next: GrpcHandler<REQ, RESP>): GrpcResponse<RESP> {
        return right(method, req) { m, r -> left(m, r, next) }
      }
    }
  }
}

fun combineInterceptors(current: GrpcInterceptor?, interceptor: GrpcInterceptor): GrpcInterceptor? {
  return current?.let { cur ->
    object: GrpcInterceptor {
      override suspend fun <REQ, RESP> invoke(method: MethodDescriptor<REQ, RESP>,
                                              req: GrpcRequest<REQ>,
                                              next: GrpcHandler<REQ, RESP>): GrpcResponse<RESP> {
        return interceptor(method, req) { m, r -> cur(m, r, next) }
      }
    }
  } ?: interceptor
}

internal suspend fun <REQ, RESP> handle(method: MethodDescriptor<REQ, RESP>, req: GrpcRequest<REQ>, handler: GrpcHandler<REQ, RESP>, interceptor: GrpcInterceptor?): GrpcResponse<RESP> {
  return interceptor?.let { it(method, req, handler) }
    ?: handler(method, req)
}
