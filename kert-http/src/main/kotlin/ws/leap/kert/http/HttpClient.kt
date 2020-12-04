package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.*
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.coroutines.coroutineContext
import io.vertx.core.http.HttpClient as VHttpClient
import io.vertx.core.http.HttpClientRequest as VHttpClientRequest

suspend fun VHttpClientRequest.write(body: Flow<Buffer>) {
  body.collect { data ->
    write(data).await()
  }
}

class HttpClient internal constructor(private val underlying: VHttpClient, private val filters: HttpClientFilter? = null) {
  suspend fun get(uri: String, headers: MultiMap? = null) = call(request(HttpMethod.GET, uri, headers))
  suspend fun head(uri: String, headers: MultiMap? = null) = call(request(HttpMethod.HEAD, uri, headers))
  suspend fun put(uri: String, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.PUT, uri, headers, body, contentLength))
  suspend fun post(uri: String, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.POST, uri, headers, body, contentLength))
  suspend fun delete(uri: String, headers: MultiMap? = null) = call(request(HttpMethod.DELETE, uri, headers))
  suspend fun patch(uri: String, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.PATCH, uri, headers, body, contentLength))

  suspend fun get(url: URL, headers: MultiMap? = null) = call(request(HttpMethod.GET, url, headers))
  suspend fun head(url: URL, headers: MultiMap? = null) = call(request(HttpMethod.HEAD, url, headers))
  suspend fun put(url: URL, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.PUT, url, headers, body, contentLength))
  suspend fun post(url: URL, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.POST, url, headers, body, contentLength))
  suspend fun delete(url: URL, headers: MultiMap? = null) = call(request(HttpMethod.DELETE, url, headers))
  suspend fun patch(url: URL, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.PATCH, url, headers, body, contentLength))

  suspend fun call(request: HttpClientRequest): HttpClientResponse {
    return handle(request, ::callHttp, filters)
  }

  suspend fun close() {
    underlying.close().await()
  }

  fun filtered(filter: HttpClientFilter): HttpClient {
    // TODO inherit current filters or not??
    return HttpClient(underlying, combineFilters(filters, filter))
  }

  fun filtered(vararg filters: HttpClientFilter): HttpClient {
    if(filters.isEmpty()) return this

    val combinedFilter = combineFilters(*filters)!!
    return filtered(combinedFilter)
  }

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

  private fun requestOptions(request: HttpClientRequest): RequestOptions {
    val options = RequestOptions()
    options.run {
      method = request.method
      host = request.host
      port = request.port
      isSsl = request.ssl
      uri = request.uri
    }
    return options
  }
}

class HttpClientBuilder {
  private val filters = mutableListOf<HttpClientFilter>()
  private val options = HttpClientOptions()

  var defaultHost: String
    get() = options.defaultHost
    set(v: String) { options.defaultHost = v }

  var defaultPort: Int
    get() = options.defaultPort
    set(v: Int) { options.defaultPort = v }

  var protocolVersion: HttpVersion
    get() = options.protocolVersion
    set(v: HttpVersion) { options.protocolVersion = v }

  var ssl: Boolean
    get() = options.isSsl
    set(v: Boolean) { options.isSsl = v }

  fun filter(filter: HttpClientFilter) {
    filters.add(filter)
  }

  fun build(): HttpClient {
    val filter = combineFilters(*filters.toTypedArray())
    val vertxClient = Kert.vertx.createHttpClient(options)
    return HttpClient(vertxClient, filter)
  }
}

fun client(configure: (HttpClientBuilder.() -> Unit)? = null): HttpClient {
  val builder = HttpClientBuilder()
  configure?.let { it(builder) }
  return builder.build()
}
