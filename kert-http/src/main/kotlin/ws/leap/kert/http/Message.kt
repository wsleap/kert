package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.flow.*

interface HttpMessage {
  val headers: MultiMap
  val body: Flow<Buffer>

  fun chunked(): Boolean = headers[HttpHeaders.TRANSFER_ENCODING] == "chunked" || contentLength() == null
  fun contentLength(): Long? = headers[HttpHeaders.CONTENT_LENGTH]?.toLong()

  fun header(name: String): String? = headers[name]

  suspend fun body(): Buffer {
    val buf = Buffer.buffer()
    body.collect {
      buf.appendBuffer(it)
    }
    return buf
  }
}

interface HttpRequest : HttpMessage {
  val method: HttpMethod
  val uri: String
}

interface HttpResponse: HttpMessage {
  val statusCode: Int
  val trailers: () -> MultiMap
}


internal fun toFlow(body: Any?): Flow<Buffer> {
  return when(body) {
    null -> emptyFlow()
    is Flow<*> -> body.map { toBuffer(it!!) }
    is ByteArray, is Buffer, is String -> flowOf(toBuffer(body))
    else -> throw IllegalArgumentException("Unsupported data type ${body.javaClass.name}")
  }
}

internal fun toBuffer(data: Any): Buffer {
  return when(data) {
    is ByteArray -> Buffer.buffer(data)
    is Buffer -> data
    is String -> Buffer.buffer(data)
    else -> throw IllegalArgumentException("Unsupported data type ${data.javaClass.name}")
  }
}
