package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.lang.IllegalArgumentException

data class HttpServerResponse internal constructor(
  override val statusCode: Int = 200,
  override val headers: MultiMap = HeadersMultiMap(),
  override val body: Flow<Buffer> = emptyFlow(),
  override val trailers: () -> MultiMap = { HeadersMultiMap() }): HttpResponse {
//  init {
//    contentType?.let {
//      headers[HttpHeaders.CONTENT_TYPE] = it
//    }
//
//    if(contentLength != null) {
//      headers[HttpHeaders.CONTENT_LENGTH] = contentLength.toString()
//    } else {
//      chunked = true
//    }
//
//    statusCode?.let { this.statusCode = it }
//  }
}

fun response(statusCode: Int = 200, headers: MultiMap? = null) =
  HttpServerResponse(statusCode, headers = headers ?: HeadersMultiMap())

fun response(statusCode: Int = 200,
             headers: MultiMap? = null,
             body: Any? = null,
             contentType: String? = null,
             contentLength: Long? = null,
             trailers: (() -> MultiMap)? = null): HttpServerResponse {
  val theHeaders = constructHeaders(headers, contentLength, body)
  contentType?.let { theHeaders[HttpHeaders.CONTENT_TYPE] = it }
  val theBody = body?.let { toFlow(it) } ?: emptyFlow()
  val theTrailers = trailers ?: { HeadersMultiMap() }
  return HttpServerResponse(statusCode, theHeaders, theBody, theTrailers)
}

