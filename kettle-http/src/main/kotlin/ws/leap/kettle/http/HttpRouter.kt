package ws.leap.kettle.http

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import io.vertx.core.http.HttpServerResponse as VHttpServerResponse
import io.vertx.ext.web.RoutingContext as VRoutingContext

suspend fun VHttpServerResponse.write(body: Flow<Buffer>) {
  body.collect { data ->
    write(data).await()
  }
}

class HttpServerContext(internal val underlying: VRoutingContext, val request: HttpServerRequest) {
  fun response(statusCode: Int = 200) = HttpServerResponse(flowOf(), 0, statusCode)
  fun response(body: ByteArray, statusCode: Int = 200) = HttpServerResponse(flowOf(Buffer.buffer(body)), body.size.toLong(), statusCode)
  fun response(body: String, statusCode: Int = 200): HttpServerResponse {
    val bodyBytes = body.toByteArray()
    return response(bodyBytes, statusCode)
  }
  fun response(body: Buffer, statusCode: Int = 200) = HttpServerResponse(flowOf(body), body.length().toLong(), statusCode)
  fun response(body: Flow<Buffer>, contentLength: Long? = null, statusCode: Int? = null) = HttpServerResponse(body, contentLength, statusCode)

  val pathParams: MutableMap<String, String> = underlying.pathParams()
}

data class VertxRoutingContext(
  val routingContext: VRoutingContext
) : AbstractCoroutineContextElement(VertxRoutingContext) {
  companion object Key : CoroutineContext.Key<VertxRoutingContext>
  override fun toString(): String = "VertxRoutingContext($routingContext)"
}

val defaultExceptionHandler = CoroutineExceptionHandler { context, exception ->
  val routingContext = context[VertxRoutingContext]?.routingContext ?: throw IllegalStateException("Routing context is not available on coroutine context")
  val response = routingContext.response()
  if (!response.ended()) {
    if (!response.headWritten()) {
      response.statusCode = 500
      response.statusMessage = exception.toString()
      response.end()
    } else {
      // head already sent, reset the connection
      response.close()
    }
  }
}

open class HttpRouter(internal val underlying: Router, private val exceptionHandler: CoroutineExceptionHandler? = null) {
  protected fun exceptionHandler(): CoroutineExceptionHandler = exceptionHandler ?: defaultExceptionHandler

  protected open fun createContext(routingContext: VRoutingContext): CoroutineContext {
    val context = Vertx.currentContext()
    return context.dispatcher() + VertxRoutingContext(routingContext) + exceptionHandler()
  }

  fun call(configure: HttpRouter.() -> Unit) {
    configure(this)
  }

  fun route(method: HttpMethod, path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    underlying.route(method, path).handler { routingContext ->
      val context = Vertx.currentContext()
      val request = HttpServerRequest(routingContext.request())
      val httpServerContext = HttpServerContext(routingContext, request)

      // TODO add tracing, request logging, request metrics

      GlobalScope.launch(createContext(routingContext)) {
        val response = handler(httpServerContext)
        val vertxResponse = routingContext.response()

        // copy headers
        for(header in response.headers) {
          vertxResponse.putHeader(header.key, header.value)
        }
        vertxResponse.isChunked = response.chunked

        // write body
        vertxResponse.write(response.body)

        // write trailers
        for(trailer in response.trailers) {
          vertxResponse.putTrailer(trailer.key, trailer.value)
        }

        // end response
        vertxResponse.end().await()
      }
    }
  }

  fun get(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.GET, path, handler)
  }

  fun head(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.HEAD, path, handler)
  }

  fun post(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.POST, path, handler)
  }

  fun put(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.PUT, path, handler)
  }

  fun delete(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.DELETE, path, handler)
  }

  fun patch(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.PATCH, path, handler)
  }

  fun options(path: String, handler: suspend HttpServerContext.() -> HttpServerResponse) {
    route(HttpMethod.OPTIONS, path, handler)
  }
}
