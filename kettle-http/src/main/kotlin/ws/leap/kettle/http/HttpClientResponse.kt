package ws.leap.kettle.http

import io.vertx.core.Context
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.reduce
import io.vertx.core.http.HttpClientResponse as VHttpClientResponse

class HttpClientResponse (private val underlying: VHttpClientResponse, private val context: Context) {
  val headers: MultiMap = underlying.headers()
  fun header(name: String): String? = underlying.getHeader(name)

  val trailers: MultiMap by lazy {
    underlying.trailers()
  }

  val body: Flow<Buffer> = underlying.toChannel(context).consumeAsFlow()
  val statusCode: Int = underlying.statusCode()

  suspend fun body(): Buffer = body.reduce { a, v -> a.appendBuffer(v) }
}
