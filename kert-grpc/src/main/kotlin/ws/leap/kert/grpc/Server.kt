package ws.leap.kert.grpc

import io.grpc.Status
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpVersion
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import ws.leap.kert.http.*

private val grpcExceptionLogger = KotlinLogging.logger {}
val defaultGrpcExceptionHandler = CoroutineExceptionHandler { context, exception ->
  val routingContext = context[VertxRoutingContext]?.routingContext
    ?: throw IllegalStateException("Routing context is not available on coroutine context")

  val method = routingContext.request().path().removePrefix("/")
  grpcExceptionLogger.warn("GRPC call failed: method=$method", exception)

  val response = routingContext.response()
  if (!response.ended()) {
    try {
      // grpc-status and grpc-message trailers
      val status = Status.fromThrowable(exception)
      val message = status.description

      // if headers haven't been sent, set grpc status in header
      if (!response.headWritten()) {
        response.putHeader(HttpHeaders.CONTENT_TYPE, Constants.contentTypeGrpcProto)
        response.putHeader(Constants.grpcStatus, status.code.value().toString())
        message?.let { response.putHeader(Constants.grpcMessage, it) }
      } else {
        // headers have been sent, put grpc status in trailers
        response.putTrailer(Constants.grpcStatus, status.code.value().toString())
        message?.let { response.putTrailer(Constants.grpcMessage, it) }
      }
    } finally {
      response.end()
    }
  }
}

fun HttpServerBuilderDsl.grpc(configure: GrpcServerBuilder.() -> Unit) {
  router(defaultGrpcExceptionHandler) {
    val builder = GrpcServerBuilder(this)
    configure(builder)
    builder.build()
  }
}

// https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
class GrpcServerBuilder(private val httpRouterBuilder: HttpRouterDsl) {
  var serverReflection: Boolean = false

  private val registry = ServiceRegistry()
  private val interceptors = mutableListOf<GrpcInterceptor>()

  fun service(service: ServerServiceDefinition): Unit = registry.addService(service)
  fun service(service: BindableService): Unit = registry.addService(service)

  fun interceptor(interceptor: GrpcInterceptor) {
    interceptors.add(interceptor)
  }

  fun build() {
    if(serverReflection) {
      service(ServerReflectionImpl(registry))
    }

    val finalInterceptor = combineInterceptors(*interceptors.toTypedArray())

    for(service in registry.services()) {
      httpRouterBuilder.call(HttpMethod.POST, "/${service.serviceDescriptor.name}/:method") { req ->
        // get method from url
        val methodName = req.pathParams["method"] ?: throw IllegalArgumentException("method is not provided")
        val method = registry.lookupMethod("${service.serviceDescriptor.name}/${methodName}")
        if (method != null) {
          // TODO the context of exceptionHandler doesn't have GrpcContext
          withContext(GrpcContext(method.methodDescriptor)) {
            handleRequest(req, method, finalInterceptor)
          }
        } else {
          notFound()
        }
      }
    }
  }

  private fun notFound(): HttpServerResponse {
    return response(
      contentType = Constants.contentTypeGrpcProto,
      trailers = { HeadersMultiMap().add(Constants.grpcStatus, Status.NOT_FOUND.code.value().toString()) }
    )
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
    return response(
      headers = grpcResponse.metadata,
      body = httpBody,
      contentType = Constants.contentTypeGrpcProto,
      trailers = { HeadersMultiMap().add(Constants.grpcStatus, Status.OK.code.value().toString()) }
    )
  }

  private fun verifyRequest(request: HttpServerRequest) {
    require(request.version == HttpVersion.HTTP_2) { "GRPC must be HTTP2, current is ${request.version}" }
  }
}
