package ws.leap.kert.http

import io.vertx.core.Vertx
import io.vertx.core.http.HttpVersion
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal class HttpClientImpl (private val underlying: io.vertx.core.http.impl.HttpClientImpl,
                               private val filters: HttpClientFilter? = null,
                               private val options: RequestOptions? = null) : HttpClient {
  override suspend fun call(request: HttpClientRequest): HttpClientResponse {
    return handle(request, ::callHttp, filters)
  }

  override suspend fun close() {
    underlying.close().await()
  }

  override fun withFilter(filter: HttpClientFilter): HttpClient {
    return HttpClientImpl(underlying, filter, options)
  }

  override fun withFilters(vararg filters: HttpClientFilter): HttpClient {
    if(filters.isEmpty()) return this

    val combinedFilter = combineFilters(*filters)!!
    return withFilter(combinedFilter)
  }

  override fun withOptions(options: RequestOptions): HttpClient {
    return HttpClientImpl(underlying, filters, options)
  }

  override val protocolVersion: HttpVersion = underlying.options.protocolVersion

  private suspend fun callHttp(request: HttpClientRequest): HttpClientResponse {
    val responseDeferred = CompletableDeferred<HttpClientResponse>()
    val scope = CoroutineScope(coroutineContext)

    underlying.request(requestOptions(request)) { ar ->
      if (ar.succeeded()) {
        val vertxRequest = ar.result()
        val vertxContext = Vertx.currentContext()

        vertxRequest.headers().addAll(request.headers)
        vertxRequest.isChunked = request.chunked()

        // start send request body
        scope.launch(vertxContext.dispatcher()) {
          try {
            vertxRequest.write(request.body)
            vertxRequest.end().await()
          } catch (t: Throwable) {
            // send request body failed
            responseDeferred.completeExceptionally(t)
          }
        }

        vertxRequest.response { ar ->
          if(ar.succeeded()) {
            val vertxResponse = ar.result()
            responseDeferred.complete(HttpClientResponse(vertxResponse, vertxContext))
          }
        }
      } else {
        // start request falied
        responseDeferred.completeExceptionally(ar.cause())
      }
    }

    return responseDeferred.await()
  }

  private fun requestOptions(request: HttpClientRequest): io.vertx.core.http.RequestOptions {
    val defaults = request.options ?: options
    val options = io.vertx.core.http.RequestOptions()
    options.run {
      method = request.method
      host = defaults?.host
      port = defaults?.port
      isSsl = defaults?.ssl
      uri = request.uri
    }
    return options
  }
}
