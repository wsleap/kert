package ws.leap.kert.grpc

import io.grpc.Status
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpVersion
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import ws.leap.kert.http.*

private val grpcExceptionLogger = KotlinLogging.logger {}
val grpcExceptionHandler = CoroutineExceptionHandler { context, exception ->
  val method = context[GrpcContext]?.method?.fullMethodName ?: "unknown"
  grpcExceptionLogger.warn(exception) { "GRPC call failed, method=$method" }

  val routingContext = context[VertxRoutingContext]?.routingContext
    ?: throw IllegalStateException("Routing context is not available on coroutine context")
  val response = routingContext.response()
  if (!response.ended()) {
    try {
      val status = Status.fromThrowable(exception)
      response.putTrailer(Constants.grpcStatus, status.code.value().toString())
      response.putTrailer(Constants.grpcMessage, status.description!!.removePrefix("${status.code}: "))
    } finally {
      response.end()
    }
  }
}

fun HttpServerBuilder.grpc(configure: GrpcServerBuilder.() -> Unit) {
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
    val interceptorChain = combineInterceptors(*interceptors.toTypedArray())

    for(service in registry.services()) {
      router.call(HttpMethod.POST, "/${service.serviceDescriptor.name}/:method") { req ->
        // get method from url
        val methodName = req.pathParams["method"] ?: throw IllegalArgumentException("method is not provided")
        val method = registry.lookupMethod("${service.serviceDescriptor.name}/${methodName}")
        if (method != null) {
          // TODO the context of exceptionHandler doesn't have GrpcContext
          withContext(GrpcContext(method.methodDescriptor)) {
            handleRequest(req, method, interceptorChain)
          }
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
    verifyRequest(request)

    val requestDeserializer = GrpcUtils.requestDeserializer(method.methodDescriptor)
    val requestMessages = GrpcUtils.readMessages(request.body, requestDeserializer)
    val grpcRequest = GrpcRequest(request.headers, requestMessages)
    val grpcResponse = handle(method.methodDescriptor, grpcRequest, method.handler, interceptors)

    val httpBody: Flow<Buffer> = grpcResponse.messages.map { msg ->
      val buf = GrpcUtils.serializeMessagePacket(msg)
      Buffer.buffer(buf)
    }
    val response = response(body = httpBody, contentType = Constants.contentTypeGrpcProto)
    response.headers.addAll(grpcResponse.metadata)
    response.trailers[Constants.grpcStatus] = Status.OK.code.value().toString()
    return response
  }

  private fun verifyRequest(request: HttpServerRequest) {
    require(request.version == HttpVersion.HTTP_2) { "GRPC must be HTTP2, current is ${request.version}" }
  }
}
