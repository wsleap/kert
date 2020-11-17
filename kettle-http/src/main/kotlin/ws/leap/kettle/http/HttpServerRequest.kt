package ws.leap.kettle.http

import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerRequest
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.reduce

class HttpServerRequest(private val underlying: HttpServerRequest) {
  private val context = Vertx.currentContext() ?: throw IllegalStateException("Request must be created on vertx context")
  val headers: MultiMap = underlying.headers()
  fun header(name: String): String? = underlying.getHeader(name)
  val params: MultiMap = underlying.params()

  val body: Flow<Buffer> = underlying.toChannel(context).consumeAsFlow()
  suspend fun body(): Buffer = body.reduce { a, v -> a.appendBuffer(v) }
}
