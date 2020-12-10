package ws.leap.kert.http

import io.vertx.core.Context
import io.vertx.core.MultiMap
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.toChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import io.vertx.core.http.HttpClientResponse as VHttpClientResponse

class HttpClientResponse (private val underlying: VHttpClientResponse, private val context: Context) : HttpResponse {
  override val headers: MultiMap = underlying.headers()
  override val trailers: () -> MultiMap = {
    underlying.trailers()
  }

  override val body: Flow<Buffer> = underlying.toChannel(context).consumeAsFlow()
  override val statusCode: Int = underlying.statusCode()
}
