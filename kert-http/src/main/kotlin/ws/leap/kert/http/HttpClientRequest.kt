package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.impl.headers.HeadersMultiMap
import kotlinx.coroutines.flow.Flow

class HttpClientRequest internal constructor(val method: HttpMethod, val path: String, var body: Flow<Buffer>, contentLength: Long? = null) {
  val headers: MultiMap = HeadersMultiMap()
  var chunked: Boolean = false

  init {
    if(contentLength != null) {
      headers[HttpHeaders.CONTENT_LENGTH] = contentLength.toString()
    } else {
      chunked = true
    }
  }
}
