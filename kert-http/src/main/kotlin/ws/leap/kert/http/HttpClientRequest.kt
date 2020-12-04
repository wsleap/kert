package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpVersion
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.net.URL

class HttpClientRequest internal constructor(val method: HttpMethod,
                                             val uri: String,
                                             val host: String? = null,
                                             val port: Int? = null,
                                             val ssl: Boolean? = null,
                                             val headers: MultiMap = HeadersMultiMap(),
                                             val body: Flow<Buffer> = flowOf()) {
  fun chunked(): Boolean = headers[HttpHeaders.TRANSFER_ENCODING] == "chunked"
  fun contentLength(): Long? = headers[HttpHeaders.CONTENT_LENGTH]?.toLong()
}

fun request(method: HttpMethod, url: URL, headers: MultiMap? = null, body: Any? = null, contentLength: Long? = null): HttpClientRequest {
  val theHeaders = headers ?: HeadersMultiMap()
  if (contentLength != null) {
    theHeaders[HttpHeaders.CONTENT_LENGTH] = contentLength.toString()
  } else {
    if (body != null) {
      theHeaders[HttpHeaders.TRANSFER_ENCODING] = "chunked"
    } else {
      theHeaders[HttpHeaders.CONTENT_LENGTH] = "0"
    }
  }

  val uri = "${url.file}${url.ref?.map { "#$it" } ?: ""}"
  val port = if (url.port != -1) url.port else url.defaultPort
  return HttpClientRequest(method, uri, url.host, port, url.protocol == "https", headers = theHeaders, body = toFlow(body))
}

fun request(method: HttpMethod, uri: String, headers: MultiMap? = null, body: Any? = null, contentLength: Long? = null): HttpClientRequest {
  val theHeaders = headers ?: HeadersMultiMap()
  if (contentLength != null) {
    theHeaders[HttpHeaders.CONTENT_LENGTH] = contentLength.toString()
  } else {
    if (body != null) {
      theHeaders[HttpHeaders.TRANSFER_ENCODING] = "chunked"
    } else {
      theHeaders[HttpHeaders.CONTENT_LENGTH] = "0"
    }
  }

  return HttpClientRequest(method, uri, headers = theHeaders, body = toFlow(body))
}

private fun toFlow(body: Any?): Flow<Buffer> {
  return when(body) {
    null -> emptyFlow()
    is Flow<*> -> body.map { toBuffer(it!!) }
    is ByteArray, is Buffer, is String -> flowOf(toBuffer(body))
    else -> throw IllegalArgumentException("Unsupported data type ${body.javaClass.name}")
  }
}

private fun toBuffer(data: Any): Buffer {
  return when(data) {
    is ByteArray -> Buffer.buffer(data)
    is Buffer -> data
    is String -> Buffer.buffer(data)
    else -> throw IllegalArgumentException("Unsupported data type ${data.javaClass.name}")
  }
}
