package ws.leap.kert.http

import io.vertx.core.*
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.streams.Pump
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

// 500 loops with 8K for each message
const val streamSize = 4000 * 1024

private val logger = KotlinLogging.logger {}

private fun createHttpServer(vertx: Vertx): HttpServer {
  val options = HttpServerOptions()
    .setSsl(false)
    .setUseAlpn(true)
  val server = vertx.createHttpServer(options)

  val router = Router.router(vertx)

  router.get("/ping").handler { ctx ->
    ctx.response().send("pong")
  }

  router.get("/server-stream").handler { ctx ->
    val resp = ctx.response()
    resp.isChunked = true
    val stream = MockReadStream(vertx, streamSize)
    stream.pipe().to(resp).onComplete { ar ->
      if(!ar.succeeded()) {
        resp.close()
      }
    }
  }

  router.post("/client-stream").handler { ctx ->
    val req = ctx.request()
    val resp = ctx.response()
    var receivedTotal = 0L

    req.exceptionHandler { e ->
      logger.error(e) { "client-stream error" }
      resp.statusCode = 500
      resp.end(e.message)
    }

    req.endHandler {
      logger.trace("received total $receivedTotal bytes")
      resp.end("$receivedTotal")
    }

    req.handler { buf ->
      logger.trace{ "received ${buf.length()} bytes" }
      receivedTotal += buf.length()
      // simulate a slow server
      Thread.sleep(1)
    }
  }

  router.post("/bidi-stream").handler { ctx ->
    val resp = ctx.response()
    resp.isChunked = true
    ctx.request().endHandler {
      resp.end()
    }
    val pump = Pump.pump(ctx.request(), resp)
    pump.start()
  }

  server.requestHandler(router)
  return server
}

fun createVertxServer(vertx: Vertx, port: Int): TestServer {
  val server = createHttpServer(vertx)
  return object: TestServer {
    override fun start() {
      server.listen(port)
    }

    override fun stop() {
      server.close()
    }
  }
}

fun createVertxVerticleServer(vertx: Vertx, port: Int): TestServer {
  class ServerVerticle: AbstractVerticle() {
    private lateinit var server: HttpServer
    override fun init(vertx: Vertx, context: Context) {
      server = createHttpServer(vertx)
    }

    override fun start() {
      server.listen(port)
    }

    override fun stop() {
      server.close()
    }
  }

  return object: TestServer {
    private var deployId: String? = null

    override fun start() {
      if(deployId != null) return
      runBlocking {
        deployId = vertx.deployVerticle( { ServerVerticle() }, DeploymentOptions().setInstances(VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE)).await()
      }
    }

    override fun stop() {
      if(deployId == null) return
      runBlocking {
        vertx.undeploy(deployId).await()
        deployId = null
      }
    }
  }
}

fun main() {
  val vertx = Vertx.vertx()
  val server = createVertxVerticleServer(vertx, 8000)
  server.start()
}
