package ws.leap.kert.grpc

import io.grpc.Status
import io.vertx.core.MultiMap
import ws.leap.kert.http.*
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

val grpcExceptionHandler = CoroutineExceptionHandler { context, exception ->
  val routingContext = context[VertxRoutingContext]?.routingContext ?: throw IllegalStateException("Routing context is not available on coroutine context")
  val response = routingContext.response()
  if (!response.ended()) {
    val status = Status.fromThrowable(exception)
    response.putTrailer(Constants.grpcStatus, status.code.value().toString())
    exception.message?.let {
      response.putTrailer(Constants.grpcMessage, it)
    }

    response.end()
  }
}

fun ServerBuilder.grpc(configure: GrpcServerBuilder.() -> Unit) {
  val router = http(grpcExceptionHandler)
  val builder = GrpcServerBuilder(router)
  configure(builder)
  builder.build()
}

// https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
class GrpcServerBuilder(private val router: HttpRouter) {
  private val registry = ServiceRegistry()
  private val interceptors = mutableListOf<GrpcInterceptor>()

  fun service(service: ServerServiceDefinition): Unit = registry.addService(service)
  fun service(service: BindableService): Unit = registry.addService(service)

  fun interceptor(interceptor: GrpcInterceptor) {
    interceptors.add(interceptor)
  }

  fun build() {
    val interceptorChain = GrpcUtils.buildInterceptorChain(interceptors)

    for(service in registry.services()) {
      router.call(HttpMethod.POST, "/${service.serviceDescriptor.name}/:method") { req ->
        // get method from url
        val methodName = req.pathParams["method"] ?: throw IllegalArgumentException("method is not provided")
        val method = registry.lookupMethod("${service.serviceDescriptor.name}/${methodName}")
        if (method != null) {
          handleRequest(req, method, interceptorChain)
        } else {
          notFound()
        }
      }
    }
  }

  private fun notFound(): HttpServerResponse {
    val resp = response(flowOf(), contentType = Constants.contentTypeGrpcProto)
    resp.trailers[Constants.grpcStatus] = Status.NOT_FOUND.code.value().toString()
    return resp
  }

  private suspend fun <REQ, RESP> handleRequest(request: HttpServerRequest, method: ServerMethodDefinition<REQ, RESP>,
                                                  interceptors: GrpcInterceptor?): HttpServerResponse {
    verifyHeaders(request)

    val requestDeserializer = GrpcUtils.requestDeserializer(method.methodDescriptor)
    val requestMessages = GrpcUtils.readMessages(request.body, requestDeserializer)
    val grpcRequest = GrpcRequest(request.headers, requestMessages)
    val grpcResponse = handleRequest(grpcRequest, method, interceptors)

    val httpBody = grpcResponse.messages.map { msg ->
      val buf = GrpcUtils.serializeMessagePacket(msg)
      Buffer.buffer(buf)
    }
    val response = response(body = httpBody, contentType = Constants.contentTypeGrpcProto)
    response.trailers[Constants.grpcStatus] = Status.OK.code.value().toString()
    response.headers.addAll(grpcResponse.metadata)
    return response
  }

  private suspend fun <REQ, RESP> handleRequest(request: GrpcRequest, method: ServerMethodDefinition<REQ, RESP>,
                                                  interceptors: GrpcInterceptor?): GrpcResponse {
    val handler: suspend (GrpcRequest) -> GrpcResponse = { req ->
      val responseMessages = method.callHandler.invoke(req.messages as Flow<REQ>)
      GrpcResponse(MultiMap.caseInsensitiveMultiMap(), responseMessages)
    }
    return interceptors?.let { it(request, handler) } ?: handler(request)
  }

  private fun verifyHeaders(request: HttpServerRequest) {
    require(request.headers[HttpHeaders.CONTENT_TYPE] == Constants.contentTypeGrpcProto) {
      "Content-Type must be ${Constants.contentTypeGrpcProto}"
    }
    // grpc headers
//        val serviceName = call.request.headers["Service-Name"]
//        val serviceName = call.request.headers["grpc-timeout"]
//        val serviceName = call.request.headers["content-type"]  // must be "application/grpc" [("+proto" / "+json" / {custom})]
//        val serviceName = call.request.headers["Content-Coding"]
//        val serviceName = call.request.headers["grpc-encoding"]
//        val serviceName = call.request.headers["grpc-accept-encoding"]
//        val serviceName = call.request.headers["user-agent"]
//        val serviceName = call.request.headers["grpc-message-type"]
//        val serviceName = call.request.headers["grpc-message-type"]
//        val serviceName = call.request.headers["grpc-message-type"]
//        val serviceName = call.request.headers["grpc-message-type"]
  }
}
