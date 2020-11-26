package ws.leap.kert.grpc

import io.grpc.MethodDescriptor
import io.grpc.Status
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ws.leap.kert.http.Client

// placeholder, nothing to configure right now
class CallOptions {

}

abstract class AbstractStub<S>(
  private val client: Client,
  protected val callOptions: CallOptions = CallOptions(),
  private val interceptors: GrpcInterceptor? = null
) {
  protected fun <REQ, RESP> newCall(method: MethodDescriptor<REQ, RESP>,
                                      callOptions: CallOptions
  ): CallHandler<REQ, RESP> {
    return object : CallHandler<REQ, RESP> {
      override suspend fun invoke(request: Flow<REQ>): Flow<RESP> {
        val grpcRequest = GrpcRequest(emptyMetadata(), request)
        val grpcResponse = interceptors?.let { it(grpcRequest, ::invokeHttp) }
          ?: invokeHttp(grpcRequest)
        return grpcResponse.messages as Flow<RESP>
      }

      private suspend fun invokeHttp(request: GrpcRequest): GrpcResponse {
        val responseDeserializer = GrpcUtils.responseDeserializer(method)

        val httpRequestBody = request.messages.map { msg ->
          val buf = GrpcUtils.serializeMessagePacket(msg)
          Buffer.buffer(buf)
        }

        val httpResponse = client.post("/${method.fullMethodName}") {
          headers.addAll(request.metadata)
          headers[HttpHeaders.CONTENT_TYPE] = Constants.contentTypeGrpcProto
          body = httpRequestBody
        }
        val responseMessages = GrpcUtils.readMessages(httpResponse.body, responseDeserializer)
        val responseMessagesFlow = flow {
          responseMessages.collect { msg ->
            emit(msg)
          }

          // fail the flow if trailer is missing or not OK
          val statusCode = httpResponse.trailers[Constants.grpcStatus]?.toInt() ?: throw IllegalStateException("GRPC status is missing")
          val status = Status.fromCodeValue(statusCode)
          if (!status.isOk) {
            throw status.withDescription(httpResponse.trailers[Constants.grpcMessage] ?: "").asException()
          }
        }

        return GrpcResponse(httpResponse.headers, responseMessagesFlow)
      }
    }
  }

  fun withInterceptors(interceptors: List<GrpcInterceptor>): S {
    val interceptorChain = GrpcUtils.buildInterceptorChain(interceptors)
    return build(client, callOptions, interceptorChain)
  }

  /**
   * Create a new stub.
   */
  protected abstract fun build(client: Client, callOptions: CallOptions = CallOptions(), interceptors: GrpcInterceptor? = null): S
}
