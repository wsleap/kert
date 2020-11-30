package ws.leap.kert.http

import io.vertx.core.*
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineExceptionHandler
import ws.leap.kert.core.Filter

class HttpServerBuilder(private val router: Router) {
  fun http(exceptionHandler: CoroutineExceptionHandler? = null): HttpRouter {
    return HttpRouter(router, null, exceptionHandler)
  }

  fun http(exceptionHandler: CoroutineExceptionHandler? = null, configure: HttpRouter.() -> Unit): HttpRouter {
    val router = http(exceptionHandler)
    configure(router)
    return router
  }
}

internal class ServerVerticle(private val port: Int, private val router: Router) : AbstractVerticle() {
  private lateinit var server: HttpServer

  private fun createServer(vertx: Vertx): HttpServer {
    val options = HttpServerOptions()
      .setSsl(false)

    return vertx.createHttpServer(options)
  }

  override fun deploymentID(): String {
    return "kettle-http"
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

class HttpServer(private val port: Int, private val configureRouter: HttpServerBuilder.() -> Unit) {
  private var deployId: String? = null

  suspend fun start() {
    if(deployId != null) return

    val router = Router.router(Kert.vertx)
    val routerConfigurator = HttpServerBuilder(router)
    configureRouter(routerConfigurator)

    val desiredInstances = VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE
    val deploymentOptions = DeploymentOptions().setInstances(desiredInstances)
    deployId = Kert.vertx.deployVerticle({ ServerVerticle(port, router) }, deploymentOptions).await()
  }

  suspend fun stop() {
    deployId?.let {
      Kert.vertx.undeploy(it).await()
      deployId = null
    }
  }
}

fun server(port: Int, configureRouter: HttpServerBuilder.() -> Unit): ws.leap.kert.http.HttpServer {
  return HttpServer(port, configureRouter)
}
