package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.net.URL

data class HttpClientRequest internal constructor(
  override val method: HttpMethod,
  override val uri: String,
  override val headers: MultiMap = HeadersMultiMap(),
  override val body: Flow<Buffer> = emptyFlow(),
  internal val options: RequestOptions? = null,
): HttpRequest

fun request(method: HttpMethod, url: URL, headers: MultiMap? = null, body: Any? = null, contentLength: Long? = null): HttpClientRequest {
  val theHeaders = constructHeaders(headers, contentLength, body)

  val requestUri = "${url.file}${url.ref?.map { "#$it" } ?: ""}"
  val actualPort = if (url.port != -1) url.port else url.defaultPort
  val defaults = RequestOptions(url.protocol == "https", url.host, actualPort)

  return HttpClientRequest(method, requestUri, theHeaders, toFlow(body), defaults)
}

fun request(method: HttpMethod, uri: String, headers: MultiMap? = null, body: Any? = null, contentLength: Long? = null): HttpClientRequest {
  val theHeaders = constructHeaders(headers, contentLength, body)

  return HttpClientRequest(method, uri, headers = theHeaders, body = toFlow(body))
}

internal fun constructHeaders(headers: MultiMap?, contentLength: Long?, body: Any?): MultiMap {
  val theHeaders = headers ?: HeadersMultiMap()
  if (contentLength != null) {
    theHeaders[HttpHeaders.CONTENT_LENGTH] = contentLength.toString()
  } else {
    if (body == null) {
      theHeaders[HttpHeaders.CONTENT_LENGTH] = "0"
    }
  }
  return theHeaders
}
