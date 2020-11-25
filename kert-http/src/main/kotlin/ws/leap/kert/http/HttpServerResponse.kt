package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.flow.Flow

class HttpServerResponse internal constructor(val body: Flow<Buffer>, contentType: String? = null, contentLength: Long? = null, statusCode: Int? = null) {
  val headers: MultiMap = HeadersMultiMap()
  val trailers: MultiMap by lazy { HeadersMultiMap() }

  var chunked: Boolean = false
  var statusCode: Int = 200

  init {
    contentType?.let {
      headers[HttpHeaders.CONTENT_TYPE] = it
    }

    if(contentLength != null) {
      headers[HttpHeaders.CONTENT_LENGTH] = contentLength.toString()
    } else {
      chunked = true
    }

    statusCode?.let { this.statusCode = it }
  }
}
