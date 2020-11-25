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

private suspend fun VHttpServerResponse.write(body: Flow<Buffer>) {
  body.collect { data ->
    write(data).await()
  }
}

fun response(statusCode: Int = 200) =
  HttpServerResponse(flowOf(), null, 0, statusCode)
fun response(body: ByteArray, contentType: String? = null, statusCode: Int = 200) =
  HttpServerResponse(flowOf(Buffer.buffer(body)), contentType, body.size.toLong(), statusCode)
fun response(body: String, contentType: String? = null, statusCode: Int = 200): HttpServerResponse {
  val bodyBytes = body.toByteArray()
  return response(bodyBytes, contentType, statusCode)
}
fun response(body: Buffer, contentType: String? = null, statusCode: Int = 200) =
  HttpServerResponse(flowOf(body), contentType, body.length().toLong(), statusCode)
fun response(body: Flow<Buffer>, contentType: String? = null, contentLength: Long? = null, statusCode: Int? = null) =
  HttpServerResponse(body, contentType, contentLength, statusCode)

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

open class HttpRouter(internal val underlying: Router, private var filters: HttpServerFilter? = null, private val exceptionHandler: CoroutineExceptionHandler? = null) {
  protected fun exceptionHandler(): CoroutineExceptionHandler = exceptionHandler ?: defaultExceptionHandler

  protected open fun createContext(routingContext: VRoutingContext): CoroutineContext {
    val context = Vertx.currentContext()
    return context.dispatcher() + VertxRoutingContext(routingContext) + exceptionHandler()
  }

  fun filter(filter: HttpServerFilter) {
    filters = filters?.let { current ->
      { req, next ->
        current(req) { filter(it, next) }
      }
    } ?: filter
  }

  private suspend fun callHandler(request: HttpServerRequest, handler: suspend (HttpServerRequest) -> HttpServerResponse): HttpServerResponse {
    return filters?.let { it(request, handler) } ?: handler(request)
  }

  fun call(method: HttpMethod, path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    underlying.route(method, path).handler { routingContext ->
      val request = HttpServerRequest(routingContext.request(), routingContext)

      GlobalScope.launch(createContext(routingContext)) {
        val response = callHandler(request, handler)
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

  fun get(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.GET, path, handler)
  }

  fun head(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.HEAD, path, handler)
  }

  fun post(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.POST, path, handler)
  }

  fun put(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.PUT, path, handler)
  }

  fun delete(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.DELETE, path, handler)
  }

  fun patch(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.PATCH, path, handler)
  }

  fun options(path: String, handler: suspend (HttpServerRequest) -> HttpServerResponse) {
    call(HttpMethod.OPTIONS, path, handler)
  }

  fun router(path: String, exceptionHandler: CoroutineExceptionHandler? = null, configure: HttpRouter.() -> Unit): HttpRouter {
    val vertxRouter = Router.router(Kettle.vertx)
    val router = HttpRouter(vertxRouter, filters, exceptionHandler ?: this.exceptionHandler)
    configure(router)

    underlying.mountSubRouter(path, vertxRouter)
    return router
  }
}
