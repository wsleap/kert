package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpVersion
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

typealias HttpClientFilter = suspend (req: HttpClientRequest, next: suspend (HttpClientRequest) -> HttpClientResponse) -> HttpClientResponse

class Client internal constructor(private val underlying: VHttpClient, private val filters: HttpClientFilter? = null) {
  companion object {
    fun create(baseAddress: URL): Client {
      val options = HttpClientOptions()
        .setDefaultHost(baseAddress.host)
        .setDefaultPort(baseAddress.port)
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setSsl(false)

      val vertxClient = Kert.vertx.createHttpClient(options)
      return Client(vertxClient)
    }
  }

  fun request(method: HttpMethod, path: String, body: Any? = null, contentLength: Long? = null, headers: MultiMap? = null): HttpClientRequest {
    val request = when(body) {
      null -> HttpClientRequest(method, path, flowOf(), contentLength)
      is Flow<*> -> HttpClientRequest(method, path, body.map { toBuffer(it!!) }, contentLength)
      is ByteArray, is Buffer, is String -> HttpClientRequest(method, path, flowOf(toBuffer(body)), contentLength)
      else -> throw IllegalArgumentException("Unsupported data type ${body.javaClass.name}")
    }

    headers?.let { request.headers.addAll(it) }

    return request
  }

  fun request(method: HttpMethod, path: String, configure: HttpClientRequest.() -> Unit): HttpClientRequest {
    val req = HttpClientRequest(method, path, flowOf())
    configure(req)
    return req
  }

  fun toBuffer(data: Any): Buffer {
    return when(data) {
      is ByteArray -> Buffer.buffer(data)
      is Buffer -> data
      is String -> Buffer.buffer(data)
      else -> throw IllegalArgumentException("Unsupported data type ${data.javaClass.name}")
    }
  }

  suspend fun get(path: String, headers: MultiMap? = null) = call(request(HttpMethod.GET, path, headers))
  suspend fun head(path: String, headers: MultiMap? = null) = call(request(HttpMethod.HEAD, path, headers))
  suspend fun put(path: String, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.PUT, path, body, contentLength, headers))
  suspend fun post(path: String, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.POST, path, body, contentLength, headers))
  suspend fun delete(path: String, headers: MultiMap? = null) = call(request(HttpMethod.DELETE, path, headers))
  suspend fun patch(path: String, body: Any, contentLength: Long? = null, headers: MultiMap? = null) = call(request(HttpMethod.PATCH, path, body, contentLength, headers))

  suspend fun post(path: String, configure: HttpClientRequest.() -> Unit) = call(request(HttpMethod.POST, path, configure))

  suspend fun call(request: HttpClientRequest): HttpClientResponse {
    return filters?.let { it(request, ::callHandler) } ?: callHandler(request)
  }

  private suspend fun callHandler(request: HttpClientRequest): HttpClientResponse {
    val responseDeferred = CompletableDeferred<HttpClientResponse>()
    val scope = CoroutineScope(coroutineContext)

    underlying.request(request.method, request.path) { ar ->
      if (ar.succeeded()) {
        val vertxRequest = ar.result()
        val vertxContext = Vertx.currentContext()

        for(header in request.headers) {
          vertxRequest.putHeader(header.key, header.value)
        }
        vertxRequest.isChunked = request.chunked

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

        vertxRequest.onComplete { ar ->
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
}

class ClientBuilder(private val address: URL) {
  private val filters = mutableListOf<HttpClientFilter>()

  var protocolVersion: HttpVersion = HttpVersion.HTTP_1_1

  fun filter(filter: HttpClientFilter) {
    filters.add(filter)
  }

  private fun constructFilterChain(): HttpClientFilter? {
    var current: HttpClientFilter? = null
    for(filter in filters) {
      current = current?.let {
        { req, next ->
          it(req) { filter(it, next) }
        }
      } ?: filter
    }

    return current
  }

  fun build(): Client {
    val options = HttpClientOptions()
      .setDefaultHost(address.host)
      .setDefaultPort(address.port)
      .setProtocolVersion(protocolVersion)
      .setSsl(address.protocol == "https")

    val filter = constructFilterChain()
    val vertxClient = Kert.vertx.createHttpClient(options)
    return Client(vertxClient, filter)
  }
}

fun client(address: URL, configure: ClientBuilder.() -> Unit): Client {
  val builder = ClientBuilder(address)
  configure(builder)
  return builder.build()
}
