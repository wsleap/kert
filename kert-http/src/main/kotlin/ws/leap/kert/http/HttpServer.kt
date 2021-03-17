package ws.leap.kert.http

import io.vertx.core.*
import io.vertx.core.http.HttpServer as VHttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineExceptionHandler

internal data class RouterDef(
  val exceptionHandler: CoroutineExceptionHandler?,
  val configure: HttpRouterBuilder.() -> Unit
)

interface HttpServerBuilderDsl {
  fun options(configure: HttpServerOptions.() -> Unit)
  fun filter(filter: HttpServerFilter)
  fun router(exceptionHandler: CoroutineExceptionHandler? = null, configure: HttpRouterDsl.() -> Unit)
}

class HttpServerBuilder(private val port: Int): HttpServerBuilderDsl {
  private val options = HttpServerOptions()
  private val filters = mutableListOf<HttpServerFilter>()
  private val routers = mutableListOf<RouterDef>()
  var exceptionHandler: CoroutineExceptionHandler = defaultHttpExceptionHandler

  override fun options(configure: HttpServerOptions.() -> Unit) {
    configure(options)
  }

  override fun filter(filter: HttpServerFilter) {
    filters.add(filter)
  }

  override fun router(exceptionHandler: CoroutineExceptionHandler?, configure: HttpRouterDsl.() -> Unit) {
    routers.add(RouterDef(exceptionHandler, configure))
  }

  fun build(): HttpServer {
    val filter = combineFilters(*filters.toTypedArray())

    val vertxRouter = Router.router(Kert.vertx)
    for(router in routers) {
      val builder = HttpRouterBuilder(vertxRouter, filter, router.exceptionHandler ?: exceptionHandler)
      router.configure(builder)
      builder.build()
    }

    return HttpServer(port, options, vertxRouter)
  }
}

internal class ServerVerticle(private val port: Int, private val options: HttpServerOptions, private val router: Router) : AbstractVerticle() {
  private lateinit var server: VHttpServer

  private fun createServer(vertx: Vertx): VHttpServer {
    return vertx.createHttpServer(options)
  }

  override fun deploymentID(): String {
    return "kert-http"
  }

  override fun init(vertx: Vertx, context: Context) {
    server = createServer(vertx)
    server.requestHandler(router)
  }

  override fun start(startPromise: Promise<Void>) {
    server.listen(port).onComplete { ar ->
      if(ar.succeeded()) startPromise.complete()
      else startPromise.fail(ar.cause())
    }
  }

  override fun stop(stopPromise: Promise<Void>) {
    server.close().onComplete { ar ->
      if(ar.succeeded()) stopPromise.complete()
      else stopPromise.fail(ar.cause())
    }
  }
}

class HttpServer(private val port: Int, private val options: HttpServerOptions, private val router: Router) {
  private var deployId: String? = null

  suspend fun start() {
    if(deployId != null) return

    val desiredInstances = VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE
    val deploymentOptions = DeploymentOptions().setInstances(desiredInstances)
    deployId = Kert.vertx.deployVerticle({ ServerVerticle(port, options, router) }, deploymentOptions).await()
  }

  suspend fun stop() {
    deployId?.let {
      Kert.vertx.undeploy(it).await()
      deployId = null
    }
  }
}

fun httpServer(port: Int, configure: HttpServerBuilderDsl.() -> Unit): HttpServer {
  val builder = HttpServerBuilder(port)
  configure(builder)
  return builder.build()
}
