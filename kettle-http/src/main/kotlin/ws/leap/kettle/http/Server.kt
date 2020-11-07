package ws.leap.kettle.http

import io.vertx.core.Vertx
import io.vertx.core.http.Http2Settings
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineExceptionHandler

class RouterConfigurator(private val router: Router) {
  fun http(exceptionHandler: CoroutineExceptionHandler? = null): HttpRouter {
    return HttpRouter(router, exceptionHandler)
  }

  fun http(exceptionHandler: CoroutineExceptionHandler? = null, configure: HttpRouter.() -> Unit): HttpRouter {
    val router = http(exceptionHandler)
    configure(router)
    return router
  }
}

class Server(private val underlying: HttpServer, private val port: Int) {
  suspend fun start() {
    underlying.listen(port).await()
  }

  suspend fun stop() {
    underlying.close().await()
  }
}

fun server(port: Int, configureRouter: RouterConfigurator.() -> Unit): Server {
  val vertx = Kettle.vertx

  val options = HttpServerOptions()
    .setSsl(false)
    .setMaxChunkSize(16 * 1024)
    .setInitialSettings(Http2Settings().setMaxFrameSize(16 * 1024))

  val httpServer = vertx.createHttpServer(options)

  val router = Router.router(vertx)
  val routerConfigurator = RouterConfigurator(router)

  configureRouter(routerConfigurator)

  httpServer.requestHandler(router)

  return Server(httpServer, port)
}
