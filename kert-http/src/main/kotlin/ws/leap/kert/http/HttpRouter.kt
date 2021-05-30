package ws.leap.kert.http

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import io.vertx.ext.web.RoutingContext as VRoutingContext

data class VertxRoutingContext(
  val routingContext: VRoutingContext
) : AbstractCoroutineContextElement(VertxRoutingContext) {
  companion object Key : CoroutineContext.Key<VertxRoutingContext>
  override fun toString(): String = "VertxRoutingContext($routingContext)"
}

private val httpExceptionLogger = KotlinLogging.logger {}
val defaultHttpExceptionHandler = CoroutineExceptionHandler { context, exception ->
  val routingContext = context[VertxRoutingContext]?.routingContext ?: throw IllegalStateException("Routing context is not available on coroutine context")
  httpExceptionLogger.warn(exception) { "HTTP call failed, path=${routingContext.request().path()}" }

  val response = routingContext.response()
  if (!response.ended()) {
    if (!response.headWritten()) {
      response.statusCode = 500
      response.statusMessage = exception.toString()
      response.end()
    } else {
      // head already sent, reset the connection
      response.reset()
      // response.close()
    }
  }
}

internal data class SubRouterDef(
  val path: String,
  val exceptionHandler: CoroutineExceptionHandler?,
  val configure: HttpRouterBuilder.() -> Unit
)

internal data class HandlerDef(
  val method: HttpMethod,
  val path: String,
  val handler: HttpServerHandler
)

open class HttpRouterBuilder(private val vertx: Vertx,
                             internal val underlying: Router,
                             private var parentFilter: HttpServerFilter?,
                             private val exceptionHandler: CoroutineExceptionHandler?): HttpRouterDsl {
  private val filters = mutableListOf<HttpServerFilter>()
  private val subRouters = mutableListOf<SubRouterDef>()
  private val handlers = mutableListOf<HandlerDef>()

  override fun filter(filter: HttpServerFilter) {
    filters.add(filter)
  }

  override fun subRouter(path: String, exceptionHandler: CoroutineExceptionHandler?, configure: HttpRouterBuilder.() -> Unit) {
    subRouters.add(SubRouterDef(path, exceptionHandler, configure))
  }

  override fun call(method: HttpMethod, path: String, handler: HttpServerHandler) {
    handlers.add(HandlerDef(method, path, handler))
  }

  fun build() {
    val combinedFilter = combineFilters(*filters.toTypedArray())
    val finalFilter = combineFilters(combinedFilter, parentFilter)

    // configure handlers
    for(handlerDef in handlers) {
      registerCall(handlerDef.method, handlerDef.path, handlerDef.handler, finalFilter)
    }

    // configure sub routers
    for(subRouter in subRouters) {
      val vertxRouter = Router.router(vertx)
      val builder = HttpRouterBuilder(vertx, vertxRouter, finalFilter, subRouter.exceptionHandler ?: exceptionHandler)
      subRouter.configure(builder)
      builder.build()

      underlying.mountSubRouter(subRouter.path, vertxRouter)
    }
  }

  private fun exceptionHandler(): CoroutineExceptionHandler = exceptionHandler ?: defaultHttpExceptionHandler

  private fun createContext(routingContext: VRoutingContext): CoroutineContext {
    val context = Vertx.currentContext()
    return context.dispatcher() + VertxRoutingContext(routingContext) + exceptionHandler()
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun registerCall(method: HttpMethod, path: String, handler: HttpServerHandler, filter: HttpServerFilter?) {
    underlying.route(method, path).handler { routingContext ->
      val request = HttpServerRequest(routingContext.request(), routingContext)
      val context = Vertx.currentContext()

      GlobalScope.launch(createContext(routingContext)) {
        val response = filter?.let { it(request, handler) } ?: handler(request)
        val vertxResponse = routingContext.response()

        // copy status code
        vertxResponse.statusCode = response.statusCode

        // copy headers
        vertxResponse.headers().addAll(response.headers)
        vertxResponse.isChunked = response.chunked()

        // write body
        write(context, response.body, vertxResponse)

        // write trailers
        vertxResponse.trailers().addAll(response.trailers())

        // end response
        vertxResponse.end().await()
      }
    }
  }
}


