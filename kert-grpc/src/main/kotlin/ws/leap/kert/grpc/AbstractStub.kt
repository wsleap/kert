package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.grpc.Status
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpVersion
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.flow.*
import ws.leap.kert.http.HttpClient

// placeholder, nothing to configure right now
class CallOptions {

}

abstract class AbstractStub<S>(
  private val client: HttpClient,
  protected val callOptions: CallOptions = CallOptions(),
  private val interceptors: GrpcInterceptor? = null
) {
  init {
    require(client.protocolVersion == HttpVersion.HTTP_2) {
      "HTTP client for GRPC must be on HTTP2"
    }
  }
  protected fun <REQ, RESP> newCall(method: MethodDescriptor<REQ, RESP>,
                                    callOptions: CallOptions): GrpcClientCallHandler<REQ, RESP> {
    return { requestMessages ->
        val handler: GrpcHandler<REQ, RESP> = { m, r -> invokeHttp(m, r) }
        val grpcRequest = GrpcRequest(emptyMetadata(), requestMessages)
        // TODO bidi streaming stuck when specify GrpcContext, why?
        val grpcResponse = //withContext(GrpcContext(method)) {
          handle(method, grpcRequest, handler, interceptors)
        //}
        grpcResponse.messages
    }
  }

  private suspend fun <REQ, RESP>  invokeHttp(method: MethodDescriptor<REQ, RESP>, request: GrpcRequest<REQ>): GrpcResponse<RESP> {
    val responseDeserializer = GrpcUtils.responseDeserializer(method)

    val httpRequestBody = request.messages.map { msg ->
      val buf = GrpcUtils.serializeMessagePacket(msg)
      Buffer.buffer(buf)
    }

    val httpRequestPath = "/${method.fullMethodName}"
    val headers = HeadersMultiMap()
    headers.addAll(request.metadata)
    headers[HttpHeaders.CONTENT_TYPE] = Constants.contentTypeGrpcProto

    val httpResponse = client.post(httpRequestPath, headers = headers, body = httpRequestBody)
    if (httpResponse.statusCode != 200) {
      throw IllegalStateException("GRPC call failed, status=${httpResponse.statusCode}")
    }

    val responseMessages = GrpcUtils.readMessages(httpResponse.body, responseDeserializer)
    val responseMessagesFlow = responseMessages.onCompletion { cause ->
      if (cause == null) {
        val trailers = httpResponse.trailers()
        // fail the flow if trailer is missing or not OK
        val statusCode = trailers[Constants.grpcStatus]?.toInt()
          ?: throw IllegalStateException("GRPC status is missing, request=$httpRequestPath")
        val status = Status.fromCodeValue(statusCode)
        if (!status.isOk) {
          throw status.withDescription(trailers[Constants.grpcMessage] ?: "").asException()
        }
      }
    }

    return GrpcResponse(httpResponse.headers, responseMessagesFlow)
  }

  fun intercepted(vararg interceptors: GrpcInterceptor): S {
    if (interceptors.isEmpty()) return this as S

    val combinedInterceptor = combineInterceptors(*interceptors)!!
    return intercepted(combinedInterceptor)
  }

  fun intercepted(interceptor: GrpcInterceptor): S {
    // TODO inherit current interceptors or not??
    return build(client, callOptions, combineInterceptors(interceptors, interceptor))
  }

  /**
   * Create a new stub.
   */
  protected abstract fun build(client: HttpClient, callOptions: CallOptions = CallOptions(), interceptors: GrpcInterceptor? = null): S
}
