package ws.leap.kettle.grpc

import io.grpc.Status
import ws.leap.kettle.http.*
import io.vertx.core.buffer.Buffer
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

fun RouterConfigurator.grpc(configureRegistry: ServiceRegistry.() -> Unit) {
  val router = http(grpcExceptionHandler)
  GrpcHandler(router, configureRegistry)
}

// https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
class GrpcHandler(router: HttpRouter, configureRegistry: ServiceRegistry.() -> Unit) {
  // register services
  private val registry = ServiceRegistry()

  init {
    configureRegistry(registry)

    for(service in registry.services()) {
      router.route(HttpMethod.POST, "/${service.serviceDescriptor.name}/:method") {
        // get method from url
        val methodName = pathParams["method"] ?: throw IllegalArgumentException("method is not provided")
        val method = registry.lookupMethod("${service.serviceDescriptor.name}/${methodName}")
        if (method != null) {
          val responseFlow = handleRequest(this, method)
          val resp = response(responseFlow, contentType = Constants.contentTypeProto)
          resp.trailers[Constants.grpcStatus] = Status.OK.code.value().toString()
          resp
        } else {
          val resp = response(flowOf(), contentType = Constants.contentTypeProto)
          resp.trailers[Constants.grpcStatus] = Status.NOT_FOUND.code.value().toString()
          resp
        }
      }
    }
  }

  private suspend fun <ReqT, RespT> handleRequest(context: HttpServerContext, method: ServerMethodDefinition<ReqT, RespT>): Flow<Buffer> {
    verifyHeaders(context.request)

    val requestDeserializer = GrpcUtils.requestDeserializer(method.methodDescriptor)
    val responseSerializer = GrpcUtils.responseSerializer(method.methodDescriptor)

    val requests = GrpcUtils.readMessages(context.request.body, requestDeserializer)
    val responses = method.callHandler.invoke(requests)
    return responses.map { msg ->
      val buf = GrpcUtils.serializeMessagePacket(msg)
      Buffer.buffer(buf)
    }
  }

  private fun verifyHeaders(request: HttpServerRequest) {
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
