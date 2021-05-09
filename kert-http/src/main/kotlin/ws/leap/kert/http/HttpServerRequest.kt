package ws.leap.kert.http

import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpVersion
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.flow.Flow

class HttpServerRequest(private val underlying: HttpServerRequest, private val routingContext: RoutingContext): HttpRequest {
  private val context = Vertx.currentContext() ?: throw IllegalStateException("Request must be created on vertx context")

  override val method: HttpMethod = underlying.method()
  override val uri: String = underlying.uri()
  override val headers: MultiMap = underlying.headers()
  override val body: Flow<Buffer> = underlying.asFlow(context)

  val params: MultiMap = underlying.params()
  val path: String = underlying.path()
  val pathParams: MutableMap<String, String> = routingContext.pathParams()
  val version: HttpVersion = underlying.version()
}
