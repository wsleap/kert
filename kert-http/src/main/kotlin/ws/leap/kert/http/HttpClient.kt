package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpVersion
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.net.URL
import io.vertx.core.http.HttpClientRequest as VHttpClientRequest

suspend fun VHttpClientRequest.write(body: Flow<Buffer>) {
  body.collect { data ->
    write(data).await()
  }
}

data class RequestOptions(
  val ssl: Boolean,
  val host: String,
  val port: Int
)

interface HttpClient {
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

  suspend fun call(request: HttpClientRequest): HttpClientResponse

  suspend fun close()

  fun withFilter(filter: HttpClientFilter): HttpClient
  fun withFilters(vararg filters: HttpClientFilter): HttpClient
  fun withOptions(options: RequestOptions): HttpClient
  val protocolVersion: HttpVersion
}

interface HttpClientBuilderDsl {
  fun options(configure: HttpClientOptions.() -> Unit)
  fun filter(filter: HttpClientFilter)
}

class HttpClientBuilder: HttpClientBuilderDsl {
  private val filters = mutableListOf<HttpClientFilter>()
  private val options = HttpClientOptions()

  override fun options(configure: HttpClientOptions.() -> Unit) {
    configure(options)
  }

  override fun filter(filter: HttpClientFilter) {
    filters.add(filter)
  }

  fun build(): HttpClient {
    val filter = combineFilters(*filters.toTypedArray())
    val vertxClient = Kert.vertx.createHttpClient(options) as io.vertx.core.http.impl.HttpClientImpl
    return HttpClientImpl(vertxClient, filter)
  }
}

fun httpClient(configure: (HttpClientBuilderDsl.() -> Unit)? = null): HttpClient {
  val builder = HttpClientBuilder()
  configure?.let { it(builder) }
  return builder.build()
}
